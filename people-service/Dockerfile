FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY PeopleService.sln ./
COPY PeopleService.Core/PeopleService.Core.csproj PeopleService.Core/
COPY PeopleService.WebApi/PeopleService.WebApi.csproj PeopleService.WebApi/
RUN dotnet restore PeopleService.sln

COPY . .
RUN dotnet publish PeopleService.WebApi/PeopleService.WebApi.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime
WORKDIR /people-service
COPY --from=build /app/publish ./

ENV ASPNETCORE_ENVIRONMENT=Development
ENV ASPNETCORE_URLS=http://0.0.0.0:18089

EXPOSE 18089

ENTRYPOINT ["dotnet", "PeopleService.WebApi.dll"]
