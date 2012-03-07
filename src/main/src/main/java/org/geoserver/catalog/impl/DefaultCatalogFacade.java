package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections.MultiHashMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;

/**
 * Default catalog facade implementation in which all objects are stored in memory.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 * TODO: look for any exceptions, move them back to catlaog as they indicate logic
 */
public class DefaultCatalogFacade implements CatalogFacade {
    
    public static WorkspaceInfo ANY_WORKSPACE = any(WorkspaceInfo.class);
    
    public static NamespaceInfo ANY_NAMESPACE = any(NamespaceInfo.class);
    
    @SuppressWarnings("unchecked")
    static <T extends CatalogInfo>  T any(Class<T> clazz) {
        
        Class proxyClass = Proxy.getProxyClass(clazz.getClassLoader(), clazz);
        try {
            return (T) proxyClass.getConstructor(
                new Class[] { InvocationHandler.class }).newInstance(new Object[] {
                    new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return null;
                        }
                    }
                } );
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    /**
     * Contains the stores keyed by implementation class
     */
    protected MultiHashMap/* <Class> */stores = new MultiHashMap();
    
    /**
     * The default store keyed by workspace id
     */
    protected Map<String, DataStoreInfo> defaultStores = new HashMap<String, DataStoreInfo>();

    /**
     * resources
     */
    protected MultiHashMap/* <Class> */resources = new MultiHashMap();

    /**
     * namespaces
     */
    protected HashMap<String, NamespaceInfo> namespaces = new HashMap<String, NamespaceInfo>();

    /**
     * workspaces
     */
    protected HashMap<String, WorkspaceInfo> workspaces = new HashMap<String, WorkspaceInfo>();
    
    //JD: Using a CopyOnWriteArrayList is a temporary measure here to deal with some 
    // concurrency issues around layer access. See GEOS-4404. Long term solution is to us
    // concurrent collections (set and map) for all the collections in this class
    /**
     * layers
     */
    protected List<LayerInfo> layers = new CopyOnWriteArrayList<LayerInfo>();

    /**
     * maps
     */
    protected List<MapInfo> maps = new ArrayList<MapInfo>();

    /**
     * layer groups
     */
    protected List<LayerGroupInfo> layerGroups = new ArrayList<LayerGroupInfo>();
    
    /**
     * styles
     */
    protected List<StyleInfo> styles = new ArrayList();

    /**
     * the catalog
     */
    private CatalogImpl catalog;
    
    public DefaultCatalogFacade(Catalog catalog) {
        setCatalog(catalog);
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = (CatalogImpl) catalog;
    }
    
    public Catalog getCatalog() {
        return null;
    }
    
    //
    // Stores
    //
    public StoreInfo add(StoreInfo store) {
        resolve(store);
        synchronized(stores) {
            stores.put(store.getClass(), store);
        }
        return ModificationProxy.create(store, StoreInfo.class);
    }
    
    public void remove(StoreInfo store) {
        store = unwrap(store);

        synchronized(stores) {
            stores.remove(store.getClass(),store);
        }
    }
    
    public void save(StoreInfo store) {
        saved(store);
    }
    
