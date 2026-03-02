# Importer

The Importer extension gives a GeoServer administrator an alternate, more-streamlined method for uploading and configuring new layers.

There are two primary advantages to using the Importer over the standard GeoServer data-loading workflow:

1.  **Supports batch operations** (loading and publishing multiple spatial files or database tables in one operation)
2.  **Creates unique styles** for each layer, rather than linking to the same (existing) styles.

!!! warning

    The importer extension allows upload of data and is currently unable to respect the file system sandbox, it uses a configurable location inside the data directory instead. Store creation will fail if the importer is used and the sandbox is set. See also [Filesystem sandboxing](../../security/sandbox.md).

This section will discuss the Importer extension.

<div class="grid cards" markdown>

- [ExtensionsImporterInstalling](installing.md)
- [ExtensionsImporterConfiguring](configuring.md)
- [ExtensionsImporterUsing](using.md)
- [ExtensionsImporterGuireference](guireference.md)
- [ExtensionsImporterFormats](formats.md)
- [ExtensionsImporterRest Reference](rest_reference.md)
- [ExtensionsImporterRest Examples](rest_examples.md)

</div>
