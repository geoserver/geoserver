/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DataAccessRule.*;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.security.core.Authentication;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessManager;
import org.geotools.util.logging.Logging;

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
public class DefaultDataAccessManager implements DataAccessManager {
    static final Logger LOGGER = Logging.getLogger(DataAccessManager.class);

    SecureTreeNode root;

//    Catalog catalog;
    
    DataAccessRuleDAO dao;

    long lastLoaded = Long.MIN_VALUE;

    public DefaultDataAccessManager(DataAccessRuleDAO dao) {
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
}
