/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.opengis.filter.Filter;

/** @author groldan */
class UnconfiguredCachedLayersProvider extends GeoServerDataProvider<TileLayer> {

    private static final long serialVersionUID = -8599398086587516574L;

    private static final Logger LOGGER = Logging.getLogger(UnconfiguredCachedLayersProvider.class);

    static final Property<TileLayer> TYPE =
            new AbstractProperty<TileLayer>("type") {

                private static final long serialVersionUID = 3215255763580377079L;

                @Override
                public GWCIconFactory.CachedLayerType getPropertyValue(TileLayer item) {
                    return GWCIconFactory.getCachedLayerType(item);
                }

                @Override
                public Comparator<TileLayer> getComparator() {
                    return new Comparator<TileLayer>() {
                        @Override
                        public int compare(TileLayer o1, TileLayer o2) {
                            GWCIconFactory.CachedLayerType r1 = getPropertyValue(o1);
                            GWCIconFactory.CachedLayerType r2 = getPropertyValue(o2);
                            return r1.compareTo(r2);
                        }
                    };
                }
            };

    static final Property<TileLayer> NAME = new BeanProperty<TileLayer>("name", "name");

    static final Property<TileLayer> ENABLED = new BeanProperty<TileLayer>("enabled", "enabled");

    static final List<Property<TileLayer>> PROPERTIES =
            Collections.unmodifiableList(Arrays.asList(TYPE, NAME, ENABLED));

    private GWCConfig defaults;

    /**
     * Simple cache for the last computed size based on the keywords, which in turn are the sole
     * input for the filter (at the moment, at least)
     */
    private class CachedSize {
        private static final long NOT_CACHED = Long.MIN_VALUE;
        private String[] cachedSizeKeywords;
        private long cachedSize = NOT_CACHED;

        public long get() {
            if (cachedSize == NOT_CACHED || !Arrays.equals(keywords, cachedSizeKeywords)) {
                long size = size(getFilter());
                cachedSize = size;
                cachedSizeKeywords =
                        keywords == null ? null : Arrays.copyOf(keywords, keywords.length);
                return size;
            } else {
                return cachedSize;
            }
        }
    }

    private CachedSize cachedSize = new CachedSize();

    public UnconfiguredCachedLayersProvider() {
        super();

        defaults = GWC.get().getConfig().saneConfig().clone();
        defaults.setCacheLayersByDefault(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Provides a page of transient TileLayers for the LayerInfo and LayerGroupInfo objects in
     * Catalog that don't already have a configured TileLayer on their metadata map and that match
     * the page {@link #getFilter() filter}, in the specified {@link #getSort() sort order}.
     */
    @Override
    public Iterator<TileLayer> iterator(long first, long count) {
        final Stream<TileLayer> stream = tileLayerStream(first, count);
        return new CloseableIteratorAdapter<TileLayer>(stream.iterator(), () -> stream.close());
    }

    private Stream<TileLayer> tileLayerStream(long first, long count) {
        final SortParam<Object> sort = getSort();
        final Filter filter = getFilter();
        final Stream<TileLayer> stream;
        if (sort == null) {
            stream =
                    unconfiguredLayers(filter)
                            .skip(first)
                            .limit(count)
                            .map(this::createUnconfiguredTileLayer);
        } else {
            Comparator<TileLayer> comparator = getComparator(sort);
            stream =
                    unconfiguredLayers(filter)
                            .map(this::createUnconfiguredTileLayer)
                            .sorted(comparator)
                            .skip(first)
                            .limit(count);
        }
        return stream;
    }

    @Override
    public int fullSize() {
        return size(Predicates.acceptAll());
    }

    @Override
    public long size() {
        return cachedSize.get();
    }

    private int size(Filter filter) {
        int size;
        try (Stream<PublishedInfo> unconfigured = unconfiguredLayers(filter)) {
            size = (int) unconfigured.count();
        }
        return size;
    }

    @Override
    protected List<TileLayer> getFilteredItems() {
        LOGGER.info("Should not be called, iterator() overridden");
        return tileLayerStream(0, Long.MAX_VALUE).collect(Collectors.toList());
    }

    /**
     * Provides a list of transient TileLayers for the LayerInfo and LayerGroupInfo objects in
     * Catalog that don't already have a configured TileLayer on their metadata map.
     *
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getItems()
     */
    @Override
    protected List<TileLayer> getItems() {
        LOGGER.info("should not be called, fullSize() and iterator() overridden");
        return unconfiguredLayers(Predicates.acceptAll())
                .map(this::createUnconfiguredTileLayer)
                .collect(Collectors.toList());
    }

    private TileLayer createUnconfiguredTileLayer(PublishedInfo info) {
        return new GeoServerTileLayer(info, defaults, GWC.get().getGridSetBroker());
    }

    @SuppressWarnings("PMD.CloseResource") // the two closeable iterators are wrapped and returned
    private Stream<PublishedInfo> unconfiguredLayers(Filter filter) {
        final Catalog catalog = getCatalog();
        final CloseableIterator<LayerInfo> layers = catalog.list(LayerInfo.class, filter);
        final CloseableIterator<LayerGroupInfo> groups = catalog.list(LayerGroupInfo.class, filter);

        Stream<PublishedInfo> all = Stream.concat(Streams.stream(layers), Streams.stream(groups));
        all =
                all.filter(this::isUnconfigured)
                        .onClose(
                                () -> {
                                    layers.close();
                                    groups.close();
                                });
        return all;
    }

    private boolean isUnconfigured(PublishedInfo info) {
        return !GWC.get().hasTileLayer(info);
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#getProperties() */
    @Override
    protected List<Property<TileLayer>> getProperties() {
        return PROPERTIES;
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object) */
    public IModel<TileLayer> newModel(final TileLayer tileLayer) {
        return new UnconfiguredTileLayerDetachableModel(((TileLayer) tileLayer).getName());
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#getComparator */
    @Override
    protected Comparator<TileLayer> getComparator(SortParam<?> sort) {
        return super.getComparator(sort);
    }

    private class UnconfiguredTileLayerDetachableModel extends LoadableDetachableModel<TileLayer> {

        private static final long serialVersionUID = -8920290470035166218L;

        private String name;

        public UnconfiguredTileLayerDetachableModel(String layerOrGroupName) {
            this.name = layerOrGroupName;
        }

        @Override
        protected TileLayer load() {
            final GWC gwc = GWC.get();
            final GridSetBroker gridsets = gwc.getGridSetBroker();
            Catalog catalog = getCatalog();

            LayerInfo layer = catalog.getLayerByName(name);
            if (layer != null) {
                return new GeoServerTileLayer(layer, defaults, gridsets);
            }
            LayerGroupInfo layerGroup = catalog.getLayerGroupByName(name);
            return new GeoServerTileLayer(layerGroup, defaults, gridsets);
        }
    }
}
