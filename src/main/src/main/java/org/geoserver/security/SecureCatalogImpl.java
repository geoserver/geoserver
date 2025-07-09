/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.or;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
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
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
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
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.decorators.DecoratingCatalogFactory;
import org.geoserver.security.decorators.SecuredCoverageInfo;
import org.geoserver.security.decorators.SecuredCoverageStoreInfo;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredLayerGroupInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geoserver.security.decorators.SecuredObjects;
import org.geoserver.security.decorators.SecuredWMSLayerInfo;
import org.geoserver.security.decorators.SecuredWMSStoreInfo;
import org.geoserver.security.decorators.SecuredWMTSLayerInfo;
import org.geoserver.security.decorators.SecuredWMTSStoreInfo;
import org.geoserver.security.impl.DefaultResourceAccessManager;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.decorate.AbstractDecorator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

/**
 * Wraps the catalog and applies the security directives provided by a {@link ResourceAccessManager} registered in the
 * Spring application context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecureCatalogImpl extends AbstractDecorator<Catalog> implements Catalog {

    /**
     * How to behave in case of mixed mode catalog access, hide the resource or challenge the user to authenticate. For
     * any direct access (by name, id) do challenge, for any "catch all" or "catch group" access, where the single
     * resource was not explicitly requested, hide.
     */
    public enum MixedModeBehavior {
        HIDE,
        CHALLENGE
    }

    protected ResourceAccessManager accessManager;

    public SecureCatalogImpl(Catalog catalog) throws Exception {
        this(catalog, lookupResourceAccessManager());
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    public ResourceAccessManager getResourceAccessManager() {
        return accessManager;
    }

    static ResourceAccessManager lookupResourceAccessManager() throws Exception {
        List<ResourceAccessManager> managers = GeoServerExtensions.extensions(ResourceAccessManager.class);
        ResourceAccessManager manager = null;
        int size = managers.size();
        if (size == 1) {
            manager = managers.get(0);
        } else {
            for (ResourceAccessManager resourceAccessManager : managers) {
                if (!DefaultResourceAccessManager.class.equals(resourceAccessManager.getClass())) {
                    manager = resourceAccessManager;
                    break;
                }
            }
        }
        // should never happen,just in case we have multiple singleton beans
        // of type DefaultResourceAccessManager
        if (manager == null) manager = managers.get(0);

        CatalogFilterAccessManager lwManager = new CatalogFilterAccessManager();
        lwManager.setDelegate(manager);
        return lwManager;
    }

    public SecureCatalogImpl(Catalog catalog, ResourceAccessManager manager) {
        super(catalog);
        this.accessManager = manager;
    }

    // -------------------------------------------------------------------
    // SECURED METHODS
    // -------------------------------------------------------------------

    @Override
    public CoverageInfo getCoverage(String id) {
        return checkAccess(user(), delegate.getCoverage(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageInfo getCoverageByName(String ns, String name) {
        return checkAccess(user(), delegate.getCoverageByName(ns, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageInfo getCoverageByName(NamespaceInfo ns, String name) {
        return checkAccess(user(), delegate.getCoverageByName(ns, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageInfo getCoverageByName(Name name) {
        return checkAccess(user(), delegate.getCoverageByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageInfo getCoverageByName(String name) {
        return checkAccess(user(), delegate.getCoverageByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<CoverageInfo> getCoverages() {
        return getResources(CoverageInfo.class);
    }

    @Override
    public List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace) {
        // filter in-place, there's logic in DefaultCatalogFacade.getResourcesByNamespace() we don't
        // want to duplicate here in order to create a filter
        return filterResources(user(), delegate.getCoveragesByNamespace(namespace));
    }

    @Override
    public List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    @Override
    public CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name) {
        return checkAccess(
                user(), delegate.getCoverageByCoverageStore(coverageStore, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store) {
        return getResourcesByStore(store, CoverageInfo.class);
    }

    @Override
    public CoverageStoreInfo getCoverageStore(String id) {
        return checkAccess(user(), delegate.getCoverageStore(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String name) {
        return checkAccess(user(), delegate.getCoverageStoreByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name) {
        return checkAccess(user(), delegate.getCoverageStoreByName(workspaceName, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name) {
        return checkAccess(user(), delegate.getCoverageStoreByName(workspace, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName) {
        return filterStores(user(), delegate.getCoverageStoresByWorkspace(workspaceName));
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace) {
        return filterStores(user(), delegate.getCoverageStoresByWorkspace(workspace));
    }

    @Override
    public List<CoverageStoreInfo> getCoverageStores() {
        return getStores(CoverageStoreInfo.class);
    }

    @Override
    public WMSStoreInfo getWMSStore(String id) {
        return checkAccess(user(), delegate.getWMSStore(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public WMSStoreInfo getWMSStoreByName(String name) {
        return checkAccess(user(), delegate.getWMSStoreByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public WMTSStoreInfo getWMTSStore(String id) {
        return checkAccess(user(), delegate.getWMTSStore(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public WMTSStoreInfo getWMTSStoreByName(String name) {
        return checkAccess(user(), delegate.getWMTSStoreByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public DataStoreInfo getDataStore(String id) {
        return checkAccess(user(), delegate.getDataStore(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String name) {
        return checkAccess(user(), delegate.getDataStoreByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public DataStoreInfo getDataStoreByName(String workspaceName, String name) {
        return checkAccess(user(), delegate.getDataStoreByName(workspaceName, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name) {
        return checkAccess(user(), delegate.getDataStoreByName(workspace, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName) {
        return filterStores(user(), delegate.getDataStoresByWorkspace(workspaceName));
    }

    @Override
    public List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace) {
        return filterStores(user(), delegate.getDataStoresByWorkspace(workspace));
    }

    @Override
    public List<DataStoreInfo> getDataStores() {
        return getStores(DataStoreInfo.class);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return delegate.getDefaultNamespace();
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return delegate.getDefaultWorkspace();
    }

    @Override
    public FeatureTypeInfo getFeatureType(String id) {
        return checkAccess(user(), delegate.getFeatureType(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String ns, String name) {
        return checkAccess(user(), delegate.getFeatureTypeByName(ns, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name) {
        return checkAccess(user(), delegate.getFeatureTypeByName(ns, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(Name name) {
        return checkAccess(user(), delegate.getFeatureTypeByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByName(String name) {
        return checkAccess(user(), delegate.getFeatureTypeByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypes() {
        return getResources(FeatureTypeInfo.class);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace) {
        return filterResources(user(), delegate.getFeatureTypesByNamespace(namespace));
    }

    @Override
    public FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name) {
        return checkAccess(user(), delegate.getFeatureTypeByDataStore(dataStore, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store) {
        return getResourcesByStore(store, FeatureTypeInfo.class);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return checkAccess(user(), delegate.getLayer(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return checkAccess(user(), delegate.getLayerByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerInfo getLayerByName(Name name) {
        return checkAccess(user(), delegate.getLayerByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return checkAccess(user(), delegate.getLayerGroup(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return checkAccess(user(), delegate.getLayerGroupByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String workspaceName, String name) {
        return checkAccess(user(), delegate.getLayerGroupByName(workspaceName, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return checkAccess(user(), delegate.getLayerGroupByName(workspace, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return getAll(LayerGroupInfo.class);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName) {
        // there's a bunch of logic in CatalogImpl.getLayerGroupsByWorkspace(String) and
        // DefaultCatalogFacade.getLayerGroupsByWorkspace(WorkspaceInfo),
        // so don't delegate to this.getAll(Filter) and hence to list(). Filtering by workspace
        // should reduce the number of matches enough anyway
        return filterGroups(user(), delegate.getLayerGroupsByWorkspace(workspaceName));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        // there's a bunch of logic in CatalogImpl.getLayerGroupsByWorkspace(WorkspaceInfo) and
        // DefaultCatalogFacade.getLayerGroupsByWorkspace(WorkspaceInfo),
        // so don't delegate to this.getAll(Filter) and hence to list(). Filtering by workspace
        // should reduce the number of matches enough anyway
        return filterGroups(user(), delegate.getLayerGroupsByWorkspace(workspace));
    }

    @Override
    public List<LayerInfo> getLayers() {
        return getAll(LayerInfo.class);
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        Filter filter = Predicates.equal("resource.id", resource.getId());
        return getAll(LayerInfo.class, filter);
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {

        String id = style.getId();
        Filter filter = or(Predicates.equal("defaultStyle.id", id), Predicates.equal("styles.id", id));

        return getAll(LayerInfo.class, filter);
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return checkAccess(user(), delegate.getNamespace(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return checkAccess(user(), delegate.getNamespaceByPrefix(prefix), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return checkAccess(user(), delegate.getNamespaceByURI(uri), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<NamespaceInfo> getNamespaces() {
        return getAll(NamespaceInfo.class);
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return checkAccess(user(), delegate.getResource(id, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz) {
        return checkAccess(user(), delegate.getResourceByName(name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getResourceByName(name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo ns, String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getResourceByName(ns, name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getResourceByName(ns, name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return getAll(clazz);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz) {
        return filterResources(user(), delegate.getResourcesByNamespace(namespace, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(String namespace, Class<T> clazz) {
        return filterResources(user(), delegate.getResourcesByNamespace(namespace, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getResourceByStore(store, name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        Filter filter = Predicates.equal("store.id", store.getId());
        return getAll(clazz, filter);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return checkAccess(user(), delegate.getStore(id, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getStoreByName(name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(String workspaceName, String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getStoreByName(workspaceName, name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return checkAccess(user(), delegate.getStoreByName(workspace, name, clazz), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return getAll(clazz);
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(String workspaceName, Class<T> clazz) {
        return filterStores(user(), delegate.getStoresByWorkspace(workspaceName, clazz));
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        return filterStores(user(), delegate.getStoresByWorkspace(workspace, clazz));
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return checkAccess(user(), delegate.getWorkspace(id), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return checkAccess(user(), delegate.getWorkspaceByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return getAll(WorkspaceInfo.class);
    }

    // -------------------------------------------------------------------
    // Security support method
    // -------------------------------------------------------------------

    protected static Authentication user() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @SuppressWarnings("unchecked")
    protected <T extends CatalogInfo> T checkAccess(Authentication user, T info, MixedModeBehavior mixedModeBehavior) {
        if (info instanceof WorkspaceInfo) {
            return (T) checkAccess(user, (WorkspaceInfo) info, mixedModeBehavior);
        }
        if (info instanceof NamespaceInfo) {
            return (T) checkAccess(user, (NamespaceInfo) info, mixedModeBehavior);
        }
        if (info instanceof StoreInfo) {
            return (T) checkAccess(user, (StoreInfo) info, mixedModeBehavior);
        }
        if (info instanceof ResourceInfo) {
            return (T) checkAccess(user, (ResourceInfo) info, mixedModeBehavior);
        }
        if (info instanceof LayerInfo) {
            return (T) checkAccess(user, (LayerInfo) info, mixedModeBehavior, Collections.emptyList());
        }
        if (info instanceof LayerGroupInfo) {
            return (T) checkAccess(user, (LayerGroupInfo) info, mixedModeBehavior, Collections.emptyList());
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    protected <T extends PublishedInfo> T checkAccess(
            Authentication user, T info, MixedModeBehavior mixedModeBehavior, List<LayerGroupInfo> containers) {
        if (info instanceof LayerInfo) {
            return (T) checkAccess(user, (LayerInfo) info, mixedModeBehavior, containers);
        }
        if (info instanceof LayerGroupInfo) {
            return (T) checkAccess(user, (LayerGroupInfo) info, mixedModeBehavior, containers);
        }

        return info;
    }

    /**
     * Given a {@link FeatureTypeInfo} and a user, returns it back if the user can access it in write mode, makes it
     * read only if the user can access it in read only mode, returns null otherwise
     */
    protected <T extends ResourceInfo> T checkAccess(Authentication user, T info, MixedModeBehavior mixedModeBehavior) {
        // handle null case
        if (info == null) return null;

        // first off, handle the case where the user cannot even read the data
        WrapperPolicy policy = buildWrapperPolicy(user, info, info.getName(), mixedModeBehavior);

        // handle the modes that do not require wrapping
        if (policy.level == AccessLevel.HIDDEN) return null;
        else if (policy.level == AccessLevel.READ_WRITE && policy.getLimits() == null) return info;

        // otherwise we are in a mixed case where the user can read but not write, or
        // cannot read but is allowed by the operation mode to access the metadata
        T result = SecuredObjects.secure(info, policy);
        return result;
    }

    /**
     * Given a {@link StyleInfo} and a user, returns it back if the user can access it.
     *
     * @return <code>null</code> if the user can't acess the style, otherwise the original style.
     */
    protected StyleInfo checkAccess(Authentication user, StyleInfo style, MixedModeBehavior mixedModeBehavior) {
        if (style == null) return null;

        WrapperPolicy policy = buildWrapperPolicy(user, style, style.getName(), mixedModeBehavior);

        // if we don't need to hide it, then we can return it as is since it
        // can only provide metadata.
        if (policy.level == AccessLevel.HIDDEN) return null;
        else return style;
    }

    /** Given a store and a user, returns it back if the user can access its workspace in read mode, null otherwise */
    protected <T extends StoreInfo> T checkAccess(Authentication user, T store, MixedModeBehavior mixedModeBehavior) {
        if (store == null) return null;

        WrapperPolicy policy = buildWrapperPolicy(user, store.getWorkspace(), store.getName(), mixedModeBehavior);

        // handle the modes that do not require wrapping
        if (policy.level == AccessLevel.HIDDEN) return null;
        else if (policy.level == AccessLevel.READ_WRITE
                || (policy.level == AccessLevel.READ_ONLY && store instanceof CoverageStoreInfo)) return store;

        // otherwise we are in a mixed case where the user can read but not
        // write, or
        // cannot read but is allowed by the operation mode to access the
        // metadata
        if (store instanceof DataStoreInfo
                || store instanceof CoverageStoreInfo
                || store instanceof WMSStoreInfo
                || store instanceof WMTSStoreInfo) {
            T result = SecuredObjects.secure(store, policy);
            return result;
        } else {
            throw new RuntimeException("Unknown store type " + store.getClass());
        }
    }

    /** Given a layer and a user, returns it back if the user can access it, null otherwise */
    protected LayerInfo checkAccess(
            Authentication user,
            LayerInfo layer,
            MixedModeBehavior mixedModeBehavior,
            List<LayerGroupInfo> containers) {
        if (layer == null) return null;

        // first off, handle the case where the user cannot even read the data
        WrapperPolicy policy = buildWrapperPolicy(user, layer, layer.getName(), mixedModeBehavior, containers);

        // handle the modes that do not require wrapping
        if (policy.level == AccessLevel.HIDDEN) return null;
        else if (policy.level == AccessLevel.READ_WRITE && policy.getLimits() == null) return layer;

        // otherwise we are in a mixed case where the user can read but not write, or
        // cannot read but is allowed by the operation mode to access the metadata
        return SecuredObjects.secure(layer, policy);
    }

    /** Given a layer group and a user, returns it back if the user can access it, null otherwise */
    protected LayerGroupInfo checkAccess(
            Authentication user,
            LayerGroupInfo group,
            MixedModeBehavior mixedModeBehavior,
            List<LayerGroupInfo> containers) {
        if (group == null) return null;

        // first check the layer group itself
        WrapperPolicy policy = buildWrapperPolicy(user, group, group.getName(), mixedModeBehavior, containers);
        if (policy.level == AccessLevel.HIDDEN) {
            return null;
        }

        LayerInfo rootLayer = group.getRootLayer();
        if (LayerGroupInfo.Mode.EO.equals(group.getMode())) {
            // if the root cannot be used, blow up with an error in mixed mode
            rootLayer = checkAccess(user, rootLayer, MixedModeBehavior.CHALLENGE);
            if (rootLayer == null) {
                return null;
            }
        }

        List<LayerGroupInfo> extendedContainers = new ArrayList<>(containers);
        extendedContainers.add(group);

        final List<PublishedInfo> layers = group.getLayers();
        final List<StyleInfo> styles = group.getStyles();
        final List<StyleInfo> selectedStyles = new ArrayList<>(layers.size());
        ArrayList<PublishedInfo> wrapped = new ArrayList<>(layers.size());
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo layer = layers.get(i);
            StyleInfo style = (styles != null && styles.size() > i) ? styles.get(i) : null;
            // for nested layers, hide in mixed mode, the inner layers were not explicitly requested
            PublishedInfo checked = checkAccess(user, layer, MixedModeBehavior.HIDE, extendedContainers);
            if (checked != null) {
                wrapped.add(checked);
                selectedStyles.add(style);
            } else if (layer == null) {
                StyleInfo styleGroup = checkAccess(user, style, MixedModeBehavior.HIDE);
                if (styleGroup != null) {
                    wrapped.add(null);
                    selectedStyles.add(styleGroup);
                }
            }
        }

        // always wrap layergroups (secured layers could be added later)
        return new SecuredLayerGroupInfo(group, rootLayer, wrapped, selectedStyles);
    }

    /** Given a namespace and user, returns it back if the user can access it, null otherwise */
    protected <T extends NamespaceInfo> T checkAccess(Authentication user, T ns, MixedModeBehavior mixedModeBehavior) {
        if (ns == null) return null;

        // route the security check thru the associated workspace info
        WorkspaceInfo ws = delegate.getWorkspaceByName(ns.getPrefix());
        if (ws == null) {
            // temporary workaround, build a fake workspace, as we're probably
            // in between a change of workspace/namespace name
            ws = delegate.getFactory().createWorkspace();
            ws.setName(ns.getPrefix());
        }
        WorkspaceInfo info = checkAccess(user, ws, mixedModeBehavior);
        if (info == null) return null;
        else return ns;
    }

    /** Given a workspace and user, returns it back if the user can access it, null otherwise */
    protected <T extends WorkspaceInfo> T checkAccess(Authentication user, T ws, MixedModeBehavior mixedModeBehavior) {
        if (ws == null) return null;

        WrapperPolicy policy = buildWrapperPolicy(user, ws, ws.getName(), mixedModeBehavior);

        // if we don't need to hide it, then we can return it as is since it
        // can only provide metadata.
        if (policy.level == AccessLevel.HIDDEN) return null;
        else return ws;
    }

    /**
     * Check how an access manager responds to a user accessing a catalog object and return the result.
     *
     * @param accessManager the access manager to ask
     * @param user the user accessing the object
     * @param info the catalog object being accessed
     * @return the combination of access level and response policy to apply to the request
     */
    WrapperPolicy buildWrapperPolicy(
            @Nonnull ResourceAccessManager accessManager,
            Authentication user,
            @Nonnull CatalogInfo info,
            MixedModeBehavior mixedModeBehavior) {
        Assert.notNull(info, "CatalogInfo must not be null");

        if (info instanceof NamespaceInfo) {
            // route the security check thru the associated workspace info
            WorkspaceInfo ws = delegate.getWorkspaceByName(((NamespaceInfo) info).getPrefix());
            if (ws == null) {
                // temporary workaround, build a fake workspace, as we're
                // probably
                // in between a change of workspace/namespace name
                ws = delegate.getFactory().createWorkspace();
                ws.setName(((NamespaceInfo) info).getPrefix());
            }
            return buildWrapperPolicy(accessManager, user, ws, ws.getName(), mixedModeBehavior);
        }

        if (info instanceof WorkspaceInfo) {
            return buildWrapperPolicy(accessManager, user, info, ((WorkspaceInfo) info).getName(), mixedModeBehavior);
        }

        if (info instanceof StoreInfo) {
            return buildWrapperPolicy(
                    accessManager,
                    user,
                    ((StoreInfo) info).getWorkspace(),
                    ((StoreInfo) info).getName(),
                    mixedModeBehavior);
        }

        if (info instanceof ResourceInfo) {
            return buildWrapperPolicy(accessManager, user, info, ((ResourceInfo) info).getName(), mixedModeBehavior);
        }

        if (info instanceof LayerInfo) {
            return buildWrapperPolicy(accessManager, user, info, ((LayerInfo) info).getName(), mixedModeBehavior);
        }

        if (info instanceof LayerGroupInfo) {
            // return the most restrictive policy that's not HIDDEN, or the
            // first HIDDEN one
            WrapperPolicy mostRestrictive = WrapperPolicy.readWrite(null);

            for (PublishedInfo layer : ((LayerGroupInfo) info).getLayers()) {
                WrapperPolicy policy =
                        buildWrapperPolicy(accessManager, user, layer, layer.getName(), mixedModeBehavior);
                if (AccessLevel.HIDDEN.equals(policy.getAccessLevel())) {
                    return policy;
                }
                int comparison = policy.compareTo(mostRestrictive);
                boolean thisOneIsMoreRestrictive = comparison < 0;
                if (thisOneIsMoreRestrictive) {
                    mostRestrictive = policy;
                }
            }

            return mostRestrictive;
        } else if (info instanceof StyleInfo) {
            return buildWrapperPolicy(accessManager, user, info, ((StyleInfo) info).getName(), mixedModeBehavior);
        }

        throw new IllegalArgumentException("Can't build wrapper policy for objects of type "
                + info.getClass().getName());
    }

    protected WrapperPolicy buildWrapperPolicy(
            Authentication user, @Nonnull CatalogInfo info, MixedModeBehavior mixedModeBehavior) {
        return buildWrapperPolicy(accessManager, user, info, mixedModeBehavior);
    }

    /**
     * Factors out the policy that decides what access level the current user has to a specific resource considering the
     * read/write access, the security mode, and the filtering status
     */
    public WrapperPolicy buildWrapperPolicy(
            Authentication user, CatalogInfo info, String resourceName, MixedModeBehavior mixedModeBehavior) {
        return SecureCatalogImpl.buildWrapperPolicy(
                accessManager, user, info, resourceName, mixedModeBehavior, Collections.emptyList());
    }

    /**
     * Factors out the policy that decides what access level the current user has to a specific resource considering the
     * read/write access, the security mode, and the filtering status
     */
    public WrapperPolicy buildWrapperPolicy(
            Authentication user,
            CatalogInfo info,
            String resourceName,
            MixedModeBehavior mixedModeBehavior,
            List<LayerGroupInfo> containers) {
        return SecureCatalogImpl.buildWrapperPolicy(
                accessManager, user, info, resourceName, mixedModeBehavior, containers);
    }

    static WrapperPolicy buildWrapperPolicy(
            ResourceAccessManager accessManager,
            Authentication user,
            CatalogInfo info,
            String resourceName,
            MixedModeBehavior mixedModeBehavior) {
        return buildWrapperPolicy(accessManager, user, info, resourceName, mixedModeBehavior, Collections.emptyList());
    }

    static WrapperPolicy buildWrapperPolicy(
            ResourceAccessManager accessManager,
            Authentication user,
            CatalogInfo info,
            String resourceName,
            MixedModeBehavior mixedModeBehavior,
            List<LayerGroupInfo> containers) {
        boolean canRead = true;
        boolean canWrite = true;

        AccessLimits limits;

        if (info instanceof WorkspaceInfo) {
            // unsure here... shall we disallow writing? Only catalog and config
            // related code should be playing with stores directly, so it's more of a
            // matter if you can admin a workspace or not
            limits = accessManager.getAccessLimits(user, (WorkspaceInfo) info);
            WorkspaceAccessLimits wl = (WorkspaceAccessLimits) limits;
            if (wl != null) {
                if (wl.isAdminable()) {
                    canRead = canWrite = true;
                } else {
                    canRead = wl.isReadable();
                    canWrite = wl.isWritable();
                }
            }
            if (AdminRequest.get() != null) {
                // admin request
                if (wl == null || !wl.isAdminable()) {
                    canRead = canWrite = false;
                }
            }
        } else if (info instanceof LayerInfo || info instanceof ResourceInfo) {
            DataAccessLimits dl;
            WorkspaceAccessLimits wl;

            if (info instanceof LayerInfo) {
                dl = accessManager.getAccessLimits(user, (LayerInfo) info, containers);
                wl = accessManager.getAccessLimits(
                        user, ((LayerInfo) info).getResource().getStore().getWorkspace());
            } else {
                dl = accessManager.getAccessLimits(user, (ResourceInfo) info);
                wl = accessManager.getAccessLimits(
                        user, ((ResourceInfo) info).getStore().getWorkspace());
            }
            if (dl != null) {
                canRead = dl.getReadFilter() != Filter.EXCLUDE;
                if (dl instanceof VectorAccessLimits) {
                    canWrite = ((VectorAccessLimits) dl).getWriteFilter() != Filter.EXCLUDE;
                } else {
                    canWrite = false;
                }
            }
            limits = dl;

            if (AdminRequest.get() != null) {
                if (wl != null && !wl.isAdminable()) {
                    canRead = false;
                }
            }
        } else if (info instanceof StyleInfo || info instanceof LayerGroupInfo) {
            WorkspaceInfo ws = null;
            if (info instanceof StyleInfo) {
                limits = accessManager.getAccessLimits(user, (StyleInfo) info);
                ws = ((StyleInfo) info).getWorkspace();
            } else {
                limits = accessManager.getAccessLimits(user, (LayerGroupInfo) info, containers);
                ws = ((LayerGroupInfo) info).getWorkspace();
            }

            if (limits != null) {
                canRead = false;
            }

            if (ws != null && AdminRequest.get() != null) {
                WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, ws);
                if (wl != null) {
                    if (!wl.isAdminable()) {
                        canRead = false;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Can't build the wrapper policy for objects " + "other than workspace, layer or resource: " + info);
        }

        final CatalogMode mode = limits != null ? limits.getMode() : CatalogMode.HIDE;
        if (!canRead) {
            // if in hide mode, we just hide the resource
            if (mode == CatalogMode.HIDE) {
                return WrapperPolicy.hide(limits);
            } else if (mode == CatalogMode.MIXED) {
                // if request is a get capabilities and mixed, we hide again
                if (mixedModeBehavior == MixedModeBehavior.HIDE) {
                    return WrapperPolicy.hide(limits);
                } else {
                    // otherwise challenge the user for credentials
                    throw unauthorizedAccess(resourceName);
                }
            } else {
                // for challenge mode we agree to show freely only the metadata, every
                // other access will trigger a security exception
                return WrapperPolicy.metadata(limits);
            }
        } else if (!canWrite) {
            if (mode == CatalogMode.HIDE) {
                return WrapperPolicy.readOnlyHide(limits);
            } else {
                return WrapperPolicy.readOnlyChallenge(limits);
            }
        }

        return WrapperPolicy.readWrite(limits);
    }

    public static RuntimeException unauthorizedAccess(String resourceName) {
        // not hide, and not filtering out a list, this
        // is an unauthorized direct resource access, complain
        Authentication user = user();
        if (user == null || user.getAuthorities().isEmpty())
            return new InsufficientAuthenticationException("Cannot access " + resourceName + " as anonymous");
        else return new AccessDeniedException("Cannot access " + resourceName + " with the current privileges");
    }

    public static RuntimeException unauthorizedAccess() {
        // not hide, and not filtering out a list, this
        // is an unauthorized direct resource access, complain
        Authentication user = user();
        if (user == null || user.getAuthorities().isEmpty())
            return new InsufficientAuthenticationException("Operation unallowed with the current privileges");
        else return new AccessDeniedException("Operation unallowed with the current privileges");
    }

    /** Given a list of resources, returns a copy of it containing only the resources the user can access */
    protected <T extends ResourceInfo> List<T> filterResources(Authentication user, List<T> resources) {
        List<T> result = new ArrayList<>();
        for (T original : resources) {
            T secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of stores, returns a copy of it containing only the resources the user can access */
    protected <T extends StoreInfo> List<T> filterStores(Authentication user, List<T> resources) {
        List<T> result = new ArrayList<>();
        for (T original : resources) {
            T secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of layer groups, returns a copy of it containing only the groups the user can access */
    protected List<LayerGroupInfo> filterGroups(Authentication user, List<LayerGroupInfo> groups) {
        List<LayerGroupInfo> result = new ArrayList<>();
        for (LayerGroupInfo original : groups) {
            LayerGroupInfo secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of layers, returns a copy of it containing only the layers the user can access */
    protected List<LayerInfo> filterLayers(Authentication user, List<LayerInfo> layers) {
        List<LayerInfo> result = new ArrayList<>();
        for (LayerInfo original : layers) {
            LayerInfo secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of styles, returns a copy of it containing only the styles the user can access. */
    protected List<StyleInfo> filterStyles(Authentication user, List<StyleInfo> styles) {
        List<StyleInfo> result = new ArrayList<>();
        for (StyleInfo original : styles) {
            StyleInfo secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of namespaces, returns a copy of it containing only the namespaces the user can access */
    protected <T extends NamespaceInfo> List<T> filterNamespaces(Authentication user, List<T> namespaces) {
        List<T> result = new ArrayList<>();
        for (T original : namespaces) {
            T secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    /** Given a list of workspaces, returns a copy of it containing only the workspaces the user can access */
    protected <T extends WorkspaceInfo> List<T> filterWorkspaces(Authentication user, List<T> workspaces) {
        List<T> result = new ArrayList<>();
        for (T original : workspaces) {
            T secured = checkAccess(user, original, MixedModeBehavior.HIDE);
            if (secured != null) result.add(secured);
        }
        return result;
    }

    // -------------------------------------------------------------------
    // Unwrappers, used to make sure the lower level does not get hit by
    // read only wrappers
    // -------------------------------------------------------------------
    static LayerGroupInfo unwrap(LayerGroupInfo layerGroup) {
        if (layerGroup instanceof SecuredLayerGroupInfo)
            return ((SecuredLayerGroupInfo) layerGroup).unwrap(LayerGroupInfo.class);
        return layerGroup;
    }

    static LayerInfo unwrap(LayerInfo layer) {
        if (layer instanceof SecuredLayerInfo) return ((SecuredLayerInfo) layer).unwrap(LayerInfo.class);
        return layer;
    }

    static ResourceInfo unwrap(ResourceInfo info) {
        if (info instanceof SecuredFeatureTypeInfo) return ((SecuredFeatureTypeInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredCoverageInfo) return ((SecuredCoverageInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMSLayerInfo) return ((SecuredWMSLayerInfo) info).unwrap(ResourceInfo.class);
        if (info instanceof SecuredWMTSLayerInfo) return ((SecuredWMTSLayerInfo) info).unwrap(ResourceInfo.class);
        return info;
    }

    static StoreInfo unwrap(StoreInfo info) {
        if (info instanceof SecuredDataStoreInfo) return ((SecuredDataStoreInfo) info).unwrap(StoreInfo.class);
        if (info instanceof SecuredCoverageStoreInfo) return ((SecuredCoverageStoreInfo) info).unwrap(StoreInfo.class);
        if (info instanceof SecuredWMSStoreInfo) return ((SecuredWMSStoreInfo) info).unwrap(StoreInfo.class);
        if (info instanceof SecuredWMTSStoreInfo) return ((SecuredWMTSStoreInfo) info).unwrap(StoreInfo.class);
        return info;
    }

    public static Object unwrap(Object obj) {
        if (obj instanceof LayerGroupInfo) {
            return unwrap((LayerGroupInfo) obj);
        }
        if (obj instanceof LayerInfo) {
            return unwrap((LayerInfo) obj);
        }
        if (obj instanceof ResourceInfo) {
            return unwrap((ResourceInfo) obj);
        }
        if (obj instanceof StoreInfo) {
            return unwrap((StoreInfo) obj);
        }
        if (obj instanceof SecureCatalogImpl) {
            return ((SecureCatalogImpl) obj).delegate;
        }

        return obj;
    }
    // -------------------------------------------------------------------
    // PURE DELEGATING METHODS
    // (MapInfo being here since its role in the grand scheme of things
    // is still undefined)
    // -------------------------------------------------------------------

    @Override
    public MapInfo getMap(String id) {
        return delegate.getMap(id);
    }

    @Override
    public MapInfo getMapByName(String name) {
        return delegate.getMapByName(name);
    }

    @Override
    public List<MapInfo> getMaps() {
        return delegate.getMaps();
    }

    @Override
    public void add(LayerGroupInfo layerGroup) {
        delegate.add(unwrap(layerGroup));
    }

    @Override
    public ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew) {
        return delegate.validate(unwrap(layerGroup), isNew);
    }

    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return delegate.detach(layerGroup);
    }

    @Override
    public void add(LayerInfo layer) {
        delegate.add(unwrap(layer));
    }

    @Override
    public ValidationResult validate(LayerInfo layer, boolean isNew) {
        return delegate.validate(unwrap(layer), isNew);
    }

    @Override
    public LayerInfo detach(LayerInfo layer) {
        return delegate.detach(layer);
    }

    @Override
    public void add(MapInfo map) {
        delegate.add(map);
    }

    @Override
    public MapInfo detach(MapInfo map) {
        return delegate.detach(map);
    }

    @Override
    public void add(NamespaceInfo namespace) {
        delegate.add(namespace);
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
    public void add(ResourceInfo resource) {
        delegate.add(unwrap(resource));
    }

    @Override
    public ValidationResult validate(ResourceInfo resource, boolean isNew) {
        return delegate.validate(unwrap(resource), isNew);
    }

    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return delegate.detach(resource);
    }

    @Override
    public void add(StoreInfo store) {
        delegate.add(unwrap(store));
    }

    @Override
    public ValidationResult validate(StoreInfo store, boolean isNew) {
        return delegate.validate(unwrap(store), isNew);
    }

    @Override
    public <T extends StoreInfo> T detach(T store) {
        return delegate.detach(store);
    }

    @Override
    public void add(StyleInfo style) {
        delegate.add(style);
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
    public void add(WorkspaceInfo workspace) {
        delegate.add(workspace);
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
    public void addListener(CatalogListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public CatalogFacade getFacade() {
        return new SecureCatalogFacade(this, delegate.getFacade());
    }

    @Override
    public CatalogFactory getFactory() {
        return new DecoratingCatalogFactory(delegate.getFactory()) {

            @Override
            public LayerGroupInfo createLayerGroup() {
                // always wrap layergroups (secured layers could be added later)
                return new SecuredLayerGroupInfo(
                        delegate.createLayerGroup(), null, new ArrayList<>(), new ArrayList<>());
            }
        };
    }

    @Override
    public Collection<CatalogListener> getListeners() {
        return delegate.getListeners();
    }

    @Override
    public void fireAdded(CatalogInfo object) {
        delegate.fireAdded(object);
    }

    @Override
    public void fireModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        delegate.fireModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void firePostModified(
            CatalogInfo object, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
        delegate.firePostModified(object, propertyNames, oldValues, newValues);
    }

    @Override
    public void fireRemoved(CatalogInfo object) {
        delegate.fireRemoved(object);
    }

    // TODO: why is resource pool being exposed???
    @Override
    public ResourcePool getResourcePool() {
        return delegate.getResourcePool();
    }

    @Override
    public StyleInfo getStyle(String id) {
        return delegate.getStyle(id);
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return checkAccess(user(), delegate.getStyleByName(name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public StyleInfo getStyleByName(String workspaceName, String name) {
        return checkAccess(user(), delegate.getStyleByName(workspaceName, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return checkAccess(user(), delegate.getStyleByName(workspace, name), MixedModeBehavior.CHALLENGE);
    }

    @Override
    public List<StyleInfo> getStyles() {
        return getAll(StyleInfo.class);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(String workspaceName) {
        return filterStyles(user(), delegate.getStylesByWorkspace(workspaceName));
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return filterStyles(user(), delegate.getStylesByWorkspace(workspace));
    }

    @Override
    public void remove(LayerGroupInfo layerGroup) {
        delegate.remove(unwrap(layerGroup));
    }

    @Override
    public void remove(LayerInfo layer) {
        delegate.remove(unwrap(layer));
    }

    @Override
    public void remove(MapInfo map) {
        delegate.remove(map);
    }

    @Override
    public void remove(NamespaceInfo namespace) {
        delegate.remove(namespace);
    }

    @Override
    public void remove(ResourceInfo resource) {
        delegate.remove(unwrap(resource));
    }

    @Override
    public void remove(StoreInfo store) {
        delegate.remove(unwrap(store));
    }

    @Override
    public void remove(StyleInfo style) {
        delegate.remove(style);
    }

    @Override
    public void remove(WorkspaceInfo workspace) {
        delegate.remove(workspace);
    }

    @Override
    public void removeListener(CatalogListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void save(LayerGroupInfo layerGroup) {
        delegate.save(unwrap(layerGroup));
    }

    @Override
    public void save(LayerInfo layer) {
        delegate.save(unwrap(layer));
    }

    @Override
    public void save(MapInfo map) {
        delegate.save(map);
    }

    @Override
    public void save(NamespaceInfo namespace) {
        delegate.save(namespace);
    }

    @Override
    public void save(ResourceInfo resource) {
        delegate.save(unwrap(resource));
    }

    @Override
    public void save(StoreInfo store) {
        delegate.save(unwrap(store));
    }

    @Override
    public void save(StyleInfo style) {
        delegate.save(style);
    }

    @Override
    public void save(WorkspaceInfo workspace) {
        delegate.save(workspace);
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        delegate.setDefaultNamespace(defaultNamespace);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        delegate.setDefaultWorkspace(workspace);
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

    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return checkAccess(user(), delegate.getDefaultDataStore(workspace), MixedModeBehavior.CHALLENGE);
    }

    @Override
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
    public <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException {

        Filter securityFilter = securityFilter(type, filter);
        T result = delegate.get(type, securityFilter);
        return result;
    }

    /** Delegates to {@link #getAll(Class, Filter) getAll(of, Filter.INCLUDE)} */
    protected <T extends CatalogInfo> List<T> getAll(Class<T> of) {
        return getAll(of, acceptAll());
    }

    /**
     * Builds up and returns a list of objects matching the {@link #securityFilter(Class, Filter) securityFilter(of,
     * filter)}, giving the {@link ResourceAccessManager} a chance to optimize the security filter (e.g. encode to a
     * native catalog back-end filter), also for bulk methods returning {@code List<T>}
     *
     * @see #list(Class, Filter, Integer, Integer, SortBy)
     */
    protected <T extends CatalogInfo> List<T> getAll(Class<T> of, Filter filter) {
        try (CloseableIterator<T> it = list(of, filter)) {
            return Lists.newArrayList(it);
        }
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter) {
        return list(of, filter, null, null, (SortBy) null);
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
        Filter securityFilter = securityFilter(of, filter);

        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        CloseableIterator<T> filtered = delegate.list(of, securityFilter, offset, count, sortBy);

        // create secured decorators on-demand. Assume this method is used only for listing, not
        // for accessing a single resource by name/id, thus use hide policy for mixed mode
        final Function<T, T> securityWrapper = securityWrapper(of, MixedModeBehavior.HIDE);
        final CloseableIterator<T> filteredWrapped = CloseableIteratorAdapter.transform(filtered, securityWrapper);

        // wrap the iterator in a notNull filter to ensure any filtered
        // layers (result is null) don't get passed on from the securityWrapper
        // Function. When the AccessLevel is HIDDEN and a layer gets filtered
        // out via a CatalogFilter - for example, this can happen with a
        // LocalWorkspaceCatalogFilter and a virtual service request
        return CloseableIteratorAdapter.filter(filteredWrapped, com.google.common.base.Predicates.notNull());
    }

    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of, Filter filter, Integer offset, Integer count, SortBy... sortBy) {
        Filter securityFilter = securityFilter(of, filter);

        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        // HACK here, go straigth to the facade of the delegate to get a method supporting sortby[]
        CloseableIterator<T> filtered = delegate.getFacade().list(of, securityFilter, offset, count, sortBy);

        // create secured decorators on-demand. Assume this method is used only for listing, not
        // for accessing a single resource by name/id, thus use hide policy for mixed mode
        final Function<T, T> securityWrapper = securityWrapper(of, MixedModeBehavior.HIDE);
        final CloseableIterator<T> filteredWrapped = CloseableIteratorAdapter.transform(filtered, securityWrapper);

        // wrap the iterator in a notNull filter to ensure any filtered
        // layers (result is null) don't get passed on from the securityWrapper
        // Function. When the AccessLevel is HIDDEN and a layer gets filtered
        // out via a CatalogFilter - for example, this can happen with a
        // LocalWorkspaceCatalogFilter and a virtual service request
        return CloseableIteratorAdapter.filter(filteredWrapped, com.google.common.base.Predicates.notNull());
    }

    /**
     * @return a Function that applies a security wrapper over the catalog object given to it as input
     * @see #checkAccess(Authentication, CatalogInfo)
     */
    private <T extends CatalogInfo> Function<T, T> securityWrapper(
            final Class<T> forClass, MixedModeBehavior mixedModeBehavior) {

        final Authentication user = user();
        return input -> {
            T checked = checkAccess(user, input, mixedModeBehavior);
            return checked;
        };
    }

    /**
     * Returns a predicate that checks whether the current user has access to a given object of type {@code infoType}.
     *
     * <p>IMPLEMENTATION NOTE: the predicate returned evaluates in-process and hence can't be encoded to the catalog's
     * native query language, if any. It calls {@link #buildWrapperPolicy(Authentication, CatalogInfo)} to check if the
     * returned access level is not "hidden" on a case by case basis. Perhaps, the check for whether a given resource is
     * accessible to the current user can be encoded as a "well known" predicate that uses one or a combination of the
     * property equals/isnull/contains/exists verbs in the {@link Predicates} utility. I (GR), at the time of writing,
     * don't know how to do that, so any help would be much appreciated. Nonetheless, this predicate is meant to be
     * "and'ed" with any other predicate this catalog wrapper is called with, giving the Catalog backend a chance to at
     * least encode the "well known" part of the resulting filter, and separate out the in-process evaluation of access
     * credentials from the construction of the security wrapper for each object.
     *
     * @return a catalog Predicate that evaluates if an object of the required type is accessible to the given user
     */
    private <T extends CatalogInfo> Filter securityFilter(final Class<T> infoType, final Filter filter) {

        final Authentication user = user();
        if (isAdmin(user)) {
            // no need to check for credentials if user is _the_ administrator
            return filter;
        }

        if (MapInfo.class.isAssignableFrom(infoType)) {
            // these kind of objects are not secured
            return filter;
        }

        Filter securityFilter = this.accessManager.getSecurityFilter(user, infoType);
        if (Filter.INCLUDE.equals(filter)) {
            return securityFilter;
        }
        // create a filter combined with the security credentials check
        return Predicates.and(filter, securityFilter);
    }

    /** Checks if the current user is authenticated and is the administrator. Protected to allow overriding in tests. */
    protected boolean isAdmin(Authentication authentication) {

        return GeoServerExtensions.bean(GeoServerSecurityManager.class).checkAuthenticationForAdminRole(authentication);
    }

    @Override
    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        delegate.removeListeners(listenerClass);
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        return delegate.getCatalogCapabilities();
    }

    public boolean isDefaultAccessManager() {
        ResourceAccessManager manager = this.accessManager;
        while (ResourceAccessManagerWrapper.class.isAssignableFrom(manager.getClass())) {
            manager = ((ResourceAccessManagerWrapper) manager).unwrap();
        }
        return manager instanceof DefaultResourceAccessManager;
    }
}
