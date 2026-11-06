using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace PeopleService.WebApi.Authentication
{
    /// <summary>
    /// Authenticates service-to-service callers that send one of the configured
    /// Authentication:ApiKeys values in the X-Api-Key header.
    /// </summary>
    public class ApiKeyAuthenticationHandler : AuthenticationHandler<AuthenticationSchemeOptions>
    {
        public const string SchemeName = "ApiKey";
        public const string HeaderName = "X-Api-Key";

        private readonly IConfiguration _configuration;

        public ApiKeyAuthenticationHandler(
            IOptionsMonitor<AuthenticationSchemeOptions> options,
            ILoggerFactory logger,
            UrlEncoder encoder,
            IConfiguration configuration)
            : base(options, logger, encoder)
        {
            _configuration = configuration;
        }

        public static IReadOnlyList<string> GetConfiguredKeys(IConfiguration configuration) =>
            (configuration.GetSection(PeopleServiceAuthentication.SectionName)["ApiKeys"] ?? string.Empty)
                .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

        protected override Task<AuthenticateResult> HandleAuthenticateAsync()
        {
            if (!Request.Headers.TryGetValue(HeaderName, out var values))
            {
                return Task.FromResult(AuthenticateResult.NoResult());
            }

            var presented = values.ToString();
            if (string.IsNullOrEmpty(presented) || !IsValidKey(presented))
            {
                return Task.FromResult(AuthenticateResult.Fail("Invalid API key."));
            }

            var identity = new ClaimsIdentity(new[] { new Claim(ClaimTypes.Name, "service") }, SchemeName);
            var ticket = new AuthenticationTicket(new ClaimsPrincipal(identity), SchemeName);
            return Task.FromResult(AuthenticateResult.Success(ticket));
        }

        private bool IsValidKey(string presented)
        {
            var presentedHash = SHA256.HashData(Encoding.UTF8.GetBytes(presented));
            var match = false;
            foreach (var key in GetConfiguredKeys(_configuration))
            {
                var keyHash = SHA256.HashData(Encoding.UTF8.GetBytes(key));
                match |= CryptographicOperations.FixedTimeEquals(presentedHash, keyHash);
            }

            return match;
        }
    }
}
