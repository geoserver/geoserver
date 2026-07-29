# Configuration of OGC API - Features module

The service operates as an additional protocol for sharing vector data alongside Web Feature Service.

## Service configuration

The service is configured using:

- The existing [Web Feature Service (WFS)](../wfs/index.md) settings to define title, abstract, and output formats.

  This is why the service page is titled `GeoServer Web Feature Service` by default.

- Contact information defined in [Contact Information](../../configuration/contact.md).

- Extra links can be added on a per-service or per-collection basis as indicated in [OGC API Service Configuration](../../configuration/ogc-api-services/index.md).

## Feature Service conformances

The OGC API Feature Service is modular, allowing you to enable/disable the functionality you wish to include.

- By default stable Standards and Community Standards are enabled. If WFS is strict, only official Standards are enabled and community standards are disabled.

- The OpenAPI service description is mandatory and may not be disabled.

- The HTML and GeoJSON output formats are built-in and may not be disabled.

  ![](img/feature-service-configuration.png)

  *Feature Service Configuration*

- CQL2 Filter conformances.

  Both the Text and JSON formats for CQL2 are available and may be enabled or disabled.

  The remaining conformances reflect the built-in CQL2 implementation and may not be edited. The conformances marked enabled have been implemented, and the disabled conformances have not yet been implemented.

  ![](img/cql2-configuration.png)

  *CQL2 Filter configuration*

- Control of ECQL Filter conformances

  ![](img/ecql-configuration.png)

  *ECQL Filter configuration*

- The enabled filter languages are listed in the `filter-lang` parameter of the API document, and the first one is
  its default. A request using any other language gets a `400` status. With every language disabled the filter
  parameters are not available at all: the filter conformance classes disappear from the `conformance` document,
  the parameters are removed from the API document, and a `filter` sent anyway is ignored.

For more information see [Status](status.md).
