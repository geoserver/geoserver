/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AbstractCatalogFilter;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.geotools.util.decorate.Wrapper;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

/**
 * Filters the resources that are not in the current workspace (used only if virtual services are
 * active)
 *
 * @author Justin DeOliveira
 */
public class LocalWorkspaceCatalogFilter extends AbstractCatalogFilter {

    /** the real/raw catalog, can't be a wrapper */
    Catalog catalog;

    public LocalWorkspaceCatalogFilter(Catalog catalog) {
        // unwrap it just to be sure
        while (catalog instanceof Wrapper && ((Wrapper) catalog).isWrapperFor(Catalog.class)) {
            Catalog unwrapped = ((Wrapper) catalog).unwrap(Catalog.class);
            if (unwrapped == catalog || unwrapped == null) {
                break;
            }

            catalog = unwrapped;
        }
        this.catalog = catalog;
    }

    public boolean hideLayer(LayerInfo layer) {
        PublishedInfo local = LocalPublished.get();
        if (local == null) {
            return false;
        } else if (local instanceof LayerInfo) {
            return !local.equals(layer);
        } else if (local instanceof LayerGroupInfo) {
            LayerGroupInfo lg = (LayerGroupInfo) local;
            Request request = Dispatcher.REQUEST.get();
            if (request != null
                    && "WMS".equalsIgnoreCase(request.getService())
                    && "GetCapabilities".equals(request.getRequest())
                    && lg.getMode() == Mode.SINGLE) {
                return true;
            } else {
                return !new LayerGroupHelper(lg).allLayers().contains(layer);
            }
        } else {
            throw new RuntimeException("Unknown PublishedInfo of type " + local.getClass());
        }
    }

    public boolean hideResource(ResourceInfo resource) {
        if (LocalPublished.get() != null) {
            for (LayerInfo l : resource.getCatalog().getLayers(resource)) {
                if (hideLayer(l)) {
                    return true;
                }
            }
        }
        return hideWorkspace(resource.getStore().getWorkspace());
    }

    public boolean hideWorkspace(WorkspaceInfo workspace) {
        return LocalWorkspace.get() != null && !LocalWorkspace.get().equals(workspace);
    }

    public boolean hideStyle(StyleInfo style) {
        if (style.getWorkspace() == null) {
            // global style, hide it if a local workspace style shars the same name, ie overrides it
            if (LocalWorkspace.get() != null) {
                if (catalog.getStyleByName(LocalWorkspace.get(), style.getName()) != null) {
                    return true;
                }
            }
            return false;
        }
        return hideWorkspace(style.getWorkspace());
    }

    static Boolean groupInherit = null;
    /** Should local workspaces include layer groups from the global workspace */
    public static boolean workspaceLayerGroupInherit() {
        if (groupInherit == null) {
            // Just sets it based on the property so no need to synchronize
            String value = GeoServerExtensions.getProperty("GEOSERVER_GLOBAL_LAYER_GROUP_INHERIT");
            if (value != null) {
                groupInherit = Boolean.parseBoolean(value);
            } else {
                // Local workspaces inherit global layer groups by default.
                groupInherit = true;
            }
        }
        return groupInherit;
    }

