/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.SLDNamedLayerValidator;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogBeforeAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * A default catalog implementation that is memory based.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *     <p>TODO: remove synchronized blocks, make setting of default workspace/namespace part of dao
 *     contract TODO: move resolve() to dao
 */
public class CatalogImpl implements Catalog {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /** data access facade */
    protected CatalogFacade facade;

    /** listeners */
    protected List listeners = new CopyOnWriteArrayList<>();

    /** resources */
    protected ResourcePool resourcePool;

    protected GeoServerResourceLoader resourceLoader;

    /** extended validation switch */
    protected boolean extendedValidation = true;

    public CatalogImpl() {
        facade = new DefaultCatalogFacade(this);
        // wrap the default catalog facade with the facade capable of handling isolated workspaces
        // behavior
        facade = new IsolatedCatalogFacade(facade);
        setFacade(facade);
        resourcePool = ResourcePool.create(this);
    }

    public CatalogFacade getFacade() {
        return facade;
    }

    /**
     * Turn on/off extended validation switch.
     *
     * <p>This is not part of the public api, it is used for testing purposes where we have to
     * bootstrap catalog contents.
     */
    public void setExtendedValidation(boolean extendedValidation) {
        this.extendedValidation = extendedValidation;
    }

    public boolean isExtendedValidation() {
        return extendedValidation;
    }

    public Iterable<CatalogValidator> getValidators() {
        return GeoServerExtensions.extensions(CatalogValidator.class);
    }

    public void setFacade(CatalogFacade facade) {
        final GeoServerConfigurationLock configurationLock =
                GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        if (configurationLock != null) {
            facade = LockingCatalogFacade.create(facade, configurationLock);
        }
        this.facade = facade;
        facade.setCatalog(this);
    }

    public String getId() {
        return "catalog";
    }

    public CatalogFactory getFactory() {
        return new CatalogFactoryImpl(this);
    }

    // Store methods
    public void add(StoreInfo store) {

        if (store.getWorkspace() == null) {
            store.setWorkspace(getDefaultWorkspace());
        }

        validate(store, true);

        // TODO: remove synchronized block, need transactions
        StoreInfo added;
        synchronized (facade) {
            StoreInfo resolved = resolve(store);
            beforeadded(resolved);
            added = facade.add(resolved);

            // if there is no default store use this one as the default
            if (getDefaultDataStore(store.getWorkspace()) == null
                    && store instanceof DataStoreInfo) {
                setDefaultDataStore(store.getWorkspace(), (DataStoreInfo) store);
            }
        }
        added(added);
    }

