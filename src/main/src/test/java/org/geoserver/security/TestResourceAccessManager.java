/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.security.core.Authentication;

/**
 * A simple {@link ResourceAccessManager} that keeps all the limits in a in memory hash map. Useful for testing purposes
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TestResourceAccessManager extends AbstractResourceAccessManager {

    Map<String, Map<String, AccessLimits>> limits = new HashMap<>();

    WorkspaceAccessLimits defaultWorkspaceAccessLimits = null;

    public WorkspaceAccessLimits getDefaultWorkspaceAccessLimits() {
        return defaultWorkspaceAccessLimits;
    }

    public void setDefaultWorkspaceAccessLimits(WorkspaceAccessLimits defaultWorkspaceAccessLimits) {
        this.defaultWorkspaceAccessLimits = defaultWorkspaceAccessLimits;
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        WorkspaceAccessLimits wal = (WorkspaceAccessLimits) getUserMap(name).get(workspace.getId());
        if (wal != null) {
            return wal;
        } else {
            return defaultWorkspaceAccessLimits;
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        DataAccessLimits limits = (DataAccessLimits) getUserMap(name).get(layer.getId());
        if (limits == null) {
            limits = getAccessLimits(user, layer.getResource());
        }
        return limits;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        if (user == null) {
            return null;
        }
        // container-specific limits win: a rule may restrict a layer only when reached through a given group
        Map<String, AccessLimits> userMap = getUserMap(user.getName());
        for (LayerGroupInfo container : containers) {
            DataAccessLimits limits = (DataAccessLimits) userMap.get(containerKey(layer, container));
            if (limits != null) {
                return limits;
            }
        }
        return getAccessLimits(user, layer);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        return (DataAccessLimits) getUserMap(name).get(resource.getId());
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        return (StyleAccessLimits) getUserMap(name).get(style.getId());
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        return (LayerGroupAccessLimits) getUserMap(name).get(layerGroup.getId());
    }

    /**
     * Saves the mock access limits for this user and secured item (this is meant only for testing, it's the caller care
     * to make sure the appropriate user limits class is used). The CatalogInfo is required to have a valid and stable
     * id.
     */
    public void putLimits(String userName, CatalogInfo securedItem, AccessLimits limits) {
        getUserMap(userName).put(securedItem.getId(), limits);
    }

    /** Limits applied to {@code layer} only when it is accessed through {@code container} (directly or nested). */
    public void putLimits(String userName, LayerInfo layer, LayerGroupInfo container, DataAccessLimits limits) {
        getUserMap(userName).put(containerKey(layer, container), limits);
    }

    private static String containerKey(LayerInfo layer, LayerGroupInfo container) {
        return layer.getId() + "@" + container.getId();
    }

    public void clearLimits() {
        limits.clear();
    }

    Map<String, AccessLimits> getUserMap(String userName) {
        Map<String, AccessLimits> userMap = limits.get(userName);
        if (userMap == null) {
            userMap = new HashMap<>();
            limits.put(userName, userMap);
        }
        return userMap;
    }
}
