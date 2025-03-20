/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.List;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.OwsUtils;

public abstract class AbstractCatalogFacade implements CatalogFacade {

    //
    // Utilities
    //
    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected void beforeSaved(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        CatalogInfo real = ModificationProxy.unwrap(object);

        // TODO: protect this original object, perhaps with another proxy
        getCatalog().fireModified(real, propertyNames, oldValues, newValues);
    }

    protected <T extends CatalogInfo> T commitProxy(T object) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(object);

        // get the real object
        @SuppressWarnings("unchecked")
        T real = (T) h.getProxyObject();

        // commit to the original object
        h.commit();

        return real;
    }

    protected void afterSaved(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        CatalogInfo real = ModificationProxy.unwrap(object);

        // fire the post modify event
        getCatalog().firePostModified(real, propertyNames, oldValues, newValues);
    }

    protected void resolve(LayerInfo layer) {
        setId(layer);
        ResolvingProxyResolver.resolve(layer, getCatalog());
    }

    protected void resolve(LayerGroupInfo layerGroup) {
        setId(layerGroup);
        ResolvingProxyResolver.resolve(layerGroup, getCatalog());
    }

    protected void resolve(StyleInfo style) {
        setId(style);
        ResolvingProxyResolver.resolve(style, getCatalog());
    }

    protected void resolve(MapInfo map) {
        setId(map);
        ResolvingProxyResolver.resolve(map, getCatalog());
    }

    protected void resolve(WorkspaceInfo workspace) {
        setId(workspace);
        ResolvingProxyResolver.resolve(workspace, getCatalog());
    }

    protected void resolve(NamespaceInfo namespace) {
        setId(namespace);
        ResolvingProxyResolver.resolve(namespace, getCatalog());
    }

    protected void resolve(StoreInfo store) {
        setId(store);
        ResolvingProxyResolver.resolve(store, getCatalog());
    }

    protected void resolve(ResourceInfo resource) {
        setId(resource);
        ResolvingProxyResolver.resolve(resource, getCatalog());
    }

    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }
}
