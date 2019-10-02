/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geoserver.catalog.Predicates.*;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Provides a filtered, sorted view over the catalog layers.
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class PreviewLayerProvider extends GeoServerDataProvider<PreviewLayer> {

    public static final long DEFAULT_CACHE_TIME = 1;

    public static final String KEY_SIZE = "key.size";

    public static final String KEY_FULL_SIZE = "key.fullsize";

    private final Cache<String, Integer> cache;

    private SizeCallable sizeCaller;

    private FullSizeCallable fullSizeCaller;

    public PreviewLayerProvider() {
        super();
        // Initialization of an inner cache in order to avoid to calculate two times
        // the size() method in a time minor than a second
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

        cache = builder.expireAfterWrite(DEFAULT_CACHE_TIME, TimeUnit.SECONDS).build();
        // Callable which internally calls the size method
        sizeCaller = new SizeCallable();
        // Callable which internally calls the fullSize() method
        fullSizeCaller = new FullSizeCallable();
    }

    public static final Property<PreviewLayer> TYPE =
            new BeanProperty<PreviewLayer>("type", "type");

    public static final AbstractProperty<PreviewLayer> NAME =
            new AbstractProperty<PreviewLayer>("name") {
                @Override
                public Object getPropertyValue(PreviewLayer item) {
                    if (item.layerInfo != null) {
                        return item.layerInfo.prefixedName();
                    }
                    if (item.groupInfo != null) {
                        return item.groupInfo.prefixedName();
                    }
                    return null;
                }
            };

    public static final Property<PreviewLayer> TITLE =
            new BeanProperty<PreviewLayer>("title", "title");

    public static final Property<PreviewLayer> ABSTRACT =
            new BeanProperty<PreviewLayer>("abstract", "abstract", false);

    public static final Property<PreviewLayer> KEYWORDS =
            new BeanProperty<PreviewLayer>("keywords", "keywords", false);

    public static final Property<PreviewLayer> COMMON =
            new PropertyPlaceholder<PreviewLayer>("commonFormats");

    public static final Property<PreviewLayer> ALL =
            new PropertyPlaceholder<PreviewLayer>("allFormats");

    public static final List<Property<PreviewLayer>> PROPERTIES =
            Arrays.asList(TYPE, TITLE, NAME, ABSTRACT, KEYWORDS, COMMON, ALL);

    @Override
    protected List<PreviewLayer> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<PreviewLayer>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected IModel<PreviewLayer> newModel(PreviewLayer object) {
        return new PreviewLayerModel(object);
    }

    @Override
    public long size() {
        try {
            if (getKeywords() != null && getKeywords().length > 0) {
                // Use a unique key for different queries
                return cache.get(KEY_SIZE + "." + String.join(",", getKeywords()), sizeCaller);
            }
            return cache.get(KEY_SIZE, sizeCaller);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int sizeInternal() {
        Filter filter = getFilter();
        int result = getCatalog().count(PublishedInfo.class, filter);
        return result;
    }

    @Override
    public int fullSize() {
        try {
            return cache.get(KEY_FULL_SIZE, fullSizeCaller);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int fullSizeInternal() {
        Filter filter = Predicates.acceptAll();
        return getCatalog().count(PublishedInfo.class, filter);
    }

    @Override
    public Iterator<PreviewLayer> iterator(final long first, final long count) {
        Iterator<PreviewLayer> iterator = filteredItems(first, count);
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

    /**
     * Returns the requested page of layer objects after applying any keyword filtering set on the
     * page
     */
    @SuppressWarnings("resource")
    private Iterator<PreviewLayer> filteredItems(long first, long count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<PreviewLayer> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty =
                        ((BeanProperty<PreviewLayer>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            } else if (property == NAME) {
                sortOrder = sortBy("prefixedName", sort.isAscending());
            }
        }

        Filter filter = getFilter();
        CloseableIterator<PublishedInfo> pi =
                catalog.list(PublishedInfo.class, filter, (int) first, (int) count, sortOrder);

        return CloseableIteratorAdapter.transform(
                pi,
                new Function<PublishedInfo, PreviewLayer>() {

                    @Override
                    public PreviewLayer apply(PublishedInfo input) {
                        if (input instanceof LayerInfo) {
                            return new PreviewLayer((LayerInfo) input);
                        } else if (input instanceof LayerGroupInfo) {
                            return new PreviewLayer((LayerGroupInfo) input);
                        }
                        return null;
                    }
                });
    }

    @Override
    protected Filter getFilter() {
        Filter filter = super.getFilter();

        // need to get only advertised and enabled layers
        Filter isLayerInfo = Predicates.isInstanceOf(LayerInfo.class);
        Filter isLayerGroupInfo = Predicates.isInstanceOf(LayerGroupInfo.class);

        Filter enabledFilter = Predicates.equal("resource.enabled", true);
        Filter storeEnabledFilter = Predicates.equal("resource.store.enabled", true);
        Filter advertisedFilter = Predicates.equal("resource.advertised", true);
        Filter enabledLayerGroup = Predicates.equal("enabled", true);
        Filter advertisedLayerGroup = Predicates.equal("advertised", true);
        // return only layer groups that are not containers
        Filter nonContainerGroup =
                Predicates.or(
                        Predicates.equal("mode", LayerGroupInfo.Mode.EO),
                        Predicates.equal("mode", LayerGroupInfo.Mode.NAMED),
                        Predicates.equal("mode", LayerGroupInfo.Mode.OPAQUE_CONTAINER),
                        Predicates.equal("mode", LayerGroupInfo.Mode.SINGLE));

        // Filter for the Layers
        Filter layerFilter =
                Predicates.and(isLayerInfo, enabledFilter, storeEnabledFilter, advertisedFilter);
        // Filter for the LayerGroups
        Filter layerGroupFilter =
                Predicates.and(
                        isLayerGroupInfo,
                        nonContainerGroup,
                        enabledLayerGroup,
                        advertisedLayerGroup);
        // Or filter for merging them
        Filter orFilter = Predicates.or(layerFilter, layerGroupFilter);
        // And between the new filter and the initial filter
        return Predicates.and(filter, orFilter);
    }

    /**
     * Inner class which calls the sizeInternal() method
     *
     * @author Nicpla Lagomarsini geosolutions
     */
    class SizeCallable implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return sizeInternal();
        }
    }

    /**
     * Inner class which calls the fullsizeInternal() method
     *
     * @author Nicpla Lagomarsini geosolutions
     */
    class FullSizeCallable implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return fullSizeInternal();
        }
    }
}
