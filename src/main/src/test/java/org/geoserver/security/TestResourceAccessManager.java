/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.*;
import org.springframework.security.core.Authentication;

/**
 * A simple {@link ResourceAccessManager} that keeps all the limits in a in memory hash map. Useful
 * for testing purposes
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TestResourceAccessManager extends AbstractResourceAccessManager {

    Map<String, Map<String, AccessLimits>> limits =
            new HashMap<String, Map<String, AccessLimits>>();

    WorkspaceAccessLimits defaultWorkspaceAccessLimits = null;

    public WorkspaceAccessLimits getDefaultWorkspaceAccessLimits() {
        return defaultWorkspaceAccessLimits;
    }

    public void setDefaultWorkspaceAccessLimits(
            WorkspaceAccessLimits defaultWorkspaceAccessLimits) {
        this.defaultWorkspaceAccessLimits = defaultWorkspaceAccessLimits;
    }

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

    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (user == null) {
            return null;
        }

        final String name = user.getName();
        return (DataAccessLimits) getUserMap(name).get(resource.getId());
    }

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
     * Saves the mock access limits for this user and secured item (this is meant only for testing,
     * it's the caller care to make sure the appropriate user limits class is used). The CatalogInfo
     * is required to have a valid and stable id.
     */
    public void putLimits(String userName, CatalogInfo securedItem, AccessLimits limits) {
        getUserMap(userName).put(securedItem.getId(), limits);
    }

    Map<String, AccessLimits> getUserMap(String userName) {
        Map<String, AccessLimits> userMap = limits.get(userName);
        if (userMap == null) {
            userMap = new HashMap<String, AccessLimits>();
            limits.put(userName, userMap);
        }
        return userMap;
    }
}
