/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.decorators.DecoratingLayerGroupInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.filter.expression.InternalVolatileFunction;

/**
 * Filters out the non advertised layers and resources.
 *
 * @author Davide Savazzi - GeoSolutions
 */
public class AdvertisedCatalog extends AbstractFilteredCatalog {

    @Serial
    private static final long serialVersionUID = 3361872345280114573L;

    /**
     * Exposes a filtered down view of a layer group
     *
     * @author Andrea Aime - GeoSolutions
     */
    public static final class AdvertisedLayerGroup extends DecoratingLayerGroupInfo {
        @Serial
        private static final long serialVersionUID = 1037043388874118840L;

        private List<PublishedInfo> filteredLayers;
        private List<StyleInfo> filteredStyles;

        public AdvertisedLayerGroup(
                LayerGroupInfo delegate, List<PublishedInfo> filteredLayers, List<StyleInfo> filteredStyles) {
            super(delegate);
            this.filteredLayers = filteredLayers;
            this.filteredStyles = filteredStyles;
        }

        @Override
        public List<PublishedInfo> getLayers() {
            return new FilteredList<>(filteredLayers, delegate.getLayers());
        }

        @Override
        public List<StyleInfo> getStyles() {
            return new FilteredList<>(filteredStyles, delegate.getStyles());
        }

        /**
         * Returns the original layers, including the advertised ones. Use this method only if strictly necessary
         * (current use case, figuring out if the group is queryable or not)
         */
        public List<PublishedInfo> getOriginalLayers() {
            return delegate.getLayers();
        }

        /**
         * Returns the original styles, including the advertised ones. Use this method only if strictly necessary
         * (current use case, figuring out if the group is queryable or not)
         */
        public List<StyleInfo> getOriginalStyles() {
            return delegate.getStyles();
        }

        /** Returns the delegate. Thread carefully when using this! */
        public LayerGroupInfo unwrap() {
            return delegate;
        }
    }

    private LayerGroupVisibilityPolicy layerGroupPolicy = LayerGroupVisibilityPolicy.HIDE_NEVER;

    /** @param catalog wrapped Catalog */
    public AdvertisedCatalog(Catalog catalog) {
        super(catalog);
    }

