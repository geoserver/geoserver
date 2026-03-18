/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import org.geoserver.config.impl.ServiceInfoImpl;

public class WMTSServiceInfoImpl extends ServiceInfoImpl {
    @Override
    public String getType() {
        return "WMTS";
    }
}
