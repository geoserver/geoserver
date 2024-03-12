# Styles

A `style` describes how a resource (feature type or coverage) should be symbolized or rendered by the Web Map Service. In GeoServer styles are specified with [SLD](../../styling/index.md).

## `/styles[.<format>]`

Controls all styles.

| Method | Action             | Status code                | Formats                                                                   | Default Format | Parameters                                                                    |
|--------|--------------------|----------------------------|---------------------------------------------------------------------------|----------------|-------------------------------------------------------------------------------|
| GET    | Return all styles  | 200                        | HTML, XML, JSON                                                           | HTML           |                                                                               |
| POST   | Create a new style | 201 with `Location` header | SLD, XML, JSON, ZIP [See note below](styles.md#rest_api_styles_post_put) |                | [name](styles.md#rest_api_styles_name) [raw](styles.md#rest_api_styles_raw) |
| PUT    |                    | 405                        |                                                                           |                |                                                                               |
| DELETE |                    | 405                        |                                                                           |                |                                                                               |

### Styles POST and PUT {: #rest_api_styles_post_put }

When executing a POST or PUT request with an SLD style, the `Content-type` header should be set to the mime type identifying the style format. Style formats supported out of the box include:

-   SLD 1.0 with a mime type of `application/vnd.ogc.sld+xml`
-   SLD 1.1 / SE 1.1 with a mime type of `application/vnd.ogc.se+xml`
-   SLD package (zip file containing sld and image files used in the style) with a mime type of application/zip

Other extensions (such as [css](../../styling/css/index.md)) add support for additional formats.

### Parameters

#### `name` {: #rest_api_styles_name }

The `name` parameter specifies the name to be given to the style. This option is most useful when executing a POST request with a style in SLD format, and an appropriate name can be not be inferred from the SLD itself.

#### `raw` {: #rest_api_styles_raw }

The `raw` parameter specifies whether to forgo parsing and encoding of the uploaded style content. When set to "true" the style payload will be streamed directly to GeoServer configuration. Use this setting if the content and formatting of the style is to be preserved exactly. Use this setting with care as it may result in an invalid and unusable style. The default is "false".

## `/styles/<s>[.<format>]`

Controls a given style.

| Method | Action           | Status code | Formats                                                                   | Default Format                            | Parameters                                                                                                |
|--------|------------------|-------------|---------------------------------------------------------------------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| GET    | Return style `s` | 200         | SLD, HTML, XML, JSON                                                      | HTML                                      | [quietOnNotFound](styles.md#rest_api_styles_quietOnNotFound) [pretty](styles.md#rest_api_styles_pretty) |
| POST   |                  | 405         |                                                                           |                                           |                                                                                                           |
| PUT    | Modify style `s` | 200         | SLD, XML, JSON, ZIP [See note above](styles.md#rest_api_styles_post_put) |                                           | [raw](styles.md#rest_api_styles_raw)                                                                     |
| DELETE | Delete style `s` | 200         |                                                                           | [purge](styles.md#rest_api_styles_purge) | [recurse](styles.md#rest_api_styles_recurse)                                                             |

### Exceptions

| Exception                                                   | Status code |
|-------------------------------------------------------------|-------------|
| GET for a style that does not exist                         | 404         |
| PUT that changes name of style                              | 403         |
| DELETE against style which is referenced by existing layers | 403         |

### Parameters

#### `purge` {: #rest_api_styles_purge }

The `purge` parameter specifies whether the underlying SLD file for the style should be deleted on disk. Allowable values for this parameter are "true" or "false". When set to "true" the underlying file will be deleted.

#### `recurse` {: #rest_api_styles_recurse }

The `recurse` parameter removes references to the specified style in existing layers. Allowed values for this parameter are "true" or "false". The default value is "false".

#### `quietOnNotFound` {: #rest_api_styles_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the style is not present. Note that 404 status code will be returned anyway.

#### `pretty` {: #rest_api_styles_pretty }

The `pretty` parameter returns the style in a human-readable format, with proper blank-space and indentation. This parameter has no effect if you request a style in its native format - in this case the API returns the exact content of the underlying file. The HTML, XML, and JSON formats do not support this parameter.

## `/workspaces/<ws>/styles[.<format>]`

Controls all styles in a given workspace.

| Method | Action                                   | Status code                | Formats                                                                   | Default Format | Parameters                                                                    |
|--------|------------------------------------------|----------------------------|---------------------------------------------------------------------------|----------------|-------------------------------------------------------------------------------|
| GET    | Return all styles within workspace `ws`  | 200                        | HTML, XML, JSON                                                           | HTML           |                                                                               |
| POST   | Create a new style within workspace `ws` | 201 with `Location` header | SLD, XML, JSON, ZIP [See note above](styles.md#rest_api_styles_post_put) |                | [name](styles.md#rest_api_styles_name) [raw](styles.md#rest_api_styles_raw) |
| PUT    |                                          | 405                        |                                                                           |                |                                                                               |
| DELETE |                                          | 405                        |                                                                           |                | [purge](styles.md#rest_api_styles_purge)                                     |

## `/workspaces/<ws>/styles/<s>[.<format>]`

Controls a particular style in a given workspace.

| Method | Action                                 | Status code | Formats                                                                   | Default Format | Parameters                                                    |
|--------|----------------------------------------|-------------|---------------------------------------------------------------------------|----------------|---------------------------------------------------------------|
| GET    | Return style `s` within workspace `ws` | 200         | SLD, HTML, XML, JSON                                                      | HTML           | [quietOnNotFound](styles.md#rest_api_styles_quietOnNotFound) |
| POST   |                                        | 405         |                                                                           |                |                                                               |
| PUT    | Modify style `s` within workspace `ws` | 200         | SLD, XML, JSON, ZIP [See note above](styles.md#rest_api_styles_post_put) |                | [raw](styles.md#rest_api_styles_raw)                         |
| DELETE | Delete style `s` within workspace `ws` | 200         |                                                                           |                |                                                               |

### Exceptions

| Exception                                              | Status code |
|--------------------------------------------------------|-------------|
| GET for a style that does not exist for that workspace | 404         |
