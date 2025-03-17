/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.ANY;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.InMemorySecurityFilter;
import org.geoserver.security.LayerGroupAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.StyleAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WMTSAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * Default implementation of {@link ResourceAccessManager}, loads simple access rules from a properties file or a
 * Properties object. The format of each property is:<br>
 * <code>workspace.layer.mode=[role]*</code><br>
 * where:
 *
 * <ul>
 *   <li>workspace: either a workspace name or a * to indicate any workspace (in this case, the layer must also be *)
 *   <li>layer: either a layer name (feature type, coverage, layer group) or * to indicate any layer
 *   <li>mode: the access mode, at the time or writing, either &quot;r&quot; (read) or &quot;w&quot; (write)
 *   <li>role: a user role
 * </ul>
 *
 * A special line is used to specify the security mode in which GeoServer operates: <code>
 * mode=HIDE|CHALLENGE|MIDEX</code> For the meaning of these three constants see {@link CatalogMode}
 *
 * <p>For more details on how the security rules are applied, see the &lt;a
 * href=&quot;http://geoserver.org/display/GEOS/GSIP+19+-+Per+layer+security&quot;/&gt;per layer security
 * proposal&lt;/a&gt; on the &lt;a href=&quot;www.geoserver.org&quot;&gt;GeoServer&lt;/a&gt; web site.
 *
 * <p>If no {@link Properties} is provided, one will be looked upon in <code>
 * GEOSERVER_DATA_DIR/security/layers.properties, and the class will
 * keep up to date vs changes in the file</code>
 *
 * @author Andrea Aime - TOPP
 */
public class DefaultResourceAccessManager implements ResourceAccessManager {
    static final Logger LOGGER = Logging.getLogger(DefaultResourceAccessManager.class);

    static final int FILTERCACHE_SIZE = 100;
    static final int FILTERCACHE_EXPIRY_TIME = 60;

    /**
     * Flag to enable previous resource filter building logic. This could be used to prevent performance regressions in
     * JDCBConfig. Defaults to False.
     */
    static final Boolean RESOURCE_EQUALITY_FILTER_ENABLED =
            Boolean.getBoolean("geoserver.access.resourceEqualityFilterEnabled");

    /** A {@link LayerGroupSummary} extended with the associated secure tree node */
    static class SecuredGroupSummary extends LayerGroupSummary {

        private SecureTreeNode node;

        SecuredGroupSummary(LayerGroupSummary origin, SecureTreeNode node) {
            super(origin);
            this.node = node;
        }

        boolean canAccess(Authentication user, AccessMode mode) {
            return node == null || node.canAccess(user, mode);
        }

        public SecureTreeNode getNode() {
            return node;
        }
    }

    SecureTreeNode root;

    DataAccessRuleDAO dao;

    Catalog rawCatalog;

    long lastLoaded = Long.MIN_VALUE;

    LayerGroupContainmentCache groupsCache;

    Cache<Pair<Authentication, Class<? extends CatalogInfo>>, Filter> filterCache = CacheBuilder.newBuilder()
            .maximumSize(FILTERCACHE_SIZE)
            .expireAfterAccess(FILTERCACHE_EXPIRY_TIME, TimeUnit.MINUTES)
            .build();

    /**
     * Pass a reference to the raw, unsecured catalog. The reference is used to evaluate the relationship between layers
     * and the groups containing them
     */
    public DefaultResourceAccessManager(DataAccessRuleDAO dao, Catalog rawCatalog) {
        this.dao = dao;
        this.rawCatalog = rawCatalog;
        this.root = buildAuthorizationTree(dao);
    }

    /**
     * Sets the layer group cache
     *
     * @param groupsCache
     */
    public void setGroupsCache(LayerGroupContainmentCache groupsCache) {
        this.groupsCache = groupsCache;
    }

    public CatalogMode getMode() {
        return dao.getMode();
    }

