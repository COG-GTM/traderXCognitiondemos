using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;
using PeopleService.Core.Queries;
using Xunit;

namespace PeopleService.WebApi.Tests
{
    public class PeopleEndpointsAuthTests
    {
        private const string ApiKey = "test-api-key-0123456789abcdef";
        private const string SigningKey = "test-signing-key-0123456789abcdef-0123456789";
        private const string Audience = "people-service";
        private const string Issuer = "https://idp.example.test";

        private static readonly string[] Endpoints =
        {
            "/People/GetPerson?LogonId=user01",
            "/People/GetMatchingPeople?SearchText=use&Take=100000",
            "/People/GetMatchingPeople?SearchText=use",
            "/People/ValidatePerson?LogonId=user01",
        };

        private static HttpClient CreateClient(IDictionary<string, string?> settings)
        {
            var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            {
                builder.UseEnvironment("Testing");
                builder.UseSetting("PeopleJsonFilePath", Path.Combine(AppContext.BaseDirectory, "MockDirectory", "people.json"));
                foreach (var (key, value) in settings)
                {
                    builder.UseSetting(key, value);
                }
            });
            return factory.CreateClient();
        }

        private static HttpClient CreateSecuredClient() => CreateClient(new Dictionary<string, string?>
        {
            ["Authentication:ApiKeys"] = "other-key," + ApiKey,
            ["Authentication:Jwt:SigningKey"] = SigningKey,
            ["Authentication:Jwt:Audience"] = Audience,
            ["Authentication:Jwt:Issuer"] = Issuer,
        });

        private static string CreateToken(string signingKey = SigningKey, string audience = Audience, DateTime? expires = null)
        {
            var now = DateTime.UtcNow;
            return new JsonWebTokenHandler().CreateToken(new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[] { new Claim("sub", "user01") }),
                Issuer = Issuer,
                Audience = audience,
                NotBefore = now.AddMinutes(-10),
                IssuedAt = now.AddMinutes(-10),
                Expires = expires ?? now.AddMinutes(5),
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(Encoding.UTF8.GetBytes(signingKey)), SecurityAlgorithms.HmacSha256),
            });
        }

        public static IEnumerable<object[]> EndpointData() => Endpoints.Select(e => new object[] { e });

        [Theory]
        [MemberData(nameof(EndpointData))]
        public async Task Anonymous_request_is_rejected(string url)
        {
            var response = await CreateSecuredClient().GetAsync(url);
            Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        }

        [Theory]
        [MemberData(nameof(EndpointData))]
        public async Task Anonymous_request_is_rejected_when_nothing_is_configured(string url)
        {
            var response = await CreateClient(new Dictionary<string, string?>()).GetAsync(url);
            Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        }

        [Fact]
        public async Task Wrong_api_key_is_rejected()
        {
            var client = CreateSecuredClient();
            client.DefaultRequestHeaders.Add("X-Api-Key", "not-the-key");
            var response = await client.GetAsync("/People/GetPerson?LogonId=user01");
            Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        }

        [Fact]
        public async Task Valid_api_key_can_get_person()
        {
            var client = CreateSecuredClient();
            client.DefaultRequestHeaders.Add("X-Api-Key", ApiKey);
            var response = await client.GetAsync("/People/GetPerson?LogonId=user01");
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
            Assert.Contains("user01", await response.Content.ReadAsStringAsync());
        }

        [Fact]
        public async Task Valid_bearer_token_can_search_people()
        {
            var client = CreateSecuredClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", CreateToken());
            var response = await client.GetFromJsonAsync<GetMatchingPeople.Response>("/People/GetMatchingPeople?SearchText=use&Take=5");
            Assert.NotNull(response?.People);
            Assert.Equal(5, response!.People!.Count);
        }

        [Theory]
        [InlineData("wrong-signing-key-0123456789abcdef-0123456789", Audience, false)]
        [InlineData(SigningKey, "some-other-api", false)]
        [InlineData(SigningKey, Audience, true)]
        public async Task Invalid_bearer_token_is_rejected(string signingKey, string audience, bool expired)
        {
            var client = CreateSecuredClient();
            var token = CreateToken(signingKey, audience, expired ? DateTime.UtcNow.AddMinutes(-5) : null);
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
            var response = await client.GetAsync("/People/GetPerson?LogonId=user01");
            Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        }

        [Theory]
        [InlineData(0)]
        [InlineData(51)]
        [InlineData(100000)]
        public async Task Take_outside_limit_is_rejected(int take)
        {
            var client = CreateSecuredClient();
            client.DefaultRequestHeaders.Add("X-Api-Key", ApiKey);
            var response = await client.GetAsync($"/People/GetMatchingPeople?SearchText=use&Take={take}");
            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        [Fact]
        public async Task Take_at_limit_is_accepted()
        {
            var client = CreateSecuredClient();
            client.DefaultRequestHeaders.Add("X-Api-Key", ApiKey);
            var response = await client.GetFromJsonAsync<GetMatchingPeople.Response>($"/People/GetMatchingPeople?SearchText=use&Take={GetMatchingPeople.Request.MaxTake}");
            Assert.NotNull(response?.People);
            Assert.True(response!.People!.Count <= GetMatchingPeople.Request.MaxTake);
        }

        [Fact]
        public async Task Allow_anonymous_opt_in_still_caps_take()
        {
            var client = CreateClient(new Dictionary<string, string?> { ["Authentication:AllowAnonymous"] = "true" });
            Assert.Equal(HttpStatusCode.OK, (await client.GetAsync("/People/GetPerson?LogonId=user01")).StatusCode);
            Assert.Equal(HttpStatusCode.BadRequest, (await client.GetAsync("/People/GetMatchingPeople?SearchText=use&Take=100000")).StatusCode);
        }
    }
}
