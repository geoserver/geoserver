/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Default implementation of {@link OpenSearchEoService}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultOpenSearchEoService implements OpenSearchEoService {

    static final Logger LOGGER = Logging.getLogger(DefaultOpenSearchEoService.class);

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    GeoServer geoServer;

    public DefaultOpenSearchEoService(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public OSEODescription description(OSEODescriptionRequest request) throws IOException {
        final OSEOInfo service = getService();
        final String parentId = request.getParentId();

        List<Parameter<?>> searchParameters;
        if (parentId != null) {
            searchParameters = getProductSearchParameters(parentId);
        } else {
            searchParameters = getCollectionSearchParameters();
        }

        return new OSEODescription(request, service, geoServer.getGlobal(), searchParameters);
    }

    public List<Parameter<?>> getProductSearchParameters(final String parentId) throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());

        // product search for a given collection, figure out what parameters apply to it
        OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();
        Feature match = getCollectionByParentId(parentId, collectionSource);
        if (match == null) {
            throw new OWS20Exception("Unknown parentId '" + parentId + "'",
                    OWSExceptionCode.InvalidParameterValue);
        }
        Property sensorTypeProperty = match
                .getProperty(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "sensorType"));
        if (sensorTypeProperty == null || !(sensorTypeProperty.getValue() instanceof String)) {
            throw new OWS20Exception("Unknown or missing sensory type " + sensorTypeProperty);
        }
        // go to the classification
        String sensorType = (String) sensorTypeProperty.getValue();
        ProductClass collectionClass = null;
        try {
            collectionClass = OpenSearchAccess.ProductClass.valueOf(sensorType);
        } catch (Exception e) {
            LOGGER.warning("Could not understand sensor type " + sensorType
                    + ", will only return generic product properties");
        }

        FeatureType productSchema = access.getProductSource().getSchema();
        searchParameters.addAll(getSearchParametersByClass(ProductClass.EO_GENERIC, productSchema));
        if(collectionClass != null) {
            searchParameters.addAll(getSearchParametersByClass(collectionClass, productSchema));
        }

        return searchParameters;
    }

    private List<Parameter<?>> getSearchParametersByClass(ProductClass pc,
            FeatureType productSchema) {
        List<Parameter<?>> result = new ArrayList<>();
        final String targetNamespace = pc.getNamespace();
        for (PropertyDescriptor pd : productSchema.getDescriptors()) {
            Name name = pd.getName();
            final String propertyNs = name.getNamespaceURI();
            if (targetNamespace.equals(propertyNs)) {
                Parameter parameter = new ParameterBuilder(name.getLocalPart(),
                        pd.getType().getBinding()).prefix(pc.getPrefix()).build();
                result.add(parameter);
            }
        }

        return result;
    }

    private Feature getCollectionByParentId(final String parentId,
            final FeatureSource<FeatureType, Feature> collectionSource) throws IOException {
        final NameImpl identifier = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");
        final PropertyIsEqualTo filter = FF.equal(FF.property(identifier), FF.literal(parentId),
                true);
        Query query = new Query(collectionSource.getName().getLocalPart(), filter);
        FeatureCollection<FeatureType, Feature> features = collectionSource.getFeatures(query);
        Feature match = DataUtilities.first(features);
        return match;
    }

    public List<Parameter<?>> getCollectionSearchParameters() throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());
        searchParameters.addAll(getCollectionEoSearchParameters());
        return searchParameters;
    }

    private Collection<? extends Parameter<?>> getCollectionEoSearchParameters()
            throws IOException {
        final OpenSearchAccess osAccess = getOpenSearchAccess();
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        List<Parameter<?>> result = new ArrayList<>();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            final Class<?> type = pd.getType().getBinding();
            if (OpenSearchAccess.EO_NAMESPACE.equals(pd.getName().getNamespaceURI())
                    && !Geometry.class.isAssignableFrom(type)) {
                Parameter<?> parameter = new ParameterBuilder(pd.getName().getLocalPart(), type)
                        .prefix("eo").build();
                result.add(parameter);
            }
        }
        return result;
    }

    @Override
    public SearchResults search(SearchRequest request) throws IOException {
        if (request.getParentId() != null) {
            throw new OWS20Exception("Product search not implemented yet");
        }

        // feature request
        final OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();

        // count
        final Query resultsQuery = request.getQuery();
        Query countQuery = new Query(resultsQuery);
        countQuery.setMaxFeatures(Query.DEFAULT_MAX);
        countQuery.setStartIndex(null);
        int totalResults = collectionSource.getCount(countQuery);

        // get actual features
        FeatureCollection features = collectionSource.getFeatures(resultsQuery);
        SearchResults results = new SearchResults(request, features, totalResults);

        return results;
    }

    OpenSearchAccess getOpenSearchAccess() throws IOException {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        if (openSearchAccessStoreId == null) {
            throw new OWS20Exception("OpenSearchAccess is not configured in the"
                    + " OpenSearch for EO panel, please do so");
        }
        DataStoreInfo dataStore = this.geoServer.getCatalog().getDataStore(openSearchAccessStoreId);
        if (dataStore == null) {
            throw new OWS20Exception("Could not locate OpenSearch data access with identifier "
                    + openSearchAccessStoreId
                    + ", please correct the configuration in the OpenSearch for EO panel");
        }

        DataAccess result = dataStore.getDataStore(null);
        if (result == null) {
            throw new OWS20Exception("Failed to locate OpenSearch data access with identifier "
                    + openSearchAccessStoreId
                    + ", please correct the configuration in the OpenSearch for EO panel");
        } else if (!(result instanceof OpenSearchAccess)) {
            throw new OWS20Exception("Data access with identifier " + openSearchAccessStoreId
                    + " does not point to a valid OpenSearchDataAccess, "
                    + "please correct the configuration in the OpenSearch for EO panel");
        }

        return (OpenSearchAccess) result;
    }

    private OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }
}
