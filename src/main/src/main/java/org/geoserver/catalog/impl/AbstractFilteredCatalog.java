/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Base class for Catalog wrappers that need to filter catalog items.
 *
 * @author Davide Savazzi - GeoSolutions
 */
public abstract class AbstractFilteredCatalog extends AbstractDecorator<Catalog>
        implements Catalog {

    public AbstractFilteredCatalog(Catalog catalog) {
        super(catalog);
    }

    public String getId() {
        return delegate.getId();
    }

    // -------------------------------------------------------------------
    // SECURED METHODS
    // -------------------------------------------------------------------

    public CoverageInfo getCoverage(String id) {
        return (CoverageInfo) checkAccess(delegate.getCoverage(id));
    }

    public CoverageInfo getCoverageByName(String ns, String name) {
        return (CoverageInfo) checkAccess(delegate.getCoverageByName(ns, name));
    }

    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return (CoverageInfo) checkAccess(delegate.getCoverageByName(ns, name));
    }

    public CoverageInfo getCoverageByName(Name name) {
        return (CoverageInfo) checkAccess(delegate.getCoverageByName(name));
    }

    public CoverageInfo getCoverageByName(String name) {
        return (CoverageInfo) checkAccess(delegate.getCoverageByName(name));
    }

    public List<CoverageInfo> getCoverages() {
        return filterResources(delegate.getCoverages());
    }

    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return filterResources(delegate.getCoveragesByNamespace(namespace));
    }

    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return filterResources(delegate.getCoveragesByCoverageStore(store));
    }

    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return checkAccess(delegate.getCoverageByCoverageStore(coverageStore, name));
    }

    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return filterResources(delegate.getCoveragesByStore(store));
    }

    public CoverageStoreInfo getCoverageStore(String id) {
        return checkAccess(delegate.getCoverageStore(id));
    }

    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return checkAccess(delegate.getCoverageStoreByName(name));
    }

    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return checkAccess(delegate.getCoverageStoreByName(workspaceName, name));
    }

    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return checkAccess(delegate.getCoverageStoreByName(workspace, name));
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return filterStores(delegate.getCoverageStoresByWorkspace(workspaceName));
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return filterStores(delegate.getCoverageStoresByWorkspace(workspace));
    }

    public List<CoverageStoreInfo> getCoverageStores() {
        return filterStores(delegate.getCoverageStores());
    }

    public DataStoreInfo getDataStore(String id) {
        return checkAccess(delegate.getDataStore(id));
    }

    public DataStoreInfo getDataStoreByName(String name) {
        return checkAccess(delegate.getDataStoreByName(name));
    }

    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return checkAccess(delegate.getDataStoreByName(workspaceName, name));
    }

    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return checkAccess(delegate.getDataStoreByName(workspace, name));
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return filterStores(delegate.getDataStoresByWorkspace(workspaceName));
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return filterStores(delegate.getDataStoresByWorkspace(workspace));
    }

    public List<DataStoreInfo> getDataStores() {
        return filterStores(delegate.getDataStores());
    }

    public NamespaceInfo getDefaultNamespace() {
        return delegate.getDefaultNamespace();
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return delegate.getDefaultWorkspace();
    }

    public FeatureTypeInfo getFeatureType(String id) {
        return checkAccess(delegate.getFeatureType(id));
    }

    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return checkAccess(delegate.getFeatureTypeByName(ns, name));
    }

    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return checkAccess(delegate.getFeatureTypeByName(ns, name));
    }

    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return checkAccess(delegate.getFeatureTypeByName(name));
    }

    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return checkAccess(delegate.getFeatureTypeByName(name));
    }

    public List<FeatureTypeInfo> getFeatureTypes() {
        return filterResources(delegate.getFeatureTypes());
    }

    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return filterResources(delegate.getFeatureTypesByNamespace(namespace));
    }

    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return checkAccess(delegate.getFeatureTypeByDataStore(dataStore, name));
    }

    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return filterResources(delegate.getFeatureTypesByDataStore(store));
    }

    public LayerInfo getLayer(String id) {
        return checkAccess(delegate.getLayer(id));
    }

    public LayerInfo getLayerByName(String name) {
        return checkAccess(delegate.getLayerByName(name));
    }

    public LayerInfo getLayerByName(Name name) {
        return checkAccess(delegate.getLayerByName(name));
    }

    public LayerGroupInfo getLayerGroup(String id) {
        return checkAccess(delegate.getLayerGroup(id));
    }

    public LayerGroupInfo getLayerGroupByName(String name) {
        return checkAccess(delegate.getLayerGroupByName(name));
    }

    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return checkAccess(delegate.getLayerGroupByName(workspaceName, name));
    }

    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return checkAccess(delegate.getLayerGroupByName(workspace, name));
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return filterGroups(delegate.getLayerGroups());
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return filterGroups(delegate.getLayerGroupsByWorkspace(workspaceName));
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return filterGroups(delegate.getLayerGroupsByWorkspace(workspace));
    }

    public List<LayerInfo> getLayers() {
        return filterLayers(delegate.getLayers());
    }

    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return filterLayers(delegate.getLayers(resource));
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        return filterLayers(delegate.getLayers(style));
    }

    public NamespaceInfo getNamespace(String id) {
        return checkAccess(delegate.getNamespace(id));
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return checkAccess(delegate.getNamespaceByPrefix(prefix));
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        return checkAccess(delegate.getNamespaceByURI(uri));
    }

    public List<NamespaceInfo> getNamespaces() {
        return filterNamespaces(delegate.getNamespaces());
    }

    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return checkAccess(delegate.getResource(id, clazz));
    }

    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return checkAccess(delegate.getResourceByName(name, clazz));
    }

    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return checkAccess(delegate.getResourceByName(name, clazz));
    }

    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {
        return checkAccess(delegate.getResourceByName(ns, name, clazz));
    }

    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return checkAccess(delegate.getResourceByName(ns, name, clazz));
    }

    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return filterResources(delegate.getResources(clazz));
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return filterResources(delegate.getResourcesByNamespace(namespace, clazz));
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        return filterResources(delegate.getResourcesByNamespace(namespace, clazz));
    }

    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return checkAccess(delegate.getResourceByStore(store, name, clazz));
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return filterResources(delegate.getResourcesByStore(store, clazz));
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return checkAccess(delegate.getStore(id, clazz));
    }

    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return checkAccess(delegate.getStoreByName(name, clazz));
    }

    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {
        return checkAccess(delegate.getStoreByName(workspaceName, name, clazz));
    }

    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return checkAccess(delegate.getStoreByName(workspace, name, clazz));
    }

    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return filterStores(delegate.getStores(clazz));
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {
        return filterStores(delegate.getStoresByWorkspace(workspaceName, clazz));
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return filterStores(delegate.getStoresByWorkspace(workspace, clazz));
    }

    public WorkspaceInfo getWorkspace(String id) {
        return checkAccess(delegate.getWorkspace(id));
    }

    public WorkspaceInfo getWorkspaceByName(String name) {
        return checkAccess(delegate.getWorkspaceByName(name));
    }

    public List<WorkspaceInfo> getWorkspaces() {
        return filterWorkspaces(delegate.getWorkspaces());
    }

    // -------------------------------------------------------------------
    // Security support method
    // -------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected <T extends CatalogInfo> T checkAccess(T info) {
        if (info instanceof WorkspaceInfo) {
            return (T) checkAccess((WorkspaceInfo) info);
        }
        if (info instanceof NamespaceInfo) {
            return (T) checkAccess((NamespaceInfo) info);
        }
        if (info instanceof StoreInfo) {
            return (T) checkAccess((StoreInfo) info);
        }
        if (info instanceof ResourceInfo) {
            return (T) checkAccess((ResourceInfo) info);
        }
        if (info instanceof LayerInfo) {
            return (T) checkAccess((LayerInfo) info);
        }
        if (info instanceof LayerGroupInfo) {
            return (T) checkAccess((LayerGroupInfo) info);
        }

        return info;
    }

    /**
     * Given a {@link FeatureTypeInfo}, returns it back if the user can access it in write mode,
     * makes it read only if the user can access it in read only mode, returns null otherwise
     */
    protected abstract <T extends ResourceInfo> T checkAccess(T info);

    /**
     * Given a {@link StyleInfo}, returns it back if the user can access it.
     *
     * @return <code>null</code> if the user can't acess the style, otherwise the original style.
     */
    protected abstract StyleInfo checkAccess(StyleInfo style);

    /**
     * Given a store, returns it back if the user can access its workspace in read mode, null
     * otherwise
     */
    protected abstract <T extends StoreInfo> T checkAccess(T store);

    /** Given a layer, returns it back if the user can access it, null otherwise */
    protected abstract LayerInfo checkAccess(LayerInfo layer);

    /** Given a layer group, returns it back if the user can access it, null otherwise */
    protected abstract LayerGroupInfo checkAccess(LayerGroupInfo group);

    /** Given a namespace, returns it back if the user can access it, null otherwise */
    protected abstract <T extends NamespaceInfo> T checkAccess(T ns);

    /** Given a workspace, returns it back if the user can access it, null otherwise */
    protected abstract <T extends WorkspaceInfo> T checkAccess(T ws);

    /**
     * Given a list of resources, returns a copy of it containing only the resources the user can
     * access
     */
    protected abstract <T extends ResourceInfo> List<T> filterResources(List<T> resources);

    /**
     * Given a list of stores, returns a copy of it containing only the resources the user can
     * access
     */
    protected abstract <T extends StoreInfo> List<T> filterStores(List<T> resources);

    /**
     * Given a list of layer groups, returns a copy of it containing only the groups the user can
     * access
     */
    protected abstract List<LayerGroupInfo> filterGroups(List<LayerGroupInfo> groups);

    /**
     * Given a list of layers, returns a copy of it containing only the layers the user can access
     */
    protected abstract List<LayerInfo> filterLayers(List<LayerInfo> layers);

    /**
     * Given a list of styles, returns a copy of it containing only the styles the user can access.
     */
    protected abstract List<StyleInfo> filterStyles(List<StyleInfo> styles);

    /**
     * Given a list of namespaces, returns a copy of it containing only the namespaces the user can
     * access
     */
    protected abstract <T extends NamespaceInfo> List<T> filterNamespaces(List<T> namespaces);

    /**
     * Given a list of workspaces, returns a copy of it containing only the workspaces the user can
     * access
     */
    protected abstract <T extends WorkspaceInfo> List<T> filterWorkspaces(List<T> workspaces);

    // -------------------------------------------------------------------
    // PURE DELEGATING METHODS
    // (MapInfo being here since its role in the grand scheme of things
    // is still undefined)
    // -------------------------------------------------------------------

    public MapInfo getMap(String id) {
        return delegate.getMap(id);
    }

    public MapInfo getMapByName(String name) {
        return delegate.getMapByName(name);
    }

    public List<MapInfo> getMaps() {
        return delegate.getMaps();
    }

    public void add(LayerGroupInfo layerGroup) {
        delegate.add(layerGroup);
    }

    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return delegate.validate(layerGroup, isNew);
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return delegate.detach(layerGroup);
    }

    public void add(LayerInfo layer) {
        delegate.add(layer);
    }

    public LayerInfo detach(LayerInfo layer) {
        return delegate.detach(layer);
    }

    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return delegate.validate(layer, isNew);
    }

    public void add(MapInfo map) {
        delegate.add(map);
    }

    public MapInfo detach(MapInfo map) {
        return delegate.detach(map);
    }

    public void add(NamespaceInfo namespace) {
        delegate.add(namespace);
    }

    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return delegate.validate(namespace, isNew);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return delegate.detach(namespace);
    }

    public void add(ResourceInfo resource) {
        delegate.add(resource);
    }

    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return delegate.validate(resource, isNew);
    }

    public <T extends ResourceInfo> T detach(T resource) {
        return delegate.detach(resource);
    }

    public void add(StoreInfo store) {
        delegate.add(store);
    }

    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return delegate.validate(store, isNew);
    }

    public <T extends StoreInfo> T detach(T store) {
        return delegate.detach(store);
    }

    public void add(StyleInfo style) {
        delegate.add(style);
    }

    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return delegate.validate(style, isNew);
    }

    public StyleInfo detach(StyleInfo style) {
        return delegate.detach(style);
    }

    public void add(WorkspaceInfo workspace) {
        delegate.add(workspace);
    }

    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return delegate.validate(workspace, isNew);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return delegate.detach(workspace);
    }

    public void addListener(CatalogListener listener) {
        delegate.addListener(listener);
    }

    public void dispose() {
        delegate.dispose();
    }

    public CatalogFacade getFacade() {
        return delegate.getFacade();
    }

    public CatalogFactory getFactory() {
        return delegate.getFactory();
    }

    public Collection<CatalogListener> getListeners() {
        return delegate.getListeners();
    }

    public void fireAdded(CatalogInfo object) {
        delegate.fireAdded(object);
    }

    @SuppressWarnings("rawtypes")
    public void fireModified(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues) {
        delegate.fireModified(object, propertyNames, oldValues, newValues);
    }

    public void firePostModified(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues) {
        delegate.firePostModified(object, propertyNames, oldValues, newValues);
    }

    public void fireRemoved(CatalogInfo object) {
        delegate.fireRemoved(object);
    }

    // TODO: why is resource pool being exposed???
    public ResourcePool getResourcePool() {
        return delegate.getResourcePool();
    }

    public StyleInfo getStyle(String id) {
        return delegate.getStyle(id);
    }

    public StyleInfo getStyleByName(String name) {
        return checkAccess(delegate.getStyleByName(name));
    }

    public StyleInfo getStyleByName(String workspaceName, String name) {
        return checkAccess(delegate.getStyleByName(workspaceName, name));
    }

    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return checkAccess(delegate.getStyleByName(workspace, name));
    }

    public List<StyleInfo> getStyles() {
        return filterStyles(delegate.getStyles());
    }

    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return filterStyles(delegate.getStylesByWorkspace(workspaceName));
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return filterStyles(delegate.getStylesByWorkspace(workspace));
    }

    public void remove(LayerGroupInfo layerGroup) {
        delegate.remove(layerGroup);
    }

    public void remove(LayerInfo layer) {
        delegate.remove(layer);
    }

    public void remove(MapInfo map) {
        delegate.remove(map);
    }

    public void remove(NamespaceInfo namespace) {
        delegate.remove(namespace);
    }

    public void remove(ResourceInfo resource) {
        delegate.remove(resource);
    }

    public void remove(StoreInfo store) {
        delegate.remove(store);
    }

    public void remove(StyleInfo style) {
        delegate.remove(style);
    }

    public void remove(WorkspaceInfo workspace) {
        delegate.remove(workspace);
    }

    public void removeListener(CatalogListener listener) {
        delegate.removeListener(listener);
    }

    public void save(LayerGroupInfo layerGroup) {
        delegate.save(layerGroup);
    }

    public void save(LayerInfo layer) {
        delegate.save(layer);
    }

    public void save(MapInfo map) {
        delegate.save(map);
    }

    public void save(NamespaceInfo namespace) {
        delegate.save(namespace);
    }

    public void save(ResourceInfo resource) {
        delegate.save(resource);
    }

    public void save(StoreInfo store) {
        delegate.save(store);
    }

    public void save(StyleInfo style) {
        delegate.save(style);
    }

    public void save(WorkspaceInfo workspace) {
        delegate.save(workspace);
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        delegate.setDefaultNamespace(defaultNamespace);
    }

    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        delegate.setDefaultWorkspace(workspace);
    }

    public void setResourcePool(ResourcePool resourcePool) {
        delegate.setResourcePool(resourcePool);
    }

    public GeoServerResourceLoader getResourceLoader() {
        return delegate.getResourceLoader();
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        delegate.setResourceLoader(resourceLoader);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return checkAccess(delegate.getDefaultDataStore(workspace));
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        delegate.setDefaultDataStore(workspace, defaultStore);
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        Filter securityFilter = securityFilter(of, filter);
        final int count = delegate.count(of, securityFilter);
        return count;
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {

        Filter securityFilter = securityFilter(type, filter);
        T result = delegate.get(type, securityFilter);
        return result;
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return list(of, filter, null, null, null);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {

        Filter securityFilter = securityFilter(of, filter);

        CloseableIterator<T> filtered;
        filtered = delegate.list(of, securityFilter, offset, count, sortBy);

        // create secured decorators on-demand
        final Function<T, T> securityWrapper = securityWrapper(of);
        final CloseableIterator<T> filteredWrapped;
        filteredWrapped = CloseableIteratorAdapter.transform(filtered, securityWrapper);

        return filteredWrapped;
    }

    /**
     * @return a Function that applies a security wrapper over the catalog object given to it as
     *     input
     */
    private <T extends CatalogInfo> Function<T, T> securityWrapper(final Class<T> forClass) {
        return new Function<T, T>() {

            @Override
            public T apply(T input) {
                T checked = checkAccess(input);
                return checked;
            }
        };
    }

    /**
     * Returns a predicate that checks whether the current user has access to a given object of type
     * {@code infoType}.
     *
     * @return a catalog Predicate that evaluates if an object of the required type is accessible
     */
    protected abstract <T extends CatalogInfo> Filter securityFilter(
            final Class<T> infoType, final Filter filter);

    @SuppressWarnings("rawtypes")
    public void removeListeners(Class listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return delegate.getCatalogCapabilities();
    }
}
