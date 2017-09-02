/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import org.geoserver.status.monitoring.rest.Infos;

public class BaseSystemInfoCollector implements SystemInfoCollector {

    public final Infos retriveAllSystemInfo() {
        Infos infos = new Infos();
        for (SystemInfoProperty sip : SystemInfoProperty.values()) {
            infos.addData(retriveSystemInfo(sip));
        }
        return infos;
    }

    SystemInfoProperty retriveSystemInfo(SystemInfoProperty systemInfo) {
        systemInfo.getValues().clear();
        systemInfo.setAvailable(false);
        return systemInfo;
    }

}
