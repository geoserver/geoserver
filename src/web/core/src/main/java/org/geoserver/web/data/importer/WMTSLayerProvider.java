/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.data.importer.LayerResource.LayerStatus;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wmts.model.WMTSLayer;

/**
 * Provides a list of resources for a specific data store
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
@SuppressWarnings("serial")
public class WMTSLayerProvider extends GeoServerDataProvider<LayerResource> {

    public static final Property<LayerResource> STATUS =
            new BeanProperty<LayerResource>("status", "status");
    public static final Property<LayerResource> NAME =
            new BeanProperty<LayerResource>("name", "localName");
    public static final Property<LayerResource> ACTION =
            new PropertyPlaceholder<LayerResource>("action");

    public static final List<Property<LayerResource>> PROPERTIES =
            Arrays.asList(NAME, ACTION, STATUS);

    String storeId;
    List<LayerResource> items;

    @Override
    protected List<LayerResource> getItems() {
        if (items == null) {
            // return an empty list in case we still don't know about the store
            if (storeId == null) return new ArrayList<>();

            // else, grab the resource list
            try {
                List<LayerResource> result;
                StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);

                Map<String, LayerResource> resources = new HashMap<>();
                WMTSStoreInfo wmtsInfo = (WMTSStoreInfo) store;

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                List<WMTSLayer> layers =
                        wmtsInfo.getWebMapTileServer(null).getCapabilities().getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new LayerResource(new NameImpl(l.getName())));
                }

                // lookup all configured layers, mark them as published in the resources
                List<ResourceInfo> configuredTypes =
                        getCatalog().getResourcesByStore(store, ResourceInfo.class);
                for (ResourceInfo type : configuredTypes) {
                    // compare with native name, which is what the DataStore provides through
                    // getNames()
                    // above
                    LayerResource resource = resources.get(type.getNativeName());
                    if (resource != null) resource.setStatus(LayerStatus.PUBLISHED);
                }
                result = new ArrayList<LayerResource>(resources.values());

                // return by natural order
                Collections.sort(result);
                items = result;
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not list layers for this WMTS store, "
                                + "an error occurred retrieving them: "
                                + e.getMessage(),
                        e);
            }
        }
        return items;
    }

    public void updateLayerOrder() {
        Collections.sort(items);
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    protected List<Property<LayerResource>> getProperties() {
        return PROPERTIES;
    }
}
