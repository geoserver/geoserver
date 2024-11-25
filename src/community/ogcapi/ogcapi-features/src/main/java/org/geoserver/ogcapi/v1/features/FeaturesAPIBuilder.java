/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.wfs.WFSInfo;

/** Builds the OGC Features OpenAPI document */
public class FeaturesAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<WFSInfo> {

    public FeaturesAPIBuilder() {
        super(
                FeaturesAPIBuilder.class,
                "openapi.yaml",
                "Features 1.0 server",
                FeatureService.class);
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param wfs The WFS configuration
     */
    @Override
    @SuppressWarnings("unchecked")
    public OpenAPI build(WFSInfo wfs) throws IOException {
        OpenAPI api = super.build(wfs);

        FeatureConformance features = FeatureConformance.configuration(wfs);
        CQL2Conformance cql2 = CQL2Conformance.configuration(wfs);
        ECQLConformance ecql = ECQLConformance.configuration(wfs);

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
        declareGetResponseFormats(api, "/collections/{collectionId}/items", FeaturesResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items/{featureId}", FeaturesResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = wfs.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getFeatureTypes().stream()
                        .filter(ft -> ft.isEnabled() && ft.isAdvertised())
                        .map(ft -> ft.prefixedName())
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        // list of valid filter-lang values
        Parameter filterLang = parameters.get("filter-lang");
        ArrayList<String> filterLangValues = new ArrayList<>(APIFilterParser.SUPPORTED_ENCODINGS);
        if (!cql2.text(wfs)) {
            filterLangValues.remove(APIFilterParser.CQL2_TEXT);
        }
        if (!cql2.json(wfs)) {
            filterLangValues.remove(APIFilterParser.CQL2_JSON);
        }
        if (!ecql.isEnabled(wfs)) {
            filterLangValues.remove(APIFilterParser.ECQL_TEXT);
        }
        filterLang.getSchema().setEnum(filterLangValues);

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
