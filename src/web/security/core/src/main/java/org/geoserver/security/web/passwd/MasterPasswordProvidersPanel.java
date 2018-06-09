/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

/**
 * Panel for providing list of master password provider configurations..
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProvidersPanel
        extends SecurityNamedServicesPanel<MasterPasswordProviderConfig> {

    public MasterPasswordProvidersPanel(String id) {
        super(id, new MasterPasswordProviderProvider());
    }

    @Override
    protected Class getServiceClass() {
        return MasterPasswordProvider.class;
    }

    @Override
    protected void validateRemoveConfig(MasterPasswordProviderConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(
                        MasterPasswordProvider.class, config.getClassName())
                .validateRemoveMasterPasswordProvider(config);
    }

    @Override
    protected void removeConfig(MasterPasswordProviderConfig config) throws Exception {
        getSecurityManager().removeMasterPasswordProvder(config);
    }
}
