/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

/** This enum defines the system metrics that can be retrieved by a collector. */
public enum MetricInfo {

    // system metrics
    OPERATING_SYSTEM("SYSTEM", 1, "Operating system"),
    SYSTEM_UPTIME("SYSTEM", 2, "Uptime", "sec"),
    SYSTEM_AVERAGE_LOAD_1("SYSTEM", 3, "System average load 1 minute", ""),
    SYSTEM_AVERAGE_LOAD_5("SYSTEM", 3, "System average load 5 minutes", ""),
    SYSTEM_AVERAGE_LOAD_15("SYSTEM", 3, "System average load 15 minutes", ""),
    // cpu metrics
    PHYSICAL_CPUS("CPU", 100, "Number of physical CPUs"),
    LOGICAL_CPUS("CPU", 101, "Number of logical CPUs"),
    RUNNING_PROCESS("CPU", 102, "Number of running process"),
    RUNNING_THREADS("CPU", 103, "Number of running threads"),
    CPU_LOAD("CPU", 104, "CPU average load", "%"),
    PER_CPU_LOAD("CPU", 105, "CPU load", "%"),
    // memory metrics
    MEMORY_USED("MEMORY", 200, "Used physical memory ", "%"),
    MEMORY_TOTAL("MEMORY", 201, "Total physical memory ", "bytes"),
    MEMORY_FREE("MEMORY", 201, "Free physical memory", "bytes"),
    // swap metrics
    SWAP_USED("SWAP", 300, "Used swap memory", "%"),
    SWAP_TOTAL("SWAP", 301, "Total swap memory", "bytes"),
    SWAP_FREE("SWAP", 302, "Free swap memory", "bytes"),
    // file system metrics
    FILE_SYSTEM_TOTAL_USAGE("FILE_SYSTEM", 400, "File system usage", "%"),
    PARTITION_USED("FILE_SYSTEM", 500, "Partition space used", "%"),
    PARTITION_TOTAL("FILE_SYSTEM", 501, "Partition total space", "bytes"),
    PARTITION_FREE("FILE_SYSTEM", 502, "Partition free space", "bytes"),
    // network metrics
    NETWORK_INTERFACES_SEND("NETWORK", 800, "Network interfaces send", "bytes"),
    NETWORK_INTERFACES_RECEIVED("NETWORK", 801, "Network interfaces received", "bytes"),
    NETWORK_INTERFACE_SEND("NETWORK", 900, "Network interface band usage", "bytes"),
    NETWORK_INTERFACE_RECEIVED("NETWORK", 901, "Network interface available band", "bytes"),
    // sensors metrics
    TEMPERATURE("SENSORS", 1200, "CPU temperature", "°C"),
    VOLTAGE("SENSORS", 1201, "CPU voltage", "V"),
    FAN_SPEED("SENSORS", 1202, "Fan speed", "rpm"),
    // geoserver metrics
    GEOSERVER_CPU_USAGE("GEOSERVER", 1300, "GeoServer CPU usage", "%"),
    GEOSERVER_THREADS("GEOSERVER", 1301, "GeoServer threads"),
    GEOSERVER_JVM_MEMORY_USAGE("GEOSERVER", 1302, "GeoServer JVM memory usage", "%");

    private String category;
    private int priority;
    private String description;
    private String unit;

    MetricInfo(String category, int priority, String description) {
        this(category, priority, description, "");
    }

    MetricInfo(String category, int priority, String description, String unit) {
        this.description = description;
        this.unit = unit;
        this.category = category;
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public int getPriority() {
        return priority;
    }
}
