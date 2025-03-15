/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;

/**
 * Utility class for resolving proxied catalog objects.
 *
 * <p>This class provides methods to replace proxy references in catalog objects with actual instances from the catalog.
 * It recursively resolves references within complex catalog objects such as layers, layer groups, and resources to
 * ensure all nested objects are properly instantiated.
 *
 * <p>The resolver handles special cases during catalog loading where certain references may not yet be available,
 * maintaining proxies when resolution isn't possible. It also unwraps modification proxies to ensure direct object
 * references are used.
 *
 * <p>Use this class when:
 *
 * <ul>
 *   <li>Loading objects from persistent storage
 *   <li>Preparing objects for serialization
 *   <li>Ensuring consistent object references throughout the catalog
 * </ul>
 *
 * <p>Each catalog object type has a specialized resolution method to handle its specific reference structure.
 */
public class ResolvingProxyResolver {

    private static final Logger LOGGER = Logging.getLogger(ResolvingProxyResolver.class);

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class that provides only static methods and should not be instantiated.
     */
    private ResolvingProxyResolver() {
        // private constructor, this is an utility class
    }

    /**
     * Unwraps a modification proxy to access the underlying object.
     *
     * <p>This helper method delegates to {@link ModificationProxy#unwrap(Object)} to ensure we're working with the
     * actual object instances rather than their proxy wrappers.
     *
     * @param <T> the type of the object being unwrapped
     * @param obj the potentially proxied object
     * @return the unwrapped object, or the original object if not a proxy
     */
    private static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    /**
     * Resolves all proxy references in a catalog object.
     *
     * <p>This method dispatches to type-specific resolvers based on the actual type of the provided catalog info
     * object. It handles all known catalog object types and logs a warning for unknown types.
     *
     * @param info the catalog object containing proxy references to resolve
     * @param catalog the catalog to use for resolution
     */
    public static void resolve(CatalogInfo info, Catalog catalog) {
        if (info instanceof WorkspaceInfo) {
            resolve((WorkspaceInfo) info, catalog);
        } else if (info instanceof NamespaceInfo) {
            resolve((NamespaceInfo) info, catalog);
        } else if (info instanceof StoreInfo) {
            resolve((StoreInfo) info, catalog);
        } else if (info instanceof ResourceInfo) {
            resolve((ResourceInfo) info, catalog);
        } else if (info instanceof LayerInfo) {
            resolve((LayerInfo) info, catalog);
        } else if (info instanceof LayerGroupInfo) {
            resolve((LayerGroupInfo) info, catalog);
        } else if (info instanceof StyleInfo) {
            resolve((StyleInfo) info, catalog);
        } else if (info instanceof MapInfo) {
            resolve((MapInfo) info, catalog);
        } else {
            LOGGER.warning("Unknown CatalogInfo: " + info);
        }
    }

    private static void resolve(LayerInfo layer, Catalog catalog) {

        ResourceInfo resource = ResolvingProxy.resolve(catalog, layer.getResource());
        if (resource != null) {
            resource = unwrap(resource);
            layer.setResource(resource);
        }

        StyleInfo style = ResolvingProxy.resolve(catalog, layer.getDefaultStyle());
        if (style != null) {
            style = unwrap(style);
            layer.setDefaultStyle(style);
        }

        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        for (StyleInfo s : layer.getStyles()) {
            s = ResolvingProxy.resolve(catalog, s);
            s = unwrap(s);
            styles.add(s);
        }
        ((LayerInfoImpl) layer).setStyles(styles);
    }

