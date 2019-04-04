/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

/**
 * Panel for providing list of role service configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RoleServicesPanel extends SecurityNamedServicesPanel<SecurityRoleServiceConfig> {

    public RoleServicesPanel(String id) {
        super(id, new RoleServiceProvider());
    }

    @Override
    protected Class getServiceClass() {
        return GeoServerRoleService.class;
    }

    @Override
    protected void validateRemoveConfig(SecurityRoleServiceConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(
                        GeoServerRoleService.class, config.getClassName())
                .validateRemoveRoleService(config);
    }

    @Override
    protected void removeConfig(SecurityRoleServiceConfig config) throws Exception {
        getSecurityManager().removeRoleService(config);
    }
}
