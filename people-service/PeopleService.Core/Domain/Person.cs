namespace PeopleService.Core.Domain
{
    /// <summary>
    /// Domain model representing a person in the directory. This is the shape
    /// returned over the REST/JSON API and is intentionally decoupled from any
    /// persistence or directory (LDAP/flat-file) representation.
    /// </summary>
    public class Person
    {
        public string? LogonId { get; set; }
        public string? FullName { get; set; }
        public string? Email { get; set; }
        public string? EmployeeId { get; set; }
        public string? Department { get; set; }
        public string? PhotoUrl { get; set; }
    }
}
