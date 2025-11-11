/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.security.EOAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.security.decorators.DecoratingDataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * An OpenSearchAccess implementation that wraps another one and applies security rules on top of it. Assumes that the
 * caller will not create an instance of this class for the administrators, won't try to check for admins here.
 */
public class SecuredOpenSearchAccess extends DecoratingDataAccess<FeatureType, Feature> implements OpenSearchAccess {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private final OpenSearchAccess delegate;
    private final GeoServer gs;

    public SecuredOpenSearchAccess(OpenSearchAccess delegate, GeoServer gs) {
        super(delegate);
        this.delegate = delegate;
        this.gs = gs;
    }

    /**
     * Returns the security filter for collections, that is the AND of the negation of all collection filters that do
     * not match the current user roles.
     */
    private Filter getCollectionFilter() {
        OSEOInfo oseoInfo = gs.getService(OSEOInfo.class);
        List<EOCollectionAccessLimitInfo> collectionLimits = oseoInfo.getCollectionLimits();
        Set<String> roles = getUserRoles();
        // figure out the collections rules that are not allowed to the current user
        List<Filter> filters = collectionLimits.stream()
                .filter(limit -> !intersects(limit.getRoles(), roles))
                .map(EOAccessLimitInfo::getCQLFilter)
                .map(SecuredOpenSearchAccess::parseCQL)
                .map(f -> (Filter) FF.not(f))
                .toList();

        return mergeFilters(filters);
    }

    /**
     * Returns the security filter for products, that is the AND of the negation of all product filters that do not
     * match the current user roles.
     */
    private Filter getProductFilter() throws IOException {
        OSEOInfo oseoInfo = gs.getService(OSEOInfo.class);
        List<EOProductAccessLimitInfo> productLimits = oseoInfo.getProductLimits();
        Set<String> roles = getUserRoles();
        // figure out the product rules that are not allowed to the current user
        List<Filter> filters = productLimits.stream()
                .filter(limit -> !intersects(limit.getRoles(), roles))
                .map(SecuredOpenSearchAccess::buildProductFilter)
                .map(f -> (Filter) FF.not(f))
                .collect(Collectors.toList());

        // restrict on the collections, if any such restriction exists
        Set<String> collectionIdenfiers = getAllowedCollectionsIdentifiers();
        if (collectionIdenfiers != null) {
            if (collectionIdenfiers.isEmpty()) {
                // no collections allowed at all
                return Filter.EXCLUDE;
            }
            Expression[] inArguments = Stream.concat(
                            Stream.of(FF.property("eo:parentIdentifier")),
                            collectionIdenfiers.stream().map(FF::literal))
                    .toArray(Expression[]::new);
            PropertyIsEqualTo collectionFilter = FF.equal(FF.function("in", inArguments), FF.literal(true), false);
            filters.add(collectionFilter);
        }

        return mergeFilters(filters);
    }

    private Filter getGranulesFilter(String typeName) throws IOException {
        String collection = getGranuleCollectionName(typeName);

        Set<String> collectionIdenfiers = getAllowedCollectionsIdentifiers();
        if (collectionIdenfiers != null && !collectionIdenfiers.contains(collection)) {
            return Filter.EXCLUDE;
        }

        // gather the product limits for the collection
        OSEOInfo oseoInfo = gs.getService(OSEOInfo.class);
        List<EOProductAccessLimitInfo> productLimits = oseoInfo.getProductLimits();
        Set<String> roles = getUserRoles();
        // figure out the product rules that are not allowed to the current user
        List<Filter> filters = productLimits.stream()
                .filter(limit -> !intersects(limit.getRoles(), roles) && collection.equals(limit.getCollection()))
                .map(limit -> (Filter) FF.not(parseCQL(limit.getCQLFilter())))
                .collect(Collectors.toList());
        Filter filter = mergeFilters(filters);

        // map back to source properties if using JDBC
        if (delegate instanceof JDBCOpenSearchAccess) {
            JDBCOpenSearchAccess jdbcDelegate = (JDBCOpenSearchAccess) delegate;
            SourcePropertyMapper productPropertyMapper = jdbcDelegate.getProductPropertyMapper();
            MappingFilterVisitor visitor = new MappingFilterVisitor(productPropertyMapper);
            filter = (Filter) filter.accept(visitor, null);
        }

        return filter;
    }

    /** Extract collection from type name */
    private static String getGranuleCollectionName(String typeName) {
        int idx = typeName.lastIndexOf(OpenSearchAccess.BAND_LAYER_SEPARATOR);
        String collection;
        // the two parts must be non-empty in order to have a valid combination
        if (idx > 1 && idx < (typeName.length() - 3)) {
            collection = typeName.substring(0, idx);
        } else {
            collection = typeName;
        }
        return collection;
    }

