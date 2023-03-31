/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

/**
 * To implements to retrieve system information metrics using low level API.
 *
 * <p>Base implementation to extends is provided by {@link BaseSystemInfoCollector}
 *
 * @author sandr
 */
public interface SystemInfoCollector {

    boolean ENABLED = true;
    boolean DISABLED = false;

    /** @return the list of metric */
    Metrics retrieveAllSystemInfo();

    void setStatisticsStatus(Boolean status);

    Boolean getStatisticsStatus();
}
