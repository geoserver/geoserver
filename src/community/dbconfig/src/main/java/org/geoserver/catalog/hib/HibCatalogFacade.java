/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;

import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.hibernate.AbstractHibFacade;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

public class HibCatalogFacade extends AbstractHibFacade implements CatalogFacade {

    /**
     * the catalog
     */
    Catalog catalog;
    
    public Catalog getCatalog() {
        return catalog;
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
    
    //
    // workspaces
    //
    
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return persist(workspace);
    }
    
    public void save(WorkspaceInfo workspace) {
        merge(workspace);
    }
    
    public void remove(WorkspaceInfo workspace) {
        delete(workspace);
    }
    
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }
    
    public WorkspaceInfo getWorkspace(String id) {
        return (WorkspaceInfo) first(
            query("from ", WorkspaceInfo.class, " where id = ", param(id)));
    }
    
    public WorkspaceInfo getWorkspaceByName(String name) {
        return (WorkspaceInfo) first(
            query("from ", WorkspaceInfo.class, " where name = ", param(name)));
    }
    
    public List<WorkspaceInfo> getWorkspaces() {
        return (List<WorkspaceInfo>) list(WorkspaceInfo.class);
    }
    
    public WorkspaceInfo getDefaultWorkspace() {
        Query query = 
            query("from ", WorkspaceInfoImpl.class, " where default = ", param(Boolean.TRUE));
        return (WorkspaceInfoImpl) first(query);
    }

    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        //TODO: remove the cast to WorkspaceInfoImpl
        WorkspaceInfo old = getDefaultWorkspace();

        if (old != null) {
            ((WorkspaceInfoImpl)old).setDefault(false);
            save(old);
        }
        
        if (workspace != null) {
            workspace = resolve(workspace);
            ((WorkspaceInfoImpl)workspace).setDefault(true);
            save(workspace);
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultWorkspace"), Arrays.asList(old), Arrays.asList(workspace));
    }
    
    //
    // namespaces
    //
    public NamespaceInfo add(NamespaceInfo namespace) {
        return persist(namespace);
    }
    
    public void save(NamespaceInfo namespace) {
        merge(namespace);
    }

    public void remove(NamespaceInfo namespace) {
        delete(namespace);
    }
    
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }
    
    public NamespaceInfo getNamespace(String id) {
        Query query = query("from ", NamespaceInfo.class, " where id = ", param(id));
        return (NamespaceInfo) first(query);
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        Query query = query("from ", NamespaceInfo.class, " where prefix = ", param(prefix));
        return (NamespaceInfo) first(query);
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        Query query = query("from ", NamespaceInfo.class, " where URI = ", param(uri));
        return (NamespaceInfo) first(query);
    }

    public List<NamespaceInfo> getNamespaces() {
        return list(NamespaceInfo.class);
    }
    
    public NamespaceInfo getDefaultNamespace() {
        Query query = query("from ", NamespaceInfo.class, " where default = ", param(Boolean.TRUE));
        return (NamespaceInfo) first(query);
    }
    
    public void setDefaultNamespace(NamespaceInfo namespace) {
        //TODO: remove the cast to NamespaceInfoImpl
        NamespaceInfo old = getDefaultNamespace();

        if (old != null) {
            ((NamespaceInfoImpl)old).setDefault(false);
            save(old);
        }
        
        if (namespace != null) {
            namespace = resolve(namespace);
            ((NamespaceInfoImpl)namespace).setDefault(true);
            save(namespace);
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultNamespace"), Arrays.asList(old), Arrays.asList(namespace));
    }
    
    
    //
    // stores
    //
    public StoreInfo add(StoreInfo store) {
        return persist(store);
    }
    
    public void save(StoreInfo store) {
        merge(store);
    }

    public void remove(StoreInfo store) {
        delete(store);
    }
    
    public <T extends StoreInfo> T detach(T store) {
        return store;
    }
    
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return (T) first(query("from ", clazz, " where id = ", param(id)));
    }

    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name,
            Class<T> clazz) {
        Query query = null;
        if (workspace == CatalogFacade.ANY_WORKSPACE) {
            query = query("from ", clazz, " where name = ", param(name));
        }
        else {
            query = query("from ", clazz, " where name = ", param(name), " and workspace = ", param(workspace));
        }
        return (T) first(query);
    }

    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return list(clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace,
            Class<T> clazz) {
        return Collections.unmodifiableList(
            query("from ", clazz, " where workspace = ", param(workspace)).list());
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        Query query = 
            query("from ", DataStoreInfoImpl.class, " where workspace = ", param(workspace), 
                 " and default = ", param(Boolean.TRUE));
        return (DataStoreInfo) first(query);
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        //TODO: remove the cast to DataStoreInfoImpl
        DataStoreInfo old = getDefaultDataStore(workspace);

        if (old != null) {
            ((DataStoreInfoImpl)old).setDefault(false);
            save(old);
        }
        
        if (store != null) {
            ((DataStoreInfoImpl)store).setDefault(true);
            save(store);
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultDataStore"), Arrays.asList(old), Arrays.asList(store));
    }

    
    //
    // resources
    //
    public ResourceInfo add(ResourceInfo resource) {
        return persist(resource);
    }
    
    public void save(ResourceInfo resource) {
        merge(resource);
    }
    
    public void remove(ResourceInfo resource) {
        delete(resource);
    }
    
    public <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }
    
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return (T) first(query("from ", clazz, " where id = ", param(id)));
    }

    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name,
            Class<T> clazz) {
        Query query = null;
        if (namespace == CatalogFacade.ANY_NAMESPACE) {
            query = query("from ", clazz, " where name = ", param(name));
        }
        else {
            query = query("from ", clazz, " where name = ", param(name), 
                " and namespace.prefix = ", param(namespace.getPrefix()));
        }
        
        return (T) first(query);
    }

    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name,
            Class<T> clazz) {
        Query query = query("from ", clazz, " r where name = ", param(name),
            " and r.store = ", param(store));
        return (T) first(query);
    }

    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return (List<T>) list(clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        Query query = query("select r from ", clazz, " r, ", NamespaceInfo.class, " n",
                " where r.namespace = n and n.prefix = ", param(namespace.getPrefix()));
        return query.list();
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        Query query = query("from ", clazz, " r where r.store = ", param(store));
        return query.list();
    }
    
    //
    // styles
    //
    public StyleInfo add(StyleInfo style) {
        return persist(style);
    }
    
    public void save(StyleInfo style) {
        merge(style);
    }

    public void remove(StyleInfo style) {
        delete(style);
    }
    
    public StyleInfo detach(StyleInfo style) {
        return style;
    }
   
    public StyleInfo getStyle(String id) {
        Query query = query("from ", StyleInfo.class, " where id = ", param(id));
        return (StyleInfo) first(query);
    }

    public StyleInfo getStyleByName(String name) {
        Query query = query("from ", StyleInfo.class, " where name = ", param(name));
        return (StyleInfo) first(query);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<StyleInfo> getStyles() {
        return (List<StyleInfo>) list(StyleInfo.class);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not implemented");
    }

    //
    // layers
    //
    public LayerInfo add(LayerInfo layer) {
        
        // FIXME we are replacing some referenced object here because hib would recognized original
        // ones as unattached.
        if (layer.getResource().getId() != null) {
            Query query = query("from ", layer.getResource().getClass(), " where id = ",
                    param(layer.getResource().getId()));
            layer.setResource((ResourceInfo) first(query));
        }

        // FIXME we are replacing some referenced object here because hib would recognized original
        // ones as unattached.
        if (layer.getDefaultStyle() != null) {
            Query query = query("from ", StyleInfo.class, " where id = ", 
                param(layer.getDefaultStyle().getId()));
            layer.setDefaultStyle((StyleInfo) first(query));
        }

        
        return persist(layer);
    }
    
    public void save(LayerInfo layer) {
        merge(layer);
    }
    
    public void remove(LayerInfo layer) {
        delete(layer);
    }
    
    public LayerInfo detach(LayerInfo layer) {
        Session s = sessionFactory.getCurrentSession();
        layer = (LayerInfo) s.createCriteria(LayerInfo.class)
            .setFetchMode("styles", FetchMode.JOIN)
            .setFetchMode("defaultStyle", FetchMode.JOIN)
            .setFetchMode("attribution", FetchMode.JOIN)
            .add(Restrictions.idEq(layer.getId())).uniqueResult();
        
        return layer;
    }
    
    public LayerInfo getLayer(String id) {
        Query query = query("from ", LayerInfo.class, " where id = ", param(id));
        return (LayerInfo) first(query);
    }

    public LayerInfo getLayerByName(String name) {
        Query query = query("from ", LayerInfo.class, " where resource.name = ", param(name));
        return (LayerInfo) first(query);
    }
    
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        Query query = 
            query("from ", LayerInfo.class, " where resource.id = ", param(resource.getId()));
        return (List<LayerInfo>) query.list();
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        Query query = query("from ", LayerInfo.class, " where defaultStyle.id = ", param(style.getId()));
          
        //TODO: we need to check layer.styles as well, nto sure how to do this with hql...
        //  "or style in elements(layer.styles)", 
        //   " and style.id = ", param(style.getId()));
        return (List<LayerInfo>) query.list();
    }

    public List<LayerInfo> getLayers() {
        return list(LayerInfo.class);
    }
    
    //
    // layer groups
    //
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return persist(layerGroup);
    }

    public void save(LayerGroupInfo layerGroup) {
        merge(layerGroup);
    }
    
    public void remove(LayerGroupInfo layerGroup) {
        delete(layerGroup);
    }
    
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }
    
    public LayerGroupInfo getLayerGroup(String id) {
        Query query = query("from ", LayerGroupInfo.class, " where id = ", param(id));
        return (LayerGroupInfo) first(query);
    }

    public LayerGroupInfo getLayerGroupByName(String name) {
        Query query = query("from ", LayerGroupInfo.class, " where name = ", param(name));
        return (LayerGroupInfo) first(query);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace,
            String name) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return list(LayerGroupInfo.class);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(
            WorkspaceInfo workspace) {
        throw new UnsupportedOperationException("not implemented");
    }
    
    //
    // maps
    //
    public MapInfo add(MapInfo map) {
        return null;
    }
    
    public void save(MapInfo map) {
    }

    public void remove(MapInfo map) {
    }

    public MapInfo detach(MapInfo map) {
        return map;
    }
    
    public MapInfo getMap(String id) {
        return null;
    }

    public MapInfo getMapByName(String name) {
        return null;
    }

    public List<MapInfo> getMaps() {
        return null;
    }

    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        return false;
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of,
            Filter filter, Integer offset, Integer count, SortBy sortOrder) {
        throw new UnsupportedOperationException();
    }
    //
    // Utilities
    //
    WorkspaceInfo resolve(WorkspaceInfo ws) {
        if (ws.getId() == null) {
            WorkspaceInfo resolved = getWorkspaceByName(ws.getName());
            if (resolved != null) {
                return resolved;
            }
        }
        return ws;
    }
    
    NamespaceInfo resolve(NamespaceInfo ns) {
        if (ns.getId() == null) {
            NamespaceInfo resolved = getNamespaceByPrefix(ns.getPrefix());
            if (resolved != null) {
                return resolved;
            }
        }
        
        return ns;
    }
    
    public void dispose() {
    }

    public void resolve() {
    }

    public void syncTo(CatalogFacade other) {
        throw new UnsupportedOperationException();
    }
    
}
