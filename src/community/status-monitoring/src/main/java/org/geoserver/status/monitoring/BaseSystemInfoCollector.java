/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring;

public class BaseSystemInfoCollector implements SystemInfoCollector {

    @Override
    public String retriveSystemInfo(SystemInfoProperty systemInfo) {
        String value = "NOT AVAILABLE";
        switch (systemInfo) {
        case FAN_SPEED:
            break;
        case GEOSERVER_CPU_USAGE:
            break;
        case GEOSERVER_JVM_MEMORY_USAGE:
            break;
        case LOGICAL_CPU_N:
            break;
        case MOUNTED_FS_USAGE:
            break;
        case NETWORK_INTERFACES:
            break;
        case NETWORK_INTERFACES_USAGE:
            break;
        case OS_TYPE:
            break;
        case OS_VERSION:
            break;
        case PER_CPU_USAGE:
            break;
        case PHYSICAL_CPU_N:
            break;
        case RUNNING_PROCESS_N:
            break;
        case RUNNING_THREADS_N:
            break;
        case SYSTEM_AVERAGE_LOAD:
            break;
        case SYSTEM_MEMORY_USAGE:
            break;
        case TEMPERATURE:
            break;
        case UPTIME:
            break;
        case VOLTAGE:
            break;
        default:
            break;
        }
        return value;
    }

}
