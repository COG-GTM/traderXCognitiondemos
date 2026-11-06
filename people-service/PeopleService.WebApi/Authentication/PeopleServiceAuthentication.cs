using System.Text;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.IdentityModel.Tokens;

namespace PeopleService.WebApi.Authentication
{
    /// <summary>
    /// Authentication for the people service. Every endpoint requires an authenticated caller,
    /// either a JWT bearer token (end users, via the front-end identity provider) or an API key
    /// in the X-Api-Key header (service-to-service callers such as account-service).
    /// When nothing is configured every request is rejected.
    ///
    /// Configuration (section "Authentication", env vars use "Authentication__" prefix):
    ///   Jwt:Authority    OIDC authority used to discover signing keys (JWKS)
    ///   Jwt:Audience     Expected "aud" claim
    ///   Jwt:Issuer       Expected "iss" claim (required with SigningKey; taken from Authority metadata otherwise)
    ///   Jwt:SigningKey   Shared HS256 key (at least 32 bytes) when no Authority is used
    ///   ApiKeys          Comma-separated list of accepted service API keys
    ///   AllowAnonymous   Local development only: disables the authentication requirement
    /// </summary>
    public static class PeopleServiceAuthentication
    {
        public const string SectionName = "Authentication";
        public const string SelectorScheme = "PeopleServiceAuth";
        private const int MinSigningKeyBytes = 32;

        public static IServiceCollection AddPeopleServiceAuthentication(this IServiceCollection services)
        {
            services
                .AddAuthentication(SelectorScheme)
                .AddPolicyScheme(SelectorScheme, SelectorScheme, options =>
                {
                    options.ForwardDefaultSelector = context =>
                        context.Request.Headers.ContainsKey(ApiKeyAuthenticationHandler.HeaderName)
                            ? ApiKeyAuthenticationHandler.SchemeName
                            : JwtBearerDefaults.AuthenticationScheme;
                })
                .AddJwtBearer()
                .AddScheme<AuthenticationSchemeOptions, ApiKeyAuthenticationHandler>(ApiKeyAuthenticationHandler.SchemeName, null);

            services.AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
                .Configure<IConfiguration>((options, configuration) => ConfigureJwt(options, configuration.GetSection(SectionName).GetSection("Jwt")));

            services.AddAuthorization();
            services.AddOptions<AuthorizationOptions>()
                .Configure<IConfiguration>((options, configuration) =>
                {
                    if (!IsAnonymousAllowed(configuration))
                    {
                        options.FallbackPolicy = new AuthorizationPolicyBuilder(SelectorScheme)
                            .RequireAuthenticatedUser()
                            .Build();
                    }
                });

            return services;
        }

        public static bool IsAnonymousAllowed(IConfiguration configuration) =>
            configuration.GetSection(SectionName).GetValue<bool>("AllowAnonymous");

        public static void CheckAuthenticationConfiguration(this WebApplication app)
        {
            var section = app.Configuration.GetSection(SectionName);
            var jwt = section.GetSection("Jwt");
            var jwtConfigured = !string.IsNullOrWhiteSpace(jwt["Authority"]) || !string.IsNullOrWhiteSpace(jwt["SigningKey"]);
            var apiKeysConfigured = ApiKeyAuthenticationHandler.GetConfiguredKeys(app.Configuration).Count > 0;

            var signingKey = jwt["SigningKey"];
            if (!string.IsNullOrWhiteSpace(signingKey) && Encoding.UTF8.GetByteCount(signingKey) < MinSigningKeyBytes)
            {
                throw new InvalidOperationException($"Authentication:Jwt:SigningKey must be at least {MinSigningKeyBytes} bytes.");
            }

            if (!string.IsNullOrWhiteSpace(signingKey) && string.IsNullOrWhiteSpace(jwt["Authority"]) && string.IsNullOrWhiteSpace(jwt["Issuer"]))
            {
                app.Logger.LogWarning("Authentication:Jwt:SigningKey is set without Authentication:Jwt:Issuer; all bearer tokens will be rejected.");
            }

            if (IsAnonymousAllowed(app.Configuration))
            {
                app.Logger.LogWarning("Authentication:AllowAnonymous is enabled; the people directory is readable without credentials. Use for local development only.");
            }
            else if (!jwtConfigured && !apiKeysConfigured)
            {
                app.Logger.LogWarning("No JWT or API key authentication is configured; all People endpoints will return 401.");
            }
        }

        private static void ConfigureJwt(JwtBearerOptions options, IConfigurationSection jwt)
        {
            var authority = jwt["Authority"];
            var audience = jwt["Audience"];
            var issuer = jwt["Issuer"];
            var signingKey = jwt["SigningKey"];

            options.MapInboundClaims = false;
            options.TokenValidationParameters = new TokenValidationParameters
            {
                // Issuer is always validated; with a shared SigningKey and no Jwt:Issuer every token is rejected.
                ValidateIssuer = true,
                ValidIssuer = string.IsNullOrWhiteSpace(issuer) ? null : issuer,
                // Audience is always validated; without Jwt:Audience every token is rejected.
                ValidateAudience = true,
                ValidAudience = string.IsNullOrWhiteSpace(audience) ? null : audience,
                ValidateLifetime = true,
                RequireExpirationTime = true,
                RequireSignedTokens = true,
                ValidateIssuerSigningKey = true,
                ClockSkew = TimeSpan.FromMinutes(1),
            };

            if (!string.IsNullOrWhiteSpace(authority))
            {
                options.Authority = authority;
            }
            else if (!string.IsNullOrWhiteSpace(signingKey))
            {
                var keyBytes = Encoding.UTF8.GetBytes(signingKey);
                options.TokenValidationParameters.IssuerSigningKey = new SymmetricSecurityKey(keyBytes);
                options.TokenValidationParameters.ValidAlgorithms = new[] { SecurityAlgorithms.HmacSha256 };
            }
            else
            {
                // No key source configured: reject every bearer token.
                options.TokenValidationParameters.IssuerSigningKeyResolver = (_, _, _, _) => Array.Empty<SecurityKey>();
            }
        }
    }
}
