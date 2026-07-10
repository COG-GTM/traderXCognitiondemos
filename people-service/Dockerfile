# Multi-stage build for the TraderX People Service (.NET 10 / ASP.NET Core).
# Stage 1 restores and publishes a Release build; stage 2 runs it on the
# lightweight ASP.NET Core runtime image.

FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Restore first (leverages Docker layer caching for dependencies).
COPY PeopleService.sln ./
COPY PeopleService.Core/PeopleService.Core.csproj PeopleService.Core/
COPY PeopleService.WebApi/PeopleService.WebApi.csproj PeopleService.WebApi/
RUN dotnet restore

# Copy the remaining source and publish.
COPY . .
RUN dotnet publish PeopleService.WebApi/PeopleService.WebApi.csproj \
    -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish ./

# People service listens on 18089 (PEOPLE_SERVICE_PORT) over plain HTTP;
# TLS is terminated upstream by the ingress (nginx).
EXPOSE 18089
ENV ASPNETCORE_URLS=http://+:18089

ENTRYPOINT ["dotnet", "/app/PeopleService.WebApi.dll"]
