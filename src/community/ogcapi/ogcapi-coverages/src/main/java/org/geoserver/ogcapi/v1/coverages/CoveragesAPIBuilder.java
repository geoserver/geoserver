/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.wcs.WCSInfo;

/** Builds the OGC Features OpenAPI document */
public class CoveragesAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<WCSInfo> {

    public CoveragesAPIBuilder() {
        super(
                CoveragesAPIBuilder.class,
                "openapi.yaml",
                "Coverages 1.0 server",
                CoveragesService.class);
    }

    /**
     * Build the document based on request, current WCS configuration, and list of available
     * extensions
     *
     * @param wcs The WCS configuration
     */
    @Override
    @SuppressWarnings("unchecked") // getSchema not generified
    public OpenAPI build(WCSInfo wcs) throws IOException {
        OpenAPI api = super.build(wcs);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("Coverages specification")
                        .url("https://github.com/opengeospatial/ogcapi-coverages"));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/coverage", CoveragesResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = wcs.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getCoverages().stream()
                        .map(ci -> ci.prefixedName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }
}
