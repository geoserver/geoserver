/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import org.geoserver.config.ServiceFactoryExtension;

public class WMTSFactoryExtension extends ServiceFactoryExtension<WMTSInfo> {

    protected WMTSFactoryExtension() {
        super(WMTSInfo.class);
    }

    @Override
    public <T> T create(Class<T> clazz) {
        return (T) new WMTSInfoImpl();
    }
}
