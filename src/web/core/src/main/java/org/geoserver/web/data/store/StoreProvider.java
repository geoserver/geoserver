/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.factory.CommonFactoryFinder;

/** Data providers for the {@link StorePanel} */
@SuppressWarnings("serial")
public class StoreProvider extends GeoServerDataProvider<StoreInfo> {

    static final Property<StoreInfo> DATA_TYPE = new AbstractProperty<>("datatype") {

        @Override
        public IModel<String> getModel(final IModel<StoreInfo> itemModel) {
            return () -> {
                StoreInfo si = itemModel.getObject();
                return (String) getPropertyValue(si);
            };
        }

        @Override
        public Object getPropertyValue(StoreInfo item) {
            if (item instanceof DataStoreInfo) return "vector";
            else return "raster";
        }
    };

    static final Property<StoreInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace.name");

    static final Property<StoreInfo> NAME = new BeanProperty<>("name", "name");

    static final Property<StoreInfo> TYPE = new AbstractProperty<>("type") {

        @Override
        public Object getPropertyValue(StoreInfo item) {
            String type = item.getType();
            if (type != null) {
                return type;
            }
            try {
                ResourcePool resourcePool = item.getCatalog().getResourcePool();
                if (item instanceof DataStoreInfo) {
                    DataStoreInfo dsInfo = (DataStoreInfo) item;
                    DataAccessFactory factory = resourcePool.getDataStoreFactory(dsInfo);
                    if (factory != null) {
                        return factory.getDisplayName();
                    }
                } else if (item instanceof CoverageStoreInfo) {
                    Format format = resourcePool.getGridCoverageFormat((CoverageStoreInfo) item);
                    if (format != null) {
                        return format.getName();
                    }
                }
            } catch (Exception e) {
                // fine, we tried
            }
            return "?";
        }
    };

    static final Property<StoreInfo> ENABLED = new BeanProperty<>("enabled", "enabled");

    static final Property<StoreInfo> MODIFIED_TIMESTAMP = new BeanProperty<>("datemodfied", "dateModified");

    static final Property<StoreInfo> CREATED_TIMESTAMP = new BeanProperty<>("datecreated", "dateCreated");

    static final List<Property<StoreInfo>> PROPERTIES = Arrays.asList(DATA_TYPE, WORKSPACE, NAME, TYPE, ENABLED);

    WorkspaceInfo workspace;

    public StoreProvider() {
        this(null);
    }

    public StoreProvider(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    @Override
    protected List<StoreInfo> getItems() {
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<StoreInfo>> getProperties() {
        List<Property<StoreInfo>> modifiedPropertiesList =
                PROPERTIES.stream().map(c -> c).collect(Collectors.toList());
        // check geoserver properties
        if (GeoServerApplication.get().getGeoServer().getSettings().isShowCreatedTimeColumnsInAdminList())
            modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (GeoServerApplication.get().getGeoServer().getSettings().isShowModifiedTimeColumnsInAdminList())
            modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        return modifiedPropertiesList;
    }

    @Override
    protected Comparator<StoreInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }

    @Override
    public IModel<StoreInfo> newModel(StoreInfo object) {
        return new StoreInfoDetachableModel(object);
    }

    /** A StoreInfo detachable model that holds the store id to retrieve it on demand from the catalog */
    static class StoreInfoDetachableModel extends LoadableDetachableModel<StoreInfo> {

        private static final long serialVersionUID = -6829878983583733186L;

        String id;

        public StoreInfoDetachableModel(StoreInfo store) {
            super(store);
            this.id = store.getId();
        }

        @Override
        protected StoreInfo load() {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            return catalog.getStore(id, StoreInfo.class);
        }
    }

    @Override
    public long size() {
        Filter filter = getFilter();
        filter = getWorkspaceFilter(filter);
        int count = getCatalog().count(StoreInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        filter = getWorkspaceFilter(filter);
        int count = getCatalog().count(StoreInfo.class, filter);
        return count;
    }

    @Override
    public Iterator<StoreInfo> iterator(final long first, final long count) {
        Iterator<StoreInfo> iterator = filteredItems(first, count);
        if (iterator instanceof CloseableIterator) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            try {
                return Lists.newArrayList(iterator).iterator();
            } finally {
                CloseableIteratorAdapter.close(iterator);
            }
        } else {
            return iterator;
        }
    }

    /** Returns the requested page of layer objects after applying any keyword filtering set on the page */
    private Iterator<StoreInfo> filteredItems(long first, long count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<StoreInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty = ((BeanProperty<StoreInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            } else if (property == ENABLED) {
                sortOrder = sortBy("enabled", sort.isAscending());
            } else if (property == TYPE) {
                sortOrder = sortBy("type", sort.isAscending());
            }
        } else {
            sortOrder = sortBy("name", true);
        }

        final Filter filter = getWorkspaceFilter(getFilter());
        // our already filtered and closeable iterator
        Iterator<StoreInfo> items = catalog.list(StoreInfo.class, filter, (int) first, (int) count, sortOrder);

        return items;
    }

    private Filter getWorkspaceFilter(Filter filter) {
        // Filter by workspace if present
        if (workspace != null) {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            Filter workspaceFilter = ff.equal(ff.property("workspace.id"), ff.literal(workspace.getId()));
            filter = ff.and(filter, workspaceFilter);
        }
        return filter;
    }
}
