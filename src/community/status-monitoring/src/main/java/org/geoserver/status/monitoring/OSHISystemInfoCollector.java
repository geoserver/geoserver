/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

public class OSHISystemInfoCollector extends BaseSystemInfoCollector {

    private SystemInfo si;

    private OperatingSystem os;

    public OSHISystemInfoCollector() {
        si = new SystemInfo();
        os = si.getOperatingSystem();
    }

    @Override
    public String retriveSystemInfo(org.geoserver.status.monitoring.SystemInfoProperty systemInfo) {
        String value = super.retriveSystemInfo(systemInfo);
        switch (systemInfo) {
        case OS_TYPE:
            value = os.getFamily();
            break;
        case OS_VERSION:
            value = os.getVersion().getVersion();
            break;
        default:
            break;
        }
        return value;
    }

}
