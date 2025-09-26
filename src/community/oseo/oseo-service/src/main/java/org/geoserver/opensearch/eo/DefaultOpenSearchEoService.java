/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.ComplexFeatureAccessor.value;
import static org.geoserver.opensearch.eo.store.OpenSearchQueries.getProductProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.geoserver.catalog.Predicates;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.response.TemplatesProcessor;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Parameter;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.DataUtilities;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * Default implementation of {@link OpenSearchEoService}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultOpenSearchEoService implements OpenSearchEoService {

    static final Logger LOGGER = Logging.getLogger(DefaultOpenSearchEoService.class);

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /* Some mime types for quicklooks */
    private static String JPEG_MIME = "image/jpeg";

    private static String PNG_MIME = "image/png";

    private static String BINARY_MIME = "application/octet-stream ";
    private final FreemarkerTemplateSupport templates;

    GeoServer geoServer;

    OpenSearchAccessProvider accessProvider;

    public DefaultOpenSearchEoService(
            GeoServer geoServer, OpenSearchAccessProvider accessProvider, FreemarkerTemplateSupport templates) {
        this.geoServer = geoServer;
        this.accessProvider = accessProvider;
        this.templates = templates;
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

    @Override
    public List<Parameter<?>> getProductSearchParameters(final String parentId) throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());

        // product search for a given collection, figure out what parameters apply to it
        Feature match = getCollectionByParentIdentifier(parentId);
        Property sensorTypeProperty = match.getProperty(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "sensorType"));
        if (sensorTypeProperty == null || !(sensorTypeProperty.getValue() instanceof String)) {
            throw new OWS20Exception("Unknown or missing sensory type " + sensorTypeProperty);
        }
        // go to the classification
        String sensorType = (String) sensorTypeProperty.getValue();
        ProductClass collectionClass = null;
        try {
            collectionClass = ProductClass.getProductClassFromName(geoServer, sensorType);
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not understand sensor type " + sensorType + ", will only return generic product properties");
        }

        OpenSearchAccess access = getOpenSearchAccess();
        FeatureType productSchema = access.getProductSource().getSchema();
        searchParameters.addAll(getSearchParametersByClass(ProductClass.GENERIC, productSchema));
        if (collectionClass != null) {
            searchParameters.addAll(getSearchParametersByClass(collectionClass, productSchema));
        }

        return searchParameters;
    }

    /** Returns the complex feature representing a collection by parentId */
    private Feature getCollectionByParentIdentifier(final String parentId) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();

        // build the query
        final NameImpl identifier = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");
        final PropertyIsEqualTo filter = FF.equal(FF.property(identifier), FF.literal(parentId), true);
        Query query = new Query(collectionSource.getName().getLocalPart(), filter);
        FeatureCollection<FeatureType, Feature> features = collectionSource.getFeatures(query);

        // get the expected matching feature
        Feature match = DataUtilities.first(features);
        if (match == null) {
            throw new OWS20Exception("Unknown parentId '" + parentId + "'", OWSExceptionCode.InvalidParameterValue);
        } else {
            Property enabled = match.getProperty(OpenSearchAccess.ENABLED);
            if (enabled == null || !Boolean.TRUE.equals(enabled.getValue())) {
                throw new OWS20Exception(
                        "Parent '" + parentId + "' is not enabled", OWSExceptionCode.InvalidParameterValue);
            }
        }

        return match;
    }

    private List<Parameter<?>> getSearchParametersByClass(ProductClass pc, FeatureType productSchema) {
        List<Parameter<?>> result = new ArrayList<>();
        final String targetNamespace = pc.getNamespace();
        for (PropertyDescriptor pd : productSchema.getDescriptors()) {
            Name name = pd.getName();
            final String propertyNs = name.getNamespaceURI();
            if (targetNamespace.equals(propertyNs)) {
                Parameter parameter = new ParameterBuilder(
                                name.getLocalPart(), pd.getType().getBinding())
                        .prefix(pc.getPrefix())
                        .build();
                result.add(parameter);
            }
        }

        return result;
    }

    @Override
    public List<Parameter<?>> getCollectionSearchParameters() throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());
        searchParameters.addAll(getCollectionEoSearchParameters());
        return searchParameters;
    }

    private Collection<? extends Parameter<?>> getCollectionEoSearchParameters() throws IOException {
        final OpenSearchAccess osAccess = getOpenSearchAccess();
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        List<Parameter<?>> result = new ArrayList<>();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            final Class<?> type = pd.getType().getBinding();
            if (OpenSearchAccess.EO_NAMESPACE.equals(pd.getName().getNamespaceURI())
                    && !Geometry.class.isAssignableFrom(type)) {
                Parameter<?> parameter = new ParameterBuilder(pd.getName().getLocalPart(), type)
                        .prefix("eo")
                        .build();
                result.add(parameter);
            }
        }
        return result;
    }

    @Override
    public SearchResults search(SearchRequest request) throws IOException {
        // grab the right feature source for the request
        final OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> featureSource;
        Query resultsQuery = filterEnabled(request.getQuery());
        final String parentId = request.getParentIdentifier();
        if (parentId == null) {
            featureSource = access.getCollectionSource();
        } else {
            featureSource = access.getProductSource();
            // go get the collection property too
            resultsQuery.setProperties(getProductProperties(accessProvider.getOpenSearchAccess()));

            // need to determine if the collection is primary or virtual
            Feature collection = getCollectionByParentIdentifier(parentId);
            if (Boolean.FALSE.equals(value(collection, "primary"))) {
                // TODO: parse and integrate virtual collection filter
                throw new OWS20Exception("Virtual collection support not implemented yet");
            } else {
                // adding parent id filter for primary collections
                final PropertyName parentIdProperty =
                        FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "parentIdentifier"));
                PropertyIsEqualTo parentIdFilter = FF.equal(parentIdProperty, FF.literal(parentId), true);
                resultsQuery = new Query(resultsQuery);
                resultsQuery.setFilter(Predicates.and(resultsQuery.getFilter(), parentIdFilter));
            }
        }

        // count
        Integer totalResults = null;
        boolean nextPage;
        FeatureCollection<FeatureType, Feature> features;
        if (getService().isSkipNumberMatched()) {
            if (resultsQuery.getMaxFeatures() == Query.DEFAULT_MAX) {
                features = featureSource.getFeatures(resultsQuery);
                nextPage = false;
            } else {
                // get the items, plus one to check for next page
                Query nextQuery = new Query(resultsQuery);
                nextQuery.setMaxFeatures(resultsQuery.getMaxFeatures() + 1);
                features = featureSource.getFeatures(nextQuery);
                int returned = features.size();
                if (returned > resultsQuery.getMaxFeatures()) {
                    nextPage = true;
                    features = new MaxFeaturesFeatureCollection<>(features, resultsQuery.getMaxFeatures());
                } else {
                    nextPage = false;
                }
            }
        } else {
            Query countQuery = new Query(resultsQuery);
            countQuery.setMaxFeatures(Query.DEFAULT_MAX);
            countQuery.setStartIndex(null);
            totalResults = featureSource.getCount(countQuery);
            nextPage = totalResults
                    > (Optional.ofNullable(resultsQuery.getStartIndex()).orElse(0) + resultsQuery.getMaxFeatures());

            // get actual features
            if (resultsQuery.getMaxFeatures() == 0) {
                // pure count query
                features = new ListComplexFeatureCollection(featureSource.getSchema(), Collections.emptyList());
            } else {
                features = featureSource.getFeatures(resultsQuery);
            }
        }

        SearchResults results = new SearchResults(request, features, totalResults, nextPage);

        return results;
    }

    /**
     * Adds an "enabled" = "true" filter to the
     *
     * @param query
     * @return
     */
    private Query filterEnabled(Query query) {
        Query result = new Query(query);
        Filter filter = query.getFilter();
        PropertyIsEqualTo enabledFilter = FF.equals(FF.property(OpenSearchAccess.ENABLED), FF.literal(true));
        if (filter == null || Filter.INCLUDE.equals(filter)) {
            result.setFilter(enabledFilter);
        } else {
            result.setFilter(FF.and(filter, enabledFilter));
        }
        return result;
    }

    OpenSearchAccess getOpenSearchAccess() throws IOException {
        return accessProvider.getOpenSearchAccess();
    }

    private OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }

    @Override
    public MetadataResults metadata(MetadataRequest request) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();

        // build the query
        String id = request.getId();
        Query query = filterEnabled(queryByIdentifier(id));

        // run it
        FeatureSource<FeatureType, Feature> source;
        String parentId = request.getParentIdentifier();
        if (parentId == null) {
            // collection request
            source = access.getCollectionSource();
        } else {
            source = access.getProductSource();
            // get the collection property as well
            query.setProperties(getProductProperties(accessProvider.getOpenSearchAccess()));
        }
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(query);
        Feature feature = DataUtilities.first(features);
        if (feature == null) {
            String msg = "Could not locate the requested product for uid = " + id;
            if (parentId != null) msg += " and parentId = " + parentId;
            throw new OWS20Exception(msg, OWSExceptionCode.NotFound);
        }

        TemplatesProcessor processor = new TemplatesProcessor(templates);
        String templateName, collectionId;
        if (parentId == null) {
            templateName = "collection-metadata";
            collectionId =
                    (String) feature.getProperty(OpenSearchAccess.EO_IDENTIFIER).getValue();
        } else {
            templateName = "product-metadata";
            collectionId = parentId;
        }
        String metadata = processor.processTemplate(collectionId, templateName, feature);
        return new MetadataResults(request, metadata);
    }

    @Override
    public QuicklookResults quicklook(QuicklookRequest request) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();

        // check collection exists
        getCollectionByParentIdentifier(request.getParentIdentifier());

        // build the query
        Query query = queryByIdentifier(request.getId());
        query.setProperties(Arrays.asList(FF.property(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME)));

        // run it
        FeatureSource<FeatureType, Feature> source;
        if (request.getParentIdentifier() == null) {
            // collection request
            source = access.getCollectionSource();
        } else {
            source = access.getProductSource();
        }
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(query);
        if (features.isEmpty()) {
            query.setProperties(Collections.emptyList());
            features = source.getFeatures(query);
            if (features.isEmpty()) {
                String msg = "Could not locate the requested product for uid = " + request.getId();
                if (request.getParentIdentifier() != null) {
                    msg += " and parentId = " + request.getParentIdentifier();
                }
                throw new OWS20Exception(msg, OWSExceptionCode.NotFound);
            } else {
                throw new OWS20Exception(
                        "Could not locate the quicklook for the requested resource", OWSExceptionCode.NotFound);
            }
        }

        byte[] payload = (byte[]) getPropertyFromFirstFeature(features, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME);
        if (payload == null) {
            throw new OWS20Exception("Could not locate the quicklook for uid = "
                    + request.getId()
                    + " and parentId = "
                    + request.getParentIdentifier());
        }

        return new QuicklookResults(request, payload, guessImageMimeType(payload));
    }

    /** Used to guess the mime type of an encoded image until we start storing the mime in the db */
    public static String guessImageMimeType(byte[] payload) {
        // guesses jpeg and png by the magic number
        if (payload.length >= 4
                && //
                (payload[0] == (byte) 0xFF)
                && //
                (payload[1] == (byte) 0xD8)
                && //
                (payload[2] == (byte) 0xFF)
                && //
                (payload[3] == (byte) 0xE0)) {
            return JPEG_MIME;
        } else if (payload.length >= 8
                && //
                (payload[0] == (byte) 0x89)
                && //
                (payload[1] == (byte) 0x50)
                && //
                (payload[2] == (byte) 0x4E)
                && //
                (payload[3] == (byte) 0x47)
                && //
                (payload[4] == (byte) 0x0D)
                && //
                (payload[5] == (byte) 0x0A)
                && //
                (payload[6] == (byte) 0x1A)
                && //
                (payload[7] == (byte) 0x0A)) {
            return PNG_MIME;
        } else {
            return BINARY_MIME;
        }
    }

    private Object getPropertyFromFirstFeature(FeatureCollection<FeatureType, Feature> features, Name propertyName) {
        Feature feature = DataUtilities.first(features);
        Property property;
        Object value;
        if (feature == null
                || //
                ((property = feature.getProperty(propertyName)) == null)
                || //
                ((value = property.getValue()) == null)) {
            return null;
        }

        return value;
    }

    private Query queryByIdentifier(String identifier) {
        Query query = new Query();
        PropertyName idProperty = FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier"));
        final PropertyIsEqualTo idFilter = FF.equal(idProperty, FF.literal(identifier), true);
        query.setFilter(idFilter);
        return filterEnabled(query);
    }
}