    /**
     * Returns the identifiers of the collections that are allowed to the current user, according to the collection
     * access limits, or null if all collections are allowed.
     *
     * @return the allowed collection identifiers (empty if none is allowed), or null if all are allowed
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private Set<String> getAllowedCollectionsIdentifiers() throws IOException {
        Filter collectionFilter = getCollectionFilter();
        if (collectionFilter == Filter.INCLUDE) {
            return null;
        }
        FeatureSource<FeatureType, Feature> collectionSource = delegate.getCollectionSource();
        UniqueVisitor uv = new UniqueVisitor(FF.property("eo:identifier"));
        collectionSource.getFeatures(collectionFilter).accepts(uv, null);
        return (Set<String>) uv.getResult().toSet();
    }

    /**
     * Merges a list of filters into a single one, that is an AND between them. If the list is empty, returns
     * Filter.INCLUDE, if it contains a single element, returns it.
     */
    private static Filter mergeFilters(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Filter.INCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return SimplifyingFilterVisitor.simplify(FF.and(filters));
        }
    }

    /**
     * Builds a product filter from a product access limit definition, that is an AND between the collection identifier
     * and the CQL filter
     */
    private static And buildProductFilter(EOProductAccessLimitInfo limit) {
        return FF.and(
                FF.equal(FF.property("eo:parentIdentifier"), FF.literal(limit.getCollection()), false),
                parseCQL(limit.getCQLFilter()));
    }

    /** Returns the roles of the current user */
    private static Set<String> getUserRoles() {
        Authentication authentication = Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .orElse(null);
        if (authentication == null) return Set.of();

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /** Checks if any roles in roles1 is also in roles2 */
    private boolean intersects(List<String> roles1, Set<String> roles2) {
        return roles1.stream().anyMatch(roles2::contains);
    }

    /** Parses a CQL filter, rethrowing any exception as a runtime one */
    private static Filter parseCQL(String s) {
        try {
            return ECQL.toFilter(s);
        } catch (CQLException e) {
            throw new RuntimeException("Failed to parse CQL filter: " + s, e);
        }
    }

    @Override
    public FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException {
        Filter collectionFilter = getCollectionFilter();
        FeatureSource<FeatureType, Feature> collectionSource = delegate.getCollectionSource();
        if (collectionFilter == Filter.INCLUDE) {
            return collectionSource;
        } else {
            return new FilteredFeatureSource(collectionSource, collectionFilter);
        }
    }

    @Override
    public FeatureSource<FeatureType, Feature> getProductSource() throws IOException {
        Filter productFilter = getProductFilter();
        FeatureSource<FeatureType, Feature> productSource = delegate.getProductSource();
        if (productFilter == Filter.INCLUDE) {
            return productSource;
        } else {
            return new FilteredFeatureSource(productSource, productFilter);
        }
    }

    private SimpleFeatureSource getGranuleSource(SimpleFeatureSource featureSource) throws IOException {
        Filter granulesFilter = getGranulesFilter(featureSource.getSchema().getTypeName());
        // need to map back the properties to source ones if using JDBC

        if (granulesFilter == Filter.INCLUDE) {
            return featureSource;
        } else {
            try {
                return DataUtilities.createView(
                        featureSource, new Query(featureSource.getSchema().getTypeName(), granulesFilter));
            } catch (SchemaException e) {
                // should only happen with property selection in the query, which we don't do here
                throw new RuntimeException("Unexpected schema exception when creating granule view", e);
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        // check for collection / product special names
        Name collectionName = delegate.getName(JDBCOpenSearchAccess.COLLECTION);
        if (typeName.equals(collectionName)) {
            return getCollectionSource();
        }
        Name productName = delegate.getName(JDBCOpenSearchAccess.PRODUCT);
        if (typeName.equals(productName)) {
            return getProductSource();
        }
        // the others are granules for a specific collection
        FeatureSource fs = delegate.getFeatureSource(typeName);
        if (fs instanceof SimpleFeatureSource) {
            return (FeatureSource) getGranuleSource((SimpleFeatureSource) fs);
        }
        return fs;
    }

    @Override
    public void updateIndexes(String collection, List<Indexable> indexables) throws IOException {
        delegate.updateIndexes(collection, indexables);
    }

    @Override
    public List<String> getIndexNames(String tableName) throws IOException {
        return delegate.getIndexNames(tableName);
    }

    @Override
    public SimpleFeatureSource getGranules(String collectionId, String productId) throws IOException {
        // not securing, it's only used by the REST API, thus admin access only
        return delegate.getGranules(collectionId, productId);
    }

    @Override
    public SimpleFeatureType getCollectionLayerSchema() throws IOException {
        return delegate.getCollectionLayerSchema();
    }

    @Override
    public SimpleFeatureType getOGCLinksSchema() throws IOException {
        return delegate.getOGCLinksSchema();
    }

    @Override
    public String getNamespaceURI() {
        return delegate.getNamespaceURI();
    }

    @Override
    public Name getName(String localPart) {
        return delegate.getName(localPart);
    }

    @Override
    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {
        delegate.createSchema(featureType);
    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {
        delegate.updateSchema(typeName, featureType);
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        delegate.removeSchema(typeName);
    }

    @Override
    public List<Name> getNames() throws IOException {
        return delegate.getNames();
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        return delegate.getSchema(name);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
