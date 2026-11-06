using FluentValidation;
using FluentValidation.Results;
using JetBrains.Annotations;
using MediatR;
using Microsoft.Extensions.Caching.Memory;
using PeopleService.Core.DirectoryService;
using System.Linq;

namespace PeopleService.Core.Queries
{
    public static class GetMatchingPeople
    {
        public const int MaxSearchTextLength = 64;
        public const int MaxTake = 50;
        public const int CacheSizeLimit = 1000;
        private static readonly TimeSpan CacheSlidingExpiration = TimeSpan.FromMinutes(1);

        public class Request : IRequest<Response?>
        {
            /// <summary>
            /// Search text (3 to 64 characters)
            /// </summary>
            public string? SearchText { get; set; }

            /// <summary>
            /// Returns the specified number of element. (Default is 10, between 1 and 50)
            /// </summary>
            public int Take { get; set; } = 10;
        }

        public class Response
        {
            public List<Person>? People { get; set; }
        }

        [UsedImplicitly]
        public class RequestValidator : AbstractValidator<Request>
        {
            public override ValidationResult Validate(ValidationContext<Request> context)
            {
                if (string.IsNullOrWhiteSpace(context.InstanceToValidate.SearchText))
                {
                    return new ValidationResult(
                        new[]
                        {
                            new ValidationFailure(
                                "",
                                $"{nameof(Request.SearchText)} must be provided")
                        });
                }

                if (context.InstanceToValidate.SearchText.Length < 3)
                {
                    return new ValidationResult(
                        new[]
                        {
                            new ValidationFailure(
                                "",
                                $"{nameof(Request.SearchText)} must be at least 3 characters long")
                        });
                }

                if (context.InstanceToValidate.SearchText.Length > MaxSearchTextLength)
                {
                    return new ValidationResult(
                        new[]
                        {
                            new ValidationFailure(
                                "",
                                $"{nameof(Request.SearchText)} must be at most {MaxSearchTextLength} characters long")
                        });
                }

                if (context.InstanceToValidate.Take < 1 || context.InstanceToValidate.Take > MaxTake)
                {
                    return new ValidationResult(
                        new[]
                        {
                            new ValidationFailure(
                                "",
                                $"{nameof(Request.Take)} must be between 1 and {MaxTake}")
                        });
                }

                return new ValidationResult();
            }
        }

        /// <summary>
        /// Size-limited cache of non-empty search results, kept separate from any shared IMemoryCache.
        /// </summary>
        public sealed class ResultCache : IDisposable
        {
            internal MemoryCache Cache { get; } = new(new MemoryCacheOptions { SizeLimit = CacheSizeLimit });

            public int Count => Cache.Count;

            public void Dispose() => Cache.Dispose();
        }

        internal class RequestHandler : IRequestHandler<Request, Response?>
        {
            private readonly IDirectoryService _directoryService;
            private readonly MemoryCache _cache;

            public RequestHandler(IDirectoryService directoryService, ResultCache resultCache)
            {
                _directoryService = directoryService;
                _cache = resultCache.Cache;
            }

            public async Task<Response?> Handle(Request request, CancellationToken cancellationToken)
            {
                var cacheKey = GetCacheKey(request);
                if (_cache.TryGetValue(cacheKey, out Response? cached))
                {
                    return cached;
                }

                var people = (await _directoryService.GetMatchingPerson(request.SearchText!, request.Take))?.ToList();
                if (people == null || people.Count == 0)
                {
                    return null;
                }

                var response = new Response
                {
                    People = people
                };

                _cache.Set(cacheKey, response, new MemoryCacheEntryOptions
                {
                    Size = 1,
                    SlidingExpiration = CacheSlidingExpiration
                });

                return response;
            }

            private static string GetCacheKey(Request request) => (request.SearchText ?? "") + "," + request.Take;
        }
    }
}
