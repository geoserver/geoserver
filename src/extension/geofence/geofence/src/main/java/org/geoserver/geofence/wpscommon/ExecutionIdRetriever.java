/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.wpscommon;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public interface ExecutionIdRetriever {

    String getCurrentExecutionId();
}