    /** Set LayerGroup visibility policy. */
    public void setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy layerGroupPolicy) {
        this.layerGroupPolicy = layerGroupPolicy;
    }

    /** Hide Layer if Request is GetCapabilities and Layer or its Resource are not advertised. */
    private boolean hideLayer(LayerInfo layer) {
        if (!layer.isAdvertised()) {
            return checkCapabilitiesRequest(layer.getResource());
        } else {
            return hideResource(layer.getResource());
        }
    }

    /** Hide Resource if it's not advertised and Request is GetCapabilities. */
    private boolean hideResource(ResourceInfo resource) {
        if (!resource.isAdvertised()) {
            return checkCapabilitiesRequest(resource);
        } else {
            return false;
        }
    }

    private boolean isOgcCapabilitiesRequest() {
        Request request = Dispatcher.REQUEST.get();
        return request != null && "GetCapabilities".equalsIgnoreCase(request.getRequest());
    }

    /**
     * Returns true if the layer should be hidden, false otherwise
     *
     * <ol>
     *   <li>has a request
     *   <li>is a GetCapabilities request
     *   <li>is not for a layer-specific virtual service
     * </ol>
     */
    boolean checkCapabilitiesRequest(ResourceInfo resource) {
        Request request = Dispatcher.REQUEST.get();
        if (request != null) {
            if ("GetCapabilities".equalsIgnoreCase(request.getRequest())) {
                String resourceContext = resource.getNamespace().getPrefix() + "/" + resource.getName();
                return !resourceContext.equalsIgnoreCase(request.getContext());
            }
        }
        return false;
    }

    @Override
    protected <T extends ResourceInfo> T checkAccess(T resource) {
        if (resource == null || hideResource(resource)) {
            return null;
        } else {
            return resource;
        }
    }

    @Override
    protected LayerInfo checkAccess(LayerInfo layer) {
        if (layer == null || hideLayer(layer)) {
            return null;
        } else {
            return layer;
        }
    }

    @Override
    protected LayerGroupInfo checkAccess(LayerGroupInfo group) {
        if (group == null) {
            return null;
        }

        // do not go and check every layer if the request is not a GetCapabilities
        Request request = Dispatcher.REQUEST.get();
        if (request == null || (!"GetCapabilities".equalsIgnoreCase(request.getRequest()))) {
            return group;
        }

        final List<PublishedInfo> layers = group.getLayers();
        final List<StyleInfo> styles = group.getStyles();
        final List<PublishedInfo> filteredLayers = new ArrayList<>();
        final List<StyleInfo> filteredStyles = new ArrayList<>();
        if (!group.isAdvertised()) {
            return null; // new AdvertisedLayerGroup(group, filteredLayers, filteredStyles);
        }
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo p = layers.get(i);
            StyleInfo style = (styles != null && styles.size() > i) ? styles.get(i) : null;

            if (p instanceof LayerInfo info) {
                p = checkAccess(info);
            } else {
                p = checkAccess((LayerGroupInfo) p);
            }

            if (p != null) {
                filteredLayers.add(p);
                filteredStyles.add(style);
            }
        }

        if (layerGroupPolicy.hideLayerGroup(group, filteredLayers)) {
            return null;
        } else {
            if (!group.getLayers().equals(filteredLayers)) {
                return new AdvertisedLayerGroup(group, filteredLayers, filteredStyles);
            } else {
                return group;
            }
        }
    }

    @Override
    protected <T extends ResourceInfo> List<T> filterResources(List<T> resources) {
        List<T> filtered = new ArrayList<>(resources.size());
        for (T resource : resources) {
            resource = checkAccess(resource);
            if (resource != null) {
                filtered.add(resource);
            }
        }
        return filtered;
    }

    @Override
    protected List<LayerGroupInfo> filterGroups(List<LayerGroupInfo> groups) {
        List<LayerGroupInfo> filtered = new ArrayList<>(groups.size());
        for (LayerGroupInfo group : groups) {
            group = checkAccess(group);
            if (group != null) {
                filtered.add(group);
            }
        }
        return filtered;
    }

    @Override
    protected List<LayerInfo> filterLayers(List<LayerInfo> layers) {
        List<LayerInfo> filtered = new ArrayList<>(layers.size());
        for (LayerInfo layer : layers) {
            layer = checkAccess(layer);
            if (layer != null) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    @Override
    protected <T extends CatalogInfo> Filter securityFilter(Class<T> infoType, Filter filter) {
        if (!isOgcCapabilitiesRequest()) {
            // Not needed for other kinds of request
            // TODO use a common implementation for GetCapabilities and Layer Preview
            return filter;
        }

        if (!ResourceInfo.class.isAssignableFrom(infoType)
                && !LayerInfo.class.isAssignableFrom(infoType)
                && !LayerGroupInfo.class.isAssignableFrom(infoType)) {
            // these kind of objects are not secured
            return filter;
        }

        org.geotools.api.filter.expression.Function visible = new InternalVolatileFunction() {
            /** Returns {@code false} if the catalog info shall be hidden, {@code true} otherwise. */
            @Override
            public Boolean evaluate(Object info) {
                if (info instanceof ResourceInfo resourceInfo) {
                    return !hideResource(resourceInfo);
                } else if (info instanceof LayerInfo layerInfo) {
                    return !hideLayer(layerInfo);
                } else if (info instanceof LayerGroupInfo groupInfo) {
                    return checkAccess(groupInfo) != null;
                } else {
                    throw new IllegalArgumentException("Can't build filter for objects of type "
                            + info.getClass().getName());
                }
            }
        };

        FilterFactory factory = Predicates.factory;

        // create a filter combined with the security credentials check
        Filter securityFilter = factory.equals(factory.literal(Boolean.TRUE), visible);
        return Predicates.and(filter, securityFilter);
    }

    @Override
    protected <T extends StoreInfo> T checkAccess(T store) {
        return store;
    }

    @Override
    protected <T extends NamespaceInfo> T checkAccess(T ns) {
        return ns;
    }

    @Override
    protected <T extends WorkspaceInfo> T checkAccess(T ws) {
        return ws;
    }

    @Override
    protected StyleInfo checkAccess(StyleInfo style) {
        return style;
    }

    @Override
    protected <T extends StoreInfo> List<T> filterStores(List<T> stores) {
        return stores;
    }

    @Override
    protected List<StyleInfo> filterStyles(List<StyleInfo> styles) {
        return styles;
    }

    @Override
    protected <T extends NamespaceInfo> List<T> filterNamespaces(List<T> namespaces) {
        return namespaces;
    }

    @Override
    protected <T extends WorkspaceInfo> List<T> filterWorkspaces(List<T> workspaces) {
        return workspaces;
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        if (layerGroup instanceof AdvertisedLayerGroup decorator) {
            LayerGroupInfo unwrapped = decorator.unwrap(LayerGroupInfo.class);
            delegate.save(unwrapped);
        } else {
            delegate.save(layerGroup);
        }
    }
}
