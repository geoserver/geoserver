/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object for GeofenceAccessManager.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class GeoFenceConfiguration implements Serializable, Cloneable {

    public static final String URL_INTERNAL = "internal:/";

    private static final long serialVersionUID = 3L;

    private String servicesUrl;

    private String instanceName;

    private boolean allowRemoteAndInlineLayers;

    private boolean grantWriteToWorkspacesToAuthenticatedUsers;

    private boolean useRolesToFilter;

    private String acceptedRoles = "";

    private List<String> roles = new ArrayList<String>();

    private String gwcContextSuffix;

    private String defaultUserGroupServiceName;

    /** Remote GeoFence services url. */
    public String getServicesUrl() {
        return servicesUrl;
    }

    /** Remote GeoFence services url. */
    public void setServicesUrl(String servicesUrl) {
        this.servicesUrl = servicesUrl;
    }

    /**
     * Name of this GeoServer instance for GeoFence rule configuration.
     *
     * @param instanceName the instanceName to set
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Name of this GeoServer instance for GeoFence rule configuration.
     *
     * @return the instanceName
     */
    public String getInstanceName() {
        return instanceName;
    }

    /** Flag to allow usage of remote and inline layers in SLDs. */
    public void setAllowRemoteAndInlineLayers(boolean allowRemoteAndInlineLayers) {
        this.allowRemoteAndInlineLayers = allowRemoteAndInlineLayers;
    }

    /** Flag to allow usage of remote and inline layers in SLDs. */
    public boolean isAllowRemoteAndInlineLayers() {
        return allowRemoteAndInlineLayers;
    }

    /**
     * Allows write access to resources to authenticated users, if false only ADMINs have write
     * access.
     *
     * @return the grantWriteToWorkspacesToAuthenticatedUsers
     */
    public boolean isGrantWriteToWorkspacesToAuthenticatedUsers() {
        return grantWriteToWorkspacesToAuthenticatedUsers;
    }

    /**
     * Allows write access to resources to authenticated users, if false only ADMINs have write
     * access.
     *
     * @param grantWriteToWorkspacesToAuthenticatedUsers the
     *     grantWriteToWorkspacesToAuthenticatedUsers to set
     */
    public void setGrantWriteToWorkspacesToAuthenticatedUsers(
            boolean grantWriteToWorkspacesToAuthenticatedUsers) {
        this.grantWriteToWorkspacesToAuthenticatedUsers =
                grantWriteToWorkspacesToAuthenticatedUsers;
    }

    /**
     * Use authenticated users roles to match rules, instead of username.
     *
     * @return the useRolesToFilter
     */
    public boolean isUseRolesToFilter() {
        return useRolesToFilter;
    }

    /**
     * Use authenticated users roles to match rules, instead of username.
     *
     * @param useRolesToFilter the useRolesToFilter to set
     */
    public void setUseRolesToFilter(boolean useRolesToFilter) {
        this.useRolesToFilter = useRolesToFilter;
    }

    /**
     * List of mutually exclusive roles used for rule matching when useRolesToFilter is true.
     *
     * @return the acceptedRoles
     */
    public String getAcceptedRoles() {
        return acceptedRoles;
    }

    /**
     * List of mutually exclusive roles used for rule matching when useRolesToFilter is true.
     *
     * @param acceptedRoles the acceptedRoles to set
     */
    public void setAcceptedRoles(String acceptedRoles) {
        if (acceptedRoles == null) {
            acceptedRoles = "";
        }

        this.acceptedRoles = acceptedRoles;

        // from comma delimited to list
        roles = Lists.newArrayList(acceptedRoles.split(","));
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isInternal() {
        return servicesUrl.startsWith(URL_INTERNAL);
    }

    /** @return */
    public String getGwcContextSuffix() {
        return gwcContextSuffix;
    }

    /** @param gwcContextSuffix the gwcContextSuffix to set */
    public void setGwcContextSuffix(String gwcContextSuffix) {
        this.gwcContextSuffix = gwcContextSuffix;
    }

    /** @param defaultUserGroupServiceName the defaultUserGroupServiceName to set */
    public void setDefaultUserGroupServiceName(String defaultUserGroupServiceName) {
        this.defaultUserGroupServiceName = defaultUserGroupServiceName;
    }

    /** @return */
    public String getDefaultUserGroupServiceName() {
        return defaultUserGroupServiceName;
    }

    /** Creates a copy of the configuration object. */
    @Override
    public GeoFenceConfiguration clone() {
        try {
            GeoFenceConfiguration clone = (GeoFenceConfiguration) super.clone();
            clone.setAcceptedRoles(
                    acceptedRoles); // make sure the computed list is properly initted
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new UnknownError("Unexpected exception: " + ex.getMessage());
        }
    }
}
