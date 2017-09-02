/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public enum SystemInfoProperty {
    OS_TYPE("OS type"),  
    //PHYSICAL_CPU_N( "Number of physical CPU"), 
    //LOGICAL_CPU_N("Number of logical CPU"), 
    //UPTIME("Uptime"), 
    //RUNNING_PROCESS_N("Number of running process"), 
    //RUNNING_THREADS_N("Number of running threads"), 
    //SYSTEM_AVERAGE_LOAD("System average load"), 
    CPU_USAGE("CPU loads");
    /*
    GEOSERVER_CPU_USAGE("Geoserver CPU usage"), 
    SYSTEM_MEMORY_USAGE("System memory usage"), 
    GEOSERVER_JVM_MEMORY_USAGE("Geoserver JVM memory usage"), 
    MOUNTED_FS_USAGE("Mounted file system usage"), 
    NETWORK_INTERFACES("Network interfaces"), 
    NETWORK_INTERFACES_USAGE("Network interfaces usage"), 
    TEMPERATURE("Temperature"), 
    FAN_SPEED("Fan speed"), 
    VOLTAGE("Voltage");*/

    String description;

    List<SystemPropertyValue> values = new ArrayList<SystemPropertyValue>(0);
    
    Boolean available;

    SystemInfoProperty(String description) {
        this.description = description;
        this.available = false;
    }

    public String getDescription() {
        return description;
    }
    
    public List<SystemPropertyValue> getValues() {
        return values;
    }

    public void addValue(SystemPropertyValue value) {
        this.values.add(value);
    }

    public Boolean getAvailable() {
        return available;
    }
    
    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
