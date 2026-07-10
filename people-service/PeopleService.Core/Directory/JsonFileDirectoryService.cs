using PeopleService.Core.Domain;

namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Flat-file / in-memory implementation of <see cref="IDirectoryService"/>.
    /// Loads people from an <see cref="IPersonDataReader"/> and serves lookups
    /// from memory. Used for local development in place of an LDAP directory.
    /// </summary>
    public class JsonFileDirectoryService : IDirectoryService
    {
        private readonly IReadOnlyList<Person> _people;

        public JsonFileDirectoryService(IPersonDataReader reader)
        {
            _people = reader.Load();
        }

        public Task<IEnumerable<Person>?> GetMatchingPerson(string searchText, int take)
        {
            IEnumerable<Person> matches = _people
                .Where(p => (p.FullName?.Contains(searchText) ?? false)
                            || (p.LogonId?.Contains(searchText) ?? false))
                .Take(take);

            return Task.FromResult<IEnumerable<Person>?>(matches);
        }

        public Task<Person?> GetPerson(string? logonId, string? employeeId)
        {
            if (!string.IsNullOrEmpty(logonId))
            {
                return Task.FromResult(_people.FirstOrDefault(p => p.LogonId == logonId));
            }

            return Task.FromResult(_people.FirstOrDefault(p => p.EmployeeId == employeeId));
        }

        public async Task<bool> ValidatePerson(string? logonId, string? employeeId)
        {
            return await GetPerson(logonId, employeeId) != null;
        }
    }
}
