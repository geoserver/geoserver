/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Provider for JDBC based security services.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("jdbcusergroupservice", JDBCUserGroupServiceConfig.class);
        xp.getXStream().alias("jdbcroleservice", JDBCRoleServiceConfig.class);
    }

    @Override
    public Map<Class<?>, Set<String>> getFieldsForEncryption() {
        Map<Class<?>, Set<String>> map = new HashMap<Class<?>, Set<String>>();

        Set<String> fields = new HashSet<String>();
        fields.add("password");
        map.put(JDBCSecurityServiceConfig.class, fields);
        return map;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return JDBCUserGroupService.class;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new JDBCUserGroupService();
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return JDBCRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new JDBCRoleService();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new JdbcSecurityConfigValidator(securityManager);
    }

    @Override
    public Class<JDBCConnectAuthProvider> getAuthenticationProviderClass() {
        return JDBCConnectAuthProvider.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        return new JDBCConnectAuthProvider();
    }
}
