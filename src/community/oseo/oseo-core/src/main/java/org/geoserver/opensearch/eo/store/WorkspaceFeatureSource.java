/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** Adds a workspace filter to the queries/filters, if a workspace is set */
public class WorkspaceFeatureSource extends DecoratingSimpleFeatureSource {
    private WorkspaceInfo workspaceInfo;
    private final JDBCOpenSearchAccess openSearchAccess;

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    public static String WORKSPACES_FIELD = "workspaces";

    /**
     * Constructor
     *
     * @param delegate the delegate feature source
     * @param workspaceInfo the workspace info
     * @param openSearchAccess the OpenSearchAccess
     */
    public WorkspaceFeatureSource(
            SimpleFeatureSource delegate,
            WorkspaceInfo workspaceInfo,
            JDBCOpenSearchAccess openSearchAccess) {
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

    private Set<FeatureId> getCollectionIdsForWorkspace() throws IOException {
        Set<FeatureId> ids = new LinkedHashSet<>();
        SimpleFeatureCollection idFeatureCollection = getWorkspaceCollection();
        idFeatureCollection.accepts(f -> ids.add(f.getIdentifier()), null);
        return ids;
    }

    private Set<String> getCollectionNamesForWorkspace() throws IOException {
        String workspace = null;
        Set<String> names = new LinkedHashSet<>();
        SimpleFeatureCollection idFeatureCollection = getWorkspaceCollection();
        idFeatureCollection.accepts(
                f -> names.add(f.getProperty("eoIdentifier").getValue().toString()), null);
        return names;
    }

    private SimpleFeatureCollection getWorkspaceCollection() throws IOException {
        // if global workspace, add check for null value in workspaces array
        Filter equalityFilter =
                FF.equals(
                        FF.function("arrayhasnull", FF.property(WORKSPACES_FIELD)),
                        FF.literal(true));
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
                openSearchAccess
                        .getDelegateStore()
                        .getFeatureSource(JDBCOpenSearchAccess.COLLECTION);
        return collectionSource.getFeatures(globalQuery);
    }

    private Filter appendWorkspaceToFilter(Filter filterIn) throws IOException {
        if (delegate.getSchema().getTypeName().equals(JDBCOpenSearchAccess.COLLECTION)) {
            Set<FeatureId> collectionIds = getCollectionIdsForWorkspace();
            if (collectionIds != null && !collectionIds.isEmpty()) {
                return appendWorkspaceIdsToFilter(filterIn, collectionIds);
            }
        } else if (delegate.getSchema().getTypeName().equals(JDBCOpenSearchAccess.PRODUCT)) {
            Set<String> collectionNames = getCollectionNamesForWorkspace();
            if (collectionNames != null && !collectionNames.isEmpty()) {
                return appendWorkspaceNamesToFilter(filterIn, collectionNames);
            }
        }
        return filterIn;
    }

    private Filter appendWorkspaceIdsToFilter(Filter filterIn, Set<FeatureId> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return Filter.EXCLUDE;
        }
        return FF.and(filterIn, FF.id(collectionIds));
    }

    private Filter appendWorkspaceNamesToFilter(Filter filterIn, Set<String> collectionNames) {
        if (collectionNames == null || collectionNames.isEmpty()) {
            return filterIn;
        }
        return FF.and(filterIn, collectionNamesToOrFilter(collectionNames));
    }

    private Filter collectionNamesToOrFilter(Set<String> collectionNames) {
        List<Filter> orClauses = new ArrayList<>();
        for (String collectionName : collectionNames) {
            orClauses.add(FF.equals(FF.property("eoParentIdentifier"), FF.literal(collectionName)));
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
