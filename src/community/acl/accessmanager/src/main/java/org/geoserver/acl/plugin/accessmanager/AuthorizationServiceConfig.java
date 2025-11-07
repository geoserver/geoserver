/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import java.io.Serializable;

/**
 * Configuration object for {@link AclResourceAccessManager}.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" - Originally as part of GeoFence's GeoServer
 *     extension
 * @author Gabriel Roldan - Camptocamp
 */
@SuppressWarnings("serial")
public class AuthorizationServiceConfig implements Serializable {

    private boolean grantWriteToWorkspacesToAuthenticatedUsers;

    private String serviceUrl;

    public AuthorizationServiceConfig() {
        initDefaults();
    }

    public void initDefaults() {
        grantWriteToWorkspacesToAuthenticatedUsers = false;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    /**
     * Whether to allow write access to resources to authenticated users ({@code true}, if {@code false}, only admins
     * (users with {@literal ROLE_ADMINISTRATOR}) have write access.
     */
    boolean isGrantWriteToWorkspacesToAuthenticatedUsers() {
        return grantWriteToWorkspacesToAuthenticatedUsers;
    }

    /**
     * Whether to allow write access to resources to authenticated users, if false only admins (users with
     * {@literal ROLE_ADMINISTRATOR}) have write access.
     */
    void setGrantWriteToWorkspacesToAuthenticatedUsers(boolean grantWriteToWorkspacesToAuthenticatedUsers) {
        this.grantWriteToWorkspacesToAuthenticatedUsers = grantWriteToWorkspacesToAuthenticatedUsers;
    }
}
