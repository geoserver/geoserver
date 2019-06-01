/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Default catalog facade implementation in which all objects are stored in memory.
 *
 * @author Justin Deoliveira, OpenGeo
 *     <p>TODO: look for any exceptions, move them back to catalog as they indicate logic
 */
public class DefaultCatalogFacade extends AbstractCatalogFacade implements CatalogFacade {

    /**
     * The name uses the workspace id as it does not need to be updated when the workspace is
     * renamed
     */
    static final Function<StoreInfo, Name> STORE_NAME_MAPPER =
            s -> new NameImpl(s.getWorkspace().getId(), s.getName());

    /**
     * The name uses the namspace id as it does not need to be updated when the namespace is renamed
     */
    static final Function<ResourceInfo, Name> RESOURCE_NAME_MAPPER =
            r -> new NameImpl(r.getNamespace().getId(), r.getName());

    /** Like LayerInfo, actually delegates to the resource logic */
    static final Function<LayerInfo, Name> LAYER_NAME_MAPPER =
            l -> RESOURCE_NAME_MAPPER.apply(l.getResource());

    /**
     * The name uses the workspace id as it does not need to be updated when the workspace is
     * renamed
     */
    static final Function<LayerGroupInfo, Name> LAYERGROUP_NAME_MAPPER =
            lg ->
                    new NameImpl(
                            lg.getWorkspace() != null ? lg.getWorkspace().getId() : null,
                            lg.getName());

    static final Function<NamespaceInfo, Name> NAMESPACE_NAME_MAPPER =
            n -> new NameImpl(n.getPrefix());

    static final Function<WorkspaceInfo, Name> WORKSPACE_NAME_MAPPER =
            w -> new NameImpl(w.getName());

    static final Function<StyleInfo, Name> STYLE_NAME_MAPPER =
            s ->
                    new NameImpl(
                            s.getWorkspace() != null ? s.getWorkspace().getId() : null,
                            s.getName());

    static final class LayerInfoLookup extends CatalogInfoLookup<LayerInfo> {

        public LayerInfoLookup() {
            super(LAYER_NAME_MAPPER);
        }

        public void update(ResourceInfo proxiedValue) {
            ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxiedValue);
            ResourceInfo actualValue = (ResourceInfo) h.getProxyObject();

            Name oldName = RESOURCE_NAME_MAPPER.apply(actualValue);
            Name newName = RESOURCE_NAME_MAPPER.apply(proxiedValue);
            if (!oldName.equals(newName)) {
                Map<Name, LayerInfo> nameMap = getMapForValue(nameMultiMap, LayerInfoImpl.class);
                LayerInfo value = nameMap.remove(oldName);
                // handle case of feature type without a corresponding layer
                if (value != null) {
                    nameMap.put(newName, value);
                }
            }
        }

