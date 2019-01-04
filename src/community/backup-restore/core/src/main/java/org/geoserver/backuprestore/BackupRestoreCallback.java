/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

/** @author Alessio Fabiani, GeoSolutions */
public interface BackupRestoreCallback {

    public void onBeginRequest(String requestedType);

    public void onEndRequest();
}
