using PeopleService.Core.Domain;

namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Abstraction over the people/user directory. The application layer depends
    /// only on this interface, so the backing data source (flat-file for local
    /// dev, LDAP user directory in other environments) can be swapped via DI
    /// without affecting controllers or query handlers.
    /// </summary>
    public interface IDirectoryService
    {
        Task<Person?> GetPerson(string? logonId, string? employeeId);
        Task<IEnumerable<Person>?> GetMatchingPerson(string searchText, int take);
        Task<bool> ValidatePerson(string? logonId, string? employeeId);
    }
}
