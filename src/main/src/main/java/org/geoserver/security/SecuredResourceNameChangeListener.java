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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
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
        // ignore
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // ignore
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        // no names where change
        if (event.getPropertyNames().indexOf("name") == -1) return;
        // if workspace has been renamed
        if (event.getSource() instanceof WorkspaceInfo) {

            String oldWorkspaceName =
                    String.valueOf(
                            event.getOldValues().get(event.getPropertyNames().indexOf("name")));
            String newWorkspaceName =
                    String.valueOf(
                            event.getNewValues().get(event.getPropertyNames().indexOf("name")));
            // dont go anything if name has not changed
            if (oldWorkspaceName.equalsIgnoreCase(newWorkspaceName)) return;

            // if a security rule exists for the workspaec whose name has been changed
            if (workspaceHasSecurityRule(oldWorkspaceName)) {
                LOGGER.info(
                        String.format(
                                "Updating Security Rules for Renamed Workspace: %s",
                                oldWorkspaceName));

                // get rules in which workspace name should be updated
                List<DataAccessRule> rulesToModifyList =
                        getDataAccessRule(event.getSource(), oldWorkspaceName);
                for (DataAccessRule rule : rulesToModifyList) rule.setRoot(newWorkspaceName);

                try {
                    dao.storeRules();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } else if (event.getSource() instanceof ResourceInfo) {
            // if a layer has been renamed
            String oldResourceName =
                    String.valueOf(
                            event.getOldValues().get(event.getPropertyNames().indexOf("name")));
            String newResourceName =
                    String.valueOf(
                            event.getNewValues().get(event.getPropertyNames().indexOf("name")));
            // dont do anything if name has not changed
            if (oldResourceName.equalsIgnoreCase(newResourceName)) return;
            // similar layer names can exist in different workspaces
            ResourceInfo resourceInfo = (ResourceInfo) event.getSource();
            String workspaceName = resourceInfo.getStore().getWorkspace().getName();
            // if a security rule exists for the layer whose name has been changed
            if (layerHasSecurityRule(workspaceName, oldResourceName)) {
                LOGGER.info(
                        String.format(
                                "Updating Security Rules for Renamed Feature Type: %s",
                                oldResourceName));

                // get rules in which workspace name should be updated
                List<DataAccessRule> rulesToModifyList =
                        getDataAccessRule(resourceInfo, oldResourceName);
                for (DataAccessRule rule : rulesToModifyList) rule.setLayer(newResourceName);

                try {
                    dao.storeRules();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
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
            }
        }
        return rulesToUpdate;
    }
}
