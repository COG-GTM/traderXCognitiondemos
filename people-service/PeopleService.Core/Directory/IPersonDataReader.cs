using PeopleService.Core.Domain;

namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Abstraction over the backing store that supplies people records to a
    /// directory service. Implementations may read from a flat file (local dev),
    /// an LDAP user directory, or any other source, and are responsible for
    /// mapping their storage representation to the <see cref="Person"/> domain model.
    /// </summary>
    public interface IPersonDataReader
    {
        IReadOnlyList<Person> Load();
    }
}
