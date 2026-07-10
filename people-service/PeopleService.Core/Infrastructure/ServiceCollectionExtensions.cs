using System.Reflection;
using CacheManager.Core;
using FluentValidation;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using PeopleService.Core.Directory;
using PeopleService.Core.Queries;

namespace PeopleService.Core.Infrastructure
{
    public static class ServiceCollectionExtensions
    {
        public static IServiceCollection AddPeopleServiceCore(
            this IServiceCollection services,
            IConfiguration configuration)
        {
            services
                .AddMediatR(cfg => cfg.RegisterServicesFromAssemblies(Assembly.GetExecutingAssembly()))
                .AddValidatorsFromAssembly(Assembly.GetExecutingAssembly())
                .AddCacheManager<GetMatchingPeople.Response>(
                    c => c.WithDictionaryHandle()
                        .WithExpiration(ExpirationMode.Sliding, TimeSpan.FromMinutes(1)));

            services.AddDirectory(configuration);

            return services;
        }

        private static IServiceCollection AddDirectory(this IServiceCollection services, IConfiguration configuration)
        {
            var options = new DirectoryOptions();
            configuration.GetSection(DirectoryOptions.SectionName).Bind(options);
            // Backwards-compatible fallback to the legacy flat configuration key.
            options.PeopleJsonFilePath ??= configuration["PeopleJsonFilePath"];

            services.Configure<DirectoryOptions>(o =>
            {
                o.Provider = options.Provider;
                o.PeopleJsonFilePath = options.PeopleJsonFilePath;
            });

            switch (options.Provider?.Trim().ToLowerInvariant())
            {
                case null:
                case "":
                case "json":
                    services.AddSingleton<IPersonDataReader, JsonFilePersonReader>();
                    services.AddSingleton<IDirectoryService, JsonFileDirectoryService>();
                    break;
                default:
                    throw new NotSupportedException(
                        $"Directory provider '{options.Provider}' is not supported. " +
                        "Implement IDirectoryService (for example an LDAP-backed service) and register it here.");
            }

            return services;
        }
    }
}
