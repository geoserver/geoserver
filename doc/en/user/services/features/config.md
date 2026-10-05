# Configuration of OGC API - Features module

The service operates as an additional protocol for sharing vector data alongside Web Feature Service.

## Service configuration

The service is configured using:

 -  The existing [Web Feature Service (WFS)](../wfs/index.md) settings to define title, abstract, and output formats.

    This is why the service page is titled `GeoServer Web Feature Service` by default.

 -  Contact information defined in [Contact Information](../../configuration/contact.md).

 -  Extra links can be added on a per-service or per-collection basis as indicated in [OGC API Service Configuration](../../configuration/ogc-api-services/index.md).

## Feature Service Conformances

The OGC API Feature Service is modular, with supported functionality listed in a `conformance` document.

 -  The OpenAPI service description is mandatory and may not be disabled.

 -  The HTML and GeoJSON output formats are built-in and may not be disabled.

Use the provided [Conformance tables](../../configuration/ogc-api-services/index.md#conformance) to manage each group of standards:

 -  OGC API Feature conformances.

    ![](img/feature-service-configuration.png)  
    *Feature Service Configuration*

 -  CQL2 Filter conformances.

    Both the Text and JSON formats for CQL2 are available and may be enabled or disabled.

    The remaining conformances describe the CQL2 capabilities implemented by GeoServer, and are only included when CQL2 Text or CQL2 JSON is enabled.

    ![](img/cql2-configuration.png)  
    *CQL2 Filter configuration*

 -  Control of ECQL Filter conformances

    ![](img/ecql-configuration.png)  
    *ECQL Filter configuration*

 -  The enabled filter languages are listed in the `filter-lang` parameter of the API document, and the first one is
    its default. A request using any other language gets a `400` status.
  
    With every language disabled the filter parameters are not available at all: the filter conformance classes are no longer included in the `conformance` document, the parameters are removed from the API document, and a `filter` sent anyway is ignored.
  
    In the tables this shows as **Filter** and **Features Filter** in light gray when every filter language is disabled. **Features Filter** also needs **Filter**.

    ![](img/features-filter-dependency.png)  
    *Filter and Features Filter require a filter language to be enabled*

For more information see [Status](status.md) and [Conformance table](../../configuration/ogc-api-services/index.md#conformance).
