# Data stores

A `data store` contains vector format spatial data. It can be a file (such as a shapefile), a database (such as PostGIS), or a server (such as a [remote Web Feature Service](../../data/cascaded/wfs.md)).

## `/workspaces/<ws>/datastores[.<format>]`

Controls all data stores in a given workspace.

| Method | Action                                 | Status code                | Formats         | Default Format |
|--------|----------------------------------------|----------------------------|-----------------|----------------|
| GET    | List all data stores in workspace `ws` | 200                        | HTML, XML, JSON | HTML           |
| POST   | Create a new data store                | 201 with `Location` header | XML, JSON       |                |
| PUT    |                                        | 405                        |                 |                |
| DELETE |                                        | 405                        |                 |                |

## `/workspaces/<ws>/datastores/<ds>[.<format>]`

Controls a particular data store in a given workspace.

| Method | Action                 | Status code | Formats         | Default Format | Parameters                                                            |
|--------|------------------------|-------------|-----------------|----------------|-----------------------------------------------------------------------|
| GET    | Return data store `ds` | 200         | HTML, XML, JSON | HTML           | [quietOnNotFound](datastores.md#rest_api_datastores_quietOnNotFound) |
| POST   |                        | 405         |                 |                |                                                                       |
| PUT    | Modify data store `ds` |             |                 |                |                                                                       |
| DELETE | Delete data store `ds` |             |                 |                | [recurse](datastores.md#rest_api_datastores_recurse)                 |

### Exceptions

| Exception                                                          | Status code |
|--------------------------------------------------------------------|-------------|
| GET for a data store that does not exist                           | 404         |
| PUT that changes name of data store                                | 403         |
| PUT that changes workspace of data store                           | 403         |
| DELETE against a data store that contains configured feature types | 403         |

### Parameters

#### `recurse` {: #rest_api_datastores_recurse }

The `recurse` parameter recursively deletes all layers referenced by the specified data store. Allowed values for this parameter are "true" or "false". The default value is "false".

#### `quietOnNotFound` {: #rest_api_datastores_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the data store is not present. Note that 404 status code will be returned anyway.

## `/workspaces/<ws>/datastores/<ds>/[file|url|external][.<extension>]`

These endpoints (`file`, `url`, and `external`) allow a file containing either spatial data or a mapping configuration (in case an app-schema data store is targeted), to be added (via a PUT request) into an existing data store, or will create a new data store if it doesn't already exist. The three endpoints are used to specify the method that is used to upload the file:

-   `file`---Uploads a file from a local source. The body of the request is the file itself.
-   `url`---Uploads a file from an remote source. The body of the request is a URL pointing to the file to upload. This URL must be visible from the server.
-   `external`---Uses an existing file on the server. The body of the request is the absolute path to the existing file.

| Method | Action                                                                                                        | Status code | Formats                                                       | Default Format | Parameters                                                                                                                                                                                                                 |
|--------|---------------------------------------------------------------------------------------------------------------|-------------|---------------------------------------------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET    | *Deprecated*. Retrieve the underlying files for the data store as a zip file with MIME type `application/zip` | 200         |                                                               |                |                                                                                                                                                                                                                            |
| POST   |                                                                                                               | 405         |                                                               |                |                                                                                                                                                                                                                            |
| PUT    | Uploads files to the data store `ds`, creating it if necessary                                                | 200         | [See note below](datastores.md#rest_api_datastores_file_put) |                | [configure](datastores.md#rest_api_datastores_configure), [target](datastores.md#rest_api_datastores_target), [update](datastores.md#rest_api_datastores_update), [charset](datastores.md#rest_api_datastores_charset) |
| DELETE |                                                                                                               | 405         |                                                               |                |                                                                                                                                                                                                                            |

### Exceptions

| Exception                                   | Status code |
|---------------------------------------------|-------------|
| GET for a data store that does not exist    | 404         |
| GET for a data store that is not file based | 404         |

### Parameters

#### `extension` {: #rest_api_datastores_extension }

The `extension` parameter specifies the type of data being uploaded. The following extensions are supported:

| Extension  | Datastore                        |
|------------|----------------------------------|
| shp        | Shapefile                        |
| properties | Property file                    |
| h2         | H2 Database                      |
| appschema  | App-schema mapping configuration |

#### File PUT {: #rest_api_datastores_file_put }

A file can be PUT to a data store as a standalone or zipped archive file. Standalone files are only suitable for data stores that work with a single file such as a GML store. Data stores that work with multiple files, such as the shapefile store, must be sent as a zip archive.

When uploading a standalone file, set the `Content-type` appropriately based on the file type. If you are loading a zip archive, set the `Content-type` to `application/zip`.

#### file PUT Application Schema {: #rest_api_datastores_file_put_appschema }

The app-schema mapping configuration can either be uploaded as a single file, or split in multiple files for reusability and/or mapping constraints (e.g. multiple mappings of the same feature type are needed). If multiple mapping files are uploaded as a zip archive, the extension of the main mapping file (the one including the others via the `<includedTypes>` tag) must be `.appschema`, otherwise it will not be recognized as the data store's primary file and publishing will fail.

The application schemas (XSD files) required to define the mapping can be added to the zip archive and uploaded along with the mapping configuration. All files contained in the archive are uploaded to the same folder, so the path to the secondary mapping files and the application schemas, as specified in the main mapping file, is simply the file name of the included resource.

#### `configure` {: #rest_api_datastores_configure }

The `configure` parameter controls how the data store is configured upon file upload. It can take one of the three values:

-   `first`---(*Default*) Only setup the first feature type available in the data store.
-   `none`---Do not configure any feature types.
-   `all`---Configure all feature types.

!!! note

    When uploading an app-schema mapping configuration, only the feature types mapped in the main mapping file are considered to be top level features and will be automatically configured when `configure=all` or `configure=first` is specified.

#### `target` {: #rest_api_datastores_target }

The `target` parameter determines what format or storage engine will be used when a new data store is created on the server for uploaded data. When importing data into an existing data store, it is ignored. The allowed values for this parameter are the same as for the [extension parameter](datastores.md#rest_api_datastores_extension), except for `appschema`, which doesn't make sense in this context.

#### `update` {: #rest_api_datastores_update }

The `update` parameter controls how existing data is handled when the file is PUT into a data store that already exists and already contains a schema that matches the content of the file. The parameter accepts one of the following values:

-   `append`---Data being uploaded is appended to the existing data. This is the default.
-   `overwrite`---Data being uploaded replaces any existing data.

The parameter is ignored for app-schema data stores, which are read-only.

#### `charset` {: #rest_api_datastores_charset }

The `charset` parameter specifies the character encoding of the file being uploaded (such as "ISO-8559-1").
