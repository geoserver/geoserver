# SpatialJSON WFS Output Format Extension

This module adds the SpatialJSON WFS output format. The SpatialJSON format is a more compact and
memory-friendly variant of GeoServer's GeoJSON format. It aims to save space by applying several
optimizations to traditional GeoJSON format for simple feature results. Most of these optimizations
work by removing redundand information from the JSON-encoded features.

A service exception is thrown if the result contains complex features as the SpatialJSON format
does not handle those.

> **Note**: The SpatialJSON format is **not compatible** with GeoJSON. A SpatialJSON enabled reader
> is required to decode features transferred in SpatialJSON format.

This module adds two additional WFS output formats for requesting simple features in SpatialJSON
format:

- `application/json; subtype=json/spatial` for requesting SpatialJSON
- `text/javascript; subtype=json/spatial` for requesting SpatialJSON as a JSONP request

> **Warning**: At the time of writing, this format is still _work in progress_ and changes may be
> applied in the future.

### Development Status

The SpatialJSON format is still a playground for implementing several optimizations to transfer
even huge amounts of spatial data from the server to the client efficiently:

1. **Opt. 1: Removing redundant schema information**, see [topic](#opt-1-removing-redundant-schema-information)
2. **Opt. 2: Removing redundant attribute values (e. g. shared string table)**, see [topic](#opt-2-removing-redundant-attribute-values)
3. Opt. 3: Handling sparse rows (most values are NULL) more efficiently
4. Opt. 4: Reducing space required for geometries (e. g. coordinates

Bold items have already been implemented.

The shown optimizations are ordered from *simple to implement* to *hard to implement* (not *really*
hard, however). That's also the intended order of implementation. Although some optimizations are
optional, all optimizations could be in effect at the same time. Then, each optimization
contributes his part to lower the space required for encoding a certain set of features.

In some cases, however, it may be useful to specify which optimizations shall be used for a
request. Several techniques are available to give a client the ability to specify the set of
SpatialJSON optimizations it is able or willing to use (e. g. parameter `format_options`,
additional `outputFormat` parameters). It's still not clear how this will be implemented and how
fine grained that will be.

## Opt. 1: Removing Redundant Schema Information

In traditional GeoJSON, every feature in a (simple feature) feature collection has its own schema
information. That is, every feature contains all its (not necessarily short) attribute names.
Except the geometry name, these names are used as the keys in the `"properties"` map:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "areas.1",
      "geometry": {
        "type": "Point",
        "coordinates": [590529, 4914625]
      },
      "geometry_name": "the_geom",
      "properties": {
        "area_no": 12,
        "area_name": "Mainland",
        "area_description": "grassland",
        "area_cost_center": "0815"
      }
    },
    {
      "type": "Feature",
      "id": "areas.2",
      "geometry": {
        "type": "Point",
        "coordinates": [590215, 4913987]
      },
      "geometry_name": "the_geom",
      "properties": {
        "area_no": 17,
        "area_name": "South region",
        "area_description" : "meadow, pasture",
        "area_cost_center": "0812"
      }
    }
  ],
  "totalFeatures": 2,
  "numberMatched": 2,
  "numberReturned": 2,
  "timeStamp": "2022-10-17T08:12:45.248Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  }
}
```

Since all features have the same schema information, SpatialJSON does not write attribute names for
every feature. Instead, a single `"schemaInformation"` property is added to the end of the
top-level `"FeatureCollection"` object:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "areas.1",
      "geometry": {
        "type": "Point",
        "coordinates": [590529, 4914625]
      },
      "properties": [12, "Mainland", "grassland", "0815"]
    },
    {
      "type": "Feature",
      "id": "areas.2",
      "geometry": {
        "type": "Point",
        "coordinates": [590215, 4913987]
      },
      "properties": [17, "South region", "meadow, pasture", "0812"]
    }
  ],
  "totalFeatures": 2,
  "numberMatched": 2,
  "numberReturned": 2,
  "timeStamp": "2022-10-17T08:14:36.521Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  },
  "schemaInformation": {
    "propertyNames": ["area_no", "area_name", "area_description", "area_cost_center"],
    "geometryName": "the_geom"
  }
}
```

With SpatialJSON, each feature's `"properties"` map becomes an *ordered list* (array) whose index
corresponds to the `"propertyNames"` array that holds the attribute names in the new
`"schemaInformation"` object. Additionally, the repeated property `"geometry_name"` is replaced by
a single property named `"geometryName"` in the new schema information object.

### Evaluation

In the above example, without whitespaces and line breaks, savings in space are only about 5%. With
much more features savings could reach almost 27% (the ratio of the sizes of a GeoJSON and a
SpatialJSON feature object), that is, the size of the SpatialJSON response is only 73% of the size
of a traditional GeoJSON response. More savings are possible with more attributes per feature.
Savings basically depend on the ratio between schema information size and data size. In tests
requesting several thousands of simple features with 200+ columns/attributes savings up to 70% have
been achieved.

These savings drop to between \~50% and \~3% when a compressing content encoding method (like gzip,
deflate or brotli) is used on the wire. However, it's not all about transfer size. The smaller the
uncompressed JSON response, the lesser characters the client has to parse. Smaller uncompressed
responses are also much more memory-friendly on both the server and the client side.

## Opt. 2: Removing Redundant Attribute Values

### Shared String Table

A SpatialJSON response **may** contain a *Shared String Table*, which **may** contain strings that
are referenced by some features' properties. Only properties expressed as JSON strings can be stored
in a shared string table (at current, temporal values, like Dates and Timestamps, which are
expressed as strings as well, are not stored in a shared string table).

