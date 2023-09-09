/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.visitor.UniqueVisitor;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/** Builds the OGC Features OpenAPI document */
public class STACAPIBuilder extends org.geoserver.ogcapi.OpenAPIBuilder<OSEOInfo> {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private final OpenSearchAccessProvider accessProvider;

    public STACAPIBuilder(OpenSearchAccessProvider accessProvider) {
        super(STACAPIBuilder.class, "openapi.yaml", "STAC server", STACService.class);
        this.accessProvider = accessProvider;
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param service The Opensearch for EO configuration
     */
    @Override
    @SuppressWarnings("unchecked")
    public OpenAPI build(OSEOInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("STAC API specification")
                        .url("https://github.com/radiantearth/stac-api-spec"));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        // TODO: these needs to be adjusted once we have
        declareGetResponseFormats(api, "/collections", CollectionsResponse.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionResponse.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items", FeatureCollectionType.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items/{featureId}", FeatureCollectionType.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        List<String> validCollectionIds = getCollectionIds();
        collectionId.getSchema().setEnum(validCollectionIds);

        // provide actual values for limit
        Parameter limit = parameters.get("limit");
        BigDecimal limitMax;
        if (service.getMaximumRecordsPerPage() > 0) {
            limitMax = BigDecimal.valueOf(service.getMaximumRecordsPerPage());
        } else {
            limitMax = BigDecimal.valueOf(OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        }
        limit.getSchema().setMaximum(limitMax);
        int recordsPerpage = service.getRecordsPerPage();
        if (recordsPerpage <= 0) recordsPerpage = OSEOInfo.DEFAULT_RECORDS_PER_PAGE;
        limit.getSchema().setDefault(recordsPerpage);

        return api;
    }

    @SuppressWarnings("unchecked")
    private List<String> getCollectionIds() throws IOException {
        FeatureSource<FeatureType, Feature> fs =
                accessProvider.getOpenSearchAccess().getCollectionSource();
        PropertyName name =
                CommonFactoryFinder.getFilterFactory2()
                        .property(new NameImpl(fs.getSchema().getName().getNamespaceURI(), "name"));
        // remove disabled collections
        Query q = new Query();
        q.setFilter(FF.equals(FF.property(OpenSearchAccess.ENABLED), FF.literal(true)));
        UniqueVisitor visitor = new UniqueVisitor(name);
        fs.getFeatures(q).accepts(visitor, null);
        Set uniqueValues = visitor.getUnique();
        return (List<String>)
                uniqueValues.stream()
                        .map(
                                a -> {
                                    if (a instanceof Attribute) {
                                        return ((Attribute) a).getValue();
                                    } else {
                                        return a.toString();
                                    }
                                })
                        .sorted()
                        .collect(Collectors.toList());
    }
}
