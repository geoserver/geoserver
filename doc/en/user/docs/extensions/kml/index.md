# KML

This section documents the KML extension, which provides Google Earth (KML/KMZ) output support for GeoServer.

KML (Keyhole Markup Language) is a markup language for geographic visualization used by Earth browsers such as Google Earth and Google Maps. GeoServer integrates with these clients by exposing KML as a native output format. Any published data can take advantage of the visualization capabilities in KML-aware clients once the KML extension is installed.

To use this functionality, install the KML extension package ([kml](https://build.geoserver.org/geoserver/main/ext-latest/kml)) into the GeoServer `WEB-INF/lib` directory.

## KMZ output

Alongside raw KML, GeoServer can generate KMZ (compressed KML) responses by requesting the `application/vnd.google-earth.kmz` output format. The resulting KMZ package is a zip archive that contains the KML document and any bundled assets, such as icon PNG files stored inside an `images/` directory. See [Requesting KMZ output](quickstart.md#google_earth_kmz) for practical usage notes.

<div class="grid cards" markdown>

- [ExtensionsKmlOverview](overview.md)
- [ExtensionsKmlQuickstart](quickstart.md)
- [ExtensionsKmlKmlstyling](kmlstyling.md)
- [ExtensionsKmlTutorialsIndex](tutorials/index.md)
- [ExtensionsKmlFeaturesIndex](features/index.md)

</div>
