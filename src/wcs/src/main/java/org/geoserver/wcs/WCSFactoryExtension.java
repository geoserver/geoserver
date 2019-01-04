/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import org.geoserver.config.ServiceFactoryExtension;

public class WCSFactoryExtension extends ServiceFactoryExtension<WCSInfo> {

    public WCSFactoryExtension() {
        super(WCSInfo.class);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        return (T) new WCSInfoImpl();
    }
}
