using System.Text.Json;
using Microsoft.Extensions.Options;
using PeopleService.Core.Domain;

namespace PeopleService.Core.Directory
{
    /// <summary>
    /// Flat-file implementation of <see cref="IPersonDataReader"/> that loads
    /// people from a JSON file. Used for local development in place of an LDAP
    /// user directory.
    /// </summary>
    public class JsonFilePersonReader : IPersonDataReader
    {
        private static readonly JsonSerializerOptions SerializerOptions = new()
        {
            PropertyNameCaseInsensitive = true
        };

        private readonly DirectoryOptions _options;

        public JsonFilePersonReader(IOptions<DirectoryOptions> options)
        {
            _options = options.Value;
        }

        public IReadOnlyList<Person> Load()
        {
            var path = _options.PeopleJsonFilePath;
            if (string.IsNullOrWhiteSpace(path))
            {
                throw new InvalidOperationException(
                    $"{DirectoryOptions.SectionName}:{nameof(DirectoryOptions.PeopleJsonFilePath)} is not configured.");
            }

            try
            {
                var json = File.ReadAllText(path);
                var records = JsonSerializer.Deserialize<List<PersonRecord>>(json, SerializerOptions)
                              ?? new List<PersonRecord>();
                return records.Select(r => r.ToPerson()).ToList();
            }
            catch (FileNotFoundException)
            {
                throw new InvalidOperationException($"People data file not found: {path}");
            }
            catch (DirectoryNotFoundException)
            {
                throw new InvalidOperationException($"Directory not found: {Path.GetDirectoryName(path)}");
            }
            catch (JsonException ex)
            {
                throw new InvalidOperationException($"Error parsing people data file '{path}': {ex.Message}", ex);
            }
        }
    }
}
