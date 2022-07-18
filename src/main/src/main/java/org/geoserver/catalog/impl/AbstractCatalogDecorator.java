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
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
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

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public CatalogFacade getFacade() {
        return delegate.getFacade();
    }

    @Override
    public CatalogFactory getFactory() {
        return delegate.getFactory();
    }

    @Override
    public ResourcePool getResourcePool() {
        return delegate.getResourcePool();
    }

    @Override
    public void setResourcePool(ResourcePool resourcePool) {
        delegate.setResourcePool(resourcePool);
    }

    @Override
    public GeoServerResourceLoader getResourceLoader() {
        return delegate.getResourceLoader();
    }

    @Override
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        delegate.setResourceLoader(resourceLoader);
    }

    //
    // StoreInfo methods
    //

    @Override
    public void add(StoreInfo store) {
        delegate.add(store);
    }

    @Override
    public void remove(StoreInfo store) {
        delegate.remove(store);
    }

    @Override
    public void save(StoreInfo store) {
        delegate.save(store);
    }

    @Override
    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return delegate.validate(store, isNew);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return delegate.detach(store);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return delegate.getStore(id, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return delegate.getStoreByName(name, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {
        return delegate.getStoreByName(workspaceName, name, clazz);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return delegate.getStoreByName(workspace, name, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return delegate.getStores(clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return delegate.getStoresByWorkspace(workspace, clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {
        return delegate.getStoresByWorkspace(workspaceName, clazz);
    }

    @Override
    public WMSStoreInfo getWMSStoreByName(String name) {
        return delegate.getWMSStoreByName(name);
    }

    @Override
    public WMSStoreInfo getWMSStore(String id) {
        return delegate.getWMSStore(id);
    }

    @Override
    public WMTSStoreInfo getWMTSStoreByName(String name) {
        return delegate.getWMTSStoreByName(name);
    }

    @Override
    public WMTSStoreInfo getWMTSStore(String id) {
        return delegate.getWMTSStore(id);
    }

    @Override
    public DataStoreInfo getDataStore(String id) {
        return delegate.getDataStore(id);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String name) {
        return delegate.getDataStoreByName(name);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return delegate.getDataStoreByName(workspaceName, name);
    }

    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return delegate.getDataStoreByName(workspace, name);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return delegate.getDataStoresByWorkspace(workspaceName);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return delegate.getDataStoresByWorkspace(workspace);
    }

    @Override
    public List<DataStoreInfo> getDataStores() {
        return delegate.getDataStores();
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return delegate.getDefaultDataStore(workspace);
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore) {
        delegate.setDefaultDataStore(workspace, defaultStore);
    }

    @Override
    public CoverageStoreInfo getCoverageStore(String id) {
        return delegate.getCoverageStore(id);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return delegate.getCoverageStoreByName(name);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return delegate.getCoverageStoreByName(workspaceName, name);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return delegate.getCoverageStoreByName(workspace, name);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return delegate.getCoverageStoresByWorkspace(workspaceName);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return delegate.getCoverageStoresByWorkspace(workspace);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        return delegate.getCoverageStores();
    }

    //
    // ResourceInfo
    //
    @Override
    public void add(ResourceInfo resource) {
        delegate.add(resource);
    }

    @Override
    public void remove(ResourceInfo resource) {
        delegate.remove(resource);
    }

    @Override
    public void save(ResourceInfo resource) {
        delegate.save(resource);
    }

    @Override
    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return delegate.validate(resource, isNew);
    }

    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return delegate.detach(resource);
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return delegate.getResource(id, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return delegate.getResourceByName(ns, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {
        return delegate.getResourceByName(ns, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return delegate.getResourceByName(name, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return delegate.getResourceByName(name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return delegate.getResources(clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        return delegate.getResourcesByNamespace(namespace, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        return delegate.getResourcesByNamespace(namespace, clazz);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return delegate.getResourceByStore(store, name, clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return delegate.getResourcesByStore(store, clazz);
    }

    @Override
    public FeatureTypeInfo getFeatureType(String id) {
        return delegate.getFeatureType(id);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return delegate.getFeatureTypeByName(ns, name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return delegate.getFeatureTypeByName(ns, name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return delegate.getFeatureTypeByName(name);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return delegate.getFeatureTypeByName(name);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        return delegate.getFeatureTypes();
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return delegate.getFeatureTypesByNamespace(namespace);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return delegate.getFeatureTypeByDataStore(dataStore, name);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return delegate.getFeatureTypesByDataStore(store);
    }

    @Override
    public CoverageInfo getCoverage(String id) {
        return delegate.getCoverage(id);
    }

    @Override
    public CoverageInfo getCoverageByName(String ns, String name) {
        return delegate.getCoverageByName(ns, name);
    }

    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return delegate.getCoverageByName(ns, name);
    }

    @Override
    public CoverageInfo getCoverageByName(Name name) {
        return delegate.getCoverageByName(name);
    }

    @Override
    public CoverageInfo getCoverageByName(String name) {
        return delegate.getCoverageByName(name);
    }

    @Override
    public List<CoverageInfo> getCoverages() {
        return delegate.getCoverages();
    }

    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        return delegate.getCoveragesByNamespace(namespace);
    }

    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return delegate.getCoverageByCoverageStore(coverageStore, name);
    }

    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return delegate.getCoveragesByCoverageStore(store);
    }

    //
    // LayerInfo
    //
    @Override
    public void add(LayerInfo layer) {
        delegate.add(layer);
    }

    @Override
    public void remove(LayerInfo layer) {
        delegate.remove(layer);
    }

    @Override
    public void save(LayerInfo layer) {
        delegate.save(layer);
    }

    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return delegate.validate(layer, isNew);
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return delegate.detach(layer);
    }

    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return delegate.getCoveragesByStore(store);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return delegate.getLayer(id);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return delegate.getLayerByName(name);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        return delegate.getLayerByName(name);
    }

    @Override
    public List<LayerInfo> getLayers() {
        return delegate.getLayers();
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return delegate.getLayers(resource);
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return delegate.getLayers(style);
    }

    //
    // MapInfo
    //
    @Override
    public void add(MapInfo map) {
        delegate.add(map);
    }

    @Override
    public void remove(MapInfo map) {
        delegate.remove(map);
    }

    @Override
    public void save(MapInfo map) {
        delegate.save(map);
    }

    @Override
    public MapInfo detach(MapInfo map) {
        return delegate.detach(map);
    }

    @Override
    public List<MapInfo> getMaps() {
        return delegate.getMaps();
    }

    @Override
    public MapInfo getMap(String id) {
        return delegate.getMap(id);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return delegate.getMapByName(name);
    }

    //
    // LayerGroupInfo
    //
    @Override
    public void add(LayerGroupInfo layerGroup) {
        delegate.add(layerGroup);
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        delegate.remove(layerGroup);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        delegate.save(layerGroup);
    }

    @Override
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return delegate.validate(layerGroup, isNew);
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return delegate.detach(layerGroup);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return delegate.getLayerGroups();
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        return delegate.getLayerGroupsByWorkspace(workspaceName);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return delegate.getLayerGroupsByWorkspace(workspace);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return delegate.getLayerGroup(id);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return delegate.getLayerGroupByName(name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return delegate.getLayerGroupByName(workspaceName, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return delegate.getLayerGroupByName(workspace, name);
    }

    //
    // StyleInfo
    //
    @Override
    public void add(StyleInfo style) {
        delegate.add(style);
    }

    @Override
    public void remove(StyleInfo style) {
        delegate.remove(style);
    }

    @Override
    public void save(StyleInfo style) {
        delegate.save(style);
    }

    @Override
    public ValidationResult validate(StyleInfo style, boolean isNew) {
        return delegate.validate(style, isNew);
    }

    @Override
    public StyleInfo detach(StyleInfo style) {
        return delegate.detach(style);
    }

    @Override
    public StyleInfo getStyle(String id) {
        return delegate.getStyle(id);
    }

    @Override
    public StyleInfo getStyleByName(String workspaceName, String name) {
        return delegate.getStyleByName(workspaceName, name);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return delegate.getStyleByName(workspace, name);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return delegate.getStyleByName(name);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return delegate.getStylesByWorkspace(workspaceName);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return delegate.getStylesByWorkspace(workspace);
    }

    //
    // NamespaceInfo
    //
    @Override
    public void add(NamespaceInfo namespace) {
        delegate.add(namespace);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        delegate.remove(namespace);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        delegate.save(namespace);
    }

    @Override
    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {
        return delegate.validate(namespace, isNew);
    }

    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return delegate.detach(namespace);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return delegate.getNamespace(id);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return delegate.getNamespaceByPrefix(prefix);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return delegate.getNamespaceByURI(uri);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return delegate.getDefaultNamespace();
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        delegate.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return delegate.getNamespaces();
    }

    //
    // WorkspaceInfo
    //
    @Override
    public void add(WorkspaceInfo workspace) {
        delegate.add(workspace);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        delegate.remove(workspace);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        delegate.save(workspace);
    }

    @Override
    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {
        return delegate.validate(workspace, isNew);
    }

    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return delegate.detach(workspace);
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return delegate.getDefaultWorkspace();
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        delegate.setDefaultWorkspace(workspace);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return delegate.getWorkspaces();
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return delegate.getWorkspace(id);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return delegate.getWorkspaceByName(name);
    }

    //
    // Events
    //

    @Override
    public Collection<CatalogListener> getListeners() {
        return delegate.getListeners();
    }

    @Override
    public void addListener(CatalogListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(CatalogListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void fireAdded(CatalogInfo object) {
        delegate.fireAdded(object);
    }

    @Override
    public void fireModified(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        delegate.fireModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void firePostModified(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        delegate.firePostModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void fireRemoved(CatalogInfo object) {
        delegate.fireRemoved(object);
    }

    //
    // Misc
    //
    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
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

    @Override
    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return delegate.getCatalogCapabilities();
    }
}
