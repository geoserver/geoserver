/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.config.ServiceFactoryExtension;

public class WPSFactoryExtension extends ServiceFactoryExtension<WPSInfo> {

    protected WPSFactoryExtension() {
        super(WPSInfo.class);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        return (T) new WPSInfoImpl();
    }
}