If present, a new `"sharedStrings"` property is available in the top-level
`"FeatureCollection"` object:

```jsonc
{
  "type": "FeatureCollection",

  /* remaining properties go here */

  "schemaInformation": {
    "propertyNames": ["str_1", "num_2", "str_3", "str_4", "bool_5"],
    "geometryName": "the_geom"
  },
  "sharedStrings": {
    "indexes": [0, 2, 3],
    "table": ["Lorem ipsum dolor sit amet",
              "consetetur sadipscing elitr",
              "sed diam nonumy eirmod tempor invidunt ut labore",
              "et dolore magna aliquyam erat",
              "sed diam voluptua."]
  }
}
```

It contains these two properties:

- `"table"` - Contains the shared strings. These are referenced by their index in the array.
- `"indexes"` - Contains the zero-based indexes of feature properties that **may** be stored in this shared string table.

In SpatialJSON, a feature's properties are basically stored in an array only (in contrast to GeoJSON
which stores properties in an object). The `"indexes"` array contains the indexes in these
properties arrays that **may** have their values stored in the shared string table. In a feature's
property array, such a value may actually be either `null`, a regular JSON `string` or a JSON
`number` (integral number). In the latter case, the property's value is actually stored in the
shared string table, the value being used as the index into the shared string table.

These examples show how some feature's properties arrays are evaluated using the above string table:

```javascript
/* showing properties array of feature #1 */
properties: ["foo", 23, 2, null, true]

/* gets evaluated to */
properties: {
  "str_1": "foo",
  "num_2": 23,
  "str_3": "sed diam nonumy eirmod tempor invidunt ut labore",
  "str_4": null,
  "bool_5": true
}

/* showing properties array of feature #2 */
properties: [1, 32, "K", 3, false]

/* gets evaluated to */
properties: {
  "str_1": "consetetur sadipscing elitr",
  "num_2": 32,
  "str_3": "K",
  "str_4": "et dolore magna aliquyam erat",
  "bool_5": false
}
```

As the examples show, there is no guarantee that all strings of a property whose index is part of
the `sharedStrings.indexes` array are actually stored in the shared string table.

### SpatialJSON Writer Implementation

It is completely up to the SpatialJSON writer to decide, which strings to add to the shared string
table. Several strategies can be used. However, the current implementation in this module makes no
attempt to create an *optimal* shared string table. In order to be fast, strings are added as they
come when features are serialized. Building an optimal table would likely require iterating features
several times, calculating frequencies of strings, etc.

Nevertheless, this module's SpatialJSON writer has some simple rules for building the shared string
table. Even for worst case scenarios, these try (at least) not to use (much) more bytes than needed
for the same result without using a shared string table. (In theory, there are cases in which the
shared string table adds some extra bytes to the result.) However, for most real world datasets,
this strategy could save a moderate to significant number of bytes.

These are the rules that prevent a string from being added to the shared string table:

- The string's UTF-8 encoded byte length is less than a hard-coded minimum (currently 2, may be configurable in the future)
- The shared sting table is full, that is, it contains 2,147,483,647 entries (not really expected)
- The string's UTF-8 encoded byte length (including quotes) is less than the number of digits of it's designated index

Obviously, most savings can be achieved if a dataset contains only a few different large strings.
That may be the case for attributes, that contain values of an enumeration, for example. The more
often a certain string is used in the dataset, the more space can be saved by using a shared string
table. In contrast, if every string in the set of encoded features is used only once (e. g.
attributes that contain random or UUID-like strings), no savings will be achieved (in fact, using a
shared string table in that case will produce even slightly bigger results).

### Shared Strings per Request Customization

By default, the current implementation will add *all* JSON string encoded properties to the shared
string table. (Except temporal values, like Dates and Timestamps, which in JSON technically are
strings as well. However, we do not expect much redundancy in temporal values.) With the
`format_options` vendor parameter it is possible to specify which properties can store values
in the shared string table or to completely skip the creation of such a table.

The supported format option is:

- `sharedstrings` (default is `*`) - Specify `false` or leave empty (e. g. `format_options=sharedstrings:`) to skip shared string table generation, or `true` or `*` to create a table including all JSON string encoded properties (that is the default behavior).  
Alternatively, a comma-separated list of property names could specify the set of properties that may store their values in the shared string table.

When a comma-separated list of property names is specified for the `sharedstrings` format
option, these additional rules apply:

- Commas in property names (really?) may be escaped with a backslash character `\`.
- The prefix `re:` may be prepended to the list in order to designate each item a *Java Regular Expression*: (e. g. `format_options=sharedstrings:re:adm_.*,\d\d_[a-z]+$`). See Java [Pattern](https://docs.oracle.com/javase/8/docs/api/index.html?java/util/regex/Pattern.html) class.

  Specifying an invalid regular expression results in a Service Exception.
- The prefix `glob:` may be prepended to the list in order to designate each item a *glob pattern*: (e. g. `format_options=sharedstrings:glob:adm_*,[0-9][0-9]_*name`). See [glob](https://en.wikipedia.org/wiki/Glob_(programming)) patterns.

  Specifying an invalid glob pattern results in a Service Exception.

Although the SpatialJSON Shared String Table feature works fine and typically saves a moderate
number of bytes arbitrary datasets in its default configuration, that is without specifying the
`sharedstrings` format option, this parameter provides a solid handle for advanced fine tuning
of the string table's creation process.