/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import com.google.common.collect.Streams;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.OpenAPIBuilder;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

/** Builds the OpenAPI definition for the Images service */
public class ImagesAPIBuilder extends OpenAPIBuilder<ImagesServiceInfo> {

    static final Logger LOGGER = Logging.getLogger(ImagesAPIBuilder.class);

    private final GeoServer geoServer;

    public ImagesAPIBuilder(GeoServer geoServer) {
        super(ImagesServiceInfo.class, "openapi.yaml", "Images API", ImagesService.class);
        this.geoServer = geoServer;
    }

    @Override
    @SuppressWarnings("unchecked") // getSchema() not generified
    public OpenAPI build(ImagesServiceInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/collections", ImagesCollectionsDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}", ImagesCollectionDocument.class);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("Images specification")
                        .url(
                                "https://app.swaggerhub.com/apis/UAB-CREAF/ogc-api-images-opf-xml/1.0.0"));

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");

        boolean skipInvalid =
                geoServer.getGlobal().getResourceErrorHandling()
                        == ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
        List<String> validCollectionIds =
                Streams.stream(geoServer.getCatalog().getCoverages().iterator())
                        .filter(
                                c -> {
                                    try {
                                        return c.getGridCoverageReader(null, null)
                                                instanceof StructuredGridCoverage2DReader;
                                    } catch (IOException e) {
                                        if (skipInvalid) {
                                            LOGGER.log(Level.WARNING, "Skipping coverage  " + c);
                                            return false;
                                        } else {
                                            throw new APIException(
                                                    "InternalError",
                                                    "Failed to iterate over the coverages in the catalog",
                                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                                    e);
                                        }
                                    }
                                })
                        .map(c -> c.prefixedName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }
}
