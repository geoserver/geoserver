/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.InMemorySecurityFilter;
import org.geoserver.security.LayerGroupAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.StyleAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Default implementation of {@link DataAccessManager}, loads simple access
 * rules from a properties file or a Properties object. The format of each
 * property is:<br>
 * <code>workspace.layer.mode=[role]*</code><br>
 * where:
 * <ul>
 * <li> workspace: either a workspace name or a * to indicate any workspace (in
 * this case, the layer must also be *) </li>
 * <li> layer: either a layer name (feature type, coverage, layer group) or * to
 * indicate any layer </li>
 * <li> mode: the access mode, at the time or writing, either &quot;r&quot;
 * (read) or &quot;w&quot; (write) </li>
 * <li> role: a user role</li>
 * </ul>
 * A special line is used to specify the security mode in which GeoServer operates:
 * <code>mode=HIDE|CHALLENGE|MIDEX</code>
 * For the meaning of these three constants see {@link CatalogMode}<p>
 * For more details on how the security rules are applied, see the &lt;a
 * href=&quot;http://geoserver.org/display/GEOS/GSIP+19+-+Per+layer+security&quot;/&gt;per
 * layer security proposal&lt;/a&gt; on the &lt;a
 * href=&quot;www.geoserver.org&quot;&gt;GeoServer&lt;/a&gt; web site.
 * <p>
 * If no {@link Properties} is provided, one will be looked upon in
 * <code>GEOSERVER_DATA_DIR/security/layers.properties, and the class will
 * keep up to date vs changes in the file</code>
 * 
 * @author Andrea Aime - TOPP
 */
public class DefaultResourceAccessManager implements ResourceAccessManager, DataAccessManager {
    static final Logger LOGGER = Logging.getLogger(DefaultResourceAccessManager.class);

    SecureTreeNode root;

//    Catalog catalog;
    
    DataAccessRuleDAO dao;

    long lastLoaded = Long.MIN_VALUE;

    public DefaultResourceAccessManager(DataAccessRuleDAO dao) {
        this.dao = dao;
        this.root = buildAuthorizationTree(dao);
    }

    public CatalogMode getMode() {
        return dao.getMode();
    }

    public boolean canAccess(Authentication user, WorkspaceInfo workspace, AccessMode mode) {
        checkPropertyFile();
        SecureTreeNode node = root.getDeepestNode(new String[] { workspace.getName() });
        return node.canAccess(user, mode);
    }

    public boolean canAccess(Authentication user, LayerInfo layer, AccessMode mode) {
        checkPropertyFile();
        if (layer.getResource() == null) {
            LOGGER.log(Level.FINE, "Layer " + layer + " has no attached resource, "
                    + "assuming it's possible to access it");
            // it's a layer whose resource we don't know about
            return true;
        } else {
            return canAccess(user, layer.getResource(), mode);
        }

    }

    public boolean canAccess(Authentication user, ResourceInfo resource, AccessMode mode) {
        checkPropertyFile();
        String workspace;
        try {
            workspace = resource.getStore().getWorkspace().getName();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Errors occurred trying to gather workspace of resource "
                    + resource.getName());
            // it's a layer whose resource we don't know about
            return true;
        }

