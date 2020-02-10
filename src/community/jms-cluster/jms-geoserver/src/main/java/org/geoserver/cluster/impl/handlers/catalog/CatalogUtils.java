/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
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
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class CatalogUtils {
    private static final CatalogUtils CREATE = new CatalogUtils(true);
    private static final CatalogUtils UPDATE = new CatalogUtils(false);

    private final boolean createIfMissing;

    private CatalogUtils(boolean createIfMissing) {
        this.createIfMissing = createIfMissing;
    }

    /**
     * Returns an instance that checks for the existence of the object passed as argument to any of
     * the {@code localize*} methods, and makes them throw a {@link CatalogException} if no such
     * object exists.
     */
    public static CatalogUtils checking() {
        return UPDATE;
    }

    /**
     * Returns an instance that creates a new instance of the of type of the object passed as
     * argument to any of the {@code localize*} methods, if such object does not exist in the
     * provided {@link Catalog}, and resolves the required {@link CatalogInfo} dependencies as a
     * {@link #checking() must exist}
     */
    public static CatalogUtils creating() {
        return CREATE;
    }

    private <T extends CatalogInfo> Optional<T> find(T info, Function<String, T> finder) {
        Objects.requireNonNull(info, "argument 'info' is null");
        Objects.requireNonNull(finder);
        return Optional.ofNullable(finder.apply(info.getId()));
    }

    private <T extends CatalogInfo> T tryCreate(
            Catalog catalog, T info, Function<CatalogFactory, T> factory) {
        Objects.requireNonNull(catalog, "argument 'catalog' is null");
        Objects.requireNonNull(info, "argument 'info' is null");
        Objects.requireNonNull(factory);

        if (createIfMissing) {
            T localObject = factory.apply(catalog.getFactory());
            // let's use the newly created object
            try {
                BeanUtils.copyProperties(localObject, info);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Error copying properties of " + info, e);
            }
            return localObject;
        }
        throw new CatalogException(
                String.format("Object not found in local catalog: %s [%s]", info.getId(), info));
    }

    private <T extends CatalogInfo> T resolveLocal(
            Catalog catalog,
            T info,
            Function<String, T> finder,
            Function<CatalogFactory, T> factory) {
        Objects.requireNonNull(catalog, "argument 'catalog' is null");
        Objects.requireNonNull(info, "argument 'info' is null");
        Objects.requireNonNull(finder);
        Objects.requireNonNull(factory);

        T localObject = finder.apply(info.getId());
        if (localObject == null) {
            if (createIfMissing) {
                localObject = factory.apply(catalog.getFactory());
                // let's use the newly created object
                try {
                    BeanUtils.copyProperties(localObject, info);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Error copying properties of " + info, e);
                }
            } else {
                throw new CatalogException(
                        String.format(
                                "Object not found in local catalog: %s [%s]", info.getId(), info));
            }
        }
        return localObject;
    }

    /**
     * @param info
     * @param catalog
     * @return the local workspace if found or the passed one (localized)
     */
    public WorkspaceInfo localizeWorkspace(final WorkspaceInfo info, final Catalog catalog) {
        return resolveLocal(catalog, info, catalog::getWorkspace, CatalogFactory::createWorkspace);
    }

    public NamespaceInfo localizeNamespace(final NamespaceInfo info, final Catalog catalog) {
        return resolveLocal(catalog, info, catalog::getNamespace, CatalogFactory::createNamespace);
    }

    /**
     * @param info
     * @param catalog
     * @return the local style or the passed one (if not exists locally)
     */
    public StyleInfo localizeStyle(final StyleInfo info, final Catalog catalog) {
        return resolveLocal(catalog, info, catalog::getStyle, CatalogFactory::createStyle);
    }

    private LinkedHashSet<StyleInfo> localizeStyles(
            final Collection<StyleInfo> styleSet, final Catalog catalog) {

        return styleSet.stream()
                .map(s -> CatalogUtils.checking().localizeStyle(s, catalog))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    public <T extends PublishedInfo> T localizePublishedInfo(final T info, final Catalog catalog) {
        if (info instanceof LayerGroupInfo) {
            return (T) localizeLayerGroup((LayerGroupInfo) info, catalog);
        }
        if (info instanceof LayerInfo) {
            return (T) localizeLayer((LayerInfo) info, catalog);
        }
        throw new IllegalArgumentException("Unknown PublishedInfo type: " + info);
    }

    private LayerInfo localizeLayer(final LayerInfo info, final Catalog catalog) {
        return find(info, catalog::getLayer)
                .orElseGet(
                        () -> {
                            LayerInfo localObject = catalog.getFactory().createLayer();
                            if (createIfMissing) {
                                localObject = catalog.getFactory().createLayer();
                            } else {
                                throw new CatalogException(
                                        String.format(
                                                "Object not found in local catalog: %s [%s]",
                                                info.getId(), info));
                            }

                            // RESOURCE
                            ResourceInfo resource = info.getResource();
                            if (resource != null) {
                                resource =
                                        CatalogUtils.checking().localizeResource(resource, catalog);
                            } else {
                                throw new NullPointerException("No resource found !!!");
                            }

                            // we have to set the resource before [and after] calling copyProperties
                            // it is needed to call setName(String)
                            localObject.setResource(resource);

                            // let's use the newly created object
                            try {
                                BeanUtils.copyProperties(localObject, info);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new IllegalStateException(
                                        "Error copying properties of " + info, e);
                            }

                            // we have to set the resource before [and after] calling copyProperties
                            // it is overwritten (set to null) by the copyProperties function
                            localObject.setResource(resource);

                            final StyleInfo deserDefaultStyle = info.getDefaultStyle();
                            if (deserDefaultStyle != null) {
                                final StyleInfo localDefaultStyle =
                                        CatalogUtils.checking()
                                                .localizeStyle(deserDefaultStyle, catalog);
                                localObject.setDefaultStyle(localDefaultStyle);
                            } else {
                                // the default style is set by the builder
                                // TODO: check: this happens when configuring a layer using
                                // GeoServer REST manager
                                // (see
                                // ImageMosaicTest)
                            }

                            // STYLES
                            localObject.getStyles().clear();
                            localObject
                                    .getStyles()
                                    .addAll(
                                            CatalogUtils.checking()
                                                    .localizeStyles(
                                                            localObject.getStyles(), catalog));
                            return localObject;
                        });
    }

    public MapInfo localizeMapInfo(final MapInfo info, final Catalog catalog) {
        return find(info, catalog::getMap)
                .orElseGet(
                        () -> {
                            final MapInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createMap);
                            if (!info.getLayers().isEmpty()) {
                                List<LayerInfo> layers =
                                        info.getLayers()
                                                .stream()
                                                .map(
                                                        l ->
                                                                CatalogUtils.checking()
                                                                        .localizeLayer(l, catalog))
                                                .collect(Collectors.toList());
                                localObject.getLayers().clear();
                                localObject.getLayers().addAll(layers);
                            }
                            return localObject;
                        });
    }

    private LayerGroupInfo localizeLayerGroup(final LayerGroupInfo info, final Catalog catalog) {
        return find(info, catalog::getLayerGroup)
                .orElseGet(
                        () -> {
                            final LayerGroupInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createLayerGroup);

                            if (!info.getLayers().isEmpty()) {
                                List<PublishedInfo> layers =
                                        info.getLayers()
                                                .stream()
                                                .map(
                                                        p ->
                                                                CatalogUtils.checking()
                                                                        .localizePublishedInfo(
                                                                                p, catalog))
                                                .collect(Collectors.toList());
                                localObject.getLayers().clear();
                                localObject.getLayers().addAll(layers);
                            }

                            // localize styles, order matters
                            LinkedHashSet<StyleInfo> styles =
                                    CatalogUtils.checking()
                                            .localizeStyles(info.getStyles(), catalog);
                            localObject.getStyles().clear();
                            localObject.getStyles().addAll(styles);

                            if (info.getRootLayer() != null)
                                localObject.setRootLayer(
                                        CatalogUtils.checking()
                                                .localizeLayer(info.getRootLayer(), catalog));

                            if (info.getRootLayerStyle() != null)
                                localObject.setRootLayerStyle(
                                        CatalogUtils.checking()
                                                .localizeStyle(info.getRootLayerStyle(), catalog));

                            if (info.getWorkspace() != null)
                                localObject.setWorkspace(
                                        CatalogUtils.checking()
                                                .localizeWorkspace(info.getWorkspace(), catalog));
                            return localObject;
                        });
    }

    public StoreInfo localizeStore(final StoreInfo info, final Catalog catalog) {
        Optional<StoreInfo> localObject;
        Function<CatalogFactory, StoreInfo> factory;
        if (info instanceof CoverageStoreInfo) {
            localObject = find(info, catalog::getCoverageStore);
            factory = CatalogFactory::createCoverageStore;
        } else if (info instanceof DataStoreInfo) {
            localObject = find(info, catalog::getDataStore);
            factory = CatalogFactory::createDataStore;
        } else if (info instanceof WMSStoreInfo) {
            localObject = find(info, id -> catalog.getStore(id, WMSStoreInfo.class));
            factory = CatalogFactory::createWebMapServer;
        } else if (info instanceof WMTSStoreInfo) {
            localObject = find(info, id -> catalog.getStore(id, WMTSStoreInfo.class));
            factory = CatalogFactory::createWebMapTileServer;
        } else {
            throw new IllegalArgumentException(
                    "Unable to provide localization for the passed instance: " + info);
        }
        return localObject.orElseGet(
                () -> {
                    StoreInfo object = tryCreate(catalog, info, factory);
                    object.setWorkspace(
                            CatalogUtils.checking()
                                    .localizeWorkspace(info.getWorkspace(), catalog));
                    return object;
                });
    }

    public ResourceInfo localizeResource(final ResourceInfo info, final Catalog catalog) {
        if (info instanceof CoverageInfo) {
            return localizeCoverage((CoverageInfo) info, catalog);
        }
        if (info instanceof FeatureTypeInfo) {
            return localizeFeatureType((FeatureTypeInfo) info, catalog);
        }
        if (info instanceof WMSLayerInfo) {
            return localizeWMSLayer((WMSLayerInfo) info, catalog);
        }
        if (info instanceof WMTSLayerInfo) {
            return localizeWMTSLayer((WMTSLayerInfo) info, catalog);
        }
        throw new IllegalArgumentException("Unknown ResourceInfo type: " + info);
    }

    private WMSLayerInfo localizeWMSLayer(final WMSLayerInfo info, final Catalog catalog) {
        return find(info, id -> catalog.getResource(id, WMSLayerInfo.class))
                .orElseGet(
                        () -> {
                            WMSLayerInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createWMSLayer);
                            localObject.setNamespace(
                                    CatalogUtils.checking()
                                            .localizeNamespace(info.getNamespace(), catalog));
                            localObject.setStore(
                                    CatalogUtils.checking()
                                            .localizeStore(info.getStore(), catalog));
                            if (info.getAllAvailableRemoteStyles() != null
                                    && !info.getAllAvailableRemoteStyles().isEmpty()) {
                                localObject.getAllAvailableRemoteStyles().clear();
                                localObject
                                        .getAllAvailableRemoteStyles()
                                        .addAll(
                                                CatalogUtils.checking()
                                                        .localizeStyles(
                                                                info.getAllAvailableRemoteStyles(),
                                                                catalog));
                            }
                            // defaultStyle is a derived property from forcedRemoteStyle:String,
                            // sigh
                            // if(null != info.getDefaultStyle())
                            // remoteStyleInfos is an on-the-fly computed property, sigh
                            /// info.getRemoteStyleInfos();
                            return localObject;
                        });
    }

    private WMTSLayerInfo localizeWMTSLayer(final WMTSLayerInfo info, final Catalog catalog) {
        return find(info, id -> catalog.getResource(id, WMTSLayerInfo.class))
                .orElseGet(
                        () -> {
                            WMTSLayerInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createWMTSLayer);
                            localObject.setNamespace(
                                    CatalogUtils.checking()
                                            .localizeNamespace(info.getNamespace(), catalog));
                            localObject.setStore(
                                    CatalogUtils.checking()
                                            .localizeStore(info.getStore(), catalog));
                            return localObject;
                        });
    }

    private FeatureTypeInfo localizeFeatureType(final FeatureTypeInfo info, final Catalog catalog) {
        return find(info, catalog::getFeatureType)
                .orElseGet(
                        () -> {
                            FeatureTypeInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createFeatureType);
                            localObject.setNamespace(
                                    CatalogUtils.checking()
                                            .localizeNamespace(info.getNamespace(), catalog));
                            localObject.setStore(
                                    CatalogUtils.checking()
                                            .localizeStore(info.getStore(), catalog));
                            return localObject;
                        });
    }

    private CoverageInfo localizeCoverage(final CoverageInfo info, final Catalog catalog) {
        return find(info, catalog::getCoverage)
                .orElseGet(
                        () -> {
                            CoverageInfo localObject =
                                    tryCreate(catalog, info, CatalogFactory::createCoverage);
                            localObject.setNamespace(
                                    CatalogUtils.checking()
                                            .localizeNamespace(info.getNamespace(), catalog));
                            localObject.setStore(
                                    CatalogUtils.checking()
                                            .localizeStore(info.getStore(), catalog));
                            return localObject;
                        });
    }
}
