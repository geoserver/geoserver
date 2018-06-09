/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;

/**
 * This listener keeps the workspaces and namespaces consistent with each other. TODO: remove once
 * namespaces become a separate entity than workspaces
 *
 * @author Andrea Aime - OpenGeo
 */
public class NamespaceWorkspaceConsistencyListener implements CatalogListener {

    Catalog catalog;

    /** This flag prevents the listener from becoming recoursive */
    boolean editing = false;

    public NamespaceWorkspaceConsistencyListener(Catalog catalog) {
        this.catalog = catalog;
        catalog.addListener(this);
    }

    /** Takes care of keeping in synch namespace and workspaces in face of modifications */
    public synchronized void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        List<String> properties = event.getPropertyNames();
        if (event.getSource() instanceof NamespaceInfo
                && !editing
                && properties.contains("prefix")) {
            int prefixIdx = properties.indexOf("prefix");
            String oldPrefix = (String) event.getOldValues().get(prefixIdx);
            String newPrefix = (String) event.getNewValues().get(prefixIdx);
            WorkspaceInfo ws = catalog.getWorkspaceByName(oldPrefix);
            if (ws != null) {
                try {
                    editing = true;
                    ws.setName(newPrefix);
                    catalog.save(ws);
                } finally {
                    editing = false;
                }
            }
        } else if (event.getSource() instanceof Catalog
                && properties.contains("defaultNamespace")
                && !editing) {
            NamespaceInfo newDefault =
                    (NamespaceInfo)
                            event.getNewValues().get(properties.indexOf("defaultNamespace"));
            if (newDefault != null) {
                WorkspaceInfo ws = catalog.getWorkspaceByName(newDefault.getPrefix());
                if (ws != null && !catalog.getDefaultWorkspace().equals(ws)) {
                    try {
                        editing = true;
                        catalog.setDefaultWorkspace(ws);
                    } finally {
                        editing = false;
                    }
                }
            }
        } else if (event.getSource() instanceof WorkspaceInfo
                && !editing
                && properties.contains("name")) {
            WorkspaceInfo ws = (WorkspaceInfo) event.getSource();
            int nameIdx = properties.indexOf("name");
            String oldName = (String) event.getOldValues().get(nameIdx);
            String newName = (String) event.getNewValues().get(nameIdx);
            NamespaceInfo ns = catalog.getNamespaceByPrefix(oldName);
            if (ns != null) {
                try {
                    editing = true;
                    ns.setPrefix(newName);
                    catalog.save(ns);
                } finally {
                    editing = false;
                }
            }
        } else if (event.getSource() instanceof Catalog
                && properties.contains("defaultWorkspace")
                && !editing) {
            WorkspaceInfo newDefault =
                    (WorkspaceInfo)
                            event.getNewValues().get(properties.indexOf("defaultWorkspace"));
            if (newDefault != null) {
                NamespaceInfo ns = catalog.getNamespaceByPrefix(newDefault.getName());
                if (ns != null && !catalog.getDefaultNamespace().equals(ns)) {
                    try {
                        editing = true;
                        catalog.setDefaultNamespace(ns);
                    } finally {
                        editing = false;
                    }
                }
            }
        }
    }

    /** Takes care of keeping the stores namespace URI in synch with namespace changes */
    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        if (event.getSource() instanceof NamespaceInfo) {
            NamespaceInfo ns = (NamespaceInfo) event.getSource();
            String namespaceURI = ns.getURI();

            WorkspaceInfo ws = catalog.getWorkspaceByName(ns.getPrefix());
            if (ws != null) {
                List<DataStoreInfo> stores = catalog.getDataStoresByWorkspace(ws);
                if (stores.size() > 0) {
                    for (DataStoreInfo store : stores) {
                        String oldURI = (String) store.getConnectionParameters().get("namespace");
                        if (oldURI != null && !namespaceURI.equals(oldURI)) {
                            store.getConnectionParameters().put("namespace", namespaceURI);
                            catalog.save(store);
                        }
                    }
                }
            }
        }
    }

    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // ignore
    }

    /** When a namespace is removed, makes sure the associated workspace is removed as well. */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof NamespaceInfo) {
            NamespaceInfo ns = (NamespaceInfo) event.getSource();

            WorkspaceInfo ws = catalog.getWorkspaceByName(ns.getPrefix());
            if (ws != null) {
                catalog.remove(ws);
            }
        }
    }

    public void reloaded() {
        // ignore
    }
}
