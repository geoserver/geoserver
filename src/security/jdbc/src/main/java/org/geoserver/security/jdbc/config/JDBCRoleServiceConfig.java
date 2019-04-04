/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc.config;

import org.geoserver.security.config.SecurityRoleServiceConfig;

public class JDBCRoleServiceConfig extends JDBCSecurityServiceConfig
        implements SecurityRoleServiceConfig {

    private static final long serialVersionUID = 1L;

    protected String adminRoleName;
    protected String groupAdminRoleName;

    public JDBCRoleServiceConfig() {
        super();
    }

    public JDBCRoleServiceConfig(JDBCRoleServiceConfig other) {
        super(other);
        adminRoleName = other.getAdminRoleName();
        groupAdminRoleName = other.getGroupAdminRoleName();
    }

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String name) {
        adminRoleName = name;
    }

    @Override
    protected String defaultDDLFilename() {
        return "rolesddl.xml";
    }

    @Override
    protected String defaultDDLFilenameMySQL() {
        return "rolesddl.mysql.xml";
    }

    @Override
    protected String defaultDMLFilename() {
        return "rolesdml.xml";
    }

    @Override
    protected String defaultDMLFilenameMySQL() {
        return defaultDMLFilename();
    }

    public String getGroupAdminRoleName() {
        return groupAdminRoleName;
    }

    public void setGroupAdminRoleName(String groupAdminRoleName) {
        this.groupAdminRoleName = groupAdminRoleName;
    }
}
