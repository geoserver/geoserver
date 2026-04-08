/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Adds a workspace filter to the queries/filters, if a workspace is set */
public class WorkspaceFeatureSource extends DecoratingSimpleFeatureSource {
    private WorkspaceInfo workspaceInfo;
    private final JDBCOpenSearchAccess openSearchAccess;

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    public static String WORKSPACES_FIELD = "workspaces";
    /** Key used to cache the list of current collections in the request context holder */
    public static String WS_COLLECTION_CACHE_KEY = "org.geoserver.os.workspaceCollections";

    /**
     * Constructor
     *
     * @param delegate the delegate feature source
     * @param workspaceInfo the workspace info
     * @param openSearchAccess the OpenSearchAccess
     */
    public WorkspaceFeatureSource(
            SimpleFeatureSource delegate, WorkspaceInfo workspaceInfo, JDBCOpenSearchAccess openSearchAccess) {
        super(delegate);
        this.workspaceInfo = workspaceInfo;

        this.openSearchAccess = openSearchAccess;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        query = appendWorkspaceToQuery(query);
        return delegate.getBounds(query);
    }

    @Override
    public int getCount(Query query) throws IOException {
        query = appendWorkspaceToQuery(query);
        return delegate.getCount(query);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return delegate.getFeatures(appendWorkspaceToFilter(filter));
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        query = appendWorkspaceToQuery(query);
        return delegate.getFeatures(query);
    }

    /**
     * The collection names can be queried over and over during a STAC/OS interaction, cache it at the request level,
     * since it depends only on the eventual workspace context
     *
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private Set<String> getCollectionNamesForWorkspace() throws IOException {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // no caching in this case
            return queryCollectionNamesForWorkspace();
        }
        // there's a request, check if the collection names have been computed already
        Object attribute = attributes.getAttribute(WS_COLLECTION_CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
        Set<String> result;
        if (attribute instanceof Set) {
            result = (Set<String>) attribute;
        } else {
            result = queryCollectionNamesForWorkspace();
            attributes.setAttribute(WS_COLLECTION_CACHE_KEY, result, RequestAttributes.SCOPE_REQUEST);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Set<String> queryCollectionNamesForWorkspace() throws IOException {
        SimpleFeatureCollection idFeatureCollection = getWorkspaceCollection();
        UniqueVisitor unique = new UniqueVisitor("eoIdentifier");
        unique.setPreserveOrder(true);
        idFeatureCollection.accepts(unique, null);
        Set<String> result = unique.getUnique();
        return result;
    }

    private SimpleFeatureCollection getWorkspaceCollection() throws IOException {
        // if global workspace, add check for null value in workspaces array
        Filter equalityFilter = FF.equals(FF.function("arrayhasnull", FF.property(WORKSPACES_FIELD)), FF.literal(true));
        // if not global workspace, add specific workspace filter
        if (workspaceInfo != null) {
            String workspace = workspaceInfo.getName();
            equalityFilter = FF.equals(FF.property(WORKSPACES_FIELD), FF.literal(workspace));
        }
        Query globalQuery = new Query();
        // if workspaces field is null we always return the collection,
        // otherwise we check for null value or the specific workspace
        globalQuery.setFilter(FF.or(FF.isNull(FF.property(WORKSPACES_FIELD)), equalityFilter));
        SimpleFeatureSource collectionSource =
                openSearchAccess.getDelegateStore().getFeatureSource(JDBCOpenSearchAccess.COLLECTION);
        return collectionSource.getFeatures(globalQuery);
    }

    private Filter appendWorkspaceToFilter(Filter filterIn) throws IOException {
        Set<String> collectionNames = getCollectionNamesForWorkspace();
        if (collectionNames != null && !collectionNames.isEmpty()) {
            if (delegate.getSchema().getTypeName().equals(JDBCOpenSearchAccess.COLLECTION)) {
                return appendWorkspaceNamesToFilter(filterIn, collectionNames, "eoIdentifier");
            } else if (delegate.getSchema().getTypeName().equals(JDBCOpenSearchAccess.PRODUCT)) {
                return appendWorkspaceNamesToFilter(filterIn, collectionNames, "eoParentIdentifier");
            }
        }
        return filterIn;
    }

    private Filter appendWorkspaceNamesToFilter(Filter filterIn, Set<String> collectionNames, String identifier) {
        if (collectionNames == null || collectionNames.isEmpty()) {
            return filterIn;
        }
        return FF.and(filterIn, collectionNamesToOrFilter(identifier, collectionNames));
    }

    private Filter collectionNamesToOrFilter(String identifier, Set<String> collectionNames) {
        List<Filter> orClauses = new ArrayList<>();
        PropertyName identifierProperty = FF.property(identifier);
        for (String collectionName : collectionNames) {
            orClauses.add(FF.equals(identifierProperty, FF.literal(collectionName)));
        }
        return FF.or(orClauses);
    }

    private Query appendWorkspaceToQuery(Query query) throws IOException {
        Filter filter = appendWorkspaceToFilter(query.getFilter());
        query = new Query(query);
        query.setFilter(filter);

        return query;
    }

    /**
     * Returns the delegate feature source
     *
     * @return the delegate feature source
     */
    public SimpleFeatureSource getDelegate() {
        return super.delegate;
    }
}
