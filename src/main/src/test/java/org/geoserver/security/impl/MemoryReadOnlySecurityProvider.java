/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.validation.SecurityConfigValidator;

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

public class MemoryReadOnlySecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("memoryreadonlygroupservice", MemoryUserGroupServiceConfigImpl.class);
        xp.getXStream().alias("memoryreadonlyroleservice", MemoryRoleServiceConfigImpl.class);
    }

    @Override
    public Map<Class<?>, Set<String>> getFieldsForEncryption() {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        Set<String> fields = new HashSet<>();
        fields.add("toBeEncrypted");
        map.put(MemoryRoleServiceConfigImpl.class, fields);
        map.put(MemoryUserGroupServiceConfigImpl.class, fields);
        return map;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return ReadOnlyUGService.class;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config) throws IOException {
        return new ReadOnlyUGService();
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return ReadOnlyRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config) throws IOException {
        return new ReadOnlyRoleService();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(GeoServerSecurityManager securityManager) {
        return new MemorySecurityConfigValidator(securityManager);
    }
}
