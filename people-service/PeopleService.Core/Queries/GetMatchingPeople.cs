using FluentValidation;
using FluentValidation.Results;
using JetBrains.Annotations;
using MediatR;
using PeopleService.Core.DirectoryService;
using System.Linq;

namespace PeopleService.Core.Queries
{
    public static class GetMatchingPeople
    {
        public const int MaxSearchTextLength = 64;
        public const int MaxTake = 100;

        public class Request : IRequest<Response?>
        {
            /// <summary>
            /// Search text (3 to 64 characters)
            /// </summary>
            public string? SearchText { get; set; }

            /// <summary>
            /// Returns the specified number of element. (Default is 10, between 1 and 100)
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

        internal class RequestHandler : IRequestHandler<Request, Response?>
        {
            private readonly IDirectoryService _directoryService;

            public RequestHandler(IDirectoryService directoryService)
            {
                _directoryService = directoryService;
            }

            public async Task<Response?> Handle(Request request, CancellationToken cancellationToken)
            {
                var people = (await _directoryService.GetMatchingPerson(request.SearchText!, request.Take))?.ToList();

                return people == null || people.Count == 0 ? null : new Response { People = people };
            }
        }
    }
}
