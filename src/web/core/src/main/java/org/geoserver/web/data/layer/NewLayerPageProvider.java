/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.*;
import org.geoserver.catalog.*;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.Name;

/**
 * Provides a list of resources for a specific data store
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class NewLayerPageProvider extends GeoServerDataProvider<Resource> {

    public static final Property<Resource> PUBLISHED =
            new BeanProperty<Resource>("published", "published");
    public static final Property<Resource> NAME = new BeanProperty<Resource>("name", "localName");
    public static final Property<Resource> ACTION = new PropertyPlaceholder<Resource>("action");

    public static final List<Property<Resource>> PROPERTIES =
            Arrays.asList(PUBLISHED, NAME, ACTION);

    boolean showPublished;

    String storeId;

    transient List<Resource> cachedItems;

    @Override
    protected List<Resource> getItems() {
        // return an empty list in case we still don't know about the store
        if (storeId == null) {
            return new ArrayList<Resource>();
        } else if (cachedItems == null) {
            cachedItems = getItemsInternal();
        }
        return cachedItems;
    }

    private List<Resource> getItemsInternal() {
        // else, grab the resource list
        try {
            List<Resource> result;
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);

            Map<String, Resource> resources = new HashMap<String, Resource>();
            if (store instanceof DataStoreInfo) {
                DataStoreInfo dstore = (DataStoreInfo) store;
                DataStoreInfo expandedStore = getCatalog().getResourcePool().clone(dstore, true);

                // collect all the type names and turn them into resources
                // for the moment we use local names as datastores are not returning
                // namespace qualified NameImpl
                List<Name> names = expandedStore.getDataStore(null).getNames();
                for (Name name : names) {
                    FeatureTypeInfo fti =
                            getCatalog()
                                    .getFeatureTypeByDataStore(expandedStore, name.getLocalPart());
                    // skip views, we cannot have two layers use the same feature type info, as the
                    // underlying definition is attached to the feature type info itself
                    if (fti == null
                            || fti.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE) == null) {
                        resources.put(name.getLocalPart(), new Resource(name));
                    }
                }

            } else if (store instanceof CoverageStoreInfo) {
                CoverageStoreInfo cstore = (CoverageStoreInfo) store;
                CoverageStoreInfo expandedStore =
                        getCatalog().getResourcePool().clone(cstore, true);

                NamespaceInfo ns =
                        getCatalog().getNamespaceByPrefix(expandedStore.getWorkspace().getName());
                GridCoverageReader reader = expandedStore.getGridCoverageReader(null, null);
                try {
                    String[] names = reader.getGridCoverageNames();
                    for (String name : names) {
                        Name qualified = new NameImpl(ns.getURI(), name);
                        Resource resource = new Resource(qualified);
                        resource.setMultiCoverageReader(true);
                        resources.put(name, resource);
                    }
                } catch (UnsupportedOperationException e) {
                    // old code, pre multi-coverage
                    // getting to the coverage name without reading the whole coverage seems to
                    // be hard stuff, let's have the catalog builder to the heavy lifting
                    CatalogBuilder builder = new CatalogBuilder(getCatalog());
                    builder.setStore(store);
                    CoverageInfo ci = builder.buildCoverage();
                    Name name = ci.getQualifiedName();
                    resources.put(name.getLocalPart(), new Resource(name));
                }

            } else if (store instanceof WMTSStoreInfo) {
                WMTSStoreInfo wmsInfo = (WMTSStoreInfo) store;
                WMTSStoreInfo expandedStore =
                        (WMTSStoreInfo) getCatalog().getResourcePool().clone(wmsInfo, true);

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                WebMapTileServer webMapTileServer = expandedStore.getWebMapTileServer(null);
                WMTSCapabilities capabilities = webMapTileServer.getCapabilities();
                List<WMTSLayer> layers = capabilities.getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new Resource(new NameImpl(l.getName())));
                }
            } else if (store instanceof WMSStoreInfo) {
                WMSStoreInfo wmsInfo = (WMSStoreInfo) store;
                WMSStoreInfo expandedStore = getCatalog().getResourcePool().clone(wmsInfo, true);

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                List<Layer> layers =
                        expandedStore.getWebMapServer(null).getCapabilities().getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new Resource(new NameImpl(l.getName())));
                }
            }

            // lookup all configured layers, mark them as published in the resources
            List<ResourceInfo> configuredTypes =
                    getCatalog().getResourcesByStore(store, ResourceInfo.class);
            for (ResourceInfo type : configuredTypes) {
                // compare with native name, which is what the DataStore provides through getNames()
                // above
                Resource resource;
                if (type instanceof CoverageInfo) {
                    CoverageInfo ci = (CoverageInfo) type;
                    if (ci.getNativeCoverageName() != null) {
                        resource = resources.get(ci.getNativeCoverageName());
                    } else {
                        resource = resources.get(type.getNativeName());
                    }
                } else {
                    resource = resources.get(type.getNativeName());
                }
                if (resource != null) {
                    resource.setPublished(true);
                }
            }
            result = new ArrayList<Resource>(resources.values());

            // return by natural order
            Collections.sort(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not list layers for this store, "
                            + "an error occurred retrieving them: "
                            + e.getMessage(),
                    e);
        }
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.cachedItems = null;
        this.storeId = storeId;
    }

    @Override
    protected List<Resource> getFilteredItems() {
        List<Resource> resources = super.getFilteredItems();
        if (showPublished) return resources;

        List<Resource> unconfigured = new ArrayList<Resource>();
        for (Resource resource : resources) {
            if (!resource.isPublished()) unconfigured.add(resource);
        }
        return unconfigured;
    }

    @Override
    protected List<Property<Resource>> getProperties() {
        return PROPERTIES;
    }

    public void setShowPublished(boolean showPublished) {
        this.showPublished = showPublished;
    }
}
