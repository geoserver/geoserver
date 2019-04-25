/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.*;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Wraps a CatalogFacade delegating all access methods to the {@link SecureCatalogImpl}, to ensure
 * the facade cannot be used by accident to breach the security restrictions
 *
 * @author Andrea Aime
 */
class SecureCatalogFacade implements CatalogFacade {

    SecureCatalogImpl catalog;

    CatalogFacade facade;

    public SecureCatalogFacade(SecureCatalogImpl catalog, CatalogFacade facade) {
        this.catalog = catalog;
        this.facade = facade;
    }

    public Catalog getCatalog() {
        return facade.getCatalog();
    }

    public void setCatalog(Catalog catalog) {
        facade.setCatalog(catalog);
    }

    public StoreInfo add(StoreInfo store) {
        return facade.add(store);
    }

    public void remove(StoreInfo store) {
        facade.remove(store);
    }

    public void save(StoreInfo store) {
        facade.save(store);
    }

    public <T extends ResourceInfo> T detach(T store) {
        return facade.detach(store);
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        facade.setDefaultDataStore(workspace, store);
    }

    public ResourceInfo add(ResourceInfo resource) {
        return facade.add(resource);
    }

    public void remove(ResourceInfo resource) {
        facade.remove(resource);
    }

    public void save(ResourceInfo resource) {
        facade.save(resource);
    }

    public LayerInfo add(LayerInfo layer) {
        return facade.add(layer);
    }

    public void remove(LayerInfo layer) {
        facade.remove(layer);
    }

    public void save(LayerInfo layer) {
        facade.save(layer);
    }

    public LayerInfo detach(LayerInfo layer) {
        return facade.detach(layer);
    }

    public MapInfo add(MapInfo map) {
        return facade.add(map);
    }

    public void remove(MapInfo map) {
        facade.remove(map);
    }

    public void save(MapInfo map) {
        facade.save(map);
    }