    public boolean canAccess(Authentication user, WorkspaceInfo workspace, AccessMode mode) {
        checkPropertyFile();
        SecureTreeNode node = root.getDeepestNode(new String[] {workspace.getName()});
        if (node.canAccess(user, mode)) {
            return true;
        }

        // perform a drill down search, we still allow access to the workspace
        // if there is anything inside the workspace that can be read (otherwise
        // we are denying access to everything below it, which is not the spirit of the
        // tree override design)
        if (mode == AccessMode.READ && canAccessChild(node, user, mode)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the user can access the specified node, or one of the nodes below it
     *
     * <p>the specified nodes
     */
    private boolean canAccessChild(SecureTreeNode node, Authentication user, AccessMode mode) {
        if (node.canAccess(user, mode)) {
            return true;
        }
        for (SecureTreeNode child : node.getChildren().values()) {
            if (canAccessChild(child, user, mode)) {
                return true;
            }
        }

        return false;
    }

    public boolean canAccess(Authentication user, LayerInfo layer, AccessMode mode, boolean directAccess) {
        checkPropertyFile();
        if (layer.getResource() == null) {
            LOGGER.log(
                    Level.FINE,
                    "Layer " + layer + " has no attached resource, " + "assuming it's possible to access it");
            // it's a layer whose resource we don't know about
            return true;
        } else {
            return canAccess(user, layer.getResource(), mode, directAccess);
        }
    }

    public boolean canAccess(Authentication user, ResourceInfo resource, AccessMode mode, boolean directAccess) {
        checkPropertyFile();
        String workspace;
        final String resourceName = resource.getName();
        try {
            workspace = resource.getStore().getWorkspace().getName();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Errors occurred trying to gather workspace of resource " + resourceName);
            // it's a layer whose resource we don't know about
            return true;
        }

        // if we have a catalog rule that is at resource level, it's the most specific type,
        // it wins. Or it could be that we do not need to check layer groups at all
        SecureTreeNode securityNode = root.getDeepestNode(new String[] {workspace, resourceName});
        int catalogNodeDepth = securityNode.getDepth();
        boolean rulesAllowAccess = securityNode.canAccess(user, mode);
        if (catalogNodeDepth == SecureTreeNode.RESOURCE_DEPTH || !layerGroupContainmentCheckRequired()) {
            return rulesAllowAccess;
        }

        // grab the groups containing the resource, if any. If none, there is no group related logic
        // to apply
        Collection<LayerGroupSummary> containers = getLayerGroupsCache().getContainerGroupsFor(resource);
        if (containers.isEmpty()) {
            return rulesAllowAccess;
        }

        // there are groups, so there might be more specific rules overriding the catalog one,
        // search for them
        List<LayerGroupSummary> groupOverrides = containers.stream()
                .filter(sg -> {
                    LayerGroupInfo gi = rawCatalog.getLayerGroup(sg.getId());
                    if (gi == null) {
                        return false;
                    }
                    SecureTreeNode node = getNodeForGroup(gi);
                    return (node != null && node.getDepth() > catalogNodeDepth)
                            || (sg.getMode() == Mode.OPAQUE_CONTAINER);
                })
                .collect(Collectors.toList());
        if (!groupOverrides.isEmpty()) {
            // if there are overrides, see if at least one of them allows access
            rulesAllowAccess = groupOverrides.stream().anyMatch(sg -> {
                if (directAccess && sg.getMode() == Mode.OPAQUE_CONTAINER) {
                    return false;
                }
                LayerGroupInfo gi = rawCatalog.getLayerGroup(sg.getId());
                return gi != null
                        && canAccess(user, gi, directAccess)
                        && (!directAccess || allowsAccessViaNonOpaqueGroup(gi, resource));
            });
        }

        if (rulesAllowAccess) {
            return true;
        }

        // the rules allow no access, but there might still be a non secured layer group allowing
        // access to the resource
        return containers.stream().anyMatch(sg -> {
            if (directAccess && sg.getMode() == Mode.OPAQUE_CONTAINER) {
                return false;
            }
            LayerGroupInfo gi = rawCatalog.getLayerGroup(sg.getId());
            if (gi == null) {
                return false;
            }
            SecureTreeNode node = getNodeForGroup(gi);
            return node == null
                    && canAccess(user, gi, directAccess)
                    && (!directAccess || allowsAccessViaNonOpaqueGroup(gi, resource));
        });
    }

    /** Returns true if there is a path from the group to the resource that does not involve crossing a opaque group */
    private boolean allowsAccessViaNonOpaqueGroup(LayerGroupInfo gi, ResourceInfo resource) {
        for (PublishedInfo pi : gi.getLayers()) {
            if (pi instanceof LayerInfo) {
                if (resource.equals(((LayerInfo) pi).getResource())) {
                    return true;
                }
            } else {
                LayerGroupInfo lg = (LayerGroupInfo) pi;
                if (lg.getMode() != LayerGroupInfo.Mode.OPAQUE_CONTAINER
                        && allowsAccessViaNonOpaqueGroup(lg, resource)) {
                    return true;
                }
            }
        }

        return false;
    }

    private SecureTreeNode getNodeForGroup(LayerGroupInfo lg) {
        SecureTreeNode node;
        if (lg.getWorkspace() == null) {
            node = root.getNode(lg.getName());
        } else {
            String[] path = getLayerGroupPath(lg);
            node = root.getNode(path);
        }
        return node;
    }

    private boolean layerGroupContainmentCheckRequired() {
        // first, is it WMS?
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return false;
        }

        // layer groups are used only in WMS
        final String service = request.getService();
        return "WMS".equalsIgnoreCase(service) || "gwc".equalsIgnoreCase(service);
    }

    void checkPropertyFile() {
        rebuildAuthorizationTree(false);
    }

    /**
     * Forcefully reloads the configuration from the file system. Mostly used for testing (Windows notifications for
     * file changes are sometimes too slow)
     */
    public void reload() {
        rebuildAuthorizationTree(true);
    }

    private void rebuildAuthorizationTree(boolean force) {
        long daoLastModified = dao.getLastModified();
        if (lastLoaded < daoLastModified || force) {
            root = buildAuthorizationTree(dao);
            lastLoaded = daoLastModified;
            // The filter cache must be invalidated, since the security rules have changed!!!
            filterCache.invalidateAll();
        }
    }

    SecureTreeNode buildAuthorizationTree(DataAccessRuleDAO dao) {
        SecureTreeNode root = new SecureTreeNode();

        for (DataAccessRule rule : dao.getRules()) {
            String workspace = rule.getRoot();
            String layer = rule.getLayer();
            AccessMode accessMode = rule.getAccessMode();

            // look for the node where the rules will have to be set
            SecureTreeNode node;

            // check for the * ws definition
            if (ANY.equals(workspace)) {
                node = root;
            } else {
                // get or create the workspace
                SecureTreeNode ws = root.getChild(workspace);
                if (ws == null) {
                    ws = root.addChild(workspace);
                }

                // if layer is "*" the rule applies to the ws, otherwise
                // get/create the layer
                if ("*".equals(layer)) {
                    node = ws;
                } else if (rule.isGlobalGroupRule()) {
                    node = ws;
                } else {
                    SecureTreeNode layerNode = ws.getChild(layer);
                    if (layerNode == null) {
                        layerNode = ws.addChild(layer);
                    }
                    node = layerNode;
                }
            }

            // actually set the rule, but don't complain for the default root contents
            if (node.getAuthorizedRoles(accessMode) != null
                    && !node.getAuthorizedRoles(accessMode).isEmpty()
                    && node != root) {
                LOGGER.warning("Rule " + rule + " is overriding another rule targetting the same resource");
            }
            node.setAuthorizedRoles(accessMode, new HashSet<>(rule.getRoles()));
        }

        return root;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer, List<LayerGroupInfo> context) {
        final boolean directAccess = context == null || context.isEmpty();
        boolean read = canAccess(user, layer, AccessMode.READ, directAccess);
        boolean write = canAccess(user, layer, AccessMode.WRITE, directAccess);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(layer.getResource().getClass(), readFilter, writeFilter);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        boolean read = canAccess(user, resource, AccessMode.READ, true);
        boolean write = canAccess(user, resource, AccessMode.WRITE, true);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(resource.getClass(), readFilter, writeFilter);
    }

    DataAccessLimits buildLimits(Class<? extends ResourceInfo> resourceClass, Filter readFilter, Filter writeFilter) {
        CatalogMode mode = getMode();

        // allow the secure catalog to avoid any kind of wrapping if there are no limits
        if ((readFilter == null || readFilter == Filter.INCLUDE)
                && (writeFilter == null
                        || writeFilter == Filter.INCLUDE
                        || WMSLayerInfo.class.isAssignableFrom(resourceClass)
                        || WMTSLayerInfo.class.isAssignableFrom(resourceClass)
                        || CoverageInfo.class.isAssignableFrom(resourceClass))) {
            return null;
        }

        // build the appropriate limit class
        if (FeatureTypeInfo.class.isAssignableFrom(resourceClass)) {
            return new VectorAccessLimits(mode, null, readFilter, null, writeFilter);
        } else if (CoverageInfo.class.isAssignableFrom(resourceClass)) {
            return new CoverageAccessLimits(mode, readFilter, null, null);
        } else if (WMSLayerInfo.class.isAssignableFrom(resourceClass)) {
            return new WMSAccessLimits(mode, readFilter, null, true);
        } else if (WMTSLayerInfo.class.isAssignableFrom(resourceClass)) {
            return new WMTSAccessLimits(mode, readFilter, null);
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Warning, adapting to generic access limits for unrecognized resource type " + resourceClass);
            return new DataAccessLimits(mode, readFilter);
        }
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        boolean readable = canAccess(user, workspace, AccessMode.READ);
        boolean writable = canAccess(user, workspace, AccessMode.WRITE);
        boolean adminable = canAccess(user, workspace, AccessMode.ADMIN);

        CatalogMode mode = getMode();

        if (readable && writable) {
            if (!adminable && AdminRequest.get() == null) {
                // It is not an admin request, read+write means full access
                return null;
            }
        }
        return new WorkspaceAccessLimits(mode, readable, writable, adminable);
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return null;
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        boolean allowAccess = canAccess(user, layerGroup, containers == null || containers.isEmpty());
        return allowAccess ? null : new LayerGroupAccessLimits(getMode());
    }

    private boolean canAccess(Authentication user, LayerGroupInfo layerGroup, boolean directAccess) {
        String[] path = getLayerGroupPath(layerGroup);
        SecureTreeNode node = root.getDeepestNode(path);
        boolean catalogNodeAllowsAccess = node.canAccess(user, AccessMode.READ);
        boolean allowAccess;
        if (node != null && !catalogNodeAllowsAccess) {
            allowAccess = false;
        } else {
            // grab the groups containing the group, if any. If none, there is no group related
            // logic to apply
            Collection<LayerGroupSummary> directContainers =
                    getLayerGroupsCache().getContainerGroupsFor(layerGroup);
            if (directContainers.isEmpty()) {
                allowAccess = true;
            } else {
                // do we have at least one path that authorizes access to this group? need to check
                // group by group
                allowAccess = directContainers.stream().anyMatch(sg -> {
                    if (directAccess && sg.getMode() == Mode.OPAQUE_CONTAINER) {
                        return false;
                    }
                    LayerGroupInfo gi = rawCatalog.getLayerGroup(sg.getId());
                    return gi != null && canAccess(user, gi, directAccess);
                });
            }
        }

        return allowAccess;
    }

    /** Returns the possible location of the group in the secured tree based on name and workspace */
    private String[] getLayerGroupPath(LayerGroupInfo layerGroup) {
        if (layerGroup.getWorkspace() == null) {
            return new String[] {layerGroup.getName()};
        } else {
            return new String[] {layerGroup.getWorkspace().getName(), layerGroup.getName()};
        }
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        checkPropertyFile();
        if (supportsPrefilter(user, clazz)) {
            try {
                return filterCache.get(Pair.of(user, clazz), () -> buildSecurityPrefilter(user, clazz));
            } catch (ExecutionException e) {
                // this should never happen
                LOGGER.log(Level.WARNING, "Failed to build security prefilter", e);
            }
        }
        return InMemorySecurityFilter.buildUserAccessFilter(this, user);
    }

    protected boolean supportsPrefilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        if (getMode() == CatalogMode.CHALLENGE) {
            // If we're in CHALLENGE mode, we cannot pre-filter
            // for the other types we have no clue, use the in memory filtering
            return false;
        } else {
            return WorkspaceInfo.class.isAssignableFrom(clazz)
                    || PublishedInfo.class.isAssignableFrom(clazz)
                    || ResourceInfo.class.isAssignableFrom(clazz)
                    || CoverageInfo.class.isAssignableFrom(clazz)
                    || StyleInfo.class.isAssignableFrom(clazz)
                    || LayerGroupInfo.class.isAssignableFrom(clazz);
            // for the other types we have no clue, use the in memory filtering
        }
    }

