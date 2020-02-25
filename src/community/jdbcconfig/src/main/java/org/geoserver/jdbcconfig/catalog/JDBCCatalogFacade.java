/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.catalog;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isNull;

import com.google.common.base.Preconditions;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LockingCatalogFacade;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.util.Assert;

/** @author groldan */
@ParametersAreNonnullByDefault
public class JDBCCatalogFacade implements CatalogFacade {

    public static final Logger LOGGER = Logging.getLogger(JDBCCatalogFacade.class);

    private final ConfigDatabase db;

    public JDBCCatalogFacade(final ConfigDatabase db) {
        this.db = db;
    }

    public ConfigDatabase getConfigDatabase() {
        return db;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getCatalog() */
    @Override
    public Catalog getCatalog() {
        return this.db.getCatalog();
    }

    /** @see org.geoserver.catalog.CatalogFacade#setCatalog(org.geoserver.catalog.Catalog) */
    @Override
    public void setCatalog(final Catalog catalog) {
        Preconditions.checkArgument(catalog instanceof CatalogImpl);
        this.db.setCatalog((CatalogImpl) catalog);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.StoreInfo) */
    @Override
    public StoreInfo add(StoreInfo store) {
        return addInternal(store);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.StoreInfo) */
    @Override
    public void remove(StoreInfo store) {
        db.remove(store);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.StoreInfo) */
    @Override
    public void save(StoreInfo store) {
        saveInternal(store, StoreInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.StoreInfo) */
    @Override
    public <T extends StoreInfo> T detach(T store) {
        return store;
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.ResourceInfo) */
    @Override
    public <T extends ResourceInfo> T detach(T resource) {
        return resource;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getStore(java.lang.String, java.lang.Class) */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return db.getById(id, clazz);
    }

    /**
     * @see org.geoserver.catalog.CatalogFacade#getStoreByName(org.geoserver.catalog.WorkspaceInfo,
     *     java.lang.String, java.lang.Class)
     */
    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        if (workspace == null || workspace == ANY_WORKSPACE) {
            return db.getByIdentity(clazz, "name", name);
        } else {
            return db.getByIdentity(clazz, "workspace.id", workspace.getId(), "name", name);
        }
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#getStoresByWorkspace(org.geoserver.catalog.WorkspaceInfo,
     *     java.lang.Class)
     */
    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        Filter filter = acceptAll();
        if (null != workspace && ANY_WORKSPACE != workspace) {
            filter = equal("workspace.id", workspace.getId());
        }

        List<T> list = db.queryAsList(clazz, filter, null, null, null);

        return list;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getStores(java.lang.Class) */
    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return db.getAll(clazz);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#getDefaultDataStore(org.geoserver.catalog.WorkspaceInfo)
     */
    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        final String target = WorkspaceInfo.class.getSimpleName() + "." + workspace.getId();
        return db.getDefault(target, DataStoreInfo.class);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#setDefaultDataStore(org.geoserver.catalog.WorkspaceInfo,
     *     org.geoserver.catalog.DataStoreInfo)
     */
    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, @Nullable DataStoreInfo store) {

        String target = "WorkspaceInfo." + workspace.getId();
        String id = store == null ? null : store.getId();

        DataStoreInfo old = getDefaultDataStore(workspace);

        // fire modify event before change
        if (!Utilities.equals(old, workspace)) {
            Catalog catalog = getCatalog();
            catalog.fireModified(
                    catalog,
                    Arrays.asList("defaultDataStore"),
                    Arrays.asList(old),
                    Arrays.asList(store));
        }
        db.setDefault(target, id);

        // fire postmodify event after change
        if (!Utilities.equals(old, workspace)) {
            Catalog catalog = getCatalog();
            catalog.firePostModified(
                    catalog,
                    Arrays.asList("defaultDataStore"),
                    Arrays.asList(old),
                    Arrays.asList(store));
        }
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.ResourceInfo) */
    @Override
    public ResourceInfo add(ResourceInfo resource) {
        return addInternal(resource);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.ResourceInfo) */
    @Override
    public void remove(ResourceInfo resource) {
        db.remove(resource);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.ResourceInfo) */
    @Override
    public void save(ResourceInfo resource) {
        saveInternal(resource, ResourceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getResource(java.lang.String, java.lang.Class) */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return db.getById(id, clazz);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#getResourceByName(org.geoserver.catalog.NamespaceInfo,
     *     java.lang.String, java.lang.Class)
     */
    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        if (namespace == null || namespace == ANY_NAMESPACE) {
            return db.getByIdentity(clazz, "name", name);
        } else {
            return db.getByIdentity(clazz, "namespace.id", namespace.getId(), "name", name);
        }
    }

    /** @see org.geoserver.catalog.CatalogFacade#getResources(java.lang.Class) */
    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return db.getAll(clazz);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#getResourcesByNamespace(org.geoserver.catalog.NamespaceInfo,
     *     java.lang.Class)
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {

        Filter filter = acceptAll();
        if (null != namespace && ANY_NAMESPACE != namespace) {
            filter = equal("namespace.id", namespace.getId());
        }

        return db.queryAsList(clazz, filter, null, null, null);
    }

    /**
     * @see org.geoserver.catalog.CatalogFacade#getResourceByStore(org.geoserver.catalog.StoreInfo,
     *     java.lang.String, java.lang.Class)
     */
    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {

        Filter filter = equal("name", name);
        Filter storeFilter = equal("store.id", store.getId());
        filter = and(filter, storeFilter);

        T res;
        try {
            res = findUnique(clazz, filter);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return res;
    }

    /**
     * @see org.geoserver.catalog.CatalogFacade#getResourcesByStore(org.geoserver.catalog.StoreInfo,
     *     java.lang.Class)
     */
    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {

        Filter filter = equal("store.id", store.getId());

        return db.queryAsList(clazz, filter, null, null, null);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.LayerInfo) */
    @Override
    public LayerInfo add(LayerInfo layer) {
        return addInternal(layer);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.LayerInfo) */
    @Override
    public void remove(LayerInfo layer) {
        db.remove(layer);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.LayerInfo) */
    @Override
    public void save(LayerInfo layer) {
        saveInternal(layer, LayerInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.LayerInfo) */
    @Override
    public LayerInfo detach(LayerInfo layer) {
        return layer;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayer(java.lang.String) */
    @Override
    public LayerInfo getLayer(String id) {
        return db.getById(id, LayerInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayerByName(java.lang.String) */
    /** @see org.geoserver.catalog.CatalogFacade#getLayerByName(java.lang.String) */
    @Override
    public LayerInfo getLayerByName(String name) {
        String resourceId = db.getIdByIdentity(ResourceInfo.class, "name", name);
        if (resourceId == null) {
            return null;
        } else {
            return db.getByIdentity(LayerInfo.class, "resource.id", resourceId);
        }
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayers(org.geoserver.catalog.ResourceInfo) */
    /** @see org.geoserver.catalog.CatalogFacade#getLayers(org.geoserver.catalog.ResourceInfo) */
    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {

        Filter filter = equal("resource.id", resource.getId());

        return db.queryAsList(LayerInfo.class, filter, null, null, null);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayers(org.geoserver.catalog.StyleInfo) */
    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {

        Filter filter = equal("defaultStyle.id", style.getId());
        Filter anyStyle = equal("styles[].id", style.getId());

        filter = and(filter, anyStyle);

        return db.queryAsList(LayerInfo.class, filter, null, null, null);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayers() */
    @Override
    public List<LayerInfo> getLayers() {
        return db.getAll(LayerInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.MapInfo) */
    @Override
    public MapInfo add(MapInfo map) {
        return addInternal(map);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.MapInfo) */
    @Override
    public void remove(MapInfo map) {
        db.remove(map);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.MapInfo) */
    @Override
    public void save(MapInfo map) {
        saveInternal(map, MapInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.MapInfo) */
    @Override
    public MapInfo detach(MapInfo map) {
        return map;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getMap(java.lang.String) */
    @Override
    public MapInfo getMap(String id) {
        return db.getById(id, MapInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getMapByName(java.lang.String) */
    @Override
    public MapInfo getMapByName(String name) {
        return getByName(name, MapInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getMaps() */
    @Override
    public List<MapInfo> getMaps() {
        return db.getAll(MapInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.LayerGroupInfo) */
    @Override
    public LayerGroupInfo add(LayerGroupInfo layerGroup) {
        return addInternal(layerGroup);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.LayerGroupInfo) */
    @Override
    public void remove(LayerGroupInfo layerGroup) {
        db.remove(layerGroup);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.LayerGroupInfo) */
    @Override
    public void save(LayerGroupInfo layerGroup) {
        saveInternal(layerGroup, LayerGroupInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.LayerGroupInfo) */
    @Override
    public LayerGroupInfo detach(LayerGroupInfo layerGroup) {
        return layerGroup;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayerGroup(java.lang.String) */
    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return db.getById(id, LayerGroupInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayerGroupByName(java.lang.String) */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return getByName(name, LayerGroupInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getLayerGroups() */
    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return db.getAll(LayerGroupInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.NamespaceInfo) */
    @Override
    public NamespaceInfo add(NamespaceInfo namespace) {
        return addInternal(namespace);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.NamespaceInfo) */
    @Override
    public void remove(NamespaceInfo namespace) {
        db.remove(namespace);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.NamespaceInfo) */
    @Override
    public void save(NamespaceInfo namespace) {
        saveInternal(namespace, NamespaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.NamespaceInfo) */
    @Override
    public NamespaceInfo detach(NamespaceInfo namespace) {
        return namespace;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getDefaultNamespace() */
    @Override
    public NamespaceInfo getDefaultNamespace() {
        return db.getDefault(NamespaceInfo.class.getSimpleName(), NamespaceInfo.class);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#setDefaultNamespace(org.geoserver.catalog.NamespaceInfo)
     */
    @Override
    public void setDefaultNamespace(@Nullable NamespaceInfo defaultNamespace) {
        String target = NamespaceInfo.class.getSimpleName();
        String id = defaultNamespace == null ? null : defaultNamespace.getId();

        NamespaceInfo old = getDefaultNamespace();

        if (!Utilities.equals(old, defaultNamespace)) {
            // fire modify event before change
            Catalog catalog = getCatalog();
            catalog.fireModified(
                    catalog,
                    Arrays.asList("defaultNamespace"),
                    Arrays.asList(old),
                    Arrays.asList(defaultNamespace));
        }
        db.setDefault(target, id);

        if (!Utilities.equals(old, defaultNamespace)) {
            // fire postmodify event after change
            Catalog catalog = getCatalog();
            catalog.firePostModified(
                    catalog,
                    Arrays.asList("defaultNamespace"),
                    Arrays.asList(old),
                    Arrays.asList(defaultNamespace));
        }
    }

    /** @see org.geoserver.catalog.CatalogFacade#getNamespace(java.lang.String) */
    @Override
    public NamespaceInfo getNamespace(String id) {
        return db.getById(id, NamespaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getNamespaceByPrefix(java.lang.String) */
    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return db.getByIdentity(NamespaceInfo.class, "prefix", prefix);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getNamespaceByURI(java.lang.String) */
    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        return db.getByIdentity(NamespaceInfo.class, "URI", uri);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getNamespaces() */
    @Override
    public List<NamespaceInfo> getNamespaces() {
        return db.getAll(NamespaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.WorkspaceInfo) */
    @Override
    public WorkspaceInfo add(WorkspaceInfo workspace) {
        return addInternal(workspace);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.WorkspaceInfo) */
    @Override
    public void remove(WorkspaceInfo workspace) {
        db.remove(workspace);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.WorkspaceInfo) */
    @Override
    public void save(WorkspaceInfo workspace) {
        saveInternal(workspace, WorkspaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.WorkspaceInfo) */
    @Override
    public WorkspaceInfo detach(WorkspaceInfo workspace) {
        return workspace;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getDefaultWorkspace() */
    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return db.getDefault(WorkspaceInfo.class.getSimpleName(), WorkspaceInfo.class);
    }

    /**
     * @see
     *     org.geoserver.catalog.CatalogFacade#setDefaultWorkspace(org.geoserver.catalog.WorkspaceInfo)
     */
    @Override
    public void setDefaultWorkspace(@Nullable WorkspaceInfo workspace) {
        String type = WorkspaceInfo.class.getSimpleName();
        String id = workspace == null ? null : workspace.getId();

        WorkspaceInfo old = getDefaultWorkspace();

        if (!Utilities.equals(old, workspace)) {
            // fire modify event before change
            Catalog catalog = getCatalog();
            catalog.fireModified(
                    catalog,
                    Arrays.asList("defaultWorkspace"),
                    Arrays.asList(old),
                    Arrays.asList(workspace));
        }
        db.setDefault(type, id);

        if (!Utilities.equals(old, workspace)) {
            // fire postmodify event after change
            Catalog catalog = getCatalog();
            catalog.firePostModified(
                    catalog,
                    Arrays.asList("defaultWorkspace"),
                    Arrays.asList(old),
                    Arrays.asList(workspace));
        }
    }

    /** @see org.geoserver.catalog.CatalogFacade#getWorkspace(java.lang.String) */
    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return db.getById(id, WorkspaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getWorkspaceByName(java.lang.String) */
    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return getByName(name, WorkspaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getWorkspaces() */
    @Override
    public List<WorkspaceInfo> getWorkspaces() {
        return db.getAll(WorkspaceInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#add(org.geoserver.catalog.StyleInfo) */
    @Override
    public StyleInfo add(StyleInfo style) {
        return addInternal(style);
    }

    /** @see org.geoserver.catalog.CatalogFacade#remove(org.geoserver.catalog.StyleInfo) */
    @Override
    public void remove(StyleInfo style) {
        db.remove(style);
    }

    /** @see org.geoserver.catalog.CatalogFacade#save(org.geoserver.catalog.StyleInfo) */
    @Override
    public void save(StyleInfo style) {
        saveInternal(style, StyleInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#detach(org.geoserver.catalog.StyleInfo) */
    @Override
    public StyleInfo detach(StyleInfo style) {
        return style;
    }

    /** @see org.geoserver.catalog.CatalogFacade#getStyle(java.lang.String) */
    @Override
    public StyleInfo getStyle(String id) {
        return db.getById(id, StyleInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getStyleByName(java.lang.String) */
    @Override
    public StyleInfo getStyleByName(String name) {
        return getStyleByName(NO_WORKSPACE, name);
    }

    /** @see org.geoserver.catalog.CatalogFacade#getStyles() */
    @Override
    public List<StyleInfo> getStyles() {
        return db.getAll(StyleInfo.class);
    }

    /** @see org.geoserver.catalog.CatalogFacade#dispose() */
    @Override
    public void dispose() {
        db.dispose();
    }

    /** @see org.geoserver.catalog.CatalogFacade#resolve() */
    @Override
    public void resolve() {
        //
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        Filter filter = equal("name", name);
        if (NO_WORKSPACE == workspace) {
            Filter wsFilter = isNull("workspace.id");
            filter = and(filter, wsFilter);
        } else if (workspace != null && ANY_WORKSPACE != workspace) {
            Filter wsFilter = equal("workspace.id", workspace.getId());
            filter = and(filter, wsFilter);
        }

        LayerGroupInfo store;
        try {
            store = findUnique(LayerGroupInfo.class, filter);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return store;
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {

        if (workspace == null) {
            workspace = getDefaultWorkspace();
        }
        if (workspace == null) {
            return Collections.emptyList();
        }
        Filter filter;
        if (NO_WORKSPACE == workspace) {
            filter = isNull("workspace.id");
        } else {
            filter = equal("workspace.id", workspace.getId());
        }
        return db.queryAsList(LayerGroupInfo.class, filter, null, null, null);
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        checkNotNull(
                workspace,
                "workspace is null. Did you mean CatalogFacade.ANY_WORKSPACE or CatalogFacade.NO_WORKSPACE?");
        checkNotNull(name, "name");

        if (workspace == ANY_WORKSPACE) {
            return db.getByIdentity(StyleInfo.class, "name", name);
        } else {
            return db.getByIdentity(
                    StyleInfo.class, "workspace.id", workspace.getId(), "name", name);
        }
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        if (workspace == null) {
            workspace = getDefaultWorkspace();
        }
        if (workspace == null) {
            return Collections.emptyList();
        }
        Filter filter;
        if (NO_WORKSPACE == workspace) {
            filter = isNull("workspace.id");
        } else {
            filter = equal("workspace.id", workspace.getId());
        }
        return db.queryAsList(StyleInfo.class, filter, null, null, null);
    }

    /** @see org.geoserver.catalog.CatalogFacade#syncTo(org.geoserver.catalog.CatalogFacade) */
    @Override
    public void syncTo(CatalogFacade other) {
        other = ProxyUtils.unwrap(other, LockingCatalogFacade.class);
        for (WorkspaceInfo w : getWorkspaces()) {
            other.add(w);
        }

        for (NamespaceInfo ns : getNamespaces()) {
            other.add(ns);
        }

        for (StoreInfo s : getStores(StoreInfo.class)) {
            other.add(s);
        }

        for (ResourceInfo r : getResources(ResourceInfo.class)) {
            other.add(r);
        }

        for (StyleInfo s : getStyles()) {
            other.add(s);
        }

        for (LayerInfo l : getLayers()) {
            other.add(l);
        }
        for (LayerGroupInfo lg : getLayerGroups()) {
            other.add(lg);
        }

        for (MapInfo m : getMaps()) {
            other.add(m);
        }

        other.setDefaultWorkspace(getDefaultWorkspace());
        other.setDefaultNamespace(getDefaultNamespace());

        for (WorkspaceInfo ws : getWorkspaces()) {
            DataStoreInfo defaultDataStore = getDefaultDataStore(ws);
            if (defaultDataStore != null) {
                other.setDefaultDataStore(ws, defaultDataStore);
            }
        }
    }

    private <T extends CatalogInfo> T findUnique(Class<T> type, Filter filter)
            throws IllegalArgumentException {

        final Integer count = Integer.valueOf(2);
        CloseableIterator<T> it = list(type, filter, null, count);
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

    /**
     * @see org.geoserver.catalog.CatalogFacade#count(java.lang.Class,
     *     org.geoserver.catalog.Predicate)
     */
    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        return db.count(of, filter);
    }

    @Override
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        boolean canSort = db.canSort(type, propertyName);
        return canSort;
    }

    /**
     * @see org.geoserver.catalog.CatalogFacade#list(java.lang.Class,
     *     org.geoserver.catalog.Predicate, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            @Nullable final Integer offset,
            @Nullable final Integer count,
            @Nullable final SortBy... sortBy) {

        if (sortBy != null) {
            for (SortBy sortOrder : sortBy) {
                Preconditions.checkArgument(
                        null == sortOrder
                                || canSort(of, sortOrder.getPropertyName().getPropertyName()),
                        "Can't sort objects of type %s by %s",
                        of,
                        sortOrder);
            }
        }
        return db.query(of, filter, offset, count, sortBy);
    }

    public <T extends CatalogInfo> void saveInternal(T info, Class<T> type) {
        Assert.notNull(info);
        Assert.notNull(info.getId(), "Can't modify a CatalogInfo with no id");

        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(info);

        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        beforeSaved(info, propertyNames, oldValues, newValues);
        commitProxy(info);
        afterSaved(info, propertyNames, oldValues, newValues);
    }

    protected void beforeSaved(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues) {
        // get the real object
        CatalogInfo real = ModificationProxy.unwrap(object);

        // TODO: protect this original object, perhaps with another proxy
        getCatalog().fireModified(real, propertyNames, oldValues, newValues);
    }

    protected <T extends CatalogInfo> T commitProxy(T object) {

        // get the real object
        try {
            return db.save(object);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save object " + object.getId(), e);
            return null;
        }
    }

    protected void afterSaved(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues) {
        CatalogInfo real = ModificationProxy.unwrap(object);

        // fire the post modify event
        getCatalog().firePostModified(real, propertyNames, oldValues, newValues);
    }

    private <T extends CatalogInfo> T getByName(final String name, final Class<T> clazz) {
        return db.getByIdentity(clazz, "name", name);
    }

    private <T extends CatalogInfo> T addInternal(T info) {
        Assert.notNull(info, "Info object cannot be null");

        Class<T> clazz = ClassMappings.fromImpl(info.getClass()).getInterface();

        setId(info, clazz);

        T added = db.add(info);

        return added;
    }

    private void setId(CatalogInfo info, Class<? extends CatalogInfo> type) {
        final String curId = info.getId();

        final String id;

        if (null != curId) {
            // HACK: is it imported from the DefaultCatalogFacade?
            // final String match = "Impl-";
            // int index = curId.indexOf(match);
            // if (index == -1) {
            // throw new IllegalArgumentException(
            // "Attempting to set id on an object already identified (" + curId + "): "
            // + info);
            // }
            id = curId;
        } else {
            String newId = UUID.randomUUID().toString();
            id = type.getSimpleName() + "." + newId;
        }

        OwsUtils.set(info, "id", id);
    }
}
