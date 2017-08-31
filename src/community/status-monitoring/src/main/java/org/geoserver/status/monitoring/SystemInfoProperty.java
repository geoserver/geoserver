/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring;

public enum SystemInfoProperty {
    OS_TYPE("OS type"), 
    OS_VERSION("OS version"), 
    PHYSICAL_CPU_N( "Number of physical CPU"), 
    LOGICAL_CPU_N("Number of logical CPU"), 
    UPTIME("Uptime"), 
    RUNNING_PROCESS_N("Number of running process"), 
    RUNNING_THREADS_N("Number of running threads"), 
    SYSTEM_AVERAGE_LOAD("System average load"), 
    PER_CPU_USAGE("CPU loads"), 
    GEOSERVER_CPU_USAGE("Geoserver CPU usage"), 
    SYSTEM_MEMORY_USAGE("System memory usage"), 
    GEOSERVER_JVM_MEMORY_USAGE("Geoserver JVM memory usage"), 
    MOUNTED_FS_USAGE("Mounted file system usage"), 
    NETWORK_INTERFACES("Network interfaces"), 
    NETWORK_INTERFACES_USAGE("Network interfaces usage"), 
    TEMPERATURE("Temperature"), 
    FAN_SPEED("Fan speed"), 
    VOLTAGE("Voltage");

    String description;

    String value;

    SystemInfoProperty(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