    protected Filter buildSecurityPrefilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            // base access
            boolean rootAccess = canAccess(user, root);
            List<Filter> exceptions = new ArrayList<>();
            // exceptions
            for (Map.Entry<String, SecureTreeNode> entry : root.getChildren().entrySet()) {
                String wsName = entry.getKey();
                SecureTreeNode node = entry.getValue();
                boolean nodeAccess = canAccess(user, node);
                if (nodeAccess != rootAccess) {
                    if (rootAccess) {
                        exceptions.add(Predicates.notEqual("name", wsName));
                    } else {
                        exceptions.add(Predicates.equal("name", wsName));
                    }
                }
            }
            if (exceptions.isEmpty()) {
                return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
            } else {
                return rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
            }
        } else if (PublishedInfo.class.isAssignableFrom(clazz)
                || ResourceInfo.class.isAssignableFrom(clazz)
                || CoverageInfo.class.isAssignableFrom(clazz)) {
            if (RESOURCE_EQUALITY_FILTER_ENABLED) {
                return buildEqualityResourceFilter(user, clazz);
            } else {
                return buildInFunctionResourceFilter(user, clazz);
            }
        } else if (StyleInfo.class.isAssignableFrom(clazz) || LayerGroupInfo.class.isAssignableFrom(clazz)) {
            // we just check for workspace containment
            boolean rootAccess = canAccess(user, root);
            List<Filter> exceptions = new ArrayList<>();
            // exceptions
            for (Map.Entry<String, SecureTreeNode> entry : root.getChildren().entrySet()) {
                String wsName = entry.getKey();
                SecureTreeNode node = entry.getValue();
                boolean nodeAccess = canAccess(user, node);
                if (nodeAccess != rootAccess) {
                    if (rootAccess) {
                        exceptions.add(Predicates.notEqual("workspace.name", wsName));
                    } else {
                        exceptions.add(Predicates.equal("workspace.name", wsName));
                    }
                }
            }
            if (exceptions.isEmpty()) {
                return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
            } else {
                return rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
            }
        } else {
            // this should never happen (if supportsPrefilter is verified first)
            LOGGER.log(Level.WARNING, "Attempted to build unsupported security prefilter");
            return Filter.EXCLUDE;
        }
    }

    private Filter buildEqualityResourceFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        // base access
        boolean rootAccess = canAccess(user, root);
        List<Filter> exceptions = new ArrayList<>();

        // workspace exceptions
        for (Map.Entry<String, SecureTreeNode> wsEntry : root.getChildren().entrySet()) {
            String wsName = wsEntry.getKey();
            SecureTreeNode wsNode = wsEntry.getValue();
            boolean wsAccess = canAccess(user, wsNode);

            List<Filter> layerExceptions = new ArrayList<>();
            for (Map.Entry<String, SecureTreeNode> layerEntry :
                    wsNode.getChildren().entrySet()) {
                String layerName = layerEntry.getKey();
                SecureTreeNode layerNode = layerEntry.getValue();
                String prefixedName = wsName + ":" + layerName;
                Filter typeFilter = getTypeFilter(prefixedName, clazz);
                if (typeFilter == null) {
                    // dangling rule, referencing a non existing layer/group, continue
                    continue;
                }

                boolean layerAccess = canAccess(user, layerNode);
                if (layerAccess != wsAccess) {
                    Filter prefixedNameFilter =
                            Predicates.and(typeFilter, Predicates.equal("prefixedName", prefixedName));
                    if (wsAccess) {
                        layerExceptions.add(Predicates.not(prefixedNameFilter));
                    } else {
                        layerExceptions.add(prefixedNameFilter);
                    }
                }
            }

            // get the right ws property name
            Filter wsNamePropertyFilter;
            if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
                // resource.store.workspace.name is not applicable for layergroups
                wsNamePropertyFilter = Predicates.equal("workspace.name", wsName);
            } else if (LayerInfo.class.isAssignableFrom(clazz)) {
                wsNamePropertyFilter = Predicates.equal("resource.store.workspace.name", wsName);
            } else if (PublishedInfo.class.isAssignableFrom(clazz)) {
                wsNamePropertyFilter = Predicates.or(
                        Predicates.and(
                                Predicates.isInstanceOf(LayerInfo.class),
                                Predicates.equal("resource.store.workspace.name", wsName)),
                        Predicates.and(
                                Predicates.isInstanceOf(PublishedInfo.class),
                                Predicates.equal("workspace.name", wsName)));
            } else {
                wsNamePropertyFilter = Predicates.equal("store.workspace.name", wsName);
            }

            Filter wsFilter = null;
            if (rootAccess && !wsAccess) {
                wsFilter = Predicates.not(wsNamePropertyFilter);
            } else if (!rootAccess && wsAccess) {
                wsFilter = wsNamePropertyFilter;
            }

            if (layerExceptions.isEmpty()) {
                if (wsFilter != null) {
                    exceptions.add(wsFilter);
                }
            } else {
                if (wsFilter != null) {
                    layerExceptions.add(wsFilter);
                }
                Filter combined = wsAccess ? Predicates.and(layerExceptions) : Predicates.or(layerExceptions);
                exceptions.add(combined);
            }
        }

        if (exceptions.isEmpty()) {
            return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
        } else {
            return rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
        }
    }

    protected Filter buildInFunctionResourceFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        // base access
        boolean rootAccess = canAccess(user, root);

        List<Filter> filters = new ArrayList<>();
        // workspace exceptions
        for (Map.Entry<String, SecureTreeNode> wsEntry : root.getChildren().entrySet()) {
            String wsName = wsEntry.getKey();
            if (rawCatalog.getWorkspaceByName(wsName) == null) {
                // dangling rule, workspace does not exist, skip it
                continue;
            }
            SecureTreeNode wsNode = wsEntry.getValue();
            boolean wsAccess = canAccess(user, wsNode);

            List<String> layerExceptionIds = new ArrayList<>();
            for (Map.Entry<String, SecureTreeNode> layerEntry :
                    wsNode.getChildren().entrySet()) {
                String layerName = layerEntry.getKey();
                SecureTreeNode layerNode = layerEntry.getValue();
                String prefixedName = wsName + ":" + layerName;
                PublishedInfo published = rawCatalog.getLayerByName(prefixedName);
                if (published == null) {
                    published = rawCatalog.getLayerGroupByName(prefixedName);
                    if (published == null) {
                        // dangling rule, referencing a non-existing layer/group, continue
                        continue;
                    }
                }
                boolean layerAccess = canAccess(user, layerNode);
                if (layerAccess != wsAccess) {
                    if (ResourceInfo.class.isAssignableFrom(clazz) && published instanceof LayerInfo) {
                        layerExceptionIds.add(
                                ((LayerInfo) published).getResource().getId());
                    } else {
                        layerExceptionIds.add(published.getId());
                    }
                }
            }

            // get the right ws property name
            Filter wsNamePropertyFilter;
            if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
                // resource.store.workspace.name is not applicable for layergroups
                wsNamePropertyFilter = Predicates.equal("workspace.name", wsName);
            } else if (LayerInfo.class.isAssignableFrom(clazz)) {
                wsNamePropertyFilter = Predicates.equal("resource.store.workspace.name", wsName);
            } else if (PublishedInfo.class.isAssignableFrom(clazz)) {
                wsNamePropertyFilter = Predicates.or(
                        Predicates.and(
                                Predicates.isInstanceOf(LayerInfo.class),
                                Predicates.equal("resource.store.workspace.name", wsName)),
                        Predicates.and(
                                Predicates.isInstanceOf(PublishedInfo.class),
                                Predicates.equal("workspace.name", wsName)));
            } else {
                wsNamePropertyFilter = Predicates.equal("store.workspace.name", wsName);
            }

            // need to set up a wsFilter if the workspace is an exception compared to the root
            Filter wsFilter = null;
            if (rootAccess && !wsAccess) {
                wsFilter = Predicates.not(wsNamePropertyFilter);
            } else if (!rootAccess && wsAccess) {
                wsFilter = wsNamePropertyFilter;
            }

            if (layerExceptionIds.isEmpty()) {
                if (wsFilter != null) {
                    filters.add(wsFilter);
                }
            } else {
                Filter id = Predicates.in("id", layerExceptionIds);

                // if we can access the workspace, the ids are the ones we want to exclude,
                // otherwise, the ids are the ones we want to make an exception for instead
                if (wsAccess) {
                    id = Predicates.not(id);
                }
                if (wsFilter != null) {
                    if (wsAccess)
                        // ws is the one, but exclude selected layers in it
                        filters.add(Predicates.and(wsFilter, id));
                    else
                        // ws is not the one, but make an exception for selected layers in it
                        filters.add(Predicates.or(wsFilter, id));
                } else {
                    filters.add(id);
                }
            }
        }

        if (filters.isEmpty()) {
            return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
        } else {
            return rootAccess ? Predicates.and(filters) : Predicates.or(filters);
        }
    }

    private Filter getTypeFilter(String prefixedName, Class<?> clazz) {
        if (rawCatalog.getLayerByName(prefixedName) != null)
            if (clazz.equals(PublishedInfo.class)) {
                // restrict to layers in this case
                return Predicates.isInstanceOf(LayerInfo.class);
            } else {
                // otherwise use the native type, e.g., CoverageInfo, FeatureTypeInfo, LayerInfo
                return Predicates.isInstanceOf(clazz);
            }
        else if (rawCatalog.getLayerGroupByName(prefixedName) != null)
            return Predicates.isInstanceOf(LayerGroupInfo.class);
        else return null;
    }

    private boolean canAccess(Authentication user, SecureTreeNode node) {
        boolean access = node.canAccess(user, AccessMode.READ);
        if (access && AdminRequest.get() != null) {
            // admin request, we need to check if we can also admin those
            return node.canAccess(user, AccessMode.ADMIN);
        } else {
            return access;
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return getAccessLimits(user, layer, Collections.emptyList());
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return getAccessLimits(user, layerGroup, Collections.emptyList());
    }

    /**
     * Retrieves the layer group containment cache. If empty, it will fetch it from the context
     *
     * @return The layer group cantainment cache
     */
    protected LayerGroupContainmentCache getLayerGroupsCache() {
        if (groupsCache == null) {
            groupsCache = GeoServerExtensions.bean(LayerGroupContainmentCache.class);
        }
        return groupsCache;
    }
}
