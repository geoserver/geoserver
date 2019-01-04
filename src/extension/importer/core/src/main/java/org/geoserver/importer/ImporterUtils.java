/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.ows.util.OwsUtils;

/**
 * Utility class.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ImporterUtils {

    static WorkspaceInfo resolve(WorkspaceInfo ws, Catalog cat, boolean lookupByName) {
        if (ws != null) {
            WorkspaceInfo resolved = null;
            if (ws.getId() != null) {
                resolved = cat.getWorkspace(ws.getId());
            }
            if (resolved == null && lookupByName) {
                // try looking up by name
                resolved = cat.getWorkspaceByName(ws.getName());
            }

            if (resolved != null) {
                ws = resolved;
            }
        }
        return ws;
    }

    static StoreInfo resolve(StoreInfo s, Catalog cat, boolean lookupByName) {
        if (s != null) {
            StoreInfo resolved = null;
            if (s.getId() != null) {
                resolved = cat.getStore(s.getId(), StoreInfo.class);
            }

            if (resolved == null && lookupByName) {
                resolved = cat.getStoreByName(s.getWorkspace(), s.getName(), StoreInfo.class);
            }
            if (resolved != null) {
                s = resolved;
            }
        }

        if (s != null && s.getCatalog() == null && s instanceof StoreInfoImpl) {
            ((StoreInfoImpl) s).setCatalog(cat);
        }
        return resolveCollections(s);
    }

    static LayerInfo resolve(LayerInfo l, Catalog cat, boolean lookupByName) {
        if (l != null && l.getId() != null) {
            LayerInfo resolved = cat.getLayer(l.getId());
            if (resolved != null) {
                l = resolved;
            }
        }

        if (l != null && l.getId() == null && lookupByName) {
            LayerInfo resolved = cat.getLayerByName(l.prefixedName());
            if (resolved != null) {
                l = resolved;
            }
        }

        if (l != null) {
            if (l.getResource() != null) {
                resolveCollections(l.getResource());
                l.getResource().setStore(resolve(l.getResource().getStore(), cat, lookupByName));
            }
        }
        return resolveCollections(l);
    }

    static <T> T resolveCollections(T object) {
        if (object != null) {
            OwsUtils.resolveCollections(object);
        }
        return object;
    }
}
