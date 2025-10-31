# GeoParquet Extension for GeoServer

This extension adds support for the GeoParquet format to GeoServer. GeoParquet is an open format for geospatial data that builds on Apache Parquet, providing efficient columnar storage for geospatial data.

## GeoParquet DataStore

The extension includes a custom DataStore implementation that allows GeoServer to read from:

- Local GeoParquet files (file://)
- Remote GeoParquet files via HTTP/HTTPS (http://, https://)
- S3-hosted GeoParquet files (s3://)
- Directories containing multiple GeoParquet files
- Hive-partitioned datasets with key=value directory structures

## Web UI Components

The extension provides a customized user interface for configuring GeoParquet data stores in the GeoServer web admin interface, with:

- Specialized form fields for GeoParquet-specific parameters
- Logical ordering of configuration parameters (URI first, required fields next)
- Visual cues for required parameters
- Helpful tooltips and descriptions

## Configuration Parameters

The GeoParquet DataStore supports the following key parameters:

- **uri**: Path to a GeoParquet file or directory (required)
    - Local files: `file:///path/to/data.parquet` or `file:///path/to/directory`
    - HTTP: `https://example.com/data.parquet`
    - S3: `s3://bucket/path/to/data.parquet?region=us-west-2&access_key=ACCESS_KEY&secret_key=SECRET_KEY`

- **max_hive_depth**: Maximum depth of Hive partition hierarchy to use (optional)
    - Null (default): Use all partition levels
    - 0: Group all files together
    - 1+: Use specific level of partitioning

- **simplify**: Enable geometry simplification for rendering optimization (default: true)

- **namespace**: Custom namespace URI for features (optional)

## Example REST requests
A few example requests for uploading and retrieving data.

To upload file to the data store:
```
curl --request PUT \
  --url <base_url>/rest/workspaces/tiger/datastores/test/file.geoparquet \
  --header 'Authorization: Basic ' \
  --header 'Content-Type: application/octet-stream' \
  --data '<file_content>'
```

To retrieve files for the data store as a zip file with MIME type application/zip:
```
curl --request GET \
--url <base_url>/rest/workspaces/tiger/datastores/test/file.geoparquet \
--header 'Authorization: Basic ' \
```

## Adding a GeoParquet Layer

To add a GeoParquet layer in GeoServer:

1. Go to "Stores" > "Add New Store"
2. Select "GeoParquet"
3. Configure the connection parameters
4. Click "Save"
5. Publish layers from the new datastore
