/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.OpenAPIBuilder;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;

public class DGGSAPIBuilder extends OpenAPIBuilder<DGGSInfo> {

    public DGGSAPIBuilder() {
        super(DGGSAPIBuilder.class, "openapi.yaml", "DGGS 1.0 server", DGGSService.class);
    }

    @Override
    @SuppressWarnings("unchecked") // getSchema not generified
    public OpenAPI build(DGGSInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(
                api, "/collections", org.geoserver.ogcapi.v1.features.CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}/zones", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/neighbors", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/parents", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/children", FeaturesResponse.class);
        declareGetResponseFormats(api, "/collections/{collectionId}/point", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/polygon", FeaturesResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = service.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getFeatureTypes().stream()
                        .filter(ft -> DGGSService.isDGGSType(ft))
                        .map(ft -> ft.prefixedName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }
}
