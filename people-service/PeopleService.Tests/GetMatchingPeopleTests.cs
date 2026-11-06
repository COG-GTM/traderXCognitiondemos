using System.Net;
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
            Assert.True(_validator.Validate(new GetMatchingPeople.Request { SearchText = searchText, Take = take }).IsValid);
        }

        [Fact]
        public void Accepts_search_text_at_max_length()
        {
            var request = new GetMatchingPeople.Request { SearchText = new string('a', GetMatchingPeople.MaxSearchTextLength) };

            Assert.True(_validator.Validate(request).IsValid);
        }

        [Theory]
        [InlineData(GetMatchingPeople.MaxSearchTextLength + 1)]
        [InlineData(8000)]
        public void Rejects_search_text_over_max_length(int length)
        {
            Assert.False(_validator.Validate(new GetMatchingPeople.Request { SearchText = new string('a', length) }).IsValid);
        }

        [Theory]
        [InlineData(0)]
        [InlineData(-1)]
        [InlineData(GetMatchingPeople.MaxTake + 1)]
        [InlineData(int.MaxValue)]
        public void Rejects_take_out_of_range(int take)
        {
            Assert.False(_validator.Validate(new GetMatchingPeople.Request { SearchText = "user01", Take = take }).IsValid);
        }
    }

    public class GetMatchingPeopleHandlerTests
    {
        private static readonly Person[] People =
        {
            new() { LogonId = "user01", FullName = "Alice Johnson" },
            new() { LogonId = "user02", FullName = "Bob Smith" }
        };

        [Fact]
        public async Task Empty_results_are_not_cached()
        {
            using var cache = new GetMatchingPeople.ResultCache();
            var handler = new GetMatchingPeople.RequestHandler(new StubDirectory(People), cache);

            for (var i = 0; i < 10_000; i++)
            {
                Assert.Null(await handler.Handle(new GetMatchingPeople.Request { SearchText = $"nobody-{i}" }, CancellationToken.None));
            }

            Assert.Equal(0, cache.Count);
        }

        [Fact]
        public async Task Cache_never_exceeds_size_limit()
        {
            using var cache = new GetMatchingPeople.ResultCache();
            var handler = new GetMatchingPeople.RequestHandler(new StubDirectory(People, matchEverything: true), cache);

            for (var i = 0; i < 10_000; i++)
            {
                await handler.Handle(new GetMatchingPeople.Request { SearchText = $"search-{i}" }, CancellationToken.None);
                Assert.True(cache.Count <= GetMatchingPeople.CacheSizeLimit);
            }
        }

        [Fact]
        public async Task Returns_matches_and_serves_repeats_from_cache()
        {
            using var cache = new GetMatchingPeople.ResultCache();
            var directory = new StubDirectory(People);
            var handler = new GetMatchingPeople.RequestHandler(directory, cache);
            var request = new GetMatchingPeople.Request { SearchText = "user", Take = 1 };

            var first = await handler.Handle(request, CancellationToken.None);
            var second = await handler.Handle(request, CancellationToken.None);

            Assert.Equal("user01", Assert.Single(first!.People!).LogonId);
            Assert.Same(first, second);
            Assert.Equal(1, directory.Calls);
        }

        private sealed class StubDirectory : IDirectoryService
        {
            private readonly Person[] _people;
            private readonly bool _matchEverything;

            public StubDirectory(Person[] people, bool matchEverything = false)
            {
                _people = people;
                _matchEverything = matchEverything;
            }

            public int Calls { get; private set; }

            public Task<IEnumerable<Person>?> GetMatchingPerson(string searchText, int take)
            {
                Calls++;
                var matches = _people.Where(p => _matchEverything || p.FullName.Contains(searchText) || p.LogonId.Contains(searchText));
                return Task.FromResult<IEnumerable<Person>?>(matches.Take(take));
            }

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
            var response = await _client.GetAsync($"/People/GetMatchingPeople?SearchText={new string('a', 4000)}");

            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        [Theory]
        [InlineData(0)]
        [InlineData(51)]
        [InlineData(int.MaxValue)]
        public async Task Out_of_range_take_returns_400(int take)
        {
            var response = await _client.GetAsync($"/People/GetMatchingPeople?SearchText=user&Take={take}");

            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        [Fact]
        public async Task Valid_search_still_returns_people()
        {
            var response = await _client.GetAsync("/People/GetMatchingPeople?SearchText=user01&Take=10");

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
