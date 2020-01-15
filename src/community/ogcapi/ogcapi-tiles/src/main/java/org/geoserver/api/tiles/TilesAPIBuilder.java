/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import com.google.common.collect.Streams;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.api.OpenAPIBuilder;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;

/** Builds the OpenAPI definition for the iles service */
public class TilesAPIBuilder extends OpenAPIBuilder<TilesServiceInfo> {

    private final GWC gwc;

    public TilesAPIBuilder(GWC gwc) {
        super(TilesServiceInfo.class, "openapi.yaml", "Tiles API", "ogc/tiles");
        this.gwc = gwc;
    }

    @Override
    public OpenAPI build(TilesServiceInfo service) {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/collections", TiledCollectionsDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}", TiledCollectionsDocument.class);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("Tiles specification")
                        .url("https://github.com/opengeospatial/OGC-API-Map-Tiles"));

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");

        List<String> validCollectionIds =
                Streams.stream(gwc.getTileLayers())
                        .map(
                                tl ->
                                        tl instanceof GeoServerTileLayer
                                                ? ((GeoServerTileLayer) tl).getContextualName()
                                                : tl.getName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }
}
