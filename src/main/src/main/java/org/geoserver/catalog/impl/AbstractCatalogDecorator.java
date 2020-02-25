/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

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
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Abstract class for catalog decorators.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AbstractCatalogDecorator extends AbstractDecorator<Catalog> implements Catalog {

    public AbstractCatalogDecorator(Catalog catalog) {
        super(catalog);
    }

    public String getId() {
        return delegate.getId();
    }

    public CatalogFacade getFacade() {
        return delegate.getFacade();
    }

    public CatalogFactory getFactory() {
        return delegate.getFactory();
    }

    public ResourcePool getResourcePool() {
        return delegate.getResourcePool();
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

    //
    // StoreInfo methods
    //

    public void add(StoreInfo store) {
        delegate.add(store);
    }

    public void remove(StoreInfo store) {
        delegate.remove(store);
    }

    public void save(StoreInfo store) {
        delegate.save(store);
    }

    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return delegate.validate(store, isNew);
    }

    public <T extends StoreInfo> T detach(T store) {
        return delegate.detach(store);
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return delegate.getStore(id, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return delegate.getStoreByName(name, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {
        return delegate.getStoreByName(workspaceName, name, clazz);
    }

    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return delegate.getStoreByName(workspace, name, clazz);
    }

    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return delegate.getStores(clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return delegate.getStoresByWorkspace(workspace, clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {
        return delegate.getStoresByWorkspace(workspaceName, clazz);
    }

    public DataStoreInfo getDataStore(String id) {
        return delegate.getDataStore(id);
    }

    public DataStoreInfo getDataStoreByName(String name) {
        return delegate.getDataStoreByName(name);
    }

    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return delegate.getDataStoreByName(workspaceName, name);
    }

    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return delegate.getDataStoreByName(workspace, name);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return delegate.getDataStoresByWorkspace(workspaceName);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return delegate.getDataStoresByWorkspace(workspace);
    }

    public List<DataStoreInfo> getDataStores() {
        return delegate.getDataStores();
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return delegate.getDefaultDataStore(workspace);
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        delegate.setDefaultDataStore(workspace, defaultStore);
    }

    public CoverageStoreInfo getCoverageStore(String id) {
        return delegate.getCoverageStore(id);
    }

    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return delegate.getCoverageStoreByName(name);
    }

    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return delegate.getCoverageStoreByName(workspaceName, name);
    }

    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return delegate.getCoverageStoreByName(workspace, name);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return delegate.getCoverageStoresByWorkspace(workspaceName);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return delegate.getCoverageStoresByWorkspace(workspace);
    }

    public List<CoverageStoreInfo> getCoverageStores() {
        return delegate.getCoverageStores();
    }

    //
    // ResourceInfo
    //
    public void add(ResourceInfo resource) {
        delegate.add(resource);
    }

    public void remove(ResourceInfo resource) {
        delegate.remove(resource);
    }

    public void save(ResourceInfo resource) {
        delegate.save(resource);
    }

    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return delegate.validate(resource, isNew);
    }

    public <T extends ResourceInfo> T detach(T resource) {
        return delegate.detach(resource);
    }

    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return delegate.getResource(id, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return delegate.getResourceByName(ns, name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {
        return delegate.getResourceByName(ns, name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return delegate.getResourceByName(name, clazz);
    }

    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return delegate.getResourceByName(name, clazz);
    }

    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return delegate.getResources(clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return delegate.getResourcesByNamespace(namespace, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        return delegate.getResourcesByNamespace(namespace, clazz);
    }

    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return delegate.getResourceByStore(store, name, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return delegate.getResourcesByStore(store, clazz);
    }

    public FeatureTypeInfo getFeatureType(String id) {
        return delegate.getFeatureType(id);
    }

    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return delegate.getFeatureTypeByName(ns, name);
    }

    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return delegate.getFeatureTypeByName(ns, name);
    }

    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return delegate.getFeatureTypeByName(name);
    }

    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return delegate.getFeatureTypeByName(name);
    }

    public List<FeatureTypeInfo> getFeatureTypes() {
        return delegate.getFeatureTypes();
    }

    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return delegate.getFeatureTypesByNamespace(namespace);
    }

    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return delegate.getFeatureTypeByDataStore(dataStore, name);
    }

    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return delegate.getFeatureTypesByDataStore(store);
    }

    public CoverageInfo getCoverage(String id) {
        return delegate.getCoverage(id);
    }

    public CoverageInfo getCoverageByName(String ns, String name) {
        return delegate.getCoverageByName(ns, name);
    }

    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return delegate.getCoverageByName(ns, name);
    }

    public CoverageInfo getCoverageByName(Name name) {
        return delegate.getCoverageByName(name);
    }

    public CoverageInfo getCoverageByName(String name) {
        return delegate.getCoverageByName(name);
    }

    public List<CoverageInfo> getCoverages() {
        return delegate.getCoverages();
    }

    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return delegate.getCoveragesByNamespace(namespace);
    }

    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return delegate.getCoverageByCoverageStore(coverageStore, name);
    }

    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return delegate.getCoveragesByCoverageStore(store);
    }

    //
    // LayerInfo
    //
    public void add(LayerInfo layer) {
        delegate.add(layer);
    }

    public void remove(LayerInfo layer) {
        delegate.remove(layer);
    }

    public void save(LayerInfo layer) {
        delegate.save(layer);
    }

    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return delegate.validate(layer, isNew);
    }

    public LayerInfo detach(LayerInfo layer) {
        return delegate.detach(layer);
    }

    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return delegate.getCoveragesByStore(store);
    }

    public LayerInfo getLayer(String id) {
        return delegate.getLayer(id);
    }

    public LayerInfo getLayerByName(String name) {
        return delegate.getLayerByName(name);
    }

    public LayerInfo getLayerByName(Name name) {
        return delegate.getLayerByName(name);
    }

    public List<LayerInfo> getLayers() {
        return delegate.getLayers();
    }

    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return delegate.getLayers(resource);
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        return delegate.getLayers(style);
    }

    //
    // MapInfo
    //
    public void add(MapInfo map) {
        delegate.add(map);
    }

    public void remove(MapInfo map) {
        delegate.remove(map);
    }

    public void save(MapInfo map) {
        delegate.save(map);
    }

    public MapInfo detach(MapInfo map) {
        return delegate.detach(map);
    }

    public List<MapInfo> getMaps() {
        return delegate.getMaps();
    }

    public MapInfo getMap(String id) {
        return delegate.getMap(id);
    }

    public MapInfo getMapByName(String name) {
        return delegate.getMapByName(name);
    }

    //
    // LayerGroupInfo
    //
    public void add(LayerGroupInfo layerGroup) {
        delegate.add(layerGroup);
    }

    public void remove(LayerGroupInfo layerGroup) {
        delegate.remove(layerGroup);
    }

    public void save(LayerGroupInfo layerGroup) {
        delegate.save(layerGroup);
    }

    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return delegate.validate(layerGroup, isNew);
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return delegate.detach(layerGroup);
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return delegate.getLayerGroups();
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return delegate.getLayerGroupsByWorkspace(workspaceName);
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return delegate.getLayerGroupsByWorkspace(workspace);
    }

    public LayerGroupInfo getLayerGroup(String id) {
        return delegate.getLayerGroup(id);
    }

    public LayerGroupInfo getLayerGroupByName(String name) {
        return delegate.getLayerGroupByName(name);
    }

    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return delegate.getLayerGroupByName(workspaceName, name);
    }

    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return delegate.getLayerGroupByName(workspace, name);
    }

    //
    // StyleInfo
    //
    public void add(StyleInfo style) {
        delegate.add(style);
    }

    public void remove(StyleInfo style) {
        delegate.remove(style);
    }

    public void save(StyleInfo style) {
        delegate.save(style);
    }

    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return delegate.validate(style, isNew);
    }

    public StyleInfo detach(StyleInfo style) {
        return delegate.detach(style);
    }

    public StyleInfo getStyle(String id) {
        return delegate.getStyle(id);
    }

    public StyleInfo getStyleByName(String workspaceName, String name) {
        return delegate.getStyleByName(workspaceName, name);
    }

    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return delegate.getStyleByName(workspace, name);
    }

    public StyleInfo getStyleByName(String name) {
        return delegate.getStyleByName(name);
    }

    public List<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return delegate.getStylesByWorkspace(workspaceName);
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return delegate.getStylesByWorkspace(workspace);
    }

    //
    // NamespaceInfo
    //
    public void add(NamespaceInfo namespace) {
        delegate.add(namespace);
    }

    public void remove(NamespaceInfo namespace) {
        delegate.remove(namespace);
    }

    public void save(NamespaceInfo namespace) {
        delegate.save(namespace);
    }

    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return delegate.validate(namespace, isNew);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return delegate.detach(namespace);
    }

    public NamespaceInfo getNamespace(String id) {
        return delegate.getNamespace(id);
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return delegate.getNamespaceByPrefix(prefix);
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        return delegate.getNamespaceByURI(uri);
    }

    public NamespaceInfo getDefaultNamespace() {
        return delegate.getDefaultNamespace();
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        delegate.setDefaultNamespace(defaultNamespace);
    }

    public List<NamespaceInfo> getNamespaces() {
        return delegate.getNamespaces();
    }

    //
    // WorkspaceInfo
    //
    public void add(WorkspaceInfo workspace) {
        delegate.add(workspace);
    }

    public void remove(WorkspaceInfo workspace) {
        delegate.remove(workspace);
    }

    public void save(WorkspaceInfo workspace) {
        delegate.save(workspace);
    }

    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return delegate.validate(workspace, isNew);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return delegate.detach(workspace);
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return delegate.getDefaultWorkspace();
    }

    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        delegate.setDefaultWorkspace(workspace);
    }

    public List<WorkspaceInfo> getWorkspaces() {
        return delegate.getWorkspaces();
    }

    public WorkspaceInfo getWorkspace(String id) {
        return delegate.getWorkspace(id);
    }

    public WorkspaceInfo getWorkspaceByName(String name) {
        return delegate.getWorkspaceByName(name);
    }

    //
    // Events
    //

    public Collection<CatalogListener> getListeners() {
        return delegate.getListeners();
    }

    public void addListener(CatalogListener listener) {
        delegate.addListener(listener);
    }

    public void removeListener(CatalogListener listener) {
        delegate.removeListener(listener);
    }

    public void fireAdded(CatalogInfo object) {
        delegate.fireAdded(object);
    }

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

    //
    // Misc
    //
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public void dispose() {
        delegate.dispose();
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return delegate.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {
        return delegate.get(type, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return delegate.list(of, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortOrder) {
        return delegate.list(of, filter, offset, count, sortOrder);
    }

    public void removeListeners(Class listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return delegate.getCatalogCapabilities();
    }
}
