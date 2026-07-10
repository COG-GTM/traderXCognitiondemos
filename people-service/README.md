# FINOS | TraderX Sample Trading App | People Service

![DEV Only Warning](http://badgen.net/badge/warning/not-for-production/red)
![Local Dev Machine Supported](http://badgen.net/badge/windows-dev/supported/green)

## Description

The People Service is used for managing users in the system and associating them
with accounts. It is a **.NET 10 / ASP.NET Core** REST service.

It exposes the following operations (external REST contract is stable and relied
upon by `account-service`, the web front end, and the `ingress`):

* `GET /People/GetPerson?LogonId={id}` or `?EmployeeId={id}` — returns a person
  by logon or employee id.
* `GET /People/GetMatchingPeople?SearchText={text}&Take={n}` — returns the people
  whose `LogonId` or `FullName` contain the search text (used by the front-end
  typeahead). Response shape: `{ "people": [ ... ] }`.
* `GET /People/ValidatePerson?LogonId={id}` or `?EmployeeId={id}` — returns `200`
  if the person can be associated to a valid person, `404` otherwise.

Default port is **18089** (bound via `ASPNETCORE_URLS` in the container).

## Architecture

The service is decomposed into two projects with clear layers:

```
people-service/
├── PeopleService.WebApi/          # API / presentation layer
│   ├── Program.cs                 #   minimal hosting (WebApplication builder)
│   ├── Controllers/               #   REST controllers (thin, delegate to MediatR)
│   └── MockDirectory/people.json  #   flat-file people data (local dev)
└── PeopleService.Core/            # application + domain + data access
    ├── Domain/                    #   domain models (Person) — the API/JSON shape
    ├── Queries/                   #   application logic (MediatR/CQRS handlers + validation)
    ├── Directory/                 #   data-access abstractions + implementations
    │   ├── IDirectoryService.cs   #     directory abstraction (LDAP-swappable)
    │   ├── JsonFileDirectoryService.cs  # flat-file / in-memory implementation
    │   ├── IPersonDataReader.cs   #     backing-store reader abstraction
    │   ├── JsonFilePersonReader.cs#     flat-file reader (maps persistence → domain)
    │   ├── PersonRecord.cs        #     persistence representation (decoupled from Person)
    │   └── DirectoryOptions.cs    #     bound from the "Directory" config section
    └── Infrastructure/            #   dependency-injection wiring
        └── ServiceCollectionExtensions.cs
```

Key points:

* **Layered separation** — controllers (API) → MediatR query handlers (application
  logic) → `IDirectoryService` (data access) → domain `Person` model.
* **Swappable data source** — the people/user data source is behind
  `IDirectoryService` and selected by configuration (`Directory:Provider`). The
  default `Json` provider reads the flat file for local dev; an LDAP-backed
  implementation of `IDirectoryService` can be registered in
  `ServiceCollectionExtensions` without touching controllers or handlers.
* **Decoupled models** — the persistence shape (`PersonRecord`) is mapped to the
  domain/API model (`Person`), so storage changes do not leak into the REST
  contract.
* **Swagger/OpenAPI** is enabled in all environments (parity with the Java
  services' springdoc), so the UI is available even in containerized deployments.

## Configuration

```jsonc
// appsettings.json
"Directory": {
  "Provider": "Json",                       // data source provider (Json)
  "PeopleJsonFilePath": "MockDirectory/people.json"
}
```

The legacy flat `PeopleJsonFilePath` key is still honored as a fallback.

## Building and Running (standalone)

Requires the [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0).

```bash
$ cd PeopleService.WebApi
$ dotnet run
```

The service listens on http://localhost:18089.

## Running via Docker

The `Dockerfile` is a multi-stage build (`dotnet/sdk:10.0` to publish,
`dotnet/aspnet:10.0` to run):

```bash
$ docker build -t people-service .
$ docker run -p 18089:18089 people-service
```

## Running via docker compose

From the repository root:

```bash
$ docker compose up
```

The full TraderX UI is then available at http://localhost:8080, and the People
Service is reachable at http://localhost:8080/people-service/ through the ingress.

## Accessing the Swagger URL

Visit `/swagger` to open the Swagger UI, e.g. http://localhost:18089/swagger.

Example request:

`/People/GetPerson?LogonId=user01`
