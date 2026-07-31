# Feature types

A `feature type` is a vector based spatial resource or data set that originates from a data store. In some cases, such as with a shapefile, a feature type has a one-to-one relationship with its data store. In other cases, such as PostGIS, the relationship of feature type to data store is many-to-one, feature types corresponding to a table in the database.

## `/workspaces/<ws>/datastores/<ds>/featuretypes[.<format>]`

Controls all feature types in a given data store / workspace.

| Method | Action | Status code | Formats | Default Format | Parameters |
|----|----|----|----|----|----|
| GET | List all feature types in data store `ds` | 200 | HTML, XML, JSON | HTML | [list](#rest_api_featuretypes_list) |
| POST | Create a new feature type, [see note below](#rest_api_featuretypes_post) | 201 with `Location` header | XML, JSON |  |  |
| PUT |  | 405 |  |  |  |
| DELETE |  | 405 |  |  |  |

### featuretypes POST {: #rest_api_featuretypes_post }

When creating a new feature type via `POST`, if no underlying dataset with the specified name exists an attempt will be made to create it. This will work only in cases where the underlying data format supports the creation of new types (such as a database). When creating a feature type in this manner the client should include all attribute information in the feature type representation.

### Exceptions

| Exception                                   | Status code |
|---------------------------------------------|-------------|
| GET for a feature type that does not exist  | 404         |
| PUT that changes name of feature type       | 403         |
| PUT that changes data store of feature type | 403         |

### Parameters

#### `list` {: #rest_api_featuretypes_list }

The `list` parameter is used to control the category of feature types that are returned. It can take one of the following values:

- `configured`---Only configured feature types are returned. This is the default value.
- `available`---Only feature types that haven't been configured but are available from the specified data store will be returned.
- `available_with_geom`---Same as `available` but only includes feature types that have a geometry attribute.
- `all`---The union of `configured` and `available`.

## `/workspaces/<ws>/datastores/<ds>/featuretypes/<ft>[.<format>]`

Controls a particular feature type in a given data store and workspace.

| Method | Action | Status code | Formats | Default Format | Parameters |
|----|----|----|----|----|----|
| GET | Return feature type `ft` | 200 | HTML, XML, JSON | HTML | [quietOnNotFound](#rest_api_featuretypes_quietOnNotFound) |
| POST |  | 405 |  |  |  |
| PUT | Modify feature type `ft` | 200 | XML,JSON |  | [recalculate](#rest_api_featuretypes_recalculate) |
| DELETE | Delete feature type `ft` | 200 |  |  | [recurse](#rest_api_featuretypes_recurse) |

### Exceptions

| Exception                                   | Status code |
|---------------------------------------------|-------------|
| GET for a feature type that does not exist  | 404         |
| PUT that changes name of feature type       | 403         |
| PUT that changes data store of feature type | 403         |

### Parameters

#### `recurse` {: #rest_api_featuretypes_recurse }

The `recurse` parameter recursively deletes all layers referenced by the specified featuretype. Allowed values for this parameter are "true" or "false". The default value is "false". A DELETE request with `recurse=false` will fail if any layers reference the featuretype.

#### `recalculate` {: #rest_api_featuretypes_recalculate }

The `recalculate` parameter specifies which feature type properties GeoServer should recalculate when processing the request. Some properties are recalculated automatically when necessary. In particular, the native bounding box is recalculated when the projection or projection policy changes, and the lat/long bounding box is recalculated when the native bounding box is recalculated or when a new native bounding box is explicitly provided in the request. The native and lat/long bounding boxes are not automatically recalculated when they are explicitly included in the request.

The client may also explicitly request a fixed set of properties to recalculate by including a comma-separated list of their names in the `recalculate` parameter. The supported values are:

- `recalculate=` (empty parameter): Do not recalculate any properties, regardless of changes to the projection, projection policy, or other feature type settings. This may be useful for avoiding slow calculations when operating against large datasets.
- `recalculate=attributes`: Reload the feature type attributes from the underlying data source. This is required when the source schema has changed, for example when columns have been added, removed, renamed, or otherwise modified. The underlying schema must be updated before this operation is invoked.
- `recalculate=nativebbox`: Recalculate the native bounding box, but do not recalculate the lat/long bounding box.
- `recalculate=nativebbox,latlonbbox`: Recalculate both the native bounding box and the lat/long bounding box.
- `recalculate=attributes,nativebbox,latlonbbox`: Reload the feature type attributes and recalculate both the native and lat/long bounding boxes.

#### `Projection Policy`

When specifying the Projection Policy in a FeatureType defined in the request body, the internal name should be used instead of the one available on the UI. The following table shows the correspondence between display and internal names:

| Display Name                 | Internal Name         |
|------------------------------|-----------------------|
| Force declared               | FORCE_DECLARED        |
| Keep native                  | NONE                  |
| Reproject native to declared | REPROJECT_TO_DECLARED |

#### `quietOnNotFound` {: #rest_api_featuretypes_quietOnNotFound }

The `quietOnNotFound` parameter avoids to log an Exception when the feature type is not present. Note that 404 status code will be returned anyway.
