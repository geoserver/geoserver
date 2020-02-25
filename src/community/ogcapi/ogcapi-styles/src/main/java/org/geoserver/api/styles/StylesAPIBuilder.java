/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.api.OpenAPIBuilder;
import org.geoserver.catalog.Catalog;

/** Builds the OpenAPI definition for the styles service */
public class StylesAPIBuilder extends OpenAPIBuilder<StylesServiceInfo> {

    public StylesAPIBuilder() {
        super(StylesAPIBuilder.class, "openapi.yaml", "Style API", "ogc/styles");
    }

    @Override
    public OpenAPI build(StylesServiceInfo service) {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/styles", StylesDocument.class);
        declareGetResponseFormats(api, "/styles/{styleId}", StylesDocument.class);

        // for the time being, remove extensions not yet implemented
        api.getPaths().remove("/resources");

        // provide a list of valid values for styleId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter styleId = parameters.get("styleId");
        Catalog catalog = service.getGeoServer().getCatalog();
        List<String> validStyleIds =
                catalog.getStyles()
                        .stream()
                        .map(si -> si.prefixedName())
                        .collect(Collectors.toList());
        styleId.getSchema().setEnum(validStyleIds);

        return api;
    }
}
