# Coordinate Reference Systems

## `/crs`

List and retrieve the Coordinate Reference Systems (CRS) available in GeoServer.

| Method | Action | Status code | Formats | Default Format | Parameters |
|----|----|----|----|----|----|
| GET | List all GeoServer Coordinate Reference Systems | 200 | JSON | JSON | [authority](#rest_api_crs_authority), [query](#rest_api_crs_query), [offset](#rest_api_crs_offset), [limit](#rest_api_crs_limit) |

### Exceptions

| Exception | Status code |
|----|----|
| GET for invalid request params | 400 |

### Parameters

#### `authority` {: #rest_api_crs_authority }

The `authority` parameter allow to filter CRS by authority (e.g. EPSG)

#### `query` {: #rest_api_crs_query }

The `query` parameter allow to query for a code by partial ID (e.g. query=32 will return all CRSs containing "32" on their ID)

#### `offset` {: #rest_api_crs_offset }

The `offset` parameter allow to specify the offset of the paging result.

#### `limit` {: #rest_api_crs_limit }

The `limit` the number of results returned by the page.

## `/crs/<identifier>[.<extension>]`

Retrieve a CRS by its identifier in the form AUTHORITY:CODE. (e.g. EPSG:4326).

| Method | Action | Status code | Formats | Default Format |
|----|----|----|----|----|
| GET | Get a specific CRS by its identifier in the form AUTHORITY:CODE. | 200 | JSON, WKT | JSON |

### Exceptions

| Exception                                   | Status code |
|---------------------------------------------|-------------|
| GET for a crs that does not exist           | 404         |
| GET for a not well formed identifier        | 400         |

### Parameters

#### `extension`

The `extension` parameter specifies the type of the returned definition.

| Extension   | Output                                                                                              |
|-------------|-----------------------------------------------------------------------------------------------------|
| json        | Return the CRS detail in json format, including ID, name, WKT, and domain of validity when available |
| wkt         | Return the CRS definition in WKT format (WKT1)                                                      |

## `/crs/authorities`

Return the supported CRS authorities.

| Method | Action | Status code | Formats | Default Format |
|----|----|----|----|----|
| GET | List all GeoServer CoordinateReferenceSystems authorities | 200 | JSON | JSON |

### Exceptions

| Exception | Status code |
|----|----|
| GET for invalid request params | 400 |