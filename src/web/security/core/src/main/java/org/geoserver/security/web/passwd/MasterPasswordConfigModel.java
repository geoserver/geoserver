/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * Model for the main {@link MasterPasswordConfig} configuration.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordConfigModel extends LoadableDetachableModel<MasterPasswordConfig> {

    @Override
    protected MasterPasswordConfig load() {
        return GeoServerApplication.get().getSecurityManager().getMasterPasswordConfig();
    }
}
