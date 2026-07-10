namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Configuration for the people directory data source. Bound from the
    /// "Directory" configuration section.
    /// </summary>
    public class DirectoryOptions
    {
        public const string SectionName = "Directory";

        /// <summary>
        /// Backing store provider for the people directory. Supported value:
        /// "Json" (flat-file, used for local development). Reserved for future
        /// providers such as "Ldap".
        /// </summary>
        public string Provider { get; set; } = "Json";

        /// <summary>
        /// Path to the flat-file people data source used by the "Json" provider.
        /// </summary>
        public string? PeopleJsonFilePath { get; set; }
    }
}
