# FINOS | TraderX Sample Trading App | People Service

![DEV Only Warning](http://badgen.net/badge/warning/not-for-production/red)
![Local Dev Machine Supported](http://badgen.net/badge/windows-dev/supported/green)

## Description 
The people service is used for managing users in the system, and associating them with accounts.

 * Returns information about a person by logonId or employeeId
 * Returns the list of persons whose logonId or fullName containes the search text
 * Returns if the logonId or employeeId can be associated to a valid person

Default Port is 18089.

TODO: Get this port configurable by env var PEOPLE_SERVICE_PORT

## Building and Running
```bash
$ cd PeopleService.WebApi
$ dotnet run
```

## Authentication

Every People endpoint requires an authenticated caller; anonymous requests get `401`.
Settings live under `Authentication` (environment variables use the `Authentication__` prefix):

| Setting | Purpose |
| --- | --- |
| `Authentication__Jwt__Authority` | OIDC authority used to validate bearer tokens (signing keys are fetched from its JWKS). |
| `Authentication__Jwt__Audience` | Required `aud` claim. Bearer tokens are rejected when this is not set. |
| `Authentication__Jwt__Issuer` | Expected `iss` claim. Required with `SigningKey` (tokens are rejected without it); taken from the authority metadata otherwise. |
| `Authentication__Jwt__SigningKey` | Shared HS256 key (at least 32 bytes) instead of an authority. |
| `Authentication__ApiKeys` | Comma-separated API keys accepted in the `X-Api-Key` header (service-to-service callers such as account-service, which sends `PEOPLE_SERVICE_API_KEY`). |
| `Authentication__AllowAnonymous` | `true` turns off the authentication requirement. Use it only for local development. |

If none of these are set, every request is rejected.

In Kubernetes, put the values in an optional `people-service-auth` Secret (keys `api-key`, `jwt-authority`, `jwt-audience`). docker-compose reads `PEOPLE_SERVICE_API_KEY`, `PEOPLE_SERVICE_JWT_AUTHORITY`, `PEOPLE_SERVICE_JWT_AUDIENCE` and `PEOPLE_SERVICE_ALLOW_ANONYMOUS`.

`GetMatchingPeople` returns at most 50 people per request (`Take` must be between 1 and 50, default 10).

## Running the tests
```bash
$ dotnet test
```

## Accessing the Swagger URL

Visit the forwarded port `/swagger` to open the SwaggerUI.

Example URL:

`/People/GetPerson?LogonId=user01` (send `X-Api-Key: <key>` or `Authorization: Bearer <token>`)
