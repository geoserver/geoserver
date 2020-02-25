/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geotools.util.logging.Logging;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public abstract class CatalogUtils {
    public static final java.util.logging.Logger LOGGER = Logging.getLogger(CatalogUtils.class);

    /** @return the local workspace if found or the passed one (localized) */
    public static WorkspaceInfo localizeWorkspace(final WorkspaceInfo info, final Catalog catalog) {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final WorkspaceInfo localObject = catalog.getWorkspaceByName(info.getName());
        if (localObject != null) {
            return localObject;
        }

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(info);

        return info;
    }

    public static NamespaceInfo localizeNamespace(final NamespaceInfo info, final Catalog catalog) {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final NamespaceInfo localObject = catalog.getNamespaceByURI(info.getURI());
        if (localObject != null) {
            return localObject;
        }

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(info);
        return info;
    }

    /** @return the local style or the passed one (if not exists locally) */
    public static StyleInfo localizeStyle(final StyleInfo info, final Catalog catalog) {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final StyleInfo localObject = catalog.getStyleByName(info.getWorkspace(), info.getName());
        if (localObject != null) {
            return localObject;
        } else {
            if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
                LOGGER.info(
                        "No such style called \'"
                                + info.getName()
                                + "\' can be found: LOCALIZATION");
            }
            final CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.attach(info);
            return info;
        }
    }

    public static Set<StyleInfo> localizeStyles(
            final Set<StyleInfo> stileSet, final Catalog catalog) {
        if (stileSet == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");
        final Set<StyleInfo> localStileSet = new HashSet<StyleInfo>();
        final Iterator<StyleInfo> deserStyleSetIterator = stileSet.iterator();
        while (deserStyleSetIterator.hasNext()) {
            final StyleInfo deserStyle = deserStyleSetIterator.next();
            final StyleInfo localStyle = localizeStyle(deserStyle, catalog);
            if (localStyle != null) {
                localStileSet.add(localStyle);
            }
        }
        return localStileSet;
    }

    public static <T extends PublishedInfo> List<LayerInfo> localizeLayers(
            final List<T> info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");
        final List<LayerInfo> localLayerList = new ArrayList<LayerInfo>(info.size());
        final Iterator<LayerInfo> it = localLayerList.iterator();
        while (it.hasNext()) {
            final LayerInfo layer = it.next();
            final LayerInfo localLayer = localizeLayer(layer, catalog);
            if (localLayer != null) {
                localLayerList.add(localLayer);
            } else {
                if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                    LOGGER.warning(
                            "No such layer called \'"
                                    + layer.getName()
                                    + "\' can be found: SKIPPING");
                }
            }
        }
        return localLayerList;
    }

    public static LayerInfo localizeLayer(final LayerInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        // make sure we use the prefixed name to include the workspace
        final LayerInfo localObject = catalog.getLayerByName(info.prefixedName());

        if (localObject != null) {
            return localObject;
        }
        final LayerInfo createdObject = catalog.getFactory().createLayer();

        // RESOURCE
        ResourceInfo resource = info.getResource();
        if (resource != null) {
            resource = localizeResource(resource, catalog);
        } else {
            throw new NullPointerException("No resource found !!!");
        }

        // we have to set the resource before [and after] calling copyProperties
        // it is needed to call setName(String)
        createdObject.setResource(resource);

        // let's use the newly created object
        BeanUtils.copyProperties(createdObject, info);

        // we have to set the resource before [and after] calling copyProperties
        // it is overwritten (set to null) by the copyProperties function
        createdObject.setResource(resource);

        final StyleInfo deserDefaultStyle = info.getDefaultStyle();
        if (deserDefaultStyle != null) {
            final StyleInfo localDefaultStyle = localizeStyle(deserDefaultStyle, catalog);
            if (localDefaultStyle == null) {
                throw new NullPointerException(
                        "No matching style called \'"
                                + deserDefaultStyle.getName()
                                + "\'found locally.");
            }
            createdObject.setDefaultStyle(localDefaultStyle);
        } else {

            // the default style is set by the builder

            // TODO: check: this happens when configuring a layer using GeoServer REST manager (see
            // ImageMosaicTest)
        }

        // STYLES
        createdObject.getStyles().addAll(localizeStyles(createdObject.getStyles(), catalog));

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(createdObject);
        return createdObject;
    }

    public static MapInfo localizeMapInfo(final MapInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final MapInfo localObject = catalog.getMapByName(info.getName());
        if (localObject != null) {
            return localObject;
            // else object is modified: continue with localization
        }

        info.getLayers().addAll(localizeLayers(info.getLayers(), catalog));

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(info);
        return info;
    }

    public static LayerGroupInfo localizeLayerGroup(
            final LayerGroupInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        // make sure we use the prefixed name to include the workspace
        final LayerGroupInfo localObject = catalog.getLayerGroupByName(info.prefixedName());

        if (localObject != null) {
            return localObject;
        }

        try {
            info.getLayers().addAll(localizeLayers(info.getLayers(), catalog));
        } catch (IllegalAccessException e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(e.getLocalizedMessage());
            throw e;
        } catch (InvocationTargetException e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(e.getLocalizedMessage());
            throw e;
        }

        // make sure catalog transient fields are properly initiated
        List<PublishedInfo> layers = info.getLayers();
        if (layers != null) {
            for (PublishedInfo layer : layers) {
                if (layer instanceof LayerInfo) {
                    ResourceInfo resource = ((LayerInfo) layer).getResource();
                    if (resource == null) {
                        continue;
                    }
                    StoreInfo store = resource.getStore();
                    // we need the non proxy instance
                    store = ModificationProxy.unwrap(store);
                    if (store instanceof StoreInfoImpl) {
                        // setting the catalog
                        ((StoreInfoImpl) store).setCatalog(catalog);
                    }
                }
            }
        }

        // localize layers
        info.getStyles().addAll(localizeStyles(new HashSet<StyleInfo>(info.getStyles()), catalog));

        // attach to the catalog
        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(info);
        return info;
    }

    public static StoreInfo localizeStore(final StoreInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        if (info instanceof CoverageStoreInfo) {
            return localizeCoverageStore((CoverageStoreInfo) info, catalog);
        } else if (info instanceof DataStoreInfo) {
            return localizeDataStore((DataStoreInfo) info, catalog);
        } else if (info instanceof WMSStoreInfo) {
            return localizeWMSStore((WMSStoreInfo) info, catalog);
        } else {
            throw new IllegalArgumentException(
                    "Unable to provide localization for the passed instance");
        }
    }

    public static DataStoreInfo localizeDataStore(final DataStoreInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final DataStoreInfo localObject =
                catalog.getDataStoreByName(info.getWorkspace(), info.getName());

        final CatalogBuilder builder = new CatalogBuilder(catalog);

        if (localObject != null) {
            return localObject;
        }

        final DataStoreInfo createdObject = catalog.getFactory().createDataStore();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setWorkspace(localizeWorkspace(info.getWorkspace(), catalog));

        builder.attach(createdObject);
        return createdObject;
    }

    public static WMSStoreInfo localizeWMSStore(final WMSStoreInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final WMSStoreInfo localObject =
                catalog.getStoreByName(info.getWorkspace(), info.getName(), WMSStoreInfo.class);

        final CatalogBuilder builder = new CatalogBuilder(catalog);

        if (localObject != null) {
            return localObject;
        }

        final WMSStoreInfo createdObject = catalog.getFactory().createWebMapServer();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setWorkspace(localizeWorkspace(info.getWorkspace(), catalog));

        builder.attach(createdObject);
        return createdObject;
    }

    public static CoverageStoreInfo localizeCoverageStore(
            final CoverageStoreInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final CoverageStoreInfo localObject =
                catalog.getCoverageStoreByName(info.getWorkspace(), info.getName());

        final CatalogBuilder builder = new CatalogBuilder(catalog);

        if (localObject != null) {
            return localObject;
        }

        final CoverageStoreInfo createdObject = catalog.getFactory().createCoverageStore();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setWorkspace(localizeWorkspace(info.getWorkspace(), catalog));

        builder.attach(createdObject);
        return createdObject;
    }

    public static ResourceInfo localizeResource(final ResourceInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        if (info instanceof CoverageInfo) {
            // coverage
            return localizeCoverage((CoverageInfo) info, catalog);

        } else if (info instanceof FeatureTypeInfo) {
            // feature
            return localizeFeatureType((FeatureTypeInfo) info, catalog);

        } else if (info instanceof WMSLayerInfo) {
            // wmslayer
            return localizeWMSLayer((WMSLayerInfo) info, catalog);

        } else {
            throw new IllegalArgumentException(
                    "Unable to provide localization for the passed instance");
        }
    }

    public static WMSLayerInfo localizeWMSLayer(final WMSLayerInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final WMSLayerInfo localObject =
                catalog.getResourceByName(info.getNamespace(), info.getName(), WMSLayerInfo.class);
        if (localObject != null) {
            return localObject;
        }

        final WMSLayerInfo createdObject = catalog.getFactory().createWMSLayer();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setNamespace(localizeNamespace(info.getNamespace(), catalog));

        final StoreInfo store = localizeStore(info.getStore(), catalog);
        createdObject.setStore(store);

        //		WMSLayerObject.setAttributes(localizeAttributes(...)); TODO(should be already
        // serialized)

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(createdObject);
        return createdObject;
    }

    public static FeatureTypeInfo localizeFeatureType(
            final FeatureTypeInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final FeatureTypeInfo localObject =
                catalog.getFeatureTypeByName(info.getNamespace(), info.getName());

        if (localObject != null) {
            return localObject;
        }

        final FeatureTypeInfo createdObject = catalog.getFactory().createFeatureType();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setNamespace(localizeNamespace(info.getNamespace(), catalog));

        final StoreInfo store = localizeStore(info.getStore(), catalog);
        createdObject.setStore(store);

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(createdObject);
        return createdObject;
    }

    public static CoverageInfo localizeCoverage(final CoverageInfo info, final Catalog catalog)
            throws IllegalAccessException, InvocationTargetException {
        if (info == null || catalog == null)
            throw new NullPointerException("Arguments may never be null");

        final CoverageInfo localObject =
                catalog.getCoverageByName(info.getNamespace(), info.getName());
        if (localObject != null) {
            return localObject;
        }

        final CoverageInfo createdObject = catalog.getFactory().createCoverage();

        // let's using the created object (see getGridCoverageReader)
        BeanUtils.copyProperties(createdObject, info);

        createdObject.setNamespace(localizeNamespace(info.getNamespace(), catalog));

        createdObject.setStore(localizeCoverageStore(info.getStore(), catalog));

        final CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.attach(createdObject);
        return createdObject;
    }
}