        SecureTreeNode node = root.getDeepestNode(new String[] { workspace, resource.getName() });
        return node.canAccess(user, mode);
    }

    void checkPropertyFile() {
        long daoLastModified = dao.getLastModified();
        if(lastLoaded < daoLastModified) {
            root = buildAuthorizationTree(dao);
            lastLoaded = daoLastModified;
        }
    }

    SecureTreeNode buildAuthorizationTree(DataAccessRuleDAO dao) {
        SecureTreeNode root = new SecureTreeNode();
        
        for(DataAccessRule rule : dao.getRules()) {
            String workspace = rule.getWorkspace();
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
                } else {
                    SecureTreeNode layerNode = ws.getChild(layer);
                    if (layerNode == null)
                        layerNode = ws.addChild(layer);
                    node = layerNode;
                }

            }

            // actually set the rule, but don't complain for the default root contents
            if (node.getAuthorizedRoles(accessMode) != null && node.getAuthorizedRoles(accessMode).size() > 0 && node != root) {
                LOGGER.warning("Rule " + rule
                        + " is overriding another rule targetting the same resource");
            }
            node.setAuthorizedRoles(accessMode, rule.getRoles());
        }
        
        return root;
    }

    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        boolean read = canAccess(user, layer, AccessMode.READ);
        boolean write = canAccess(user, layer, AccessMode.WRITE);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(layer.getResource().getClass(), readFilter, writeFilter);
    }

    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        boolean read = canAccess(user, resource, AccessMode.READ);
        boolean write = canAccess(user, resource, AccessMode.WRITE);
        Filter readFilter = read ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = write ? Filter.INCLUDE : Filter.EXCLUDE;
        return buildLimits(resource.getClass(), readFilter, writeFilter);
    }

    DataAccessLimits buildLimits(Class<? extends ResourceInfo> resourceClass, Filter readFilter,
            Filter writeFilter) {
        CatalogMode mode = getMode();

        // allow the secure catalog to avoid any kind of wrapping if there are no limits
        if ((readFilter == null || readFilter == Filter.INCLUDE)
                && (writeFilter == null || writeFilter == Filter.INCLUDE
                        || WMSLayerInfo.class.isAssignableFrom(resourceClass) || CoverageInfo.class
                            .isAssignableFrom(resourceClass))) {
            return null;
        }

        // build the appropriate limit class
        if (FeatureTypeInfo.class.isAssignableFrom(resourceClass)) {
            return new VectorAccessLimits(mode, null, readFilter, null, writeFilter);
        } else if (CoverageInfo.class.isAssignableFrom(resourceClass)) {
            return new CoverageAccessLimits(mode, readFilter, null, null);
        } else if (WMSLayerInfo.class.isAssignableFrom(resourceClass)) {
            return new WMSAccessLimits(mode, readFilter, null, true);
        } else {
            LOGGER.log(Level.INFO,
                    "Warning, adapting to generic access limits for unrecognized resource type "
                            + resourceClass);
            return new DataAccessLimits(mode, readFilter);
        }
    }

    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        boolean readable = canAccess(user, workspace, AccessMode.READ);
        boolean writable = canAccess(user, workspace, AccessMode.WRITE);
        boolean adminable = canAccess(user, workspace, AccessMode.ADMIN);

        CatalogMode mode = getMode();

        if (readable && writable) {
            if (AdminRequest.get() == null) {
                // not admin request, read+write means full acesss
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
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return null;
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        if(getMode() == CatalogMode.CHALLENGE) {
            // If we're in CHALLENGE mode, we cannot pre-filter
            // for the other types we have no clue, use the in memory filtering
            return InMemorySecurityFilter.buildUserAccessFilter(this, user);
        }

        
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
            if (exceptions.size() == 0) {
                return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
            } else {
                return rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
            }
        } else if (PublishedInfo.class.isAssignableFrom(clazz)
                || ResourceInfo.class.isAssignableFrom(clazz)
                || CoverageInfo.class.isAssignableFrom(clazz)) {
            // base access
            boolean rootAccess = canAccess(user, root);
            List<Filter> exceptions = new ArrayList<>();

            // get the right ws property name
            String wsNameProperty;
            if (PublishedInfo.class.isAssignableFrom(clazz)) {
                wsNameProperty = "resource.store.workspace.name";
            } else {
                wsNameProperty = "store.workspace.name";
            }

            // workspace exceptions
            for (Map.Entry<String, SecureTreeNode> wsEntry : root.getChildren().entrySet()) {
                String wsName = wsEntry.getKey();
                SecureTreeNode wsNode = wsEntry.getValue();
                boolean wsAccess = canAccess(user, wsNode);

                List<Filter> layerExceptions = new ArrayList<>();
                for (Map.Entry<String, SecureTreeNode> layerEntry : wsNode.getChildren().entrySet()) {
                    String layerName = layerEntry.getKey();
                    SecureTreeNode layerNode = layerEntry.getValue();
                    boolean layerAccess = canAccess(user, layerNode);
                    if (layerAccess != wsAccess) {
                        if (wsAccess) {
                            layerExceptions.add(Predicates.notEqual("prefixedName", wsName + ":" + layerName));
                        } else {
                            layerExceptions.add(Predicates.equal("prefixedName", wsName + ":" + layerName));
                        }
                    }
                }

                Filter wsFilter = null;
                if (rootAccess && !wsAccess) {
                    wsFilter = Predicates.notEqual(wsNameProperty, wsName);
                } else if (!rootAccess && wsAccess) {
                    wsFilter = Predicates.equal(wsNameProperty, wsName);
                }
                
                if(layerExceptions.isEmpty()) {
                    if (wsFilter != null) {
                        exceptions.add(wsFilter);
                    }
                } else {
                    if (wsFilter != null) {
                        layerExceptions.add(wsFilter);
                    }
                    Filter combined = wsAccess ? Predicates.and(layerExceptions) : Predicates
                            .or(layerExceptions);
                    exceptions.add(combined);
                }
            }

            if (exceptions.size() == 0) {
                return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
            } else {
                Filter filter = rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
                // in case of published info, we have to filter the layer groups as a separate
                // entity
                if (PublishedInfo.class.equals(clazz)) {
                    Filter layerFilter = Predicates.and(Predicates.isInstanceOf(LayerInfo.class),
                            filter);
                    Filter layerGroupFilter = Predicates.isInstanceOf(LayerGroupInfo.class);
                    return Predicates.or(layerFilter, layerGroupFilter);
                } else {
                    return filter;
                }
            }
        } else if (StyleInfo.class.isAssignableFrom(clazz)
                || LayerGroupInfo.class.isAssignableFrom(clazz)) {
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
            if (exceptions.size() == 0) {
                return rootAccess ? Filter.INCLUDE : Filter.EXCLUDE;
            } else {
                return rootAccess ? Predicates.and(exceptions) : Predicates.or(exceptions);
            }
        } else {
            // for the other types we have no clue, use the in memory filtering
            return InMemorySecurityFilter.buildUserAccessFilter(this, user);
        }
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

}