    public ValidationResult validate(StoreInfo store, boolean isNew) {
        if (isNull(store.getName())) {
            throw new IllegalArgumentException("Store name must not be null");
        }
        if (store.getWorkspace() == null) {
            throw new IllegalArgumentException("Store must be part of a workspace");
        }

        WorkspaceInfo workspace = store.getWorkspace();
        StoreInfo existing = getStoreByName(workspace, store.getName(), StoreInfo.class);
        if (existing != null && (isNew || !existing.getId().equals(store.getId()))) {
            String msg =
                    "Store '"
                            + store.getName()
                            + "' already exists in workspace '"
                            + workspace.getName()
                            + "'";
            throw new IllegalArgumentException(msg);
        }

        return postValidate(store, isNew);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // setDefaultDataStore allows for null store
    public void remove(StoreInfo store) {
        if (!getResourcesByStore(store, ResourceInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete non-empty store.");
        }

        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(store);

            WorkspaceInfo workspace = store.getWorkspace();
            DataStoreInfo defaultStore = getDefaultDataStore(workspace);
            if (store.equals(defaultStore) || defaultStore == null) {
                // TODO: this will fire multiple events, we want to fire only one
                setDefaultDataStore(workspace, null);

                // default removed, choose another store to become default if possible
                List dstores = getStoresByWorkspace(workspace, DataStoreInfo.class);
                if (!dstores.isEmpty()) {
                    setDefaultDataStore(workspace, (DataStoreInfo) dstores.get(0));
                }
            }
        }

        removed(store);
    }

    public void save(StoreInfo store) {
        if (store.getId() == null) {
            // add it instead of saving
            add(store);
            return;
        }
        validate(store, false);
        facade.save(store);
    }

    public <T extends StoreInfo> T detach(T store) {
        return detached(store, facade.detach(store));
    }

    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return facade.getStore(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return getStoreByName((WorkspaceInfo) null, name, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {

        WorkspaceInfo ws = workspace;
        if (ws == null) {
            ws = getDefaultWorkspace();
        }

        if (clazz != null
                && clazz.isAssignableFrom(DataStoreInfo.class)
                && (name == null || name.equals(Catalog.DEFAULT))) {
            return (T) getDefaultDataStore(workspace);
        }

        T store = facade.getStoreByName(ws, name, clazz);
        if (store == null && workspace == null) {
            store = facade.getStoreByName(CatalogFacade.ANY_WORKSPACE, name, clazz);
        }
        return store;
    }

    public <T extends StoreInfo> T getStoreByName(
            String workspaceName, String name, Class<T> clazz) {

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStoreByName(workspace, name, clazz);
        }
        return null;
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            String workspaceName, Class<T> clazz) {

        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.EMPTY_LIST;
            }
        }

        return getStoresByWorkspace(workspace, clazz);
    }

    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        return facade.getStoresByWorkspace(workspace, clazz);
    }

    public List getStores(Class clazz) {
        return facade.getStores(clazz);
    }

    public DataStoreInfo getDataStore(String id) {
        return (DataStoreInfo) getStore(id, DataStoreInfo.class);
    }

    public DataStoreInfo getDataStoreByName(String name) {
        return (DataStoreInfo) getStoreByName(name, DataStoreInfo.class);
    }

    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return (DataStoreInfo) getStoreByName(workspaceName, name, DataStoreInfo.class);
    }

    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return (DataStoreInfo) getStoreByName(workspace, name, DataStoreInfo.class);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, DataStoreInfo.class);
    }

    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, DataStoreInfo.class);
    }

    public List getDataStores() {
        return getStores(DataStoreInfo.class);
    }

    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return facade.getDefaultDataStore(workspace);
    }

    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        if (store != null) {
            // basic sanity check
            if (store.getWorkspace() == null) {
                throw new IllegalArgumentException("The store has not been assigned a workspace");
            }

            if (!store.getWorkspace().equals(workspace)) {
                throw new IllegalArgumentException(
                        "Trying to mark as default "
                                + "for workspace "
                                + workspace.getName()
                                + " a store that "
                                + "is contained in "
                                + store.getWorkspace().getName());
            }
        }
        facade.setDefaultDataStore(workspace, store);
    }

    public CoverageStoreInfo getCoverageStore(String id) {
        return (CoverageStoreInfo) getStore(id, CoverageStoreInfo.class);
    }

    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return (CoverageStoreInfo) getStoreByName(name, CoverageStoreInfo.class);
    }

    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return getStoreByName(workspaceName, name, CoverageStoreInfo.class);
    }

    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return getStoreByName(workspace, name, CoverageStoreInfo.class);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return getStoresByWorkspace(workspaceName, CoverageStoreInfo.class);
    }

    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return getStoresByWorkspace(workspace, CoverageStoreInfo.class);
    }

    public List getCoverageStores() {
        return getStores(CoverageStoreInfo.class);
    }

    // Resource methods
    public void add(ResourceInfo resource) {
        if (resource.getNamespace() == null) {
            // default to default namespace
            resource.setNamespace(getDefaultNamespace());
        }
        if (resource.getNativeName() == null) {
            resource.setNativeName(resource.getName());
        }
        ResourceInfo resolved = resolve(resource);
        validate(resolved, true);
        beforeadded(resolved);
        ResourceInfo added = facade.add(resolved);
        added(added);
    }

    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        if (isNull(resource.getName())) {
            throw new NullPointerException("Resource name must not be null");
        }

        if (isNull(resource.getNativeName())
                && !(resource instanceof CoverageInfo
                        && ((CoverageInfo) resource).getNativeCoverageName() != null)) {
            throw new NullPointerException("Resource native name must not be null");
        }
        if (resource.getStore() == null) {
            throw new IllegalArgumentException("Resource must be part of a store");
        }
        if (resource.getNamespace() == null) {
            throw new IllegalArgumentException("Resource must be part of a namespace");
        }

        StoreInfo store = resource.getStore();
        ResourceInfo existing = getResourceByStore(store, resource.getName(), ResourceInfo.class);
        if (existing != null && !existing.getId().equals(resource.getId())) {
            String msg =
                    "Resource named '"
                            + resource.getName()
                            + "' already exists in store: '"
                            + store.getName()
                            + "'";
            throw new IllegalArgumentException(msg);
        }

        NamespaceInfo namespace = resource.getNamespace();
        existing = getResourceByName(namespace, resource.getName(), ResourceInfo.class);
        if (existing != null && !existing.getId().equals(resource.getId())) {
            String msg =
                    "Resource named '"
                            + resource.getName()
                            + "' already exists in namespace: '"
                            + namespace.getPrefix()
                            + "'";
            throw new IllegalArgumentException(msg);
        }

        validateKeywords(resource.getKeywords());
        return postValidate(resource, isNew);
    }

    public void remove(ResourceInfo resource) {
        // ensure no references to the resource
        if (!getLayers(resource).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete resource referenced by layer");
        }
        facade.remove(resource);
        removed(resource);
    }

    public void save(ResourceInfo resource) {
        validate(resource, false);
        facade.save(resource);
    }

    public <T extends ResourceInfo> T detach(T resource) {
        return detached(resource, facade.detach(resource));
    }

    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return facade.getResource(id, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        if ("".equals(ns)) {
            ns = null;
        }

        if (ns != null) {
            NamespaceInfo namespace = getNamespaceByPrefix(ns);
            if (namespace == null) {
                namespace = getNamespaceByURI(ns);
            }

            if (namespace != null) {
                return getResourceByName(namespace, name, clazz);
            }

            return null;
        }

        return getResourceByName((NamespaceInfo) null, name, clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo ns, String name, Class<T> clazz) {

        NamespaceInfo namespace = ns;
        if (namespace == null) {
            namespace = getDefaultNamespace();
        }
        T resource = facade.getResourceByName(namespace, name, clazz);
        if (resource == null && ns == null) {
            resource = facade.getResourceByName(CatalogFacade.ANY_NAMESPACE, name, clazz);
        }
        return resource;
    }

    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return getResourceByName(name.getNamespaceURI(), name.getLocalPart(), clazz);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        // check is the name is a fully qualified one
        int colon = name.indexOf(':');
        if (colon != -1) {
            String ns = name.substring(0, colon);
            String localName = name.substring(colon + 1);
            return getResourceByName(ns, localName, clazz);
        } else {
            return getResourceByName((String) null, name, clazz);
        }
    }

    public List getResources(Class clazz) {
        return facade.getResources(clazz);
    }

    public List getResourcesByNamespace(NamespaceInfo namespace, Class clazz) {
        return facade.getResourcesByNamespace(namespace, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz) {
        if (namespace == null) {
            return getResourcesByNamespace((NamespaceInfo) null, clazz);
        }

        NamespaceInfo ns = getNamespaceByPrefix(namespace);
        if (ns == null) {
            ns = getNamespaceByURI(namespace);
        }
        if (ns == null) {
            return Collections.EMPTY_LIST;
        }

        return getResourcesByNamespace(ns, clazz);
    }

    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return facade.getResourceByStore(store, name, clazz);
    }

    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return facade.getResourcesByStore(store, clazz);
    }

    public FeatureTypeInfo getFeatureType(String id) {
        return (FeatureTypeInfo) getResource(id, FeatureTypeInfo.class);
    }

    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return (FeatureTypeInfo) getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return getResourceByName(ns, name, FeatureTypeInfo.class);
    }

    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return getResourceByName(name, FeatureTypeInfo.class);
    }

    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return (FeatureTypeInfo) getResourceByName(name, FeatureTypeInfo.class);
    }

    public List getFeatureTypes() {
        return getResources(FeatureTypeInfo.class);
    }

    public List getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, FeatureTypeInfo.class);
    }

    public FeatureTypeInfo getFeatureTypeByStore(DataStoreInfo dataStore, String name) {
        return getFeatureTypeByDataStore(dataStore, name);
    }

    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return getResourceByStore(dataStore, name, FeatureTypeInfo.class);
    }

    public List<FeatureTypeInfo> getFeatureTypesByStore(DataStoreInfo store) {
        return getFeatureTypesByDataStore(store);
    }

    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return getResourcesByStore(store, FeatureTypeInfo.class);
    }

    public CoverageInfo getCoverage(String id) {
        return (CoverageInfo) getResource(id, CoverageInfo.class);
    }

    public CoverageInfo getCoverageByName(String ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return (CoverageInfo) getResourceByName(ns, name, CoverageInfo.class);
    }

    public CoverageInfo getCoverageByName(Name name) {
        return getResourceByName(name, CoverageInfo.class);
    }

    public CoverageInfo getCoverageByName(String name) {
        return (CoverageInfo) getResourceByName(name, CoverageInfo.class);
    }

    public List getCoverages() {
        return getResources(CoverageInfo.class);
    }

    public List getCoveragesByNamespace(NamespaceInfo namespace) {
        return getResourcesByNamespace(namespace, CoverageInfo.class);
    }

    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return getResourceByStore(coverageStore, name, CoverageInfo.class);
    }

    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    // Layer methods
    public void add(LayerInfo layer) {
        layer = resolve(layer);
        validate(layer, true);

        if (layer.getType() == null) {
            if (layer.getResource() instanceof FeatureTypeInfo) {
                layer.setType(PublishedType.VECTOR);
            } else if (layer.getResource() instanceof CoverageInfo) {
                layer.setType(PublishedType.RASTER);
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                layer.setType(PublishedType.WMTS);
            } else if (layer.getResource() instanceof WMSLayerInfo) {
                layer.setType(PublishedType.WMS);
            } else {
                String msg = "Layer type not set and can't be derived from resource";
                throw new IllegalArgumentException(msg);
            }
        }
        beforeadded(layer);
        LayerInfo added = facade.add(layer);
        added(added);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        // TODO: bring back when the layer/publishing split is in act
        //        if ( isNull(layer.getName()) ) {
        //            throw new NullPointerException( "Layer name must not be null" );
        //        }

        if (layer.getResource() == null) {
            throw new NullPointerException("Layer resource must not be null");
        }

        // calling LayerInfo.setName(String) updates the resource (until the layer/publishing split
        // is in act), but that doesn't mean the resource was saved previously, which can leave the
        // catalog in an inconsistent state
        final NamespaceInfo ns = layer.getResource().getNamespace();
        if (null == getResourceByName(ns, layer.getResource().getName(), ResourceInfo.class)) {
            throw new IllegalStateException(
                    "Found no resource named "
                            + layer.prefixedName()
                            + " , Layer with that name can't be added");
        }
        final String prefix = ns != null ? ns.getPrefix() : null;
        LayerInfo existing = getLayerByName(prefix, layer.getName());
        if (existing != null && !existing.getId().equals(layer.getId())) {
            throw new IllegalArgumentException(
                    "Layer named '"
                            + layer.getName()
                            + "' in workspace '"
                            + prefix
                            + "' already exists.");
        }

        // if the style is missing associate a default one, to avoid breaking WMS
        if (layer.getDefaultStyle() == null) {
            try {
                LOGGER.log(
                        Level.INFO,
                        "Layer "
                                + layer.prefixedName()
                                + " is missing the default style, assigning one automatically");
                StyleInfo style = new CatalogBuilder(this).getDefaultStyle(layer.getResource());
                layer.setDefaultStyle(style);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Layer "
                                + layer.prefixedName()
                                + " is missing the default style, "
                                + "failed to associate one automatically",
                        e);
            }
        }

        // clean up eventual dangling references to missing alternate styles
        Set<StyleInfo> styles = layer.getStyles();
        for (Iterator it = styles.iterator(); it.hasNext(); ) {
            StyleInfo styleInfo = (StyleInfo) it.next();
            if (styleInfo == null) {
                it.remove();
            }
        }

        return postValidate(layer, isNew);
    }

    public void remove(LayerInfo layer) {
        // ensure no references to the layer
        for (LayerGroupInfo lg : facade.getLayerGroups()) {
            if (lg.getLayers().contains(layer) || layer.equals(lg.getRootLayer())) {
                String msg =
                        "Unable to delete layer referenced by layer group '" + lg.getName() + "'";
                throw new IllegalArgumentException(msg);
            }
        }
        facade.remove(layer);
        removed(layer);
    }

    public void save(LayerInfo layer) {
        validate(layer, false);
        facade.save(layer);
    }

    public LayerInfo detach(LayerInfo layer) {
        return detached(layer, facade.detach(layer));
    }

    public LayerInfo getLayer(String id) {
        return facade.getLayer(id);
    }

    public LayerInfo getLayerByName(Name name) {
        if (name.getNamespaceURI() != null) {
            NamespaceInfo ns = getNamespaceByURI(name.getNamespaceURI());
            if (ns != null) {
                return getLayerByName(ns.getPrefix() + ":" + name.getLocalPart());
            }
        }

        return getLayerByName(name.getLocalPart());
    }

    public LayerInfo getLayerByName(String name) {
        LayerInfo result = null;
        int colon = name.indexOf(':');
        if (colon != -1) {
            // search by resource name
            String prefix = name.substring(0, colon);
            String resource = name.substring(colon + 1);

            result = getLayerByName(prefix, resource);
        } else {
            // search in default workspace first
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                result = getLayerByName(ws.getName(), name);
            }
        }

        if (result == null) {
            result = facade.getLayerByName(name);
        }

        return result;
    }

    private LayerInfo getLayerByName(String workspace, String resourceName) {
        ResourceInfo r = getResourceByName(workspace, resourceName, ResourceInfo.class);
        if (r == null) {
            return null;
        }
        List<LayerInfo> layers = getLayers(r);
        if (layers.size() == 1) {
            return layers.get(0);
        } else {
            return null;
        }
    }

    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return facade.getLayers(resource);
    }

    public List<LayerInfo> getLayers(StyleInfo style) {
        return facade.getLayers(style);
    }

    public List<LayerInfo> getLayers() {
        return facade.getLayers();
    }

    // Map methods
    public MapInfo getMap(String id) {
        return facade.getMap(id);
    }

    public MapInfo getMapByName(String name) {
        return facade.getMapByName(name);
    }

    public List<MapInfo> getMaps() {
        return facade.getMaps();
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void add(LayerGroupInfo layerGroup) {
        layerGroup = resolve(layerGroup);
        validate(layerGroup, true);

        if (layerGroup.getStyles().isEmpty()) {
            for (PublishedInfo l : layerGroup.getLayers()) {
                // default style
                layerGroup.getStyles().add(null);
            }
        }
        beforeadded(layerGroup);
        LayerGroupInfo added = facade.add(layerGroup);
        added(added);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        if (isNull(layerGroup.getName())) {
            throw new NullPointerException("Layer group name must not be null");
        }

        WorkspaceInfo ws = layerGroup.getWorkspace();
        LayerGroupInfo existing = getLayerGroupByName(ws, layerGroup.getName());
        if (existing != null && !existing.getId().equals(layerGroup.getId())) {
            // null workspace can cause layer group in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            if ((ws == null && ews == null) || (ws != null && ws.equals(ews))) {
                String msg = "Layer group named '" + layerGroup.getName() + "' already exists";
                if (ws != null) {
                    msg += " in workspace " + ws.getName();
                }
                throw new IllegalArgumentException(msg);
            }
        }

        // sanitize a bit broken layer references
        List<PublishedInfo> layers = layerGroup.getLayers();
        List<StyleInfo> styles = layerGroup.getStyles();
        for (int i = 0; i < layers.size(); ) {
            if (layers != null
                    && styles != null
                    && layers.get(i) == null
                    && styles.get(i) == null) {
                layers.remove(i);
                styles.remove(i);
            } else {
                // Validate style group
                if (layers.get(i) == null) {
                    try {
                        // validate style groups
                        StyledLayerDescriptor sld = styles.get(i).getSLD();
                        List<Exception> errors = SLDNamedLayerValidator.validate(this, sld);
                        if (errors.size() > 0) {
                            throw new IllegalArgumentException(
                                    "Invalid style group: " + errors.get(0).getMessage(),
                                    errors.get(0));
                        }
                    } catch (IOException e) {
                        throw new IllegalArgumentException(
                                "Error validating style group: " + e.getMessage(), e);
                    }
                }
                i++;
            }
        }

        if (layerGroup.getLayers() == null || layerGroup.getLayers().isEmpty()) {
            throw new IllegalArgumentException("Layer group must not be empty");
        }

        if (layerGroup.getStyles() != null
                && !layerGroup.getStyles().isEmpty()
                && !(layerGroup.getStyles().size() == layerGroup.getLayers().size())) {
            throw new IllegalArgumentException(
                    "Layer group has different number of styles than layers");
        }

        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
        Stack<LayerGroupInfo> loopPath = helper.checkLoops();
        if (loopPath != null) {
            throw new IllegalArgumentException(
                    "Layer group is in a loop: " + helper.getLoopAsString(loopPath));
        }

        // if the layer group has a workspace assigned, ensure that every resource in that layer
        // group lives within the same workspace
        if (ws != null) {
            checkLayerGroupResourceIsInWorkspace(layerGroup, ws);
        }

        if (layerGroup.getMode() == null) {
            throw new IllegalArgumentException("Layer group mode must not be null");
        } else if (LayerGroupInfo.Mode.EO.equals(layerGroup.getMode())) {
            if (layerGroup.getRootLayer() == null) {
                throw new IllegalArgumentException(
                        "Layer group in mode "
                                + LayerGroupInfo.Mode.EO.getName()
                                + " must have a root layer");
            }

            if (layerGroup.getRootLayerStyle() == null) {
                throw new IllegalArgumentException(
                        "Layer group in mode "
                                + LayerGroupInfo.Mode.EO.getName()
                                + " must have a root layer style");
            }
        } else {
            if (layerGroup.getRootLayer() != null) {
                throw new IllegalArgumentException(
                        "Layer group in mode "
                                + layerGroup.getMode().getName()
                                + " must not have a root layer");
            }

            if (layerGroup.getRootLayerStyle() != null) {
                throw new IllegalArgumentException(
                        "Layer group in mode "
                                + layerGroup.getMode().getName()
                                + " must not have a root layer style");
            }
        }

        return postValidate(layerGroup, isNew);
    }

    private void checkLayerGroupResourceIsInWorkspace(LayerGroupInfo layerGroup, WorkspaceInfo ws) {
        if (layerGroup == null) return;

        if (layerGroup.getWorkspace() != null && !ws.equals(layerGroup.getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace ("
                            + ws.getName()
                            + ") can not contain resources from other workspace: "
                            + layerGroup.getWorkspace().getName());
        }

        checkLayerGroupResourceIsInWorkspace(layerGroup.getRootLayer(), ws);
        checkLayerGroupResourceIsInWorkspace(layerGroup.getRootLayerStyle(), ws);
        if (layerGroup.getLayers() != null) {
            for (PublishedInfo p : layerGroup.getLayers()) {
                if (p instanceof LayerGroupInfo) {
                    checkLayerGroupResourceIsInWorkspace((LayerGroupInfo) p, ws);
                } else if (p instanceof LayerInfo) {
                    checkLayerGroupResourceIsInWorkspace((LayerInfo) p, ws);
                }
            }
        }

        if (layerGroup.getStyles() != null) {
            for (StyleInfo s : layerGroup.getStyles()) {
                checkLayerGroupResourceIsInWorkspace(s, ws);
            }
        }
    }

    private void checkLayerGroupResourceIsInWorkspace(StyleInfo style, WorkspaceInfo ws) {
        if (style == null) return;

        if (style.getWorkspace() != null && !ws.equals(style.getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace ("
                            + ws.getName()
                            + ") can not contain styles from other workspace: "
                            + style.getWorkspace());
        }
    }

    private void checkLayerGroupResourceIsInWorkspace(LayerInfo layer, WorkspaceInfo ws) {
        if (layer == null) return;

        ResourceInfo r = layer.getResource();
        if (r.getStore().getWorkspace() != null && !ws.equals(r.getStore().getWorkspace())) {
            throw new IllegalArgumentException(
                    "Layer group within a workspace ("
                            + ws.getName()
                            + ") can not contain resources from other workspace: "
                            + r.getStore().getWorkspace().getName());
        }
    }

    public void remove(LayerGroupInfo layerGroup) {
        // ensure no references to the layer group
        for (LayerGroupInfo lg : facade.getLayerGroups()) {
            if (lg.getLayers().contains(layerGroup)) {
                String msg =
                        "Unable to delete layer group referenced by layer group '"
                                + lg.getName()
                                + "'";
                throw new IllegalArgumentException(msg);
            }
        }

        facade.remove(layerGroup);
        removed(layerGroup);
    }

    public void save(LayerGroupInfo layerGroup) {
        validate(layerGroup, false);
        facade.save(layerGroup);
    }

    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return detached(layerGroup, facade.detach(layerGroup));
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return facade.getLayerGroups();
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.EMPTY_LIST;
            }
        }

        return getLayerGroupsByWorkspace(workspace);
    }

    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return facade.getLayerGroupsByWorkspace(workspace);
    }

    public LayerGroupInfo getLayerGroup(String id) {
        return facade.getLayerGroup(id);
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public LayerGroupInfo getLayerGroupByName(String name) {

        final LayerGroupInfo layerGroup = getLayerGroupByName((String) null, name);

        if (layerGroup != null) return layerGroup;

        // last chance: checking handle prefixed name case
        String workspaceName = null;
        String layerGroupName = null;

        int colon = name.indexOf(':');
        if (colon == -1) {
            // if there is no prefix, try the default workspace
            WorkspaceInfo defaultWs = getDefaultWorkspace();
            workspaceName = defaultWs == null ? null : defaultWs.getName();
            layerGroupName = name;
        }
        if (colon != -1) {
            workspaceName = name.substring(0, colon);
            layerGroupName = name.substring(colon + 1);
        }

        return getLayerGroupByName(workspaceName, layerGroupName);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return null;
            }
        }

        return getLayerGroupByName(workspace, name);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {

        if (null == workspace) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }

        LayerGroupInfo layerGroup = facade.getLayerGroupByName(workspace, name);
        return layerGroup;
    }

    public void add(MapInfo map) {
        beforeadded(map);
        MapInfo added = facade.add(resolve(map));
        added(added);
    }

    public void remove(MapInfo map) {
        facade.remove(map);
        removed(map);
    }

    public void save(MapInfo map) {
        facade.save(map);
    }

    public MapInfo detach(MapInfo map) {
        return detached(map, facade.detach(map));
    }

    // Namespace methods
    public NamespaceInfo getNamespace(String id) {
        return facade.getNamespace(id);
    }

    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        if (prefix == null || Catalog.DEFAULT.equals(prefix)) {
            NamespaceInfo ns = getDefaultNamespace();
            if (ns != null) {
                prefix = ns.getPrefix();
            }
        }

        return facade.getNamespaceByPrefix(prefix);
    }

    public NamespaceInfo getNamespaceByURI(String uri) {
        return facade.getNamespaceByURI(uri);
    }

    public List getNamespaces() {
        return facade.getNamespaces();
    }

    public void add(NamespaceInfo namespace) {
        validate(namespace, true);

        NamespaceInfo added;
        synchronized (facade) {
            final NamespaceInfo resolved = resolve(namespace);
            beforeadded(namespace);
            added = facade.add(resolved);
            if (getDefaultNamespace() == null) {
                setDefaultNamespace(resolved);
            }
        }

        added(added);
    }

    public ValidationResult validate(NamespaceInfo namespace, boolean isNew) {

        if (namespace.isIsolated() && !getCatalogCapabilities().supportsIsolatedWorkspaces()) {
            // isolated namespaces \ workspaces are not supported by this catalog
            throw new IllegalArgumentException(
                    String.format(
                            "Namespace '%s:%s' is isolated but isolated workspaces are not supported by this catalog.",
                            namespace.getPrefix(), namespace.getURI()));
        }

        if (isNull(namespace.getPrefix())) {
            throw new NullPointerException("Namespace prefix must not be null");
        }

        if (namespace.getPrefix().equals(DEFAULT)) {
            throw new IllegalArgumentException(
                    DEFAULT + " is a reserved keyword, can't be used as the namespace prefix");
        }

        NamespaceInfo existing = getNamespaceByPrefix(namespace.getPrefix());
        if (existing != null && !existing.getId().equals(namespace.getId())) {
            throw new IllegalArgumentException(
                    "Namespace with prefix '" + namespace.getPrefix() + "' already exists.");
        }

        if (!namespace.isIsolated()) {
            // not an isolated namespace \ workplace so we need to check for duplicates
            existing = getNamespaceByURI(namespace.getURI());
            if (existing != null && !existing.getId().equals(namespace.getId())) {
                throw new IllegalArgumentException(
                        "Namespace with URI '" + namespace.getURI() + "' already exists.");
            }
        }

        if (isNull(namespace.getURI())) {
            throw new NullPointerException("Namespace uri must not be null");
        }

        try {
            new URI(namespace.getURI());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid URI syntax for '"
                            + namespace.getURI()
                            + "' in namespace '"
                            + namespace.getPrefix()
                            + "'");
        }

        return postValidate(namespace, isNew);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF") // I don't see this happening...
    public void remove(NamespaceInfo namespace) {
        if (!getResourcesByNamespace(namespace, ResourceInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Unable to delete non-empty namespace.");
        }

        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(namespace);

            NamespaceInfo defaultNamespace = getDefaultNamespace();
            if (namespace.equals(defaultNamespace) || defaultNamespace == null) {
                List<NamespaceInfo> namespaces = facade.getNamespaces();

                defaultNamespace = null;
                if (!namespaces.isEmpty()) {
                    defaultNamespace = namespaces.get(0);
                }

                setDefaultNamespace(defaultNamespace);
                if (defaultNamespace != null) {
                    WorkspaceInfo defaultWorkspace =
                            getWorkspaceByName(defaultNamespace.getPrefix());
                    if (defaultWorkspace != null) {
                        setDefaultWorkspace(defaultWorkspace);
                    }
                }
            }
        }
        removed(namespace);
    }

    public void save(NamespaceInfo namespace) {
        validate(namespace, false);

        facade.save(namespace);
    }

    public NamespaceInfo detach(NamespaceInfo namespace) {
        return detached(namespace, facade.detach(namespace));
    }

    public NamespaceInfo getDefaultNamespace() {
        return facade.getDefaultNamespace();
    }

    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        if (defaultNamespace != null) {
            NamespaceInfo ns = getNamespaceByPrefix(defaultNamespace.getPrefix());
            if (ns == null) {
                throw new IllegalArgumentException(
                        "No such namespace: '" + defaultNamespace.getPrefix() + "'");
            } else {
                defaultNamespace = ns;
            }
        }
        facade.setDefaultNamespace(defaultNamespace);
    }

    // Workspace methods
    public void add(WorkspaceInfo workspace) {
        workspace = resolve(workspace);
        validate(workspace, true);

        if (getWorkspaceByName(workspace.getName()) != null) {
            throw new IllegalArgumentException(
                    "Workspace with name '" + workspace.getName() + "' already exists.");
        }

        WorkspaceInfo added;
        synchronized (facade) {
            beforeadded(workspace);
            added = facade.add(workspace);
            // if there is no default workspace use this one as the default
            if (getDefaultWorkspace() == null) {
                setDefaultWorkspace(workspace);
            }
        }

        added(added);
    }

    public ValidationResult validate(WorkspaceInfo workspace, boolean isNew) {

        if (workspace.isIsolated() && !getCatalogCapabilities().supportsIsolatedWorkspaces()) {
            // isolated namespaces \ workspaces are not supported by this catalog
            throw new IllegalArgumentException(
                    String.format(
                            "Workspace '%s' is isolated but isolated workspaces are not supported by this catalog.",
                            workspace.getName()));
        }

        if (isNull(workspace.getName())) {
            throw new NullPointerException("workspace name must not be null");
        }

        if (workspace.getName().equals(DEFAULT)) {
            throw new IllegalArgumentException(
                    DEFAULT + " is a reserved keyword, can't be used as the workspace name");
        }

        WorkspaceInfo existing = getWorkspaceByName(workspace.getName());
        if (existing != null && !existing.getId().equals(workspace.getId())) {
            throw new IllegalArgumentException(
                    "Workspace named '" + workspace.getName() + "' already exists.");
        }

        return postValidate(workspace, isNew);
    }

    @SuppressFBWarnings("NP_NULL_PARAM_DEREF") // I don't see this happening...
    public void remove(WorkspaceInfo workspace) {
        // JD: maintain the link between namespace and workspace, remove this when this is no
        // longer necessary
        if (getNamespaceByPrefix(workspace.getName()) != null) {
            throw new IllegalArgumentException("Cannot delete workspace with linked namespace");
        }
        if (!getStoresByWorkspace(workspace, StoreInfo.class).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete non-empty workspace.");
        }

        // TODO: remove synchronized block, need transactions
        synchronized (facade) {
            facade.remove(workspace);

            WorkspaceInfo defaultWorkspace = getDefaultWorkspace();
            if (workspace.equals(defaultWorkspace) || defaultWorkspace == null) {
                List<WorkspaceInfo> workspaces = facade.getWorkspaces();

                defaultWorkspace = null;
                if (!workspaces.isEmpty()) {
                    defaultWorkspace = workspaces.get(0);
                }

                setDefaultWorkspace(defaultWorkspace);
                if (defaultWorkspace != null) {
                    NamespaceInfo defaultNamespace =
                            getNamespaceByPrefix(defaultWorkspace.getName());
                    if (defaultNamespace != null) {
                        setDefaultNamespace(defaultNamespace);
                    }
                }
            }
        }

        removed(workspace);
    }

    public void save(WorkspaceInfo workspace) {
        validate(workspace, false);

        facade.save(workspace);
    }

    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return detached(workspace, facade.detach(workspace));
    }

    public WorkspaceInfo getDefaultWorkspace() {
        return facade.getDefaultWorkspace();
    }

    public void setDefaultWorkspace(WorkspaceInfo defaultWorkspace) {
        if (defaultWorkspace != null) {
            WorkspaceInfo ws = facade.getWorkspaceByName(defaultWorkspace.getName());
            if (ws == null) {
                throw new IllegalArgumentException(
                        "No such workspace: '" + defaultWorkspace.getName() + "'");
            } else {
                defaultWorkspace = ws;
            }
        }
        facade.setDefaultWorkspace(defaultWorkspace);
    }

    public List<WorkspaceInfo> getWorkspaces() {
        return facade.getWorkspaces();
    }

    public WorkspaceInfo getWorkspace(String id) {
        return facade.getWorkspace(id);
    }

    public WorkspaceInfo getWorkspaceByName(String name) {
        if (name == null || Catalog.DEFAULT.equals(name)) {
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                name = ws.getName();
            }
        }
        return facade.getWorkspaceByName(name);
    }

    // Style methods
    public StyleInfo getStyle(String id) {
        return facade.getStyle(id);
    }

    public StyleInfo getStyleByName(String name) {
        StyleInfo result = null;
        int colon = name.indexOf(':');
        if (colon != -1) {
            // search by resource name
            String prefix = name.substring(0, colon);
            String resource = name.substring(colon + 1);

            result = getStyleByName(prefix, resource);
        } else {
            // search in default workspace first
            WorkspaceInfo ws = getDefaultWorkspace();
            if (ws != null) {
                result = getStyleByName(ws, name);
            }
        }
        if (result == null) {
            result = facade.getStyleByName(name);
        }

        return result;
    }

    public StyleInfo getStyleByName(String workspaceName, String name) {
        if (workspaceName == null) {
            return getStyleByName((WorkspaceInfo) null, name);
        }

        WorkspaceInfo workspace = getWorkspaceByName(workspaceName);
        if (workspace != null) {
            return getStyleByName(workspace, name);
        }
        return null;
    }

    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        if (workspace == null) {
            workspace = DefaultCatalogFacade.NO_WORKSPACE;
        }
        StyleInfo style = facade.getStyleByName(workspace, name);
        return style;
    }

    public List getStyles() {
        return facade.getStyles();
    }

    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        WorkspaceInfo workspace = null;
        if (workspaceName != null) {
            workspace = getWorkspaceByName(workspaceName);
            if (workspace == null) {
                return Collections.EMPTY_LIST;
            }
        }

        return getStylesByWorkspace(workspace);
    }

    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return facade.getStylesByWorkspace(workspace);
    }

    public void add(StyleInfo style) {
        style = resolve(style);
        validate(style, true);
        // set creation time before persisting
        beforeadded(style);
        StyleInfo added = facade.add(style);
        added(added);
    }

    public ValidationResult validate(StyleInfo style, boolean isNew) {
        if (isNull(style.getName())) {
            throw new NullPointerException("Style name must not be null");
        }
        if (isNull(style.getFilename())) {
            throw new NullPointerException("Style fileName must not be null");
        }

        WorkspaceInfo ws = style.getWorkspace();
        StyleInfo existing = getStyleByName(ws, style.getName());
        if (existing != null && (isNew || !existing.getId().equals(style.getId()))) {
            // null workspace can cause style in any workspace to be returned, check that
            // workspaces match
            WorkspaceInfo ews = existing.getWorkspace();
            String msg = "Style named '" + style.getName() + "' already exists";
            if (ews != null) {
                msg += " in workspace " + ews.getName();
            }
            throw new IllegalArgumentException(msg);
        }

        if (!isNew) {
            StyleInfo current = getStyle(style.getId());

            // Default style validation
            if (isDefaultStyle(current)) {

                if (!current.getName().equals(style.getName())) {
                    throw new IllegalArgumentException("Cannot rename default styles");
                }
                if (null != style.getWorkspace()) {
                    throw new IllegalArgumentException(
                            "Cannot change the workspace of default styles");
                }
            }
        }

        return postValidate(style, isNew);
    }

    public void remove(StyleInfo style) {
        // ensure no references to the style
        for (LayerInfo l : facade.getLayers(style)) {
            throw new IllegalArgumentException(
                    "Unable to delete style referenced by '" + l.getName() + "'");
        }

        for (LayerGroupInfo lg : facade.getLayerGroups()) {
            if (lg.getStyles().contains(style) || style.equals(lg.getRootLayerStyle())) {
                String msg =
                        "Unable to delete style referenced by layer group '" + lg.getName() + "'";
                throw new IllegalArgumentException(msg);
            }
        }

        if (isDefaultStyle(style)) {
            throw new IllegalArgumentException("Unable to delete a default style");
        }

        facade.remove(style);
        removed(style);
    }

    private boolean isDefaultStyle(StyleInfo s) {
        return s.getWorkspace() == null
                && (StyleInfo.DEFAULT_POINT.equals(s.getName())
                        || StyleInfo.DEFAULT_LINE.equals(s.getName())
                        || StyleInfo.DEFAULT_POLYGON.equals(s.getName())
                        || StyleInfo.DEFAULT_RASTER.equals(s.getName())
                        || StyleInfo.DEFAULT_GENERIC.equals(s.getName()));
    }

    public void save(StyleInfo style) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(style);
        validate(style, false);

        // here we handle name changes
        int i = h.getPropertyNames().indexOf("name");
        if (i > -1 && !h.getNewValues().get(i).equals(h.getOldValues().get(i))) {
            String newName = (String) h.getNewValues().get(i);
            try {
                renameStyle(style, newName);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to rename style file along with name.", e);
            }
        }

        facade.save(style);
    }

    private void renameStyle(StyleInfo s, String newName) throws IOException {
        // rename style definition file
        Resource style = new GeoServerDataDirectory(resourceLoader).style(s);
        StyleHandler format = Styles.handler(s.getFormat());

        Resource target = Resources.uniqueResource(style, newName, format.getFileExtension());
        style.renameTo(target);
        s.setFilename(target.name());

        // rename generated sld if appropriate
        if (!SLDHandler.FORMAT.equals(format.getFormat())) {
            Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
            if (sld.getType() == Type.RESOURCE) {
                LOGGER.fine("Renaming style resource " + s.getName() + " to " + newName);

                Resource generated = Resources.uniqueResource(sld, newName, "sld");
                sld.renameTo(generated);
            }
        }
    }

    public StyleInfo detach(StyleInfo style) {
        return detached(style, facade.detach(style));
    }

    // Event methods
    public Collection getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    public void addListener(CatalogListener listener) {
        listeners.add(listener);
        Collections.sort(listeners, ExtensionPriority.COMPARATOR);
    }

    public void removeListener(CatalogListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void removeListeners(Class listenerClass) {
        new ArrayList<>(listeners)
                .stream()
                .filter(l -> listenerClass.isInstance(l))
                .forEach(l -> listeners.remove(l));
    }

    public Iterator search(String cql) {
        // TODO Auto-generated method stub
        return null;
    }

    public ResourcePool getResourcePool() {
        return resourcePool;
    }

    public void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void dispose() {
        if (resourcePool != null) resourcePool.dispose();
        facade.dispose();
    }

    protected void added(CatalogInfo object) {
        fireAdded(object);
    }

    protected void beforeadded(CatalogInfo object) {
        fireBeforeAdded(object);
    }

    protected void removed(CatalogInfo object) {
        fireRemoved(object);
    }

    public void fireBeforeAdded(CatalogInfo object) {
        CatalogBeforeAddEventImpl event = new CatalogBeforeAddEventImpl();
        event.setSource(object);
        event(event);
    }

    public void fireAdded(CatalogInfo object) {
        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(object);

        event(event);
    }

    public void fireModified(
            CatalogInfo object, List propertyNames, List oldValues, List newValues) {
        CatalogModifyEventImpl event = new CatalogModifyEventImpl();

        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);

        event(event);
    }

    public void firePostModified(
            CatalogInfo object, List propertyNames, List oldValues, List newValues) {
        CatalogPostModifyEventImpl event = new CatalogPostModifyEventImpl();
        event.setSource(object);
        event.setPropertyNames(propertyNames);
        event.setOldValues(oldValues);
        event.setNewValues(newValues);
        event(event);
    }

    public void fireRemoved(CatalogInfo object) {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(object);

        event(event);
    }

    protected void event(CatalogEvent event) {
        CatalogException toThrow = null;

        for (Iterator l = listeners.iterator(); l.hasNext(); ) {
            try {
                CatalogListener listener = (CatalogListener) l.next();

                if (event instanceof CatalogAddEvent) {
                    listener.handleAddEvent((CatalogAddEvent) event);
                } else if (event instanceof CatalogRemoveEvent) {
                    listener.handleRemoveEvent((CatalogRemoveEvent) event);
                } else if (event instanceof CatalogModifyEvent) {
                    listener.handleModifyEvent((CatalogModifyEvent) event);
                } else if (event instanceof CatalogPostModifyEvent) {
                    listener.handlePostModifyEvent((CatalogPostModifyEvent) event);
                } else if (event instanceof CatalogBeforeAddEvent) {
                    listener.handlePreAddEvent((CatalogBeforeAddEvent) event);
                }
            } catch (Throwable t) {
                if (t instanceof CatalogException && toThrow == null) {
                    toThrow = (CatalogException) t;
                } else if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING, "Catalog listener threw exception handling event.", t);
                }
            }
        }

        if (toThrow != null) {
            throw toThrow;
        }
    }

    public static Object unwrap(Object obj) {
        return obj;
    }

    public static void validateKeywords(List<KeywordInfo> keywords) {
        if (keywords != null) {
            for (KeywordInfo kw : keywords) {
                Matcher m = KeywordInfo.RE.matcher(kw.getValue());
                if (!m.matches()) {
                    throw new IllegalArgumentException(
                            "Illegal keyword '"
                                    + kw
                                    + "'. "
                                    + "Keywords must not be empty and must not contain the '\\' character");
                }
                if (kw.getVocabulary() != null) {
                    m = KeywordInfo.RE.matcher(kw.getVocabulary());
                    if (!m.matches()) {
                        throw new IllegalArgumentException(
                                "Keyword vocbulary must not contain the '\\' character");
                    }
                }
            }
        }
    }

    /** Implementation method for resolving all {@link ResolvingProxy} instances. */
    public void resolve() {
        facade.setCatalog(this);
        facade.resolve();

        if (listeners == null) {
            listeners = new ArrayList<CatalogListener>();
        }

        if (resourcePool == null) {
            resourcePool = ResourcePool.create(this);
        }
    }

    protected WorkspaceInfo resolve(WorkspaceInfo workspace) {
        resolveCollections(workspace);
        return workspace;
    }

    protected NamespaceInfo resolve(NamespaceInfo namespace) {
        resolveCollections(namespace);
        return namespace;
    }

    protected StoreInfo resolve(StoreInfo store) {
        resolveCollections(store);

        StoreInfoImpl s = (StoreInfoImpl) store;
        s.setCatalog(this);

        return store;
    }

    protected ResourceInfo resolve(ResourceInfo resource) {

        ResourceInfoImpl r = (ResourceInfoImpl) resource;
        r.setCatalog(this);

        if (resource instanceof FeatureTypeInfo) {
            resolve((FeatureTypeInfo) resource);
        }
        if (r instanceof CoverageInfo) {
            resolve((CoverageInfo) resource);
        }
        if (r instanceof WMSLayerInfo) {
            resolve((WMSLayerInfo) resource);
        }
        if (r instanceof WMTSLayerInfo) {
            resolve((WMTSLayerInfo) resource);
        }

        return resource;
    }

    private CoverageInfo resolve(CoverageInfo r) {
        CoverageInfoImpl c = (CoverageInfoImpl) r;
        if (c.getDimensions() != null) {
            for (CoverageDimensionInfo dim : c.getDimensions()) {
                if (dim.getNullValues() == null) {
                    ((CoverageDimensionImpl) dim).setNullValues(new ArrayList<Double>());
                }
            }
        }
        resolveCollections(r);
        return r;
    }

    /**
     * We don't want the world to be able and call this without going trough {@link
     * #resolve(ResourceInfo)}
     */
    private FeatureTypeInfo resolve(FeatureTypeInfo featureType) {
        FeatureTypeInfoImpl ft = (FeatureTypeInfoImpl) featureType;
        resolveCollections(ft);
        return ft;
    }

    private WMSLayerInfo resolve(WMSLayerInfo wmsLayer) {
        WMSLayerInfoImpl impl = (WMSLayerInfoImpl) wmsLayer;
        resolveCollections(impl);
        return wmsLayer;
    }

    private WMTSLayerInfo resolve(WMTSLayerInfo wmtsLayer) {
        WMTSLayerInfoImpl impl = (WMTSLayerInfoImpl) wmtsLayer;
        resolveCollections(impl);
        return wmtsLayer;
    }

    protected LayerInfo resolve(LayerInfo layer) {
        if (layer.getAttribution() == null) {
            layer.setAttribution(getFactory().createAttribution());
        }
        resolveCollections(layer);
        return layer;
    }

    protected LayerGroupInfo resolve(LayerGroupInfo layerGroup) {
        resolveCollections(layerGroup);
        return layerGroup;
    }

    protected StyleInfo resolve(StyleInfo style) {
        ((StyleInfoImpl) style).setCatalog(this);
        return style;
    }

    protected MapInfo resolve(MapInfo map) {
        resolveCollections(map);
        return map;
    }

    /** Method which reflectively sets all collections when they are null. */
    protected void resolveCollections(Object object) {
        OwsUtils.resolveCollections(object);
    }

    protected boolean isNull(String string) {
        return string == null || "".equals(string.trim());
    }

    <T extends CatalogInfo> T detached(T original, T detached) {
        return detached != null ? detached : original;
    }

    protected ValidationResult postValidate(CatalogInfo info, boolean isNew) {
        List<RuntimeException> errors = new ArrayList<RuntimeException>();

        if (!extendedValidation) {
            return new ValidationResult(null);
        }

        for (CatalogValidator constraint : getValidators()) {
            try {
                info.accept(new CatalogValidatorVisitor(constraint, isNew));
            } catch (RuntimeException e) {
                errors.add(e);
            }
        }
        return new ValidationResult(errors);
    }

    static class CatalogValidatorVisitor implements CatalogVisitor {

        CatalogValidator validator;
        boolean isNew;

        CatalogValidatorVisitor(CatalogValidator validator, boolean isNew) {
            this.validator = validator;
            this.isNew = isNew;
        }

        public void visit(Catalog catalog) {}

        public void visit(WorkspaceInfo workspace) {
            validator.validate(workspace, isNew);
        }

        public void visit(NamespaceInfo namespace) {
            validator.validate(namespace, isNew);
        }

        public void visit(DataStoreInfo dataStore) {
            validator.validate(dataStore, isNew);
        }

        public void visit(CoverageStoreInfo coverageStore) {
            validator.validate(coverageStore, isNew);
        }

        public void visit(WMSStoreInfo wmsStore) {
            validator.validate(wmsStore, isNew);
        }

        public void visit(WMTSStoreInfo wmtsStore) {
            validator.validate(wmtsStore, isNew);
        }

        public void visit(FeatureTypeInfo featureType) {
            validator.validate(featureType, isNew);
        }

        public void visit(CoverageInfo coverage) {
            validator.validate(coverage, isNew);
        }

        public void visit(LayerInfo layer) {
            validator.validate(layer, isNew);
        }

        public void visit(StyleInfo style) {
            validator.validate(style, isNew);
        }

        public void visit(LayerGroupInfo layerGroup) {
            validator.validate(layerGroup, isNew);
        }

        public void visit(WMSLayerInfo wmsLayer) {
            validator.validate(wmsLayer, isNew);
        }

        @Override
        public void visit(WMTSLayerInfo wmtsLayer) {
            validator.validate(wmtsLayer, isNew);
        }
    }

    public void sync(CatalogImpl other) {
        other.facade.syncTo(facade);
        listeners = other.listeners;

        if (resourcePool != other.resourcePool) {
            resourcePool.dispose();
            resourcePool = other.resourcePool;
            resourcePool.setCatalog(this);
        }

        resourceLoader = other.resourceLoader;
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public void resolve(CatalogInfo info) {
        if (info instanceof LayerGroupInfo) {
            resolve((LayerGroupInfo) info);
        } else if (info instanceof LayerInfo) {
            resolve((LayerInfo) info);
        } else if (info instanceof MapInfo) {
            resolve((MapInfo) info);
        } else if (info instanceof NamespaceInfo) {
            resolve((NamespaceInfo) info);
        } else if (info instanceof ResourceInfo) {
            resolve((ResourceInfo) info);
        } else if (info instanceof StoreInfo) {
            resolve((StoreInfo) info);
        } else if (info instanceof StyleInfo) {
            resolve((StyleInfo) info);
        } else if (info instanceof WorkspaceInfo) {
            resolve((WorkspaceInfo) info);
        } else {
            throw new IllegalArgumentException("Unknown resource type: " + info);
        }
    }

    @Override
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {
        final CatalogFacade facade = getFacade();
        return facade.count(of, filter);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of, final Filter filter) {
        return list(of, filter, null, null, null);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            Integer offset,
            Integer count,
            SortBy sortOrder) {
        CatalogFacade facade = getFacade();
        if (sortOrder != null
                && !facade.canSort(of, sortOrder.getPropertyName().getPropertyName())) {
            // TODO: use GeoTools' merge-sort code to provide sorting anyways
            throw new UnsupportedOperationException(
                    "Catalog backend can't sort on property "
                            + sortOrder.getPropertyName()
                            + " in-process sorting is pending implementation");
        }
        if (sortOrder != null) {
            return facade.list(of, filter, offset, count, sortOrder);
        } else {
            return facade.list(of, filter, offset, count);
        }
    }

    @Override
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter)
            throws IllegalArgumentException {

        final Integer limit = Integer.valueOf(2);
        CloseableIterator<T> it = list(type, filter, null, limit, null);
        T result = null;
        try {
            if (it.hasNext()) {
                result = it.next();
                if (it.hasNext()) {
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
                }
            }
        } finally {
            it.close();
        }
        return result;
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return facade.getCatalogCapabilities();
    }
}
