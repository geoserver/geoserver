/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;

/** Builds the OGC Features OpenAPI document */
public class FeaturesAPIBuilder extends org.geoserver.api.OpenAPIBuilder<WFSInfo> {

    public FeaturesAPIBuilder() {
        super(FeaturesAPIBuilder.class, "openapi.yaml", "Features 1.0 server", "ogc/features");
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param wfs The WFS configuration
     */
    public OpenAPI build(WFSInfo wfs) {
        OpenAPI api = super.build(wfs);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("WFS specification")
                        .url("https://github.com/opengeospatial/WFS_FES"));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionsDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items", FeatureCollectionResponse.class);
        declareGetResponseFormats(
                api,
                "/collections/{collectionId}/items/{featureId}",
                FeatureCollectionResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = wfs.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getFeatureTypes()
                        .stream()
                        .map(ft -> NCNameResourceCodec.encode(ft))
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        // provide actual values for limit
        Parameter limit = parameters.get("limit");
        BigDecimal limitMax;
        if (wfs.getMaxFeatures() > 0) {
            limitMax = BigDecimal.valueOf(wfs.getMaxFeatures());
        } else {
            limitMax = BigDecimal.valueOf(Integer.MAX_VALUE);
        }
        limit.getSchema().setMaximum(limitMax);
        // for the moment we don't have a setting for the default, keep it same as max
        limit.getSchema().setDefault(limitMax);

        return api;
    }
}
