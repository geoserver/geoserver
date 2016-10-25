/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class GeoServerRestRoleServiceConfig extends BaseSecurityNamedServiceConfig
        implements SecurityRoleServiceConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = -8380244566532287415L;

    private String adminGroup;

    private String groupAdminGroup;

    private String baseUrl;
    
    private String rolesRESTEndpoint = "/api/roles";
    
    private String adminRoleRESTEndpoint = "/api/adminRole";
    
    private String usersRESTEndpoint = "/api/users";
    
    private String rolesJSONPath = "$.groups";
    
    private String adminRoleJSONPath = "$.adminRole";
    
    private String usersJSONPath = "$.users[0].groups";
    
    @Override
    public String getAdminRoleName() {
        return adminGroup;
    }

    @Override
    public void setAdminRoleName(String adminRoleName) {
        this.adminGroup = adminRoleName;
    }

    @Override
    public String getGroupAdminRoleName() {
        return groupAdminGroup;
    }

    @Override
    public void setGroupAdminRoleName(String adminRoleName) {
        this.groupAdminGroup = adminRoleName;
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl the baseUrl to set
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @return the rolesRESTEndpoint
     */
    public String getRolesRESTEndpoint() {
        return rolesRESTEndpoint;
    }

    /**
     * @param rolesRESTEndpoint the rolesRESTEndpoint to set
     */
    public void setRolesRESTEndpoint(String rolesRESTEndpoint) {
        this.rolesRESTEndpoint = rolesRESTEndpoint;
    }

    /**
     * @return the adminRoleRESTEndpoint
     */
    public String getAdminRoleRESTEndpoint() {
        return adminRoleRESTEndpoint;
    }

    /**
     * @param adminRoleRESTEndpoint the adminRoleRESTEndpoint to set
     */
    public void setAdminRoleRESTEndpoint(String adminRoleRESTEndpoint) {
        this.adminRoleRESTEndpoint = adminRoleRESTEndpoint;
    }

    /**
     * @return the usersRESTEndpoint
     */
    public String getUsersRESTEndpoint() {
        return usersRESTEndpoint;
    }

    /**
     * @param usersRESTEndpoint the usersRESTEndpoint to set
     */
    public void setUsersRESTEndpoint(String usersRESTEndpoint) {
        this.usersRESTEndpoint = usersRESTEndpoint;
    }

    /**
     * @return the rolesJSONPath
     */
    public String getRolesJSONPath() {
        return rolesJSONPath;
    }

    /**
     * @param rolesJSONPath the rolesJSONPath to set
     */
    public void setRolesJSONPath(String rolesJSONPath) {
        this.rolesJSONPath = rolesJSONPath;
    }

    /**
     * @return the adminRoleJSONPath
     */
    public String getAdminRoleJSONPath() {
        return adminRoleJSONPath;
    }

    /**
     * @param adminRoleJSONPath the adminRoleJSONPath to set
     */
    public void setAdminRoleJSONPath(String adminRoleJSONPath) {
        this.adminRoleJSONPath = adminRoleJSONPath;
    }

    /**
     * @return the usersJSONPath
     */
    public String getUsersJSONPath() {
        return usersJSONPath;
    }

    /**
     * @param usersJSONPath the usersJSONPath to set
     */
    public void setUsersJSONPath(String usersJSONPath) {
        this.usersJSONPath = usersJSONPath;
    }

}
