/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

/**
 * Panel for providing list of user group service configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupServicesPanel
        extends SecurityNamedServicesPanel<SecurityUserGroupServiceConfig> {

    public UserGroupServicesPanel(String id) {
        super(id, new UserGroupServiceProvider());
    }

    @Override
    protected Class getServiceClass() {
        return GeoServerUserGroupService.class;
    }

    @Override
    protected void validateRemoveConfig(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(
                        GeoServerUserGroupService.class, config.getClassName())
                .validateRemoveUserGroupService(config);
    }

    @Override
    protected void removeConfig(SecurityUserGroupServiceConfig config) throws Exception {
        getSecurityManager().removeUserGroupService(config);
    }
}
