/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.resource.Resource.Lock;
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
        Lock lock = dao.lock();
        try {
            final String removedObjectName; // for logging
            final Predicate<DataAccessRule> filter;
            final CatalogInfo eventSource = event.getSource();
            if (eventSource instanceof WorkspaceInfo) {
                WorkspaceInfo ws = (WorkspaceInfo) eventSource;
                filter = workspaceFilter(ws.getName());
                removedObjectName = "Workspace " + ws.getName();
            } else if (eventSource instanceof LayerInfo) {
                LayerInfo l = (LayerInfo) eventSource;
                WorkspaceInfo ws = l.getResource().getStore().getWorkspace();
                filter = layerFilter(ws.getName(), l.getName());
                removedObjectName = "Layer " + l.getName();
            } else if (eventSource instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) eventSource;
                filter = layerGroupFilter(lg.getWorkspace(), lg.getName());
                removedObjectName = "Layer Group " + lg.getName();
            } else {
                return;
            }

            Supplier<String> message =
                    () ->
                            String.format(
                                    "Removing Security Rules for deleted %s", removedObjectName);
            apply(filter, dao::removeRule, message);
        } finally {
            lock.release();
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // ignore
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        Lock lock = dao.lock();
        try {
            final String oldName;
            final String newName;
            {
                final int indexOfName = event.getPropertyNames().indexOf("name");
                // no names where changed
                if (indexOfName == -1) return;
                oldName = String.valueOf(event.getOldValues().get(indexOfName));
                newName = String.valueOf(event.getNewValues().get(indexOfName));
                // dont go anything if name has not changed
                if (oldName.equalsIgnoreCase(newName)) return;
            }

            final String renamedObject; // for logging
            final CatalogInfo eventSource = event.getSource();
            final Predicate<DataAccessRule> filter;
            final Consumer<DataAccessRule> updater;
            if (eventSource instanceof WorkspaceInfo) {
                // if workspace has been renamed
                filter = workspaceFilter(oldName);
                updater = r -> r.setRoot(newName);
                renamedObject = "Workspace";
            } else if (eventSource instanceof ResourceInfo) {
                // if a layer has been renamed
                // similar layer names can exist in different workspaces
                String wsName = ((ResourceInfo) eventSource).getStore().getWorkspace().getName();
                filter = layerFilter(wsName, oldName);
                updater = r -> r.setLayer(newName);
                renamedObject = "Resource";
            } else if (eventSource instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) eventSource;
                filter = layerGroupFilter(lg.getWorkspace(), oldName);
                updater = layerGroupRuleUpdater(lg.getWorkspace(), newName);
                renamedObject = "LayerGroup";
            } else {
                return;
            }

            Supplier<String> message =
                    () ->
                            String.format(
                                    "Updating Security Rules for renamed %s: %s -> %s",
                                    renamedObject, oldName, newName);

            // modifying directly a rule is not a good idea, depending on the DAO implementation
            // it might do nothing (the DAO uses defensive copies or secondary storage) or it
            // might cause troubles with the DAO internal data structures (e.g. sorted structures,
            // when the field update changes the position of the rule in the order).
            // This bit removes the rule, changes it, and adds it back
            final Consumer<DataAccessRule> safeUpdater =
                    r -> {
                        dao.removeRule(r);
                        updater.accept(r);
                        dao.addRule(r);
                    };
            apply(filter, safeUpdater, message);
        } finally {
            lock.release();
        }
    }

    /**
     * Updates the {@link DataAccessRule#setRoot root} if it's a global layer group (i.e. {@code ws
     * == null}), or the {@link DataAccessRule#setLayer layer} otherwsie.
     */
    private Consumer<DataAccessRule> layerGroupRuleUpdater(WorkspaceInfo ws, String newName) {
        if (ws == null) {
            return r -> r.setRoot(newName);
        }
        return r -> r.setLayer(newName);
    }

    /**
     * @return rule predicate that checks the rule relates to a workspace, matching {@link
     *     DataAccessRule#getRoot() root} with the workspace name, and checking the {@link
     *     DataAccessRule#getLayer() layer} <b>is not</b> {@code null}.
     */
    private Predicate<DataAccessRule> workspaceFilter(String workspaceName) {
        return r -> r.getRoot().equalsIgnoreCase(workspaceName) && r.getLayer() != null;
    }

    /**
     * @return rule predicate that matches both the {@link DataAccessRule#getRoot() root} and {@link
     *     DataAccessRule#getLayer() layer} against the non-null {@code worksapce} and {@code
     *     layerName}, respectively.
     */
    private Predicate<DataAccessRule> layerFilter(String workspace, String layerName) {
        return filter(workspace::equalsIgnoreCase, layerName::equalsIgnoreCase);
    }

    /**
     * Predicate that matches if a rule applies to the given layer group.
     *
     * <p>For global layer groups, checks the layer group name matches the rule's {@link
     * DataAccessRule#getRoot() root}, and the rule's layer is {@code null}. For non global layer
     * groups, behaves like {@link #layerFilter(String, String)}
     */
    private Predicate<DataAccessRule> layerGroupFilter(WorkspaceInfo ws, String lgName) {
        final boolean isGlobalLayerGroup = null == ws;
        // for root layer groups, its name is set as the rule's root, and the rule's layer is null
        if (isGlobalLayerGroup) return filter(lgName::equalsIgnoreCase, Objects::isNull);
        // otherwise the rule's root is the workspace name as with LayerInfo's
        return layerFilter(ws.getName(), lgName);
    }

    void apply(
            Predicate<DataAccessRule> filter,
            Consumer<DataAccessRule> action,
            Supplier<String> logMessage) {
        List<DataAccessRule> matches = getDataAccessRules(filter);
        if (!matches.isEmpty()) {
            LOGGER.info(logMessage);
            matches.forEach(action);
            save();
        }
    }

    private void save() {
        try {
            dao.storeRules();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new CatalogException(e);
        }
    }

    @Override
    public void reloaded() {
        // ignore
    }

    /**
     * Returns a {@code Predicate<DataAccessRule>} that satisfies both {@link
     * DataAccessRule#getRoot() root} {@link DataAccessRule#getLayer() layer} predicates
     */
    private Predicate<DataAccessRule> filter(
            Predicate<String> rootFilter, Predicate<String> layerFilter) {
        return r -> rootFilter.test(r.getRoot()) && layerFilter.test(r.getLayer());
    }

    private List<DataAccessRule> getDataAccessRules(Predicate<DataAccessRule> filter) {
        return this.dao.getRules().stream().filter(filter).collect(Collectors.toList());
    }
}