    @Override
    public boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        PublishedInfo local = LocalPublished.get();
        if (local != null) {
            if (local instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) local;
                Request request = Dispatcher.REQUEST.get();
                if (request != null
                        && "WMS".equalsIgnoreCase(request.getService())
                        && "GetCapabilities".equals(request.getRequest())
                        && lg.getMode() == Mode.SINGLE) {
                    return !lg.equals(layerGroup);
                } else if (!lg.equals(layerGroup)
                        && !new LayerGroupHelper(lg).allGroups().contains(layerGroup)) {
                    return true;
                }
            } else {
                // simple layer, not a layer group
                return true;
            }
        }

        if (layerGroup.getWorkspace() == null) {
            if (workspaceLayerGroupInherit()) {
                // global layer group, hide it if a local workspace layer group shared the same
                // name, ie
                // overrides it
                if (LocalWorkspace.get() != null) {
                    if (catalog.getLayerGroupByName(LocalWorkspace.get(), layerGroup.getName())
                            != null) {
                        return true;
                    }
                }
            } else {
                // Only show a global layer group in the global workspace.
                return LocalWorkspace.get() != null;
            }
            return false;
        }
        return hideWorkspace(layerGroup.getWorkspace());
    }

    /** Returns true if the sublayers of a layer group are all hidden. */
    protected boolean subLayersHidden(LayerGroupInfo layerGroup) {
        boolean anySublayersVisible = false;
        for (PublishedInfo subLayer : layerGroup.getLayers()) {
            if (subLayer instanceof LayerInfo) {
                if (!hideLayer((LayerInfo) subLayer)) {
                    anySublayersVisible = true;
                    break;
                }
            } else if (subLayer instanceof LayerGroupInfo) {
                if (!hideLayerGroup((LayerGroupInfo) subLayer)) {
                    anySublayersVisible = true;
                    break;
                }
            }
        }

        return !anySublayersVisible;
    }

    private Filter inWorkspace() {
        WorkspaceInfo localWS = LocalWorkspace.get();
        if (localWS == null) return Predicates.acceptAll();
        return Predicates.equal("workspace.id", localWS.getId());
    }

    private Filter standardFilter(Class<? extends CatalogInfo> clazz) {
        final Filter forGlobal;
        if (LocalWorkspace.get() != null) {
            // TODO need a well known implementation
            // Show globals unless an object with the same name is in the local workspace
            forGlobal = super.getSecurityFilter(clazz);
        } else {
            // Global request, show all globals
            forGlobal = Predicates.acceptAll();
        }
        // If it's a global use the global filter, otherwise check if it's in the local workspace
        return Predicates.or(
                Predicates.and(Predicates.isNull("workspace.id"), forGlobal),
                Predicates.and(
                        Predicates.factory.not(Predicates.isNull("workspace.id")), inWorkspace()));
    }

    @Override
    public Filter getSecurityFilter(final Class<? extends CatalogInfo> clazz) {
        WorkspaceInfo localWS = LocalWorkspace.get();
        PublishedInfo localPublished = LocalPublished.get();
        if (localWS == null && localPublished == null) {
            return Predicates.acceptAll();
        }
        if (ResourceInfo.class.isAssignableFrom(clazz)) {
            // Show if it's in a visible workspace or used by the local layer
            Filter localLayerFilter;
            if (localPublished == null) {
                localLayerFilter = Predicates.acceptAll();
            } else {
                // TODO Well known check if it's used by the local layer
                return super.getSecurityFilter(clazz);
            }
            return Predicates.or(localLayerFilter, inWorkspace());
        } else if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            // Show if there's no local workspace or if it is the local workspace
            if (localWS == null) return Predicates.acceptAll();
            return Predicates.equal("id", localWS.getId());
        } else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            Filter filter = standardFilter(clazz);

            // Only show a layer group in a layer local request if it is the local layer
            if (localPublished != null) {
                if (localPublished instanceof LayerInfo) {
                    // TODO Need a well known recursive filter for layer groups instead of using an
                    // InternalVolatileFunction, KS
                    Function subLayersHidden =
                            new InternalVolatileFunction() {
                                @Override
                                public Boolean evaluate(Object object) {
                                    return !subLayersHidden((LayerGroupInfo) object);
                                }
                            };
                    FilterFactory factory = Predicates.factory;

                    // hide the layer if its sublayers are hidden
                    filter =
                            Predicates.and(
                                    filter,
                                    factory.equals(factory.literal(Boolean.TRUE), subLayersHidden));

                    Predicates.and(filter, Predicates.equal("id", localPublished.getId()));
                } else if (localPublished instanceof LayerGroupInfo) {
                    LayerGroupInfo lg = (LayerGroupInfo) localPublished;
                    List<LayerGroupInfo> groups = new LayerGroupHelper(lg).allGroups();
                    List<Filter> groupIdFilters = new ArrayList<>();
                    for (LayerGroupInfo group : groups) {
                        groupIdFilters.add(Predicates.equal("id", group.getId()));
                    }
                    return Predicates.or(groupIdFilters);
                }
            }
            return filter;
        } else if (StyleInfo.class.isAssignableFrom(clazz)) {
            return standardFilter(clazz);
        } else if (LayerInfo.class.isAssignableFrom(clazz)) {
            // If there's a local Layer, only show that layer, otherwise show all.
            if (localPublished == null) {
                return Predicates.acceptAll();
            } else if (localPublished instanceof LayerInfo) {
                return Predicates.equal("id", localPublished.getId());
            } else if (localPublished instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) localPublished;
                Request request = Dispatcher.REQUEST.get();
                if (request != null
                        && "WMS".equalsIgnoreCase(request.getService())
                        && "GetCapabilities".equals(request.getRequest())
                        && lg.getMode() == Mode.SINGLE) {
                    // wms GetCapabilies with a group in "single" mode, meaning the layers are also
                    // showing up stand alone
                    // but we only asked for the group, so don't accept any sub-layer
                    return Predicates.acceptNone();
                } else {
                    // not a WMS capabilities or not a "single" mode layer group, allow any layer in
                    // the group,
                    List<LayerInfo> layers = new LayerGroupHelper(lg).allLayers();
                    List<Filter> layersIdFilters = new ArrayList<>();
                    for (LayerInfo layer : layers) {
                        layersIdFilters.add(Predicates.equal("id", layer.getId()));
                    }
                    return Predicates.or(layersIdFilters);
                }
            } else {
                throw new RuntimeException(
                        "Unexpected local published reference of type "
                                + localPublished.getClass());
            }
        } else if (NamespaceInfo.class.isAssignableFrom(clazz)) {
            // TODO
            return super.getSecurityFilter(clazz);
        } else {
            return super.getSecurityFilter(clazz);
        }
    }
}
