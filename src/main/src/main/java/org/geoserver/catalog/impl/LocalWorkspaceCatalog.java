/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.LocalWorkspaceCatalogFilter;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Catalog decorator handling cases when a {@link LocalWorkspace} is set.
 *
 * <p>This wrapper handles some additional cases that {@link LocalWorkspaceCatalogFilter} can not
 * handle by simple filtering.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalWorkspaceCatalog extends AbstractCatalogDecorator implements Catalog {

    private GeoServer geoServer;

    public LocalWorkspaceCatalog(Catalog delegate) {
        super(delegate);
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        if (LocalWorkspace.get() != null) {
            StyleInfo style = super.getStyleByName(LocalWorkspace.get(), name);
            if (style != null) {
                return style;
            }
        }
        return super.getStyleByName(name);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return wrap(super.getLayer(id));
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        if (LocalWorkspace.get() != null) {
            String wsName = LocalWorkspace.get().getName();

            // prefix the unqualified name
            if (name.contains(":")) {
                // name already prefixed, ensure it is prefixed with the correct one
                if (name.startsWith(wsName + ":")) {
                    // good to go, just pass call through
                    return wrap(super.getLayerByName(name));
                }
            }

            // prefix it explicitly
            NamespaceInfo ns = super.getNamespaceByPrefix(LocalWorkspace.get().getName());
            LayerInfo layer = super.getLayerByName(new NameImpl(ns.getURI(), name));
            return wrap(layer);
        }
        return super.getLayerByName(name);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        if (LocalWorkspace.get() != null) {
            // if local workspace active drop the prefix
            return getLayerByName(name.getLocalPart());
        } else {
            return super.getLayerByName(name);
        }
    }

    @Override
    public List<LayerInfo> getLayers() {
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.createList(super.getLayers(), LayerInfo.class);
        }
        return super.getLayers();
    }

    private boolean useNameDequalifyingProxy() {
        WorkspaceInfo workspaceInfo = LocalWorkspace.get();
        if (workspaceInfo == null) {
            return false;
        }
        return geoServer == null || !geoServer.getSettings().isLocalWorkspaceIncludesPrefix();
    }

    @Override
    public void add(LayerInfo layer) {
        super.add(unwrap(layer));
    }

    @Override
    public void save(LayerInfo layer) {
        super.save(unwrap(layer));
    }

    @Override
    public void remove(LayerInfo layer) {
        super.remove(unwrap(layer));
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return super.detach(unwrap(layer));
    }

    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return super.validate(unwrap(layer), isNew);
    }

    LayerInfo wrap(LayerInfo layer) {
        return wrap(layer, LayerInfo.class);
    }

    LayerInfo unwrap(LayerInfo layer) {
        return NameDequalifyingProxy.unwrap(layer);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return wrap(super.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        if (LocalWorkspace.get() != null) {
            String wsName = LocalWorkspace.get().getName();

            // prefix the unqualified name
            if (name.contains(":")) {
                // name already prefixed, ensure it is prefixed with the correct one
                if (name.startsWith(wsName + ":")) {
                    // good to go, just pass call through
                    LayerGroupInfo layerGroup = super.getLayerGroupByName(name);
                    if (layerGroup != null) {
                        return wrap(layerGroup);
                    }
                    // else fall back on unqualified lookup
                }
            }
            // prefix it explicitly
            LayerGroupInfo layerGroup =
                    super.getLayerGroupByName(LocalWorkspace.get().getName(), name);
            if (layerGroup != null) {
                return wrap(layerGroup);
            }
        }

        return wrap(super.getLayerGroupByName(name));
    }

    /*
     * check that the layer group workspace matches the
     */
    LayerGroupInfo check(LayerGroupInfo layerGroup) {
        if (LocalWorkspace.get() != null) {
            if (layerGroup.getWorkspace() != null
                    && !LocalWorkspace.get().equals(layerGroup.getWorkspace())) {
                return null;
            }
        }
        return layerGroup;
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return wrap(super.getLayerGroupByName(workspaceName, name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return wrap(super.getLayerGroupByName(workspace, name));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return wrap(super.getLayerGroups());
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return wrap(super.getLayerGroupsByWorkspace(workspaceName));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return wrap(super.getLayerGroupsByWorkspace(workspace));
    }

    public void add(LayerGroupInfo layerGroup) {
        super.add(unwrap(layerGroup));
    }

    public void save(LayerGroupInfo layerGroup) {
        super.save(unwrap(layerGroup));
    }

    public void remove(LayerGroupInfo layerGroup) {
        super.remove(unwrap(layerGroup));
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return super.detach(unwrap(layerGroup));
    }

    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return super.validate(unwrap(layerGroup), isNew);
    }

    LayerGroupInfo wrap(LayerGroupInfo layerGroup) {
        return wrap(layerGroup, LayerGroupInfo.class);
    }

    <T> T wrap(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.create(obj, clazz);
        }
        return obj;
    }

    <T> T unwrap(T obj) {
        return NameDequalifyingProxy.unwrap(obj);
    }

    List<LayerGroupInfo> wrap(List<LayerGroupInfo> layerGroups) {
        if (useNameDequalifyingProxy()) {
            return NameDequalifyingProxy.createList(layerGroups, LayerGroupInfo.class);
        }
        return layerGroups;
    }

    static class NameDequalifyingProxy implements WrappingProxy, Serializable {

        Object object;

        NameDequalifyingProxy(Object object) {
            this.object = object;
        }

        public Object getProxyObject() {
            return object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("prefixedName".equals(method.getName())
                    || "getPrefixedName".equals(method.getName())
                    || "getName".equals(method.getName())) {
                String val = (String) method.invoke(object, args);
                if (val == null || val.indexOf(':') == -1) {
                    return val;
                }

                return val.split(":")[1];
            }

            return method.invoke(object, args);
        }

        public static <T> T create(T object, Class<T> clazz) {
            return ProxyUtils.createProxy(object, clazz, new NameDequalifyingProxy(object));
        }

        public static <T> List<T> createList(List<T> object, Class<T> clazz) {
            return new ProxyList(object, clazz) {
                @Override
                protected <T> T createProxy(T proxyObject, Class<T> proxyInterface) {
                    return create(proxyObject, proxyInterface);
                }

                @Override
                protected <T> T unwrapProxy(T proxy, Class<T> proxyInterface) {
                    return unwrap(proxy);
                }
            };
        }

        public static <T> T unwrap(T object) {
            return ProxyUtils.unwrap(object, NameDequalifyingProxy.class);
        }
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return delegate.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        return wrap(delegate.get(type, filter), type);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return list(of, filter, (Integer) null, (Integer) null, (SortBy) null);
    }

    /**
     * Returns a decorating iterator over the one returned by the delegate that wraps every object
     * it returns, if possible.
     *
     * @see #wrap(Object, Class)
     * @see org.geoserver.catalog.Catalog#list(java.lang.Class, org.geoserver.catalog.Predicate,
     *     java.lang.Integer, java.lang.Integer, org.geoserver.catalog.OrderBy)
     */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            final Integer offset,
            final Integer count,
            final SortBy sortBy) {

        CloseableIterator<T> iterator = delegate.list(of, filter, offset, count, sortBy);
        if (iterator.hasNext() && useNameDequalifyingProxy()) {
            return CloseableIteratorAdapter.transform(
                    iterator, obj -> obj == null ? null : NameDequalifyingProxy.create(obj, of));
        }
        return iterator;
    }

    public void removeListeners(Class listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        if (LocalWorkspace.get() != null) {
            WorkspaceInfo ws = LocalWorkspace.get();
            NamespaceInfo ns = delegate.getNamespaceByPrefix(ws.getName());
            if (ns != null) {
                return ns;
            }
        }

        return super.getDefaultNamespace();
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        if (LocalWorkspace.get() != null) {
            return LocalWorkspace.get();
        }
        return super.getDefaultWorkspace();
    }
}
