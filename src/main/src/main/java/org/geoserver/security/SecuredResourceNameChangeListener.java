/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.*;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geotools.util.logging.Logging;

/**
 * Listens for changes on Workspace and Layer/LayerGroup names and updates Data Security Rules if
 * they exist on the modified Resource
 *
 * @author Imran Rajjad
 */
public class SecuredResourceNameChangeListener implements CatalogListener {

    static final Logger LOGGER = Logging.getLogger(SecuredResourceNameChangeListener.class);

    Catalog catalog;
    DataAccessRuleDAO dao;

    public SecuredResourceNameChangeListener(Catalog catalog, DataAccessRuleDAO dao) {
        this.catalog = catalog;
        this.dao = dao;
        catalog.addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // ignore
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        String resourceWorkSpaceName = null;
        CatalogInfo resourceInfo = event.getSource();
        String resourceName = null;
        if (resourceInfo instanceof LayerInfo) {
            resourceWorkSpaceName =
                    ((LayerInfo) resourceInfo).getResource().getStore().getWorkspace().getName();
            resourceName = ((LayerInfo) resourceInfo).getName();
        } else if (event.getSource() instanceof LayerGroupInfo) {
            resourceWorkSpaceName = ((LayerGroupInfo) resourceInfo).getWorkspace().getName();
            resourceName = ((LayerGroupInfo) resourceInfo).getName();
        }

        if (resourceWorkSpaceName != null && resourceInfo != null) {
            // if a security rule exists for the layer whose name has been changed
            if (layerHasSecurityRule(resourceWorkSpaceName, resourceName)) {
                LOGGER.info(
                        String.format(
                                "Removing Security Rules for removed layer: %s", resourceName));

                List<DataAccessRule> rulesToRemove = getDataAccessRule(resourceInfo, resourceName);
                for (DataAccessRule r : rulesToRemove) {
                    dao.removeRule(r);
                }
                try {
                    dao.storeRules();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    throw new CatalogException(e);
                }
            }
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // ignore
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        // no names where change
        if (event.getPropertyNames().indexOf("name") == -1) return;
        String oldName =
                String.valueOf(event.getOldValues().get(event.getPropertyNames().indexOf("name")));
        String newName =
                String.valueOf(event.getNewValues().get(event.getPropertyNames().indexOf("name")));
        // dont go anything if name has not changed
        if (oldName.equalsIgnoreCase(newName)) return;
        // will be used if name of
        String resourceWorkSpaceName = null;
        CatalogInfo resourceInfo = null;
        // if workspace has been renamed
        if (event.getSource() instanceof WorkspaceInfo) {
            // if a security rule exists for the workspaec whose name has been changed
            if (workspaceHasSecurityRule(oldName)) {
                LOGGER.info(
                        String.format(
                                "Updating Security Rules for Renamed Workspace: %s", oldName));

                // get rules in which workspace name should be updated
                List<DataAccessRule> rulesToModifyList =
                        getDataAccessRule(event.getSource(), oldName);
                for (DataAccessRule rule : rulesToModifyList) rule.setRoot(newName);
            }
        } else if (event.getSource() instanceof ResourceInfo) {
            // if a layer has been renamed
            // similar layer names can exist in different workspaces
            resourceInfo = event.getSource();
            resourceWorkSpaceName =
                    ((ResourceInfo) resourceInfo).getStore().getWorkspace().getName();

        } else if (event.getSource() instanceof LayerGroupInfo) {
            // if a layergroup has been renamed
            // similar layergroup names can exist in different workspaces
            resourceInfo = event.getSource();
            resourceWorkSpaceName = ((LayerGroupInfo) resourceInfo).getWorkspace().getName();
        }

        if (resourceWorkSpaceName != null && resourceInfo != null) {
            // if a security rule exists for the layer whose name has been changed
            if (layerHasSecurityRule(resourceWorkSpaceName, oldName)) {
                LOGGER.info(
                        String.format(
                                "Updating Security Rules for Renamed Feature Type: %s", oldName));

                // get rules in which workspace name should be updated
                List<DataAccessRule> rulesToModifyList = getDataAccessRule(resourceInfo, oldName);
                for (DataAccessRule rule : rulesToModifyList) rule.setLayer(newName);
            }
        }

        try {
            dao.storeRules();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void reloaded() {
        // ignore
    }

    private boolean workspaceHasSecurityRule(String workspaceName) {
        List<DataAccessRule> rules = this.dao.getRules();

        for (DataAccessRule rule : rules) {
            if (rule.getRoot().equalsIgnoreCase(workspaceName)) return true;
        }

        return false;
    }

    private boolean layerHasSecurityRule(String workspaceName, String layerName) {
        List<DataAccessRule> rules = this.dao.getRules();

        for (DataAccessRule rule : rules) {
            if (rule.getRoot().equalsIgnoreCase(workspaceName)
                    && rule.getLayer().equalsIgnoreCase(layerName)) return true;
        }

        return false;
    }

    private List<DataAccessRule> getDataAccessRule(CatalogInfo catalogInfo, String oldName) {
        List<DataAccessRule> rules = this.dao.getRules();
        List<DataAccessRule> rulesToUpdate = new ArrayList<DataAccessRule>();

        for (DataAccessRule rule : rules) {
            if (catalogInfo instanceof WorkspaceInfo) {
                if (rule.getRoot().equalsIgnoreCase(oldName)) rulesToUpdate.add(rule);
            } else if (catalogInfo instanceof ResourceInfo) {
                ResourceInfo resourceInfo = (ResourceInfo) catalogInfo;
                String workspaceName = resourceInfo.getStore().getWorkspace().getName();
                if (rule.getRoot().equalsIgnoreCase(workspaceName)
                        && rule.getLayer().equalsIgnoreCase(oldName)) rulesToUpdate.add(rule);
            } else if (catalogInfo instanceof LayerGroupInfo) {
                LayerGroupInfo resourceInfo = (LayerGroupInfo) catalogInfo;
                String workspaceName = resourceInfo.getWorkspace().getName();
                if (rule.getRoot().equalsIgnoreCase(workspaceName)
                        && rule.getLayer().equalsIgnoreCase(oldName)) rulesToUpdate.add(rule);
            } else if (catalogInfo instanceof LayerInfo) {
                LayerInfo resourceInfo = (LayerInfo) catalogInfo;
                String workspaceName =
                        resourceInfo.getResource().getStore().getWorkspace().getName();
                if (rule.getRoot().equalsIgnoreCase(workspaceName)
                        && rule.getLayer().equalsIgnoreCase(oldName)) rulesToUpdate.add(rule);
            }
        }
        return rulesToUpdate;
    }
}
