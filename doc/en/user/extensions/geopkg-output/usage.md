# Using the GeoPackage Output Extension

The GeoPackage Output Extension adds support to WFS and WMS to request `GetFeature` and `GetMap` results in GeoPackage Format.

## WFS

Add `&outputFormat=geopkg` to your request. The result will be a GeoPackage (MIME type `application/geopackage+sqlite3`) containing the requested features.

```bash
curl "http://localhost:8080/geoserver/wfs?service=wfs&version=2.0.0&request=GetFeature&typeNames=ws:layername&outputFormat=geopkg" \
-o wfs.gpkg
```

You can use `geopkg`, `geopackage`, or `gpkg` as the output format in the request. Use `1.0.0`, `1.1.0`, or `2.0.0` as `version=` to specify which WFS version to use.

!!! note
    GeoPackages always have the ordinates in X,Y (`EAST_NORTH`) format.

## WFS Output Configuration

GeoPackage output format configuration properties are available. For information on use of configuration properties see [running in a production environment](../../production/config.md) instructions.

### geopackage.wfs.indexed

By default a spatial index is generated when generating GeoPackage output.

Use java system property `-Dgeopackage.wfs.indexed=false` to suppress the generation of a spatial index in generated geopackage output.

### geopackage.wfs.tempdir

The GeoPackage file format is an SQLite database which can only be managed as a file locally. To produce a GeoPackage GeoServer makes use of a temporary file created in `java.io.tmpdir` location. This temporary file is removed once the response is completed.

Some container environments recommend use of a network share for their `java.io.tmpdir` location. This approach is not compatible with SQLite database driver which requires a local disk location and file lock.

To override the temporary file location used for GeoPackage output format file generation use property `-Dgeopackage.wfs.tempdir=<path location>` to provide an alternate path.

## WMS

Add `&format=geopkg` to your request. The result will be a GeoPackage (MIME type `application/geopackage+sqlite3`) containing the requested tiles.

Using WMS 1.1.0 to access tiled image geopkg:

```bash
curl "http://localhost:8080/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ws:layername&bbox=-123.43670607166865%2C48.3956835%2C-123.2539813%2C48.5128362547052&width=1536&height=984&srs=EPSG%3A4326&styles=&format=geopkg" \
-o wms.gpkg
```

Using WMS 1.3.0 to access tiled image geopkg:

```bash
curl "http://localhost:8080/geoserver/wms?service=WMS&version=1.3.0&request=GetMap&layers=ws:layername&bbox=48.3956835,-123.43670607166865,48.5128362547052,-123.2539813&width=768&height=492&srs=EPSG%3A4326&styles=&format=geopkg" \
-o wms.gpkg
```

You can use `format=geopkg`, `format=geopackage`, or `format=gpkg` as the output format in the request. Use WMS `version=1.1.0`, or `version=1.3.0` to specify which WMS version to use, keeping in mind axis order for `bbox` differences.

!!! note
    Regardless of WMS axis order used for `bbox` the resulting GeoPackages always have the ordinates in X,Y (`EAST_NORTH`) order as required by the specification.

### WMS Format options

You can also add format options (`format_options=param1:value1;param2:value2;...`) to the request. With all default values, you will get a GeoPackage with PNG tiles of multiple resolutions. There will be a little more than 255 total tiles - all occupying the area in the request's bbox.

| Parameter    | Description                                                                                                                                                 |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| min_zoom | Grid Zoom level for tiles to start.<br>default: zoom level based on a single tile covering the bbox area. |
| max_zoom | Grid Zoom level for tiles to end.<br>default: zoom where there's >255 tiles in total in the geopkg (could be a bit more) |
| num_zooms | Number of zoom levels in the geopkg.<br>If present then `max_zoom = min_zoom + num_zooms` |
| format | Format for the image tiles in the geopkg.<br>default: PNG |
| tileset_name | Name of tile set ("layer") used in the geopkg.<br>default: based on the layer names given in the request ('_' separated) |
| min_column | First column number (from the gridset) to use.<br>default: use request bbox to determine which tiles to produce |
| max_column | Last column number (from the gridset) to use.<br>default: use request bbox to determine which tiles to produce |
| min_row | First row number (from the gridset) to use.<br>default: use request bbox to determine which tiles to produce |
| max_row | Last row number (from the gridset) to use.<br>default: use request bbox to determine which tiles to produce |
| gridset | Name of the gridset (from GWC GridSetBroker) to uses.<br>default: find based on request SRS |
| flipy | Do NOT set.<br>default: TRUE (required for GeoPackage - `The tile coordinate (0,0) always refers to the tile in the upper left corner of the tile matrix...`) |

## Concurrent Read Safety

GeoPackage files are SQLite databases. By default, SQLite acquires file-level locks even for
read operations to guard against concurrent writes by other processes. Under high concurrent WMS
or WFS load, multiple GeoServer worker threads can block inside the native SQLite locking code,
eventually causing request timeouts and 503 responses (see [GEOS-12094](https://osgeo-org.atlassian.net/browse/GEOS-12094)).

GeoTools' `GeoPkgDataStoreFactory` provides two parameters that address this:

| Parameter    | Description |
| ------------ | ----------- |
| `read_only`  | Opens the SQLite file in read-only mode. Prevents write operations and relaxes internal locking. |
| `immutable`  | Passes the `immutable=1` URI flag to SQLite, which completely bypasses all file locking. Use this when the GeoPackage file will never be modified while GeoServer is running. |

These parameters already appear in the **Data Store** configuration form in the GeoServer Admin UI.
For GeoPackage layers that are only ever served (never written to by GeoServer), enabling
`immutable` provides the best concurrent throughput:

1. In the GeoServer Admin UI, go to **Data** > **Stores** and open the GeoPackage store.
2. In the connection parameters form, set **immutable** to `true`.
3. Save the store. Existing layers are unaffected; no data migration is required.

!!! note
    `immutable=true` is safe only when no other process will modify the GeoPackage file while
    GeoServer is running. If the file may be updated (for example by an ETL process), use
    `read_only=true` instead, which permits shared-read access without exclusive locks but still
    allows SQLite's normal change-detection to operate.

!!! note
    GeoServer logs a WARNING at startup when a GeoPackage store has neither `read_only` nor
    `immutable` enabled, as a reminder to review this setting for production deployments.