    public MapInfo detach(MapInfo map) {
        return facade.detach(map);
    }

    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return facade.add(layerGroup);
    }

    public void remove(LayerGroupInfo layerGroup) {
        facade.remove(layerGroup);
    }

    public void save(LayerGroupInfo layerGroup) {
        facade.save(layerGroup);
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return facade.detach(layerGroup);
    }

    public NamespaceInfo add(NamespaceInfo namespace) {
        return facade.add(namespace);
    }

    public void remove(NamespaceInfo namespace) {
        facade.remove(namespace);
    }

    public void save(NamespaceInfo namespace) {
        facade.save(namespace);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return facade.detach(namespace);
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        facade.setDefaultNamespace(defaultNamespace);
    }

    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return facade.add(workspace);
    }

    public void remove(WorkspaceInfo workspace) {
        facade.remove(workspace);
    }

    public void save(WorkspaceInfo workspace) {
        facade.save(workspace);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return facade.detach(workspace);
    }

    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        facade.setDefaultWorkspace(workspace);
    }

    public StyleInfo add(StyleInfo style) {
        return facade.add(style);
    }

    public void remove(StyleInfo style) {
        facade.remove(style);
    }

    public void save(StyleInfo style) {
        facade.save(style);
    }

    public StyleInfo detach(StyleInfo style) {
        return facade.detach(style);
    }

    public void dispose() {
        facade.dispose();
    }

    public void resolve() {
        facade.resolve();
    }

    public void syncTo(CatalogFacade other) {
        facade.syncTo(other);
    }

    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return facade.count(of, filter);
    }

    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        return facade.canSort(type, propertyName);
    }

    public CatalogFactory getFactory() {
        return catalog.getFactory();
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return catalog.getStore(id, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return catalog.getStoreByName(name, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspaceName, name, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return catalog.getStoreByName(workspace, name, clazz);
    }

    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return catalog.getStores(clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspace, clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {
        return catalog.getStoresByWorkspace(workspaceName, clazz);
    }

    public DataStoreInfo getDataStore(String id) {
        return catalog.getDataStore(id);
    }

    public DataStoreInfo getDataStoreByName(String name) {
        return catalog.getDataStoreByName(name);
    }

    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return catalog.getDataStoreByName(workspaceName, name);
    }

    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getDataStoreByName(workspace, name);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return catalog.getDataStoresByWorkspace(workspaceName);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getDataStoresByWorkspace(workspace);
    }

    public List<DataStoreInfo> getDataStores() {
        return catalog.getDataStores();
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return catalog.getDefaultDataStore(workspace);
    }

    public CoverageStoreInfo getCoverageStore(String id) {
        return catalog.getCoverageStore(id);
    }

    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return catalog.getCoverageStoreByName(name);
    }

    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return catalog.getCoverageStoreByName(workspaceName, name);
    }

    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return catalog.getCoverageStoreByName(workspace, name);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return catalog.getCoverageStoresByWorkspace(workspaceName);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return catalog.getCoverageStoresByWorkspace(workspace);
    }

    public List<CoverageStoreInfo> getCoverageStores() {
        return catalog.getCoverageStores();
    }

    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return catalog.getResource(id, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {
        return catalog.getResourceByName(ns, name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return catalog.getResourceByName(name, clazz);
    }

    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return catalog.getResources(clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        return catalog.getResourcesByNamespace(namespace, clazz);
    }

    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return catalog.getResourceByStore(store, name, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return catalog.getResourcesByStore(store, clazz);
    }

    public FeatureTypeInfo getFeatureType(String id) {
        return catalog.getFeatureType(id);
    }

    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return catalog.getFeatureTypeByName(ns, name);
    }

    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return catalog.getFeatureTypeByName(name);
    }

    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return catalog.getFeatureTypeByName(name);
    }

    public List<FeatureTypeInfo> getFeatureTypes() {
        return catalog.getFeatureTypes();
    }

    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return catalog.getFeatureTypesByNamespace(namespace);
    }

    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return catalog.getFeatureTypeByDataStore(dataStore, name);
    }

    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return catalog.getFeatureTypesByDataStore(store);
    }

    public CoverageInfo getCoverage(String id) {
        return catalog.getCoverage(id);
    }

    public CoverageInfo getCoverageByName(String ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return catalog.getCoverageByName(ns, name);
    }

    public CoverageInfo getCoverageByName(Name name) {
        return catalog.getCoverageByName(name);
    }

    public CoverageInfo getCoverageByName(String name) {
        return catalog.getCoverageByName(name);
    }

    public List<CoverageInfo> getCoverages() {
        return catalog.getCoverages();
    }

    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return catalog.getCoveragesByNamespace(namespace);
    }

    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return catalog.getCoverageByCoverageStore(coverageStore, name);
    }

    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByCoverageStore(store);
    }

    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return catalog.getCoveragesByStore(store);
    }

    public LayerInfo getLayer(String id) {
        return catalog.getLayer(id);
    }

    public LayerInfo getLayerByName(String name) {
        return catalog.getLayerByName(name);
    }

    public LayerInfo getLayerByName(Name name) {
        return catalog.getLayerByName(name);
    }

    public List<LayerInfo> getLayers() {
        return catalog.getLayers();
    }

    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return catalog.getLayers(resource);
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        return catalog.getLayers(style);
    }

    public List<MapInfo> getMaps() {
        return catalog.getMaps();
    }

    public MapInfo getMap(String id) {
        return catalog.getMap(id);
    }

    public MapInfo getMapByName(String name) {
        return catalog.getMapByName(name);
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return catalog.getLayerGroups();
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return catalog.getLayerGroupsByWorkspace(workspaceName);
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return catalog.getLayerGroupsByWorkspace(workspace);
    }

    public LayerGroupInfo getLayerGroup(String id) {
        return catalog.getLayerGroup(id);
    }

    public LayerGroupInfo getLayerGroupByName(String name) {
        return catalog.getLayerGroupByName(name);
    }

    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return catalog.getLayerGroupByName(workspaceName, name);
    }

    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return catalog.getLayerGroupByName(workspace, name);
    }

    public StyleInfo getStyle(String id) {
        return catalog.getStyle(id);
    }

    public StyleInfo getStyleByName(String workspaceName, String name) {
        return catalog.getStyleByName(workspaceName, name);
    }

    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return catalog.getStyleByName(workspace, name);
    }

    public StyleInfo getStyleByName(String name) {
        return catalog.getStyleByName(name);
    }

    public List<StyleInfo> getStyles() {
        return catalog.getStyles();
    }

    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return catalog.getStylesByWorkspace(workspaceName);
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return catalog.getStylesByWorkspace(workspace);
    }

    public NamespaceInfo getNamespace(String id) {
        return catalog.getNamespace(id);
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return catalog.getNamespaceByPrefix(prefix);
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        return catalog.getNamespaceByURI(uri);
    }

    public NamespaceInfo getDefaultNamespace() {
        return catalog.getDefaultNamespace();
    }

    public List<NamespaceInfo> getNamespaces() {
        return catalog.getNamespaces();
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return catalog.getDefaultWorkspace();
    }

    public List<WorkspaceInfo> getWorkspaces() {
        return catalog.getWorkspaces();
    }

    public WorkspaceInfo getWorkspace(String id) {
        return catalog.getWorkspace(id);
    }

    public WorkspaceInfo getWorkspaceByName(String name) {
        return catalog.getWorkspaceByName(name);
    }

    public Collection<CatalogListener> getListeners() {
        return catalog.getListeners();
    }

    public ResourcePool getResourcePool() {
        return catalog.getResourcePool();
    }

    public GeoServerResourceLoader getResourceLoader() {
        return catalog.getResourceLoader();
    }

    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        return catalog.get(type, filter);
    }

    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return catalog.list(of, filter);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return facade.detach(store);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy... sortOrder) {
        return catalog.list(of, filter, offset, count, sortOrder);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return catalog.getCatalogCapabilities();
    }
}
