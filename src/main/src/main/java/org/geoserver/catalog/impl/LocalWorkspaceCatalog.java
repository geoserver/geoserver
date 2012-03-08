/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.LocalWorkspaceCatalogFilter;

/**
 * Catalog decorator handling cases when a {@link LocalWorkspace} is set.
 * <p>
 * This wrapper handles some additional cases that {@link LocalWorkspaceCatalogFilter} can not 
 * handle by simple filtering.
 * </p> 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LocalWorkspaceCatalog extends AbstractCatalogDecorator implements Catalog {

    public LocalWorkspaceCatalog(Catalog delegate) {
        super(delegate);
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
    public LayerGroupInfo getLayerGroup(String id) {
        return wrap(super.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        if (LocalWorkspace.get() != null) {
            LayerGroupInfo layerGroup = super.getLayerGroupByName(LocalWorkspace.get(), name);
            if (layerGroup != null) {
                return wrap(layerGroup);
            }
            //else fall back on unqualified lookup
        }

        return wrap(super.getLayerGroupByName(name));
    }

    /*
     * check that the layer group workspace matches the 
     */
    LayerGroupInfo check(LayerGroupInfo layerGroup) {
        if (LocalWorkspace.get() != null) {
            if (layerGroup.getWorkspace() != null && 
                !LocalWorkspace.get().equals(layerGroup.getWorkspace())) {
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
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace,
            String name) {
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
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(
            WorkspaceInfo workspace) {
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

    public List<RuntimeException> validate(LayerGroupInfo layerGroup, boolean isNew) {
        return super.validate(unwrap(layerGroup), isNew);
    }

    LayerGroupInfo wrap(LayerGroupInfo layerGroup) {
        if (layerGroup == null) {
            return null;
        }
        if (LocalWorkspace.get() != null) {
            return NameDequalifyingProxy.create(layerGroup, LayerGroupInfo.class);
        }
        return layerGroup;
    }

    LayerGroupInfo unwrap(LayerGroupInfo layerGroup) {
        return NameDequalifyingProxy.unwrap(layerGroup);
    }

    List<LayerGroupInfo> wrap(List<LayerGroupInfo> layerGroups) {
        if (LocalWorkspace.get() != null) {
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
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            
            if ("prefixedName".equals(method.getName()) || 
                "getPrefixedName".equals(method.getName())) {
                String val = (String) method.invoke(object, args);
                if (val == null || val.indexOf(':') == -1) {
                    return val;
                }

                return val.split(":")[1];
            }

            return method.invoke(object, args);
        }
    
        public static <T> T create( T object, Class<T> clazz) {
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

        public static <T> T unwrap( T object ) {
            return ProxyUtils.unwrap(object, NameDequalifyingProxy.class);
        }

    }
}
;