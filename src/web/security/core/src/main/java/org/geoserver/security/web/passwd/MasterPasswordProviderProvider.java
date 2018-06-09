/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.SecurityNamedServiceProvider;

/**
 * Data provider for master password provider configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderProvider
        extends SecurityNamedServiceProvider<MasterPasswordProviderConfig> {

    @Override
    protected List<MasterPasswordProviderConfig> getItems() {
        List<MasterPasswordProviderConfig> configs = new ArrayList<MasterPasswordProviderConfig>();
        try {
            for (String name : getSecurityManager().listMasterPasswordProviders()) {
                configs.add(getSecurityManager().loadMasterPassswordProviderConfig(name));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return configs;
    }
}
