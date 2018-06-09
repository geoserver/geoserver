/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;

/** @author groldan */
class UnconfiguredCachedLayersProvider extends GeoServerDataProvider<TileLayer> {

    private static final long serialVersionUID = -8599398086587516574L;

    static final Property<TileLayer> TYPE =
            new AbstractProperty<TileLayer>("type") {

                private static final long serialVersionUID = 3215255763580377079L;

                @Override
                public PackageResourceReference getPropertyValue(TileLayer item) {
                    return GWCIconFactory.getSpecificLayerIcon(item);
                }

                @Override
                public Comparator<TileLayer> getComparator() {
                    return new Comparator<TileLayer>() {
                        @Override
                        public int compare(TileLayer o1, TileLayer o2) {
                            PackageResourceReference r1 = getPropertyValue(o1);
                            PackageResourceReference r2 = getPropertyValue(o2);
                            return r1.getName().compareTo(r2.getName());
                        }
                    };
                }
            };

    static final Property<TileLayer> NAME = new BeanProperty<TileLayer>("name", "name");

    static final Property<TileLayer> ENABLED = new BeanProperty<TileLayer>("enabled", "enabled");

    static final List<Property<TileLayer>> PROPERTIES =
            Collections.unmodifiableList(Arrays.asList(TYPE, NAME, ENABLED));

    /**
     * Provides a list of transient TileLayers for the LayerInfo and LayerGroupInfo objects in
     * Catalog that don't already have a configured TileLayer on their metadata map.
     *
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getItems()
     */
    @Override
    protected List<TileLayer> getItems() {
        final GWC gwc = GWC.get();
        final GWCConfig defaults = gwc.getConfig().saneConfig().clone();
        final GridSetBroker gridsets = gwc.getGridSetBroker();
        final Catalog catalog = getCatalog();

        defaults.setCacheLayersByDefault(true);

        List<String> unconfiguredLayerIds = getUnconfiguredLayers();

        List<TileLayer> layers =
                Lists.transform(
                        unconfiguredLayerIds,
                        new Function<String, TileLayer>() {
                            @Override
                            public TileLayer apply(String input) {
                                GeoServerTileLayer geoServerTileLayer;

                                LayerInfo layer = catalog.getLayer(input);
                                if (layer != null) {
                                    geoServerTileLayer =
                                            new GeoServerTileLayer(layer, defaults, gridsets);
                                } else {
                                    LayerGroupInfo layerGroup = catalog.getLayerGroup(input);
                                    geoServerTileLayer =
                                            new GeoServerTileLayer(layerGroup, defaults, gridsets);
                                }
                                /*
                                 * Set it to enabled regardless of the default settins, so it only shows up
                                 * as disabled if the actual layer/groupinfo is disabled
                                 */
                                geoServerTileLayer.getInfo().setEnabled(true);
                                return geoServerTileLayer;
                            }
                        });

        return layers;
    }

    private List<String> getUnconfiguredLayers() {
        Catalog catalog = getCatalog();
        List<String> layerIds = new LinkedList<String>();

        GWC gwc = GWC.get();

        List<LayerInfo> layers = catalog.getLayers();
        for (LayerInfo l : layers) {
            if (!gwc.hasTileLayer(l)) {
                layerIds.add(l.getId());
            }
        }

        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
        for (LayerGroupInfo lg : layerGroups) {
            if (!gwc.hasTileLayer(lg)) {
                layerIds.add(lg.getId());
            }
        }
        return layerIds;
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
            final GWCConfig defaults = gwc.getConfig().saneConfig().clone();
            defaults.setCacheLayersByDefault(true);
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