    private static void resolve(LayerGroupInfo layerGroup, Catalog catalog) {

        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;
        // resolve the workspace
        WorkspaceInfo ws = lg.getWorkspace();
        if (ws != null) {
            WorkspaceInfo resolved = ResolvingProxy.resolve(catalog, ws);
            if (resolved != null) {
                resolved = unwrap(resolved);
                lg.setWorkspace(resolved);
            } else {
                LOGGER.log(
                        Level.INFO,
                        "Failed to resolve workspace for layer group \""
                                + lg.getName()
                                + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
            }
        }

        lg.setRootLayer(ResolvingProxy.resolve(catalog, lg.getRootLayer()));
        lg.setRootLayerStyle(ResolvingProxy.resolve(catalog, lg.getRootLayerStyle()));

        resolveLayerGroupLayers(lg.getLayers(), catalog);
        resolveLayerGroupStyles(lg.getLayers(), lg.getStyles(), catalog);
        // now resolves layers and styles defined in layer group styles
        for (LayerGroupStyle groupStyle : lg.getLayerGroupStyles()) {
            resolveLayerGroupLayers(groupStyle.getLayers(), catalog);
            resolveLayerGroupStyles(groupStyle.getLayers(), groupStyle.getStyles(), catalog);
        }
    }

    private static void resolveLayerGroupStyles(
            List<PublishedInfo> assignedLayers, List<StyleInfo> styles, Catalog catalog) {
        for (int i = 0; i < styles.size(); i++) {
            StyleInfo s = styles.get(i);
            if (s != null) {
                PublishedInfo assignedLayer = assignedLayers.get(i);
                StyleInfo resolved = null;
                if (assignedLayer instanceof LayerGroupInfo) {
                    // special case we might have a StyleInfo representing
                    // only the name of a LayerGroupStyle thus not present in Catalog.
                    // We take the ref and create a new object
                    // without searching in catalog.
                    String ref = ResolvingProxy.getRef(s);
                    if (ref != null) {
                        StyleInfo styleInfo = new StyleInfoImpl(catalog);
                        styleInfo.setName(ref);
                        resolved = styleInfo;
                    }
                }
                if (resolved == null) resolved = unwrap(ResolvingProxy.resolve(catalog, s));

                styles.set(i, resolved);
            }
        }
    }

    private static void resolveLayerGroupLayers(List<PublishedInfo> layers, Catalog catalog) {
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo l = layers.get(i);

            if (l != null) {
                PublishedInfo resolved;
                if (l instanceof LayerGroupInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(catalog, (LayerGroupInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else if (l instanceof LayerInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(catalog, (LayerInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else {
                    // Special case for null layer (style group)
                    resolved = unwrap(ResolvingProxy.resolve(catalog, l));
                }
                layers.set(i, resolved);
            }
        }
    }

    private static void resolve(StyleInfo style, Catalog catalog) {
        StyleInfoImpl s = (StyleInfoImpl) style;
        s.setCatalog(catalog);

        // resolve the workspace
        WorkspaceInfo ws = s.getWorkspace();
        if (ws != null) {
            WorkspaceInfo resolved = ResolvingProxy.resolve(catalog, ws);
            if (resolved != null) {
                resolved = unwrap(resolved);
                s.setWorkspace(resolved);
            } else {
                LOGGER.log(
                        Level.INFO,
                        "Failed to resolve workspace for style \""
                                + style.getName()
                                + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
            }
        }
    }

    private static void resolve(MapInfo map, Catalog catalog) {
        // no-op
    }

    private static void resolve(WorkspaceInfo workspace, Catalog catalog) {
        // no-op
    }

    private static void resolve(NamespaceInfo namespace, Catalog catalog) {
        // no-op
    }

    private static void resolve(StoreInfo store, Catalog catalog) {
        StoreInfoImpl s = (StoreInfoImpl) store;
        s.setCatalog(catalog);

        // resolve the workspace
        WorkspaceInfo resolved = ResolvingProxy.resolve(catalog, s.getWorkspace());
        if (resolved != null) {
            resolved = unwrap(resolved);
            s.setWorkspace(resolved);
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Failed to resolve workspace for store \""
                            + store.getName()
                            + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
        }
    }

    private static void resolve(ResourceInfo resource, Catalog catalog) {
        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        r.setCatalog(catalog);

        // resolve the store
        // note, gotta use ResourceInfoImpl.rawStore() cause the subclasses will override getStore() with a cast to
        // their concrete store types that will fail when the store is a ResolvingProxy
        StoreInfo store = ResolvingProxy.resolve(catalog, r.rawStore());
        if (store != null) {
            store = unwrap(store);
            r.setStore(store);
        }

        // resolve the namespace
        NamespaceInfo namespace = ResolvingProxy.resolve(catalog, r.getNamespace());
        if (namespace != null) {
            namespace = unwrap(namespace);
            r.setNamespace(namespace);
        }
    }
}
