using System.Net;
using System.Reflection;
using FluentValidation;
using Microsoft.AspNetCore.Mvc.Testing;
using PeopleService.Core.DirectoryService;
using PeopleService.Core.Queries;
using Xunit;

namespace PeopleService.Tests
{
    public class GetMatchingPeopleValidatorTests
    {
        private readonly GetMatchingPeople.RequestValidator _validator = new();

        [Theory]
        [InlineData("use", 10)]
        [InlineData("user01", 1)]
        [InlineData("user01", GetMatchingPeople.MaxTake)]
        public void Accepts_valid_requests(string searchText, int take)
        {
            var result = _validator.Validate(new GetMatchingPeople.Request { SearchText = searchText, Take = take });

            Assert.True(result.IsValid);
        }

        [Fact]
        public void Accepts_search_text_at_max_length()
        {
            var result = _validator.Validate(new GetMatchingPeople.Request { SearchText = new string('a', GetMatchingPeople.MaxSearchTextLength) });

            Assert.True(result.IsValid);
        }

        [Theory]
        [InlineData(GetMatchingPeople.MaxSearchTextLength + 1)]
        [InlineData(1000)]
        [InlineData(8000)]
        public void Rejects_search_text_over_max_length(int length)
        {
            var result = _validator.Validate(new GetMatchingPeople.Request { SearchText = new string('a', length) });

            Assert.False(result.IsValid);
        }

        [Theory]
        [InlineData(0)]
        [InlineData(-1)]
        [InlineData(GetMatchingPeople.MaxTake + 1)]
        [InlineData(int.MaxValue)]
        public void Rejects_take_out_of_range(int take)
        {
            var result = _validator.Validate(new GetMatchingPeople.Request { SearchText = "user01", Take = take });

            Assert.False(result.IsValid);
        }
    }

    public class GetMatchingPeopleHandlerTests
    {
        [Fact]
        public void Handler_does_not_depend_on_a_cache()
        {
            var ctor = Assert.Single(typeof(GetMatchingPeople.RequestHandler).GetConstructors());

            Assert.Equal(new[] { typeof(IDirectoryService) }, ctor.GetParameters().Select(p => p.ParameterType));
            Assert.DoesNotContain(
                typeof(GetMatchingPeople.RequestHandler).GetFields(BindingFlags.Instance | BindingFlags.NonPublic),
                f => f.FieldType.Name.Contains("Cache"));
        }

        [Fact]
        public async Task Returns_matches_and_null_when_none()
        {
            var handler = new GetMatchingPeople.RequestHandler(new StubDirectory(
                new Person { LogonId = "user01", FullName = "Alice Johnson" },
                new Person { LogonId = "user02", FullName = "Bob Smith" }));

            var found = await handler.Handle(new GetMatchingPeople.Request { SearchText = "user", Take = 1 }, CancellationToken.None);
            var missing = await handler.Handle(new GetMatchingPeople.Request { SearchText = "nobody" }, CancellationToken.None);

            Assert.Equal("user01", Assert.Single(found!.People!).LogonId);
            Assert.Null(missing);
        }

        private sealed class StubDirectory : IDirectoryService
        {
            private readonly Person[] _people;

            public StubDirectory(params Person[] people) => _people = people;

            public Task<IEnumerable<Person>?> GetMatchingPerson(string searchText, int take) =>
                Task.FromResult<IEnumerable<Person>?>(_people.Where(p => p.FullName.Contains(searchText) || p.LogonId.Contains(searchText)).Take(take));

            public Task<Person?> GetPerson(string logonId, string employeeId) => throw new NotSupportedException();

            public Task<bool> ValidatePerson(string logonId, string employeeId) => throw new NotSupportedException();
        }
    }

    public class GetMatchingPeopleEndpointTests : IClassFixture<WebApplicationFactory<Program>>
    {
        private readonly HttpClient _client;

        public GetMatchingPeopleEndpointTests(WebApplicationFactory<Program> factory)
        {
            _client = factory.CreateClient();
        }

        [Fact]
        public async Task Long_search_text_returns_400()
        {
            var response = await _client.GetAsync($"/People/GetMatchingPeople?SearchText={new string('a', 1000)}");

            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        [Theory]
        [InlineData(0)]
        [InlineData(101)]
        public async Task Out_of_range_take_returns_400(int take)
        {
            var response = await _client.GetAsync($"/People/GetMatchingPeople?SearchText=user&Take={take}");

            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        [Fact]
        public async Task Valid_search_still_returns_people()
        {
            var response = await _client.GetAsync("/People/GetMatchingPeople?SearchText=user01");

            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
            Assert.Contains("user01", await response.Content.ReadAsStringAsync());
        }

        [Fact]
        public async Task Unknown_search_still_returns_404()
        {
            var response = await _client.GetAsync("/People/GetMatchingPeople?SearchText=zzzz-nobody");

            Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        }
    }
}
