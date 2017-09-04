/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

/**
 * 
 * Defines the data associated at each system informations to retrive:
 * <ul>
 * <li>identifier</li>
 * <li>description</li>
 * <li>unit of measurement</li>
 * </ul>
 * 
 * @author sandr
 *
 */
public enum MetricInfo {
    OS_TYPE("OS type"),
    PHYSICAL_CPU_N( "Number of physical CPU"),
    LOGICAL_CPU_N("Number of logical CPU"),
    CPU_LOAD("CPU load", "%"),  
    UPTIME("Uptime","h"),
    SYSTEM_AVERAGE_LOAD("System average load","%"),
    RUNNING_PROCESS_N("Number of running process"),
    RUNNING_THREADS_N("Number of running threads"),
    GEOSERVER_CPU_USAGE("Geoserver CPU usage", "%"), 
    SYSTEM_MEMORY_USAGE_P("Memory physical","MiB"),
    SYSTEM_MEMORY_USAGE_S("Memory swap", "MiB"),
    GEOSERVER_JVM_MEMORY_USAGE("Geoserver JVM memory usage", "%"),
    MOUNTED_FS_USAGE("Mounted file system usage","MiB"), 
    NETWORK_INTERFACES("Network interfaces","MiB"),
    TEMPERATURE("Temperature","°C"),
    FAN_SPEED("Fan speed","rpm"), 
    VOLTAGE("Voltage","V");

    String description;

    String unit;
    
    private MetricInfo(){}

    private MetricInfo(String description) {
        this.description = description;
        this.unit = "";
    }

    private MetricInfo(String description, String unit) {
        this.description = description;
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

}
