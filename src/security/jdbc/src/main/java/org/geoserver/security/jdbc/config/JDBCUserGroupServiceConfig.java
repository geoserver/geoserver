/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc.config;

import org.geoserver.security.config.SecurityUserGroupServiceConfig;

public class JDBCUserGroupServiceConfig extends JDBCSecurityServiceConfig
        implements SecurityUserGroupServiceConfig {

    private static final long serialVersionUID = 1L;
    protected String passwordEncoderName;
    protected String passwordPolicyName;

    public JDBCUserGroupServiceConfig() {
        super();
    }

    public JDBCUserGroupServiceConfig(JDBCUserGroupServiceConfig other) {
        super(other);
        passwordEncoderName = other.getPasswordEncoderName();
        passwordPolicyName = other.getPasswordPolicyName();
    }

    public String getPasswordPolicyName() {
        return passwordPolicyName;
    }

    public void setPasswordPolicyName(String passwordPolicyName) {
        this.passwordPolicyName = passwordPolicyName;
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public void setPasswordEncoderName(String name) {
        passwordEncoderName = name;
    }

    @Override
    protected String defaultDDLFilename() {
        return "usersddl.xml";
    }

    @Override
    protected String defaultDDLFilenameMySQL() {
        return "usersddl.mysql.xml";
    }

    @Override
    protected String defaultDMLFilename() {
        return "usersdml.xml";
    }

    @Override
    protected String defaultDMLFilenameMySQL() {
        return defaultDMLFilename();
    }
}