        @Override
        public LayerInfoLookup setCatalog(Catalog catalog) {
            super.setCatalog(catalog);
            return this;
        }
    }

    /** Contains the stores keyed by implementation class */
    protected CatalogInfoLookup<StoreInfo> stores = new CatalogInfoLookup<>(STORE_NAME_MAPPER);

    /** The default store keyed by workspace id */
    protected Map<String, DataStoreInfo> defaultStores =
            new ConcurrentHashMap<String, DataStoreInfo>();

    /** resources */
    protected CatalogInfoLookup<ResourceInfo> resources =
            new CatalogInfoLookup<>(RESOURCE_NAME_MAPPER);

    /** The default namespace */
    protected volatile NamespaceInfo defaultNamespace;

    /** namespaces */
    protected CatalogInfoLookup<NamespaceInfo> namespaces =
            new CatalogInfoLookup<>(NAMESPACE_NAME_MAPPER);

    /** The default workspace */
    protected volatile WorkspaceInfo defaultWorkspace;

    /** workspaces */
    protected CatalogInfoLookup<WorkspaceInfo> workspaces =
            new CatalogInfoLookup<>(WORKSPACE_NAME_MAPPER);

    /** layers */
    protected LayerInfoLookup layers = new LayerInfoLookup();

    /** maps */
    protected List<MapInfo> maps = new CopyOnWriteArrayList<MapInfo>();

    /** layer groups */
    protected CatalogInfoLookup<LayerGroupInfo> layerGroups =
            new CatalogInfoLookup<>(LAYERGROUP_NAME_MAPPER);

    /** styles */
    protected CatalogInfoLookup<StyleInfo> styles = new CatalogInfoLookup<>(STYLE_NAME_MAPPER);

    /** the catalog */
    private CatalogImpl catalog;

    public DefaultCatalogFacade(Catalog catalog) {
        setCatalog(catalog);
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = (CatalogImpl) catalog;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    //
    // Stores
    //
    public StoreInfo add(StoreInfo store) {
        resolve(store);
        stores.add(store);
        return ModificationProxy.create(store, StoreInfo.class);
    }

    public void remove(StoreInfo store) {
        store = unwrap(store);

        synchronized (stores) {
            stores.remove(store);
        }
    }

    public void save(StoreInfo store) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(store);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(store, propertyNames, oldValues, newValues);
        stores.update(store);
        commitProxy(store);
        afterSaved(store, propertyNames, oldValues, newValues);
    }

    public <T extends StoreInfo> T detach(T store) {
        return store;
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        T store = stores.findById(id, clazz);
        return wrapInModificationProxy(store, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        T result;
        if (workspace == ANY_WORKSPACE) {
            result = stores.findFirst(clazz, s -> name.equals(s.getName()));
        } else {
            Name qname = new NameImpl((workspace != null) ? workspace.getId() : null, name);
            result = stores.findByName(qname, clazz);
        }

        return wrapInModificationProxy(result, clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        // TODO: support ANY_WORKSPACE?
        WorkspaceInfo ws;
        if (workspace == null) {
            ws = getDefaultWorkspace();
        } else {
            ws = workspace;
        }

        List<T> matches = stores.list(clazz, s -> ws.equals(s.getWorkspace()));
        return ModificationProxy.createList(matches, clazz);
    }

    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return ModificationProxy.createList(stores.list(clazz, CatalogInfoLookup.TRUE), clazz);
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        if (defaultStores.containsKey(workspace.getId())) {
            DataStoreInfo defaultStore = defaultStores.get(workspace.getId());
            return ModificationProxy.create(defaultStore, DataStoreInfo.class);
        } else {
            return null;
        }
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        DataStoreInfo old = defaultStores.get(workspace.getId());

        // fire modify event before change
        catalog.fireModified(
                catalog,
                Arrays.asList("defaultDataStore"),
                Arrays.asList(old),
                Arrays.asList(store));

        synchronized (defaultStores) {
            if (store != null) {
                defaultStores.put(workspace.getId(), store);
            } else {
                defaultStores.remove(workspace.getId());
            }
        }

        // fire postmodify event after change
        catalog.firePostModified(
                catalog,
                Arrays.asList("defaultDataStore"),
                Arrays.asList(old),
                Arrays.asList(store));
    }

    //
    // Resources
    //
    public ResourceInfo add(ResourceInfo resource) {
        resolve(resource);
        synchronized (resources) {
            resources.add(resource);
        }
        return ModificationProxy.create(resource, ResourceInfo.class);
    }

    public void remove(ResourceInfo resource) {
        resource = unwrap(resource);
        synchronized (resources) {
            resources.remove(resource);
        }
    }

    public void save(ResourceInfo resource) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(resource);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(resource, propertyNames, oldValues, newValues);
        resources.update(resource);
        layers.update(resource);
        commitProxy(resource);
        afterSaved(resource, propertyNames, oldValues, newValues);
    }

    public <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }

    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        T result = resources.findById(id, clazz);
        return wrapInModificationProxy(result, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        T result;
        if (namespace == ANY_NAMESPACE) {
            result = resources.findFirst(clazz, r -> name.equals(r.getName()));
        } else {
            Name qname = new NameImpl(namespace != null ? namespace.getId() : null, name);
            result = resources.findByName(qname, clazz);
        }

        return wrapInModificationProxy(result, clazz);
    }

    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return ModificationProxy.createList(resources.list(clazz, CatalogInfoLookup.TRUE), clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        // TODO: support ANY_NAMESPACE?
        NamespaceInfo ns;
        if (namespace == null) {
            ns = getDefaultNamespace();
        } else {
            ns = namespace;
        }

        List<T> matches = resources.list(clazz, r -> ns.equals(r.getNamespace()));
        return ModificationProxy.createList(matches, clazz);
    }

    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        T resource = null;
        NamespaceInfo ns = null;
        if (store.getWorkspace() != null
                && store.getWorkspace().getName() != null
                && (ns = getNamespaceByPrefix(store.getWorkspace().getName())) != null) {
            resource = resources.findByName(new NameImpl(ns.getId(), name), clazz);
            if (resource != null && !(store.equals(resource.getStore()))) {
                return null;
            }
        } else {
            // should not happen, but some broken test code sets up namespaces without equivalent
            // workspaces
            // or stores without workspaces
            resource =
                    resources.findFirst(
                            clazz, r -> name.equals(r.getName()) && store.equals(r.getStore()));
        }
        return wrapInModificationProxy(resource, clazz);
    }

    private <T extends CatalogInfo> T wrapInModificationProxy(T ci, Class<T> clazz) {
        if (ci != null) {
            return ModificationProxy.create(ci, clazz);
        } else {
            return null;
        }
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        List<T> matches = resources.list(clazz, r -> store.equals(r.getStore()));
        return ModificationProxy.createList(matches, clazz);
    }

    //
    // Layers
    //
    public LayerInfo add(LayerInfo layer) {
        resolve(layer);
        layers.add(layer);

        return ModificationProxy.create(layer, LayerInfo.class);
    }

    public void remove(LayerInfo layer) {
        layers.remove(unwrap(layer));
    }

    public void save(LayerInfo layer) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(layer);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(layer, propertyNames, oldValues, newValues);
        layers.update(layer);
        commitProxy(layer);
        afterSaved(layer, propertyNames, oldValues, newValues);
    }

    public LayerInfo detach(LayerInfo layer) {
        return layer;
    }

    public LayerInfo getLayer(String id) {
        LayerInfo li = layers.findById(id, LayerInfo.class);
        return wrapInModificationProxy(li, LayerInfo.class);
    }

    public LayerInfo getLayerByName(String name) {
        LayerInfo result = layers.findFirst(LayerInfo.class, li -> name.equals(li.getName()));
        return wrapInModificationProxy(result, LayerInfo.class);
    }

    public List<LayerInfo> getLayers(ResourceInfo resource) {
        // in the current setup we cannot have multiple layers associated to the same
        // resource, as they would all share the same name (the one of the resource) so
        // a direct lookup becomes possible
        Name name = RESOURCE_NAME_MAPPER.apply(resource);
        LayerInfo layer = layers.findByName(name, LayerInfo.class);
        if (layer == null) {
            return Collections.emptyList();
        } else {
            List<LayerInfo> matches = new ArrayList<>();
            matches.add(layer);
            return ModificationProxy.createList(matches, LayerInfo.class);
        }

        // we check the id first as it's faster to compare than a full blown equals
        // String id = resource.getId();
        // List<LayerInfo> matches = layers.list(LayerInfo.class, li ->
        // id.equals(li.getResource().getId()) && resource.equals(li.getResource()));
        //     return ModificationProxy.createList(matches,LayerInfo.class);
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        List<LayerInfo> matches =
                layers.list(
                        LayerInfo.class,
                        li -> style.equals(li.getDefaultStyle()) || li.getStyles().contains(style));
        return ModificationProxy.createList(matches, LayerInfo.class);
    }

    public List<LayerInfo> getLayers() {
        return ModificationProxy.createList(new ArrayList<>(layers.values()), LayerInfo.class);
    }

    //
    // Maps
    //
    public MapInfo add(MapInfo map) {
        resolve(map);
        synchronized (maps) {
            maps.add(map);
        }

        return ModificationProxy.create(map, MapInfo.class);
    }

    public void remove(MapInfo map) {
        synchronized (maps) {
            maps.remove(unwrap(map));
        }
    }

    public void save(MapInfo map) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(map);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(map, propertyNames, oldValues, newValues);
        commitProxy(map);
        afterSaved(map, propertyNames, oldValues, newValues);
    }

    public MapInfo detach(MapInfo map) {
        return map;
    }

    public MapInfo getMap(String id) {
        for (MapInfo map : maps) {
            if (id.equals(map.getId())) {
                return ModificationProxy.create(map, MapInfo.class);
            }
        }

        return null;
    }

    public MapInfo getMapByName(String name) {
        for (MapInfo map : maps) {
            if (name.equals(map.getName())) {
                return ModificationProxy.create(map, MapInfo.class);
            }
        }

        return null;
    }

    public List<MapInfo> getMaps() {
        return ModificationProxy.createList(new ArrayList(maps), MapInfo.class);
    }

    //
    // Layer groups
    //
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        resolve(layerGroup);
        synchronized (layerGroups) {
            layerGroups.add(layerGroup);
        }
        return ModificationProxy.create(layerGroup, LayerGroupInfo.class);
    }

    /* (non-Javadoc)
     * @see org.geoserver.catalog.impl.CatalogDAO#remove(org.geoserver.catalog.LayerGroupInfo)
     */
    public void remove(LayerGroupInfo layerGroup) {
        synchronized (layerGroups) {
            layerGroups.remove(unwrap(layerGroup));
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.catalog.impl.CatalogDAO#save(org.geoserver.catalog.LayerGroupInfo)
     */
    public void save(LayerGroupInfo layerGroup) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(layerGroup);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(layerGroup, propertyNames, oldValues, newValues);
        layerGroups.update(layerGroup);
        commitProxy(layerGroup);
        afterSaved(layerGroup, propertyNames, oldValues, newValues);
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return ModificationProxy.createList(
                new ArrayList<>(layerGroups.values()), LayerGroupInfo.class);
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        // TODO: support ANY_WORKSPACE?

        WorkspaceInfo ws;
        if (workspace == null) {
            ws = getDefaultWorkspace();
        } else {
            ws = workspace;
        }
        Predicate<LayerGroupInfo> predicate;
        if (workspace == NO_WORKSPACE) {
            predicate = lg -> lg.getWorkspace() == null;
        } else {
            predicate = lg -> ws.equals(lg.getWorkspace());
        }

        List<LayerGroupInfo> matches = layerGroups.list(LayerGroupInfo.class, predicate);
        return ModificationProxy.createList(matches, LayerGroupInfo.class);
    }

    public LayerGroupInfo getLayerGroup(String id) {
        LayerGroupInfo result = layerGroups.findById(id, LayerGroupInfo.class);
        return wrapInModificationProxy(result, LayerGroupInfo.class);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return getLayerGroupByName(NO_WORKSPACE, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        LayerGroupInfo match;
        if (workspace == NO_WORKSPACE) {
            match = layerGroups.findByName(new NameImpl(null, name), LayerGroupInfo.class);
        } else if (ANY_WORKSPACE == workspace) {
            match = layerGroups.findFirst(LayerGroupInfo.class, lg -> name.equals(lg.getName()));
        } else {
            match =
                    layerGroups.findByName(
                            new NameImpl(workspace.getId(), name), LayerGroupInfo.class);
        }
        return wrapInModificationProxy(match, LayerGroupInfo.class);
    }

    //
    // Namespaces
    //
    public NamespaceInfo add(NamespaceInfo namespace) {
        resolve(namespace);
        NamespaceInfo unwrapped = unwrap(namespace);
        namespaces.add(unwrapped);

        return ModificationProxy.create(unwrapped, NamespaceInfo.class);
    }

    public void remove(NamespaceInfo namespace) {
        if (namespace.equals(defaultNamespace)) {
            defaultNamespace = null;
        }

        namespaces.remove(namespace);
    }

    public void save(NamespaceInfo namespace) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(namespace);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(namespace, propertyNames, oldValues, newValues);
        namespaces.update(namespace);
        commitProxy(namespace);
        afterSaved(namespace, propertyNames, oldValues, newValues);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }

    public NamespaceInfo getDefaultNamespace() {
        return wrapInModificationProxy(defaultNamespace, NamespaceInfo.class);
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        NamespaceInfo old = this.defaultNamespace;
        // fire modify event before change
        catalog.fireModified(
                catalog,
                Arrays.asList("defaultNamespace"),
                Arrays.asList(old),
                Arrays.asList(defaultNamespace));

        this.defaultNamespace = unwrap(defaultNamespace);

        // fire postmodify event after change
        catalog.firePostModified(
                catalog,
                Arrays.asList("defaultNamespace"),
                Arrays.asList(old),
                Arrays.asList(defaultNamespace));
    }

    public NamespaceInfo getNamespace(String id) {
        NamespaceInfo ns = namespaces.findById(id, NamespaceInfo.class);
        return wrapInModificationProxy(ns, NamespaceInfo.class);
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        NamespaceInfo ns = namespaces.findByName(new NameImpl(prefix), NamespaceInfo.class);
        return wrapInModificationProxy(ns, NamespaceInfo.class);
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        NamespaceInfo result =
                namespaces.findFirst(NamespaceInfo.class, ns -> uri.equals(ns.getURI()));
        return wrapInModificationProxy(result, NamespaceInfo.class);
    }

    @Override
    public List<NamespaceInfo> getNamespacesByURI(String uri) {
        List<NamespaceInfo> found =
                namespaces.list(
                        NamespaceInfo.class, namespaceInfo -> namespaceInfo.getURI().equals(uri));
        return ModificationProxy.createList(found, NamespaceInfo.class);
    }

    public List<NamespaceInfo> getNamespaces() {
        return ModificationProxy.createList(
                new ArrayList<>(namespaces.values()), NamespaceInfo.class);
    }

    //
    // Workspaces
    //
    // Workspace methods
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        resolve(workspace);
        WorkspaceInfo unwrapped = unwrap(workspace);
        workspaces.add(unwrapped);
        return ModificationProxy.create(unwrapped, WorkspaceInfo.class);
    }

    public void remove(WorkspaceInfo workspace) {
        if (workspace.equals(this.defaultWorkspace)) {
            this.defaultWorkspace = null;
        }
        workspaces.remove(workspace);
    }

    public void save(WorkspaceInfo workspace) {
        // need to synch up the default store lookup
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(workspace);
        WorkspaceInfo ws = (WorkspaceInfo) h.getProxyObject();
        if (!workspace.getName().equals(ws.getName())) {
            DataStoreInfo ds = defaultStores.remove(ws.getName());
            if (ds != null) {
                defaultStores.put(workspace.getName(), ds);
            }
        }

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(workspace, propertyNames, oldValues, newValues);
        workspaces.update(workspace);
        commitProxy(workspace);
        afterSaved(workspace, propertyNames, oldValues, newValues);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return wrapInModificationProxy(defaultWorkspace, WorkspaceInfo.class);
    }

    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo old = defaultWorkspace;
        // fire modify event before change
        catalog.fireModified(
                catalog,
                Arrays.asList("defaultWorkspace"),
                Arrays.asList(old),
                Arrays.asList(workspace));

        this.defaultWorkspace = unwrap(workspace);

        // fire postmodify event after change
        catalog.firePostModified(
                catalog,
                Arrays.asList("defaultWorkspace"),
                Arrays.asList(old),
                Arrays.asList(workspace));
    }

    public List<WorkspaceInfo> getWorkspaces() {
        return ModificationProxy.createList(
                new ArrayList<>(workspaces.values()), WorkspaceInfo.class);
    }

    public WorkspaceInfo getWorkspace(String id) {
        WorkspaceInfo ws = workspaces.findById(id, WorkspaceInfo.class);
        return wrapInModificationProxy(ws, WorkspaceInfo.class);
    }

    public WorkspaceInfo getWorkspaceByName(String name) {
        WorkspaceInfo ws = workspaces.findByName(new NameImpl(name), WorkspaceInfo.class);
        return wrapInModificationProxy(ws, WorkspaceInfo.class);
    }

    //
    // Styles
    //
    public StyleInfo add(StyleInfo style) {
        resolve(style);
        synchronized (styles) {
            styles.add(style);
        }
        return ModificationProxy.create(style, StyleInfo.class);
    }

    public void remove(StyleInfo style) {
        synchronized (styles) {
            styles.remove(unwrap(style));
        }
    }

    public void save(StyleInfo style) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);

        // figure out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(style, propertyNames, oldValues, newValues);
        styles.update(style);
        commitProxy(style);
        afterSaved(style, propertyNames, oldValues, newValues);
    }

    public StyleInfo detach(StyleInfo style) {
        return style;
    }

    public StyleInfo getStyle(String id) {
        StyleInfo match = styles.findById(id, StyleInfo.class);
        return wrapInModificationProxy(match, StyleInfo.class);
    }

    public StyleInfo getStyleByName(String name) {
        StyleInfo match = styles.findByName(new NameImpl(null, name), StyleInfo.class);
        if (match == null) {
            match = styles.findFirst(StyleInfo.class, s -> name.equals(s.getName()));
        }
        return wrapInModificationProxy(match, StyleInfo.class);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        if (null == workspace) {
            throw new NullPointerException("workspace");
        }
        if (null == name) {
            throw new NullPointerException("name");
        }
        if (workspace == ANY_WORKSPACE) {
            return getStyleByName(name);
        } else {
            Name sn = new NameImpl(workspace == null ? null : workspace.getId(), name);
            StyleInfo match = styles.findByName(sn, StyleInfo.class);
            return wrapInModificationProxy(match, StyleInfo.class);
        }
    }

    public List<StyleInfo> getStyles() {
        return ModificationProxy.createList(new ArrayList<>(styles.values()), StyleInfo.class);
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        // TODO: support ANY_WORKSPACE?
        List<StyleInfo> matches;
        if (workspace == NO_WORKSPACE) {
            matches = styles.list(StyleInfo.class, s -> s.getWorkspace() == null);
        } else {
            WorkspaceInfo ws;
            if (workspace == null) {
                ws = getDefaultWorkspace();
            } else {
                ws = workspace;
            }

            matches = styles.list(StyleInfo.class, s -> ws.equals(s.getWorkspace()));
        }

        return ModificationProxy.createList(matches, StyleInfo.class);
    }

    public void dispose() {
        if (stores != null) stores.clear();
        if (defaultStores != null) defaultStores.clear();
        if (resources != null) resources.clear();
        if (namespaces != null) namespaces.clear();
        if (workspaces != null) workspaces.clear();
        if (layers != null) layers.clear();
        if (layerGroups != null) layerGroups.clear();
        if (maps != null) maps.clear();
        if (styles != null) styles.clear();
    }

    public void resolve() {
        // JD creation checks are done here b/c when xstream depersists
        // some members may be left null

        // workspaces
        if (workspaces == null) {
            workspaces = new CatalogInfoLookup<>(WORKSPACE_NAME_MAPPER);
        }
        for (WorkspaceInfo ws : workspaces.values()) {
            resolve(ws);
        }

        // namespaces
        if (namespaces == null) {
            namespaces = new CatalogInfoLookup<>(NAMESPACE_NAME_MAPPER);
        }
        for (NamespaceInfo ns : namespaces.values()) {
            resolve(ns);
        }

        // stores
        if (stores == null) {
            stores = new CatalogInfoLookup<>(STORE_NAME_MAPPER);
        }
        for (Object o : stores.values()) {
            resolve((StoreInfoImpl) o);
        }

        // styles
        if (styles == null) {
            styles = new CatalogInfoLookup<>(STYLE_NAME_MAPPER);
        }
        for (StyleInfo s : styles.values()) {
            resolve(s);
        }

        // resources
        if (resources == null) {
            resources = new CatalogInfoLookup<>(RESOURCE_NAME_MAPPER);
        }
        for (Object o : resources.values()) {
            resolve((ResourceInfo) o);
        }

        // layers
        if (layers == null) {
            layers = new LayerInfoLookup();
        }
        for (LayerInfo l : layers.values()) {
            resolve(l);
        }

        // layer groups
        if (layerGroups == null) {
            layerGroups = new CatalogInfoLookup<>(LAYERGROUP_NAME_MAPPER);
        }
        for (LayerGroupInfo lg : layerGroups.values()) {
            resolve(lg);
        }

        // maps
        if (maps == null) {
            maps = new ArrayList<MapInfo>();
        }
        for (MapInfo m : maps) {
            resolve(m);
        }
    }

    public void syncTo(CatalogFacade dao) {
        dao = ProxyUtils.unwrap(dao, LockingCatalogFacade.class);
        if (dao instanceof DefaultCatalogFacade) {
            // do an optimized sync
            DefaultCatalogFacade other = (DefaultCatalogFacade) dao;

            other.stores = stores.setCatalog(catalog);
            other.defaultStores = defaultStores;
            other.resources = resources.setCatalog(catalog);
            other.defaultNamespace = defaultNamespace;
            other.namespaces = namespaces.setCatalog(catalog);
            other.defaultWorkspace = defaultWorkspace;
            other.workspaces = workspaces.setCatalog(catalog);
            other.layers = layers.setCatalog(catalog);
            other.maps = maps;
            other.layerGroups = layerGroups.setCatalog(catalog);
            other.styles = styles.setCatalog(catalog);
        } else {
            // do a manual import
            for (WorkspaceInfo ws : workspaces.values()) {
                dao.add(ws);
            }
            for (NamespaceInfo ns : namespaces.values()) {
                dao.add(ns);
            }
            for (StoreInfo store : stores.values()) {
                dao.add(store);
            }
            for (ResourceInfo resource : resources.values()) {
                dao.add(resource);
            }
            for (StyleInfo s : styles.values()) {
                dao.add(s);
            }
            for (LayerInfo l : layers.values()) {
                dao.add(l);
            }
            for (LayerGroupInfo lg : layerGroups.values()) {
                dao.add(lg);
            }
            for (MapInfo m : maps) {
                dao.add(m);
            }
            if (defaultWorkspace != null) {
                dao.setDefaultWorkspace(defaultWorkspace);
            }
            if (defaultNamespace != null) {
                dao.setDefaultNamespace(defaultNamespace);
            }

            for (Map.Entry<String, DataStoreInfo> e : defaultStores.entrySet()) {
                WorkspaceInfo ws = workspaces.findById(e.getKey(), WorkspaceInfo.class);
                if (null != ws) {
                    dao.setDefaultDataStore(ws, e.getValue());
                }
            }
        }
    }

    @Override
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        return Iterables.size(iterable(of, filter, null));
    }

    /**
     * This default implementation supports sorting against properties (could be nested) that are
     * either of a primitive type or implement {@link Comparable}.
     *
     * @param type the type of object to sort
     * @param propertyName the property name of the objects of type {@code type} to sort by
     * @see org.geoserver.catalog.CatalogFacade#canSort(java.lang.Class, java.lang.String)
     */
    @Override
    public boolean canSort(final Class<? extends CatalogInfo> type, final String propertyName) {
        final String[] path = propertyName.split("\\.");
        Class<?> clazz = type;
        for (int i = 0; i < path.length; i++) {
            String property = path[i];
            Method getter;
            try {
                getter = OwsUtils.getter(clazz, property, null);
            } catch (RuntimeException e) {
                return false;
            }
            clazz = getter.getReturnType();
            if (i == path.length - 1) {
                boolean primitive = clazz.isPrimitive();
                boolean comparable = Comparable.class.isAssignableFrom(clazz);
                boolean canSort = primitive || comparable;
                return canSort;
            }
        }
        throw new IllegalStateException("empty property name");
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {

        if (sortOrder != null) {
            for (SortBy so : sortOrder) {
                if (sortOrder != null && !canSort(of, so.getPropertyName().getPropertyName())) {
                    throw new IllegalArgumentException(
                            "Can't sort objects of type "
                                    + of.getName()
                                    + " by "
                                    + so.getPropertyName());
                }
            }
        }

        Iterable<T> iterable = iterable(of, filter, sortOrder);

        if (offset != null && offset.intValue() > 0) {
            iterable = Iterables.skip(iterable, offset.intValue());
        }

        if (count != null && count.intValue() >= 0) {
            iterable = Iterables.limit(iterable, count.intValue());
        }

        Iterator<T> iterator = iterable.iterator();

        return new CloseableIteratorAdapter<T>(iterator);
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo> Iterable<T> iterable(
            final Class<T> of, final Filter filter, final SortBy[] sortByList) {
        List<T> all;

        if (NamespaceInfo.class.isAssignableFrom(of)) {
            all = (List<T>) namespaces.list(of, toPredicate(filter));
        } else if (WorkspaceInfo.class.isAssignableFrom(of)) {
            all = (List<T>) workspaces.list(of, toPredicate(filter));
        } else if (StoreInfo.class.isAssignableFrom(of)) {
            all = (List<T>) stores.list(of, toPredicate(filter));
        } else if (ResourceInfo.class.isAssignableFrom(of)) {
            all = (List<T>) resources.list(of, toPredicate(filter));
        } else if (LayerInfo.class.isAssignableFrom(of)) {
            all = (List<T>) layers.list(of, toPredicate(filter));
        } else if (LayerGroupInfo.class.isAssignableFrom(of)) {
            all = (List<T>) layerGroups.list(of, toPredicate(filter));
        } else if (PublishedInfo.class.isAssignableFrom(of)) {
            all = new ArrayList<>();
            all.addAll((List<T>) layers.list(LayerInfo.class, toPredicate(filter)));
            all.addAll((List<T>) layerGroups.list(LayerGroupInfo.class, toPredicate(filter)));
        } else if (StyleInfo.class.isAssignableFrom(of)) {
            all = (List<T>) styles.list(of, toPredicate(filter));
        } else if (MapInfo.class.isAssignableFrom(of)) {
            all = (List<T>) new ArrayList<>(maps);
        } else {
            throw new IllegalArgumentException("Unknown type: " + of);
        }

        if (null != sortByList) {
            for (int i = sortByList.length - 1; i >= 0; i--) {
                SortBy sortBy = sortByList[i];
                Ordering<Object> ordering = Ordering.from(comparator(sortBy));
                if (SortOrder.DESCENDING.equals(sortBy.getSortOrder())) {
                    ordering = ordering.reverse();
                }
                all = ordering.sortedCopy(all);
            }
        }

        return ModificationProxy.createList(all, of);
    }

    private <T> Predicate<T> toPredicate(Filter filter) {
        if (filter != null && filter != Filter.INCLUDE) {
            return o -> filter.evaluate(o);
        } else {
            return CatalogInfoLookup.TRUE;
        }
    }

    private Comparator<Object> comparator(final SortBy sortOrder) {
        return new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                Object v1 = OwsUtils.get(o1, sortOrder.getPropertyName().getPropertyName());
                Object v2 = OwsUtils.get(o2, sortOrder.getPropertyName().getPropertyName());
                if (v1 == null) {
                    if (v2 == null) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (v2 == null) {
                    return 1;
                }
                Comparable c1 = (Comparable) v1;
                Comparable c2 = (Comparable) v2;
                return c1.compareTo(c2);
            }
        };
    }
}
