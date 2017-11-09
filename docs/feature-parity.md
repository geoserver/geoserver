# GeoServices REST for GeoServer

Please reference http://www.esri.com/industries/landing-pages/geoservices/geoservices and http://resources.arcgis.com/en/help/arcgis-rest-api/ for authoritative information on the GeoServices API.  This document describes only the GeoServer extension.

# GeoServices REST Functionality

## Formats

While the Esri implementation of GeoServices supports multiple file formats, all responses from the GeoServer plugin use JSON (for example, browser-friendly HTML is not provided.)  The ``?f=json`` parameter specifying JSON is still required.

## Capabilities

* Catalog: yes
* Map:
  * All layers and tables. Supported.
  * Attachment. Not supported.
  * Attachment info. Not supported.
  * Dynamic Layer/Table. Not supported.
  * Estimate export tile size. Not supported.
  * Export map. Not supported.
  * Export tiles. Not supported.
  * Service extension. Not supported.
  * Feature (dynamic layer). Not supported.
  * Feature (layer). Not supported.
  * Find. Not supported.
  * Generate KML. Not supported.
  * Generate renderer (Dynamic layer). Not supported.
  * Generate renderer (Layer). Not supported.
  * HTML Popup (Dynamic layer). Not supported.
  * HTML Popup (Layer). Not supported.
  * Identify. Not supported.
  * Image. Not supported.
  * KML Image. Not supported.
  * Layer/Table. Supported.
  * Legend. Supported.
  * Map tile. Not supported.
  * Map service input. Not supported.
  * Map service job. Not supported.
  * Map service result. Not supported.
  * Query (Dynamic layer.) Not supported.
  * Query (Layer.) Supported.
  * Query related records (Dynamic layer.) Not supported.
  * Query related records (Layer.) Not supported.
  * WMTS. Not supported. (GeoServer does support this, but via GeoWebCache integration rather than the GeoServices extension.)
  * WMTS Capabilities. Not supported. (GeoServer does support this, but via GeoWebCache integration rather than the GeoServices extension.)
  * WMTS Tile. Not supported. (GeoServer does support this, but via GeoWebCache integration rather than the GeoServices extension.)

## Technical Limitations

### Features

While GeoServer supports fields of arbitrary type (determined by the data store) the GeoServices REST API constrains field types to a short list of types.

The GeoServer plugin maintains a mapping between these types and native Java classes in the FieldTypeEnum class.
Fields whose types are not translatable will be omitted from responses.

### Layer

GeoServer does not model layers the same way that they are presented in the GeoServices REST API.
GeoServer layers may have multiple associated styles, while GeoServices allows for one style per layer.
The GeoServer plugin uses the "default" style for the layer and ignores any alternative styles.
GeoServer also has no concept corresponding to the "sublayers" supported by GeoServices.

## Styles

GeoServer does not model styles the same way that they are presented in the GeoServices REST API.
GeoServer styles use the SLD format, which allows a wide variety of conditional styling, while "Renderers" in the GeoServices REST API are much more constrained.
The GeoServices plugin checks for SLD styles conforming to a small number of patterns and converts these to equivalent renderers, but when an SLD uses capabilities with no analog in GeoServices a simple default style will be advertised instead.