    public <T extends StoreInfo> T detach(T store) {
        return store;
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        List l = lookup(clazz, stores);
        for (Iterator i = l.iterator(); i.hasNext();) {
            StoreInfo store = (StoreInfo) i.next();
            if (id.equals(store.getId())) {
                return ModificationProxy.create( (T) store, clazz );
            }
        }

        return null;
    }

    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace,
            String name, Class<T> clazz) {
        
        List l = lookup(clazz, stores);
        if (workspace == ANY_WORKSPACE) {
            //do an exhaustive search through all workspaces
            ArrayList matches = new ArrayList();
            for (Iterator i = l.iterator(); i.hasNext();) {
                T store = (T) i.next();
                if ( name.equals( store.getName() ) ) {
                    matches.add( store );
                }
            }
            
            if ( matches.size() == 1 ) {
                return ModificationProxy.create( (T) matches.get( 0 ), clazz);
            }
        }
        else {
            
            for (Iterator i = l.iterator(); i.hasNext();) {
                StoreInfo store = (StoreInfo) i.next();
                if (name.equals(store.getName()) && store.getWorkspace().equals( workspace )) {
                    return ModificationProxy.create( (T) store, clazz );
                }
            }
        }
        return null;
    }
    
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        //TODO: support ANY_WORKSPACE?
        
        if ( workspace == null ) {
            workspace = getDefaultWorkspace();
        }

        List all = lookup(clazz, stores);
        List matches = new ArrayList();

        for (Iterator s = all.iterator(); s.hasNext();) {
            StoreInfo store = (StoreInfo) s.next();
            if (workspace.equals(store.getWorkspace())) {
                matches.add(store);
            }
        }

        return ModificationProxy.createList(matches,clazz);
    }
    
    public List getStores(Class clazz) {
        return ModificationProxy.createList(lookup(clazz, stores) , clazz);
    }
    
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        if(defaultStores.containsKey(workspace.getId())) {
            DataStoreInfo defaultStore = defaultStores.get(workspace.getId());
            return ModificationProxy.create(defaultStore, DataStoreInfo.class);
        } else {
            return null;
        }
    }
    
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        DataStoreInfo old = defaultStores.get(workspace.getId());
        synchronized(defaultStores) {
            if (store != null) {
                defaultStores.put(workspace.getId(), store);    
            }
            else {
                defaultStores.remove(workspace.getId());
            }
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultDataStore"), Arrays.asList(old), Arrays.asList(store));
    }
    
    //
    // Resources
    //
    public ResourceInfo add(ResourceInfo resource) {
        resolve(resource);
        synchronized(resources) {
            resources.put(resource.getClass(), resource);
        }
        return ModificationProxy.create(resource, ResourceInfo.class);
    }
    
    public void remove(ResourceInfo resource) {
        resource = unwrap(resource);
        synchronized(resources) {
            resources.remove(resource.getClass(), resource);
        }
    }
    
   
    public void save(ResourceInfo resource) {
        saved(resource);
    }
    
    public <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }
    
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        List l = lookup(clazz, resources);
        for (Iterator i = l.iterator(); i.hasNext();) {
            ResourceInfo resource = (ResourceInfo) i.next();
            if (id.equals(resource.getId())) {
                return ModificationProxy.create((T) resource, clazz );
            }
        }

        return null;
    }
    
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        
        List l = lookup(clazz, resources);
        
        if (namespace == ANY_NAMESPACE) {
            //do an exhaustive lookup
            List matches = new ArrayList();
            for (Iterator i = l.iterator(); i.hasNext();) {
                ResourceInfo resource = (ResourceInfo) i.next();
                if (name.equals(resource.getName())) {
                    matches.add( resource );
                }
            }
            
            if ( matches.size() == 1 ) {
                return ModificationProxy.create( (T) matches.get( 0 ), clazz );
            }
        }
        else {
            for (Iterator i = l.iterator(); i.hasNext();) {
                ResourceInfo resource = (ResourceInfo) i.next();
                if (name.equals(resource.getName())) {
                    NamespaceInfo namespace1 = resource.getNamespace();
                    if (namespace1 != null && namespace1.equals( namespace )) {
                            return ModificationProxy.create( (T) resource, clazz );
                    }
                }
            }
        }

        return null;
    }
 
    public List getResources(Class clazz) {
        return ModificationProxy.createList( lookup(clazz,resources), clazz );
    }
    
    public List getResourcesByNamespace(NamespaceInfo namespace, Class clazz) {
        //TODO: support ANY_NAMESPACE?
        
        List all = lookup(clazz, resources);
        List matches = new ArrayList();

        if ( namespace == null ) {
            namespace = getDefaultNamespace();
        }

        for (Iterator r = all.iterator(); r.hasNext();) {
            ResourceInfo resource = (ResourceInfo) r.next();
            if (namespace != null ) {
                if (namespace.equals(resource.getNamespace())) {
                    matches.add( resource );
                }
            }
            else if ( resource.getNamespace() == null ) {
                matches.add(resource);
            }
        }

        return ModificationProxy.createList( matches, clazz );
    }
    
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store,
            String name, Class<T> clazz) {
        List all = lookup(clazz,resources);
        for (Iterator r = all.iterator(); r.hasNext(); ) {
            ResourceInfo resource = (ResourceInfo) r.next();
            if ( name.equals( resource.getName() ) && store.equals( resource.getStore() ) ) {
                return ModificationProxy.create((T)resource, clazz);
            }
        }
        
        return null;
    }
    
    public <T extends ResourceInfo> List<T> getResourcesByStore(
            StoreInfo store, Class<T> clazz) {
        List all = lookup(clazz,resources);
        List matches = new ArrayList();
        
        for (Iterator r = all.iterator(); r.hasNext();) {
            ResourceInfo resource = (ResourceInfo) r.next();
            if (store.equals(resource.getStore())) {
                matches.add(resource);
            }
        }

        return  ModificationProxy.createList( matches, clazz );
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
        saved(layer);
    }
    
    public LayerInfo detach(LayerInfo layer) {
        return layer;
    }
    
    public LayerInfo getLayer(String id) {
        for (LayerInfo layer : layers) {
            if (id.equals(layer.getId())) {
                return ModificationProxy.create( layer, LayerInfo.class );
            }
        }

        return null;
    }
    
    public LayerInfo getLayerByName(String name) {
        for (LayerInfo layer : layers) {
            if ( name.equals( layer.getName() ) ) {
                return ModificationProxy.create( layer, LayerInfo.class );
            }
        }
      
        return null;
    }
    
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        List<LayerInfo> matches = new ArrayList<LayerInfo>();
        for (LayerInfo layer : layers) {
            if ( resource.equals( layer.getResource() ) ) {
                matches.add( layer );
            }
        }

        return ModificationProxy.createList(matches,LayerInfo.class);
    }
    
    public List<LayerInfo> getLayers(StyleInfo style) {
        List<LayerInfo> matches = new ArrayList<LayerInfo>();
        for (LayerInfo layer : layers) {
            if ( style.equals( layer.getDefaultStyle() ) || layer.getStyles().contains( style ) ) {
                matches.add( layer );
            }
        }

        return ModificationProxy.createList(matches,LayerInfo.class);
    }
    
    public List<LayerInfo> getLayers() {
        return ModificationProxy.createList( new ArrayList(layers), LayerInfo.class );
    }
    
    //
    // Maps
    //
    public MapInfo add(MapInfo map) {
        resolve(map);
        synchronized(maps) {
            maps.add(map);
        }
        
        return ModificationProxy.create(map, MapInfo.class);
    }

    public void remove(MapInfo map) {
        synchronized(maps) {
            maps.remove(unwrap(map));
        }
    }

    public void save(MapInfo map) {
        saved( map );
    }
    
    public MapInfo detach(MapInfo map) {
        return map;
    }
    
    public MapInfo getMap(String id) {
        for (MapInfo map : maps) {
            if (id.equals(map.getId())) {
                return ModificationProxy.create(map,MapInfo.class);
            }
        }

        return null;
    }

    public MapInfo getMapByName(String name) {
        for (MapInfo map : maps) {
            if (name.equals(map.getName())) {
                return ModificationProxy.create(map,MapInfo.class);
            }
        }

        return null;
    }
    
    public List<MapInfo> getMaps() {
        return ModificationProxy.createList( new ArrayList(maps), MapInfo.class );
    }
    
    //
    // Layer groups
    //
    public LayerGroupInfo add (LayerGroupInfo layerGroup) {
        resolve(layerGroup);
        synchronized(layerGroups) {
            layerGroups.add( layerGroup );
        }
        return ModificationProxy.create(layerGroup, LayerGroupInfo.class);
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.catalog.impl.CatalogDAO#remove(org.geoserver.catalog.LayerGroupInfo)
     */
    public void remove(LayerGroupInfo layerGroup) {
        synchronized(layerGroups) {
            layerGroups.remove( unwrap(layerGroup) );
        }
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.catalog.impl.CatalogDAO#save(org.geoserver.catalog.LayerGroupInfo)
     */
    public void save(LayerGroupInfo layerGroup) {
        saved(layerGroup);
    }
    
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }
    
    public List<LayerGroupInfo> getLayerGroups() {
        return ModificationProxy.createList( new ArrayList(layerGroups), LayerGroupInfo.class );
    }


    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        //TODO: support ANY_WORKSPACE?
        
        if ( workspace == null ) {
            workspace = getDefaultWorkspace();
        }

        List<LayerGroupInfo> matches = new ArrayList();

        for (Iterator s = layerGroups.iterator(); s.hasNext();) {
            LayerGroupInfo layerGroup = (LayerGroupInfo) s.next();
            if (workspace.equals(layerGroup.getWorkspace())) {
                matches.add(layerGroup);
            }
        }

        return ModificationProxy.createList(matches,LayerGroupInfo.class);

    }

    public LayerGroupInfo getLayerGroup(String id) {
        for (LayerGroupInfo layerGroup : layerGroups ) {
            if ( id.equals( layerGroup.getId() ) ) {
                return ModificationProxy.create(layerGroup,LayerGroupInfo.class);
            }
        }
        
        return null;
    }
    
    public LayerGroupInfo getLayerGroupByName(String name) {
        for (LayerGroupInfo layerGroup : layerGroups ) {
            if ( name.equals( layerGroup.getName() ) ) {
                return ModificationProxy.create(layerGroup,LayerGroupInfo.class);
            }
        }
        
        return null;
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace,
            String name) {
        if (workspace == ANY_WORKSPACE) {
            //do an exhaustive search through all workspaces
            ArrayList<LayerGroupInfo> matches = new ArrayList();
            for (Iterator i = layerGroups.iterator(); i.hasNext();) {
                LayerGroupInfo layerGroup = (LayerGroupInfo) i.next();
                if ( name.equals( layerGroup.getName() ) ) {
                    matches.add( layerGroup );
                }
            }
            
            if ( matches.size() == 1 ) {
                return ModificationProxy.create( matches.get( 0 ), LayerGroupInfo.class);
            }
        }
        else {
            for (Iterator i = layerGroups.iterator(); i.hasNext();) {
                LayerGroupInfo layerGroup = (LayerGroupInfo) i.next();
                if (name.equals(layerGroup.getName())) {
                    if (layerGroup.getWorkspace() != null && layerGroup.getWorkspace().equals(workspace) || 
                        layerGroup.getWorkspace() == null && workspace == null) {
                        return ModificationProxy.create( layerGroup, LayerGroupInfo.class );
                    }
                }
            }
        }
        return null;
    }

    //
    // Namespaces
    //
    public NamespaceInfo add(NamespaceInfo namespace) {
        resolve(namespace);
        synchronized(namespaces) {
            namespaces.put(namespace.getPrefix(),namespace);
        }
        
        return ModificationProxy.create(namespace, NamespaceInfo.class);
    }
    
    public void remove(NamespaceInfo namespace) {
        synchronized(namespaces) {
            NamespaceInfo defaultNamespace = getDefaultNamespace();
            if (namespace.equals(defaultNamespace)) {
                namespaces.remove(null);
                namespaces.remove(Catalog.DEFAULT);
            }
            
            namespaces.remove(namespace.getPrefix());
        }
    }

    public void save(NamespaceInfo namespace) {
        ModificationProxy h = 
            (ModificationProxy) Proxy.getInvocationHandler(namespace);
        
        NamespaceInfo ns = (NamespaceInfo) h.getProxyObject();
        if ( !namespace.getPrefix().equals( ns.getPrefix() ) ) {
            synchronized (namespaces) {
                namespaces.remove( ns.getPrefix() );
                namespaces.put( namespace.getPrefix(), ns );
            }
        }
        
        saved(namespace);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }
    
    public NamespaceInfo getDefaultNamespace() {
        return namespaces.get(null) != null ? 
                ModificationProxy.create(namespaces.get( null ),NamespaceInfo.class) : null;
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        NamespaceInfo ns = defaultNamespace != null ? namespaces.get(defaultNamespace.getPrefix()) : null;
        NamespaceInfo old = namespaces.get(null);
        if(ns != null) {
            namespaces.put( null, ns );
            namespaces.put( Catalog.DEFAULT, ns );
        } else {
            namespaces.remove( null);
            namespaces.remove( Catalog.DEFAULT);
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultNamespace"), Arrays.asList(old), Arrays.asList(defaultNamespace));
        
    }
    
    public NamespaceInfo getNamespace(String id) {
        for (NamespaceInfo namespace : namespaces.values() ) {
            if (id.equals(namespace.getId())) {
                return ModificationProxy.create( namespace, NamespaceInfo.class ); 
            }
        }

        return null;
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        NamespaceInfo ns = namespaces.get( prefix ); 
        return ns != null ? ModificationProxy.create(ns, NamespaceInfo.class ) : null;
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        for (NamespaceInfo namespace : namespaces.values() ) {
            if (uri.equals(namespace.getURI())) {
                return ModificationProxy.create( namespace, NamespaceInfo.class );
            }
        }

        return null;
    }

    public List getNamespaces() {
        ArrayList<NamespaceInfo> ns = new ArrayList<NamespaceInfo>();
        for ( Map.Entry<String,NamespaceInfo> e : namespaces.entrySet() ) {
            if ( e.getKey() == null || e.getKey().equals(Catalog.DEFAULT)) 
                continue;
            ns.add( e.getValue() );
        }
        
        return ModificationProxy.createList( ns, NamespaceInfo.class );
    }

    //
    // Workspaces
    //
    // Workspace methods
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        resolve(workspace);
        synchronized (workspaces) {
            workspaces.put( workspace.getName(), workspace );
        }
        return ModificationProxy.create(workspace, WorkspaceInfo.class);
    }
    
    public void remove(WorkspaceInfo workspace) {
        synchronized(workspaces) {
            workspaces.remove( workspace.getName() );
        }
    }
    
    public void save(WorkspaceInfo workspace) {
        ModificationProxy h = 
            (ModificationProxy) Proxy.getInvocationHandler(workspace);
        
        WorkspaceInfo ws = (WorkspaceInfo) h.getProxyObject();
        if ( !workspace.getName().equals( ws.getName() ) ) {
            synchronized (workspaces) {
                workspaces.remove( ws.getName() );
                workspaces.put( workspace.getName(), ws );
            }
        }
        
        saved(workspace);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return workspaces.containsKey( null ) ? 
                ModificationProxy.create( workspaces.get( null ), WorkspaceInfo.class ) : null;
    }
    
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        WorkspaceInfo old = workspaces.get(null);
        
        synchronized(workspaces) {
            if (workspace != null) {
                WorkspaceInfo ws = workspaces.get(workspace.getName());
                workspaces.put( null, ws );
                workspaces.put( "default", ws );
            }
            else {
                workspaces.remove(null);
                workspaces.remove("default");
            }
            
        }
        
        //fire change event
        catalog.fireModified(catalog, 
            Arrays.asList("defaultWorkspace"), Arrays.asList(old), Arrays.asList(workspace));
    }
    
    public List<WorkspaceInfo> getWorkspaces() {
        ArrayList<WorkspaceInfo> ws = new ArrayList<WorkspaceInfo>();
        
        //strip out default namespace
        for ( Map.Entry<String, WorkspaceInfo> e : workspaces.entrySet() ) {
            if ( e.getKey() == null || e.getKey().equals(Catalog.DEFAULT) ) {
                continue;
            }
            
            ws.add( e.getValue() );
        }
        
        return ModificationProxy.createList( ws, WorkspaceInfo.class );
    }
    
    public WorkspaceInfo getWorkspace(String id) {
        for ( WorkspaceInfo ws : workspaces.values() ) {
            if ( id.equals( ws.getId() ) ) {
                return ModificationProxy.create(ws,WorkspaceInfo.class);
            }
        }
        
        return null;
    }
    
    public WorkspaceInfo getWorkspaceByName(String name) {
        return workspaces.containsKey(name) ? 
                ModificationProxy.create( workspaces.get( name ), WorkspaceInfo.class ) : null;
    }
    
    //
    // Styles
    //
    public StyleInfo add(StyleInfo style) {
        resolve(style);
        synchronized(styles) {
            styles.add(style);
        }
        return ModificationProxy.create(style, StyleInfo.class);
    }

    public void remove(StyleInfo style) {
        synchronized(styles) {
            styles.remove(unwrap(style));
        }
    }

    public void save(StyleInfo style) {
        saved( style );
    }

    public StyleInfo detach(StyleInfo style) {
        return style;
    }

    public StyleInfo getStyle(String id) {
        for (Iterator s = styles.iterator(); s.hasNext();) {
            StyleInfo style = (StyleInfo) s.next();
            if (id.equals(style.getId())) {
                return ModificationProxy.create(style,StyleInfo.class);
            }
        }

        return null;
    }

    public StyleInfo getStyleByName(String name) {
        for (Iterator s = styles.iterator(); s.hasNext();) {
            StyleInfo style = (StyleInfo) s.next();
            if (name.equals(style.getName())) {
                return ModificationProxy.create(style,StyleInfo.class);
            }
        }

        return null;
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        if (workspace == ANY_WORKSPACE) {
            //do an exhaustive search through all workspaces
            ArrayList<StyleInfo> matches = new ArrayList();
            for (Iterator i = styles.iterator(); i.hasNext();) {
                StyleInfo style = (StyleInfo) i.next();
                if ( name.equals( style.getName() ) ) {
                    matches.add( style );
                }
            }
            
            if ( matches.size() == 1 ) {
                return ModificationProxy.create( matches.get( 0 ), StyleInfo.class);
            }
        }
        else {
            for (Iterator i = styles.iterator(); i.hasNext();) {
                StyleInfo style = (StyleInfo) i.next();
                if (name.equals(style.getName())) {
                    if (style.getWorkspace() != null && style.getWorkspace().equals(workspace) || 
                        style.getWorkspace() == null && workspace == null) {
                        return ModificationProxy.create( style, StyleInfo.class );
                    }
                }
            }
        }
        return null;
    }

    public List getStyles() {
        return ModificationProxy.createList(styles,StyleInfo.class);
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        //TODO: support ANY_WORKSPACE?
        
        if ( workspace == null ) {
            workspace = getDefaultWorkspace();
        }

        List<StyleInfo> matches = new ArrayList();

        for (Iterator s = styles.iterator(); s.hasNext();) {
            StyleInfo style = (StyleInfo) s.next();
            if (workspace.equals(style.getWorkspace())) {
                matches.add(style);
            }
        }

        return ModificationProxy.createList(matches,StyleInfo.class);
    }

    //
    // Utilities
    //
    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }
    
    protected void saved(CatalogInfo object) {
        //this object is a proxy
        ModificationProxy h = 
            (ModificationProxy) Proxy.getInvocationHandler(object);
        
        //get the real object
        CatalogInfo real = (CatalogInfo) h.getProxyObject();
        
        //fire out what changed
        List propertyNames = h.getPropertyNames();
        List newValues = h.getNewValues();
        List oldValues = h.getOldValues();
        
        //TODO: protect this original object, perhaps with another proxy 
        catalog.fireModified( real, propertyNames, oldValues, newValues );
        
        //commit to the original object
        h.commit();    
        
        //resolve to do a sync on the object
        //syncIdWithName(real);
        
        //fire the post modify event
        catalog.firePostModified( real );
    }
    
    List lookup(Class clazz, MultiHashMap map) {
        ArrayList result = new ArrayList();
        for (Iterator k = map.keySet().iterator(); k.hasNext();) {
            Class key = (Class) k.next();
            if (clazz.isAssignableFrom(key)) {
                result.addAll(map.getCollection(key));
            }
        }

        return result;
    }

    public void dispose() {
        if ( stores != null ) stores.clear();
        if ( defaultStores != null ) defaultStores.clear();
        if ( resources != null ) resources.clear();
        if ( namespaces != null ) namespaces.clear();
        if ( workspaces != null ) workspaces.clear();
        if ( layers != null ) layers.clear();
        if ( layerGroups != null ) layerGroups.clear();
        if ( maps != null ) maps.clear();
        if ( styles != null ) styles.clear();
    }
    
    public void resolve() {
        //JD creation checks are done here b/c when xstream depersists 
        // some members may be left null
        
        //workspaces
        if ( workspaces == null ) {
            workspaces = new HashMap<String, WorkspaceInfo>();
        }
        for ( WorkspaceInfo ws : workspaces.values() ) {
            resolve(ws);
        }
        
        //namespaces
        if ( namespaces == null ) {
            namespaces = new HashMap<String, NamespaceInfo>();
        }
        for ( NamespaceInfo ns : namespaces.values() ) {
            resolve(ns);
        }
        
        //stores
        if ( stores == null ) {
            stores = new MultiHashMap();
        }
        for ( Object o : stores.values() ) {
            resolve((StoreInfoImpl)o);
        }
        
        //styles
        if ( styles == null ) {
            styles = new ArrayList<StyleInfo>();
        }
        for ( StyleInfo s : styles ) {
            resolve(s);
        }
        
        //resources
        if ( resources == null ) {
            resources = new MultiHashMap();    
        }
        for( Object o : resources.values() ) {
            resolve((ResourceInfo)o);
        }
        
        //layers
        if ( layers == null ) {
            layers = new CopyOnWriteArrayList<LayerInfo>();
        }
        for ( LayerInfo l : layers ) { 
            resolve(l);
        }
        
        //layer groups
        if ( layerGroups == null ) {
            layerGroups = new ArrayList<LayerGroupInfo>();    
        }
        for ( LayerGroupInfo lg : layerGroups ) {
            resolve(lg);
        }
        
        //maps
        if ( maps == null ) {
            maps = new ArrayList<MapInfo>();
        }
        for ( MapInfo m : maps ) {
            resolve(m);
        }
    }
    
    protected void resolve(WorkspaceInfo workspace) {
        setId(workspace);
    }
    
    protected void resolve(NamespaceInfo namespace) {
        setId(namespace);
    }
    
    protected void resolve(StoreInfo store) {
        setId(store);
        StoreInfoImpl s = (StoreInfoImpl) store;
        
        //resolve the workspace
        WorkspaceInfo resolved = ResolvingProxy.resolve( catalog, s.getWorkspace());
        if ( resolved != null ) {
            resolved = unwrap(resolved);
            s.setWorkspace( resolved );
        }
        else {
            //this means the workspace has not yet been added to the catalog, keep the proxy around
        }
    }

    protected void resolve(ResourceInfo resource) {
        setId(resource);
        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        
        //resolve the store
        StoreInfo store = ResolvingProxy.resolve( catalog, r.getStore() );
        if ( store != null ) {
            store = unwrap(store);
            r.setStore(store);
        }
        
        //resolve the namespace
        NamespaceInfo namespace = ResolvingProxy.resolve( catalog, r.getNamespace() );
        if (namespace != null) {
            namespace = unwrap(namespace);
            r.setNamespace(namespace);
        }
    }

    protected void resolve(LayerInfo layer) {
        setId(layer);
        
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
        
        LinkedHashSet<StyleInfo> styles = new LinkedHashSet();
        for (StyleInfo s : layer.getStyles()) {
            s = ResolvingProxy.resolve(catalog, s);
            s = unwrap(s);
            styles.add(s);
        }
        ((LayerInfoImpl)layer).setStyles(styles);
    }
    
    protected void resolve(LayerGroupInfo layerGroup) {
        setId(layerGroup);
        
        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;
        
        for ( int i = 0; i < lg.getLayers().size(); i++ ) {
            LayerInfo l = lg.getLayers().get( i );
            LayerInfo resolved = unwrap(ResolvingProxy.resolve( catalog, l ));
            lg.getLayers().set( i, resolved );
        }
        
        for ( int i = 0; i < lg.getStyles().size(); i++ ) {
            StyleInfo s = lg.getStyles().get( i );
            if(s != null) {
                StyleInfo resolved = unwrap(ResolvingProxy.resolve( catalog, s ));
                lg.getStyles().set( i, resolved );
            }
        }
        
    }
    
    protected void resolve(StyleInfo style) {
        setId(style);
    }
    
    protected void resolve(MapInfo map) {
        setId(map);
    }
    
    protected void setId( Object o ) {
        if ( OwsUtils.get( o, "id") == null ) {
            String uid = new UID().toString();
            OwsUtils.set( o, "id", o.getClass().getSimpleName() + "-"+uid );
        }
    }
    
    public void syncTo(CatalogFacade dao) {
        if (dao instanceof DefaultCatalogFacade) {
            //do an optimized sync
            DefaultCatalogFacade other = (DefaultCatalogFacade) dao;
            
            other.stores = stores;
            other.defaultStores = defaultStores;
            other.resources = resources;
            other.namespaces = namespaces;
            other.workspaces = workspaces;
            other.layers = layers;
            other.maps = maps;
            other.layerGroups = layerGroups;
            other.styles = styles;
        }
        else {
            //do a manual import
            for (Map.Entry<String,WorkspaceInfo> e : workspaces.entrySet()) {
                if (e.getKey() != null && !"default".equals(e.getKey())) {
                    dao.add(e.getValue());
                }
            }
            for (Map.Entry<String,NamespaceInfo> e : namespaces.entrySet()) {
                if (e.getKey() != null && !"default".equals(e.getKey())) {
                    dao.add(e.getValue());
                }
            }
            
            for (Iterator k = stores.keySet().iterator(); k.hasNext();) {
                Class key = (Class) k.next();
                Collection<StoreInfo> val = stores.getCollection(key);
                for (StoreInfo s : val) {
                    dao.add(s);
                }
            }
            
            for (Iterator k = resources.keySet().iterator(); k.hasNext();) {
                Class key = (Class) k.next();
                Collection<ResourceInfo> val = resources.getCollection(key);
                for (ResourceInfo r : val) {
                    dao.add(r);
                }
            }
            
            for (StyleInfo s : styles) { dao.add(s); }
            for (LayerInfo l : layers) { dao.add(l); }
            for (LayerGroupInfo lg : layerGroups) { dao.add(lg); }
            for (MapInfo m : maps) { dao.add(m); }
            
            if (workspaces.containsKey(null)) {
                dao.setDefaultWorkspace(workspaces.get(null));
            }
            if (namespaces.containsKey(null)) {
                dao.setDefaultNamespace(namespaces.get(null));
            }
            
            for (Map.Entry<String, DataStoreInfo> e : defaultStores.entrySet()) {
                WorkspaceInfo ws = workspaces.get(e.getKey());
                dao.setDefaultDataStore(ws, e.getValue());
            }
        }
        
    }
}

