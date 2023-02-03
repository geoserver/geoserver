/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.wps;

import org.geoserver.geofence.wpscommon.ExecutionIdRetriever;
import org.geoserver.wps.resource.WPSResourceManager;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class DefaultExecutionIdRetriever implements ExecutionIdRetriever {
    WPSResourceManager wpsManager;

    public DefaultExecutionIdRetriever(WPSResourceManager wpsManager) {
        this.wpsManager = wpsManager;
    }

    @Override
    public String getCurrentExecutionId() {
        return wpsManager.getCurrentExecutionId();
    }
}
