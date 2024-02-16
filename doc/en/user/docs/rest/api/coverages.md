# Coverages {: #rest_api_coverages }

A `coverage` is a raster data set which originates from a coverage store.

!!! info "Todo"

    JC: "The second level headings [don't] work so well for the longer paths - maybe another heading format?"

## `/workspaces/<ws>/coveragestores/<cs>/coverages[.<format>]`

Controls all coverages in a given coverage store and workspace.

| Method | Action                                    | Status code                | Formats         | Default Format |
|--------|-------------------------------------------|----------------------------|-----------------|----------------|
| GET    | List all coverages in coverage store `cs` | 200                        | HTML, XML, JSON | HTML           |
| POST   | Create a new coverage                     | 201 with `Location` header | XML, JSON       |                |
| PUT    |                                           | 405                        |                 |                |
| DELETE |                                           | 405                        |                 |                |

## `/workspaces/<ws>/coveragestores/<cs>/coverages/<c>[.<format>]`

Controls a particular coverage in a given coverage store and workspace.

| Method | Action              | Status code | Formats         | Default Format | Parameters                                                          |
|--------|---------------------|-------------|-----------------|----------------|---------------------------------------------------------------------|
| GET    | Return coverage `c` | 200         | HTML, XML, JSON | HTML           | [quietOnNotFound](coverages.md#rest_api_coverages_quietOnNotFound) |
| POST   |                     | 405         |                 |                |                                                                     |
| PUT    | Modify coverage `c` | 200         | XML,JSON        |                |                                                                     |
| DELETE | Delete coverage `c` | 200         |                 |                | [recurse](coverages.md#rest_api_coverages_recurse)                 |

### Exceptions

| Exception                                   | Status code |
|---------------------------------------------|-------------|
| GET for a coverage that does not exist      | 404         |
| PUT that changes name of coverage           | 403         |
| PUT that changes coverage store of coverage | 403         |

### Parameters

#### `recurse` {: #rest_api_coverages_recurse }

The `recurse` parameter recursively deletes all layers referenced by the specified coverage. Permitted values for this parameter are "true" or "false". The default value is "false".

#### `quietOnNotFound` {: #rest_api_coverages_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the coverage is not present. Note that 404 status code will be returned anyway.

# Structured coverages

Structured coverages are the ones whose content is made of granules, normally associated to attributes, often used to represent time, elevation and other custom dimensions attached to the granules themselves. Image mosaic is an example of a writable structured coverage reader, in which each of the mosaic granules is associated with attributes. NetCDF is an example of a read only one, in which the multidimensional grid contained in the file is exposed as a set of 2D slices, each associated with a different set of variable values.

The following API applies exclusively to structured coverage readers.

## `/workspaces/<ws>/coveragestores/<cs>/coverages/<coverage>/index[.<format>]`

Declares the set of attributes associated to the specified coverage, their name, type and min/max occurrences.

| Method | Action                                              | Status code | Formats   | Default Format | Parameters |
|--------|-----------------------------------------------------|-------------|-----------|----------------|------------|
| GET    | Returns the attributes, their names and their types | 200         | XML, JSON | XML            |            |
| POST   |                                                     | 405         |           |                |            |
| PUT    |                                                     | 405         |           |                |            |
| DELETE |                                                     | 405         |           |                |            |

## `/workspaces/<ws>/coveragestores/<cs>/coverages/<coverage>/index/granules.<format>`

Returns the full list of granules, each with its attributes vales and geometry, and allows to selectively remove them

| Method | Action                                                                                                             | Status code | Formats   | Default Format | Parameters                                                                                                                                            |
|--------|--------------------------------------------------------------------------------------------------------------------|-------------|-----------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET    | Returns the list of granules and their attributes, either in GML (when XML is used) or GeoJSON (when JSON is used) | 200         | XML, JSON | XML            | [offset](coverages.md#rest_api_coverages_offset), [limit](coverages.md#rest_api_coverages_limit), [filter](coverages.md#rest_api_coverages_filter) |
| POST   |                                                                                                                    | 405         |           |                |                                                                                                                                                       |
| PUT    |                                                                                                                    | 405         |           |                |                                                                                                                                                       |
| DELETE | Deletes the granules (all, or just the ones selected via the filter parameter)                                     | 200         |           |                | [filter](coverages.md#rest_api_coverages_filter)                                                                                                     |

### Parameters

#### `offset` {: #rest_api_coverages_offset }

The `offset` parameter instructs GeoServer to skip the specified number of first granules when returning the data.

#### `limit` {: #rest_api_coverages_limit }

The `limit` parameter instructs GeoServer to return at most the specified number of granules when returning the data.

#### `filter` {: #rest_api_coverages_filter }

The `filter` parameter is a CQL filter that allows to select which granules will be returned based on their attribute values.

## `/workspaces/<ws>/coveragestores/<cs>/coverages/<mosaic>/index/granules/<granuleId>.<format>`

Returns a single granule and allows for its removal.

| Method | Action                                                                                                                | Status code | Formats   | Default Format | Parameters                                                                    |
|--------|-----------------------------------------------------------------------------------------------------------------------|-------------|-----------|----------------|-------------------------------------------------------------------------------|
| GET    | Returns the specified of granules and its attributes, either in GML (when XML is used) or GeoJSON (when JSON is used) | 200         | XML, JSON | XML            | [quietOnNotFound](coverages.md#rest_api_structuredcoverages_quietOnNotFound) |
| POST   |                                                                                                                       | 405         |           |                |                                                                               |
| PUT    |                                                                                                                       | 405         |           |                |                                                                               |
| DELETE | Deletes the granule                                                                                                   | 200         |           |                |                                                                               |

### Exceptions

| Exception                             | Status code |
|---------------------------------------|-------------|
| GET for a granule that does not exist | 404         |

### Parameters

#### `quietOnNotFound` {: #rest_api_structuredcoverages_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the granule is not present. Note that 404 status code will be returned anyway.
