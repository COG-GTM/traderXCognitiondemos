using PeopleService.Core.Domain;

namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Persistence representation of a person as stored in the flat-file backing
    /// store. Kept separate from the <see cref="Person"/> domain model so that
    /// changes to the storage format do not leak into the domain or API contract.
    /// </summary>
    internal sealed class PersonRecord
    {
        public string? LogonId { get; set; }
        public string? FullName { get; set; }
        public string? Email { get; set; }
        public string? EmployeeId { get; set; }
        public string? Department { get; set; }
        public string? PhotoUrl { get; set; }

        public Person ToPerson() => new()
        {
            LogonId = LogonId,
            FullName = FullName,
            Email = Email,
            EmployeeId = EmployeeId,
            Department = Department,
            PhotoUrl = PhotoUrl
        };
    }
}
