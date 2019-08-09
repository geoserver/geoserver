/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;

/**
 * Configuration for the {@linkplain WebServiceBodyResponseUserGroupService}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class WebServiceBodyResponseUserGroupServiceConfig extends BaseSecurityNamedServiceConfig
        implements SecurityUserGroupServiceConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = 4071134289430150933L;

    public WebServiceBodyResponseUserGroupServiceConfig() {}

    public WebServiceBodyResponseUserGroupServiceConfig(
            WebServiceBodyResponseUserGroupServiceConfig other) {
        super(other);
    }

    String passwordEncoderName;

    String passwordPolicyName;

    private String searchRoles;

    private String availableGroups;

    private String roleServiceName;

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public void setPasswordEncoderName(String passwordEncoderName) {
        this.passwordEncoderName = passwordEncoderName;
    }

    @Override
    public String getPasswordPolicyName() {
        return passwordPolicyName;
    }

    @Override
    public void setPasswordPolicyName(String passwordPolicyName) {
        this.passwordPolicyName = passwordPolicyName;
    }

    /**
     * Regular expression, used to extract the roles name from the webservice response
     *
     * @return the searchRoles
     */
    public String getSearchRoles() {
        return searchRoles;
    }

    /**
     * Regular expression, used to extract the roles name from the webservice response
     *
     * @param searchRoles the searchRoles to set
     */
    public void setSearchRoles(String searchRoles) {
        this.searchRoles = searchRoles;
    }

    /**
     * Optional static comma-separated list of available Groups from the webservice response. They
     * must be in the form
     *
     * <pre>"GROUP_&lt;ROLENAME 1&gt;, ..., GROUP_&lt;ROLENAME N&gt;"</pre>
     *
     * , where
     *
     * <pre>ROLE_&lt;ROLENAME 1&gt;, ..., ROLE_&lt;ROLENAME N&gt;</pre>
     *
     * represent all the possible Roles returned by the Web Service.
     *
     * @return the availableGroups
     */
    public String getAvailableGroups() {
        return availableGroups;
    }

    /**
     * Optional static comma-separated list of available Groups from the webservice response. They
     * must be in the form
     *
     * <pre>"GROUP_&lt;ROLENAME 1&gt;, ..., GROUP_&lt;ROLENAME N&gt;"</pre>
     *
     * , where
     *
     * <pre>ROLE_&lt;ROLENAME 1&gt;, ..., ROLE_&lt;ROLENAME N&gt;</pre>
     *
     * represent all the possible Roles returned by the Web Service.
     *
     * @param availableGroups the availableGroups to set
     */
    public void setAvailableGroups(String availableGroups) {
        this.availableGroups = availableGroups;
    }

    /**
     * Optional name of the Role Service to use for Roles resolution. If null it will use the
     * Security Default Active Service.
     *
     * @return the roleServiceName
     */
    public String getRoleServiceName() {
        return roleServiceName;
    }

    /**
     * Optional name of the Role Service to use for Roles resolution. If null it will use the
     * Security Default Active Service.
     *
     * @param roleServiceName the roleServiceName to set
     */
    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }
}
