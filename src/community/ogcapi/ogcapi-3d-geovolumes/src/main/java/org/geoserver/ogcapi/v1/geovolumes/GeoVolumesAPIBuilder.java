/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.OpenAPIBuilder;

/** Builds the OpenAPI definition for the tiles service */
public class GeoVolumesAPIBuilder extends OpenAPIBuilder<GeoVolumesServiceInfo> {

    private final GeoVolumes geoVolumes;

    public GeoVolumesAPIBuilder(GeoVolumes geoVolumes) {
        super(
                GeoVolumesService.class,
                "openapi.yaml",
                "3D GeoVolumes API",
                GeoVolumesService.class);
        this.geoVolumes = geoVolumes;
    }

    @Override
    @SuppressWarnings("unchecked") // getSchema not generified
    public OpenAPI build(GeoVolumesServiceInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/collections", GeoVolumes.class);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("3D GeoVolumes specification")
                        .url("https://github.com/opengeospatial/ogcapi-3d-geovolumes"));

        List<String> validCollectionIds =
                geoVolumes.getCollections().stream()
                        .map(gv -> gv.getId())
                        .collect(Collectors.toList());
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("3d-containerID");
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }
}
