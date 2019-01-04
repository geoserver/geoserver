/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

public interface FileReference {

    FileService getService();

    String getLatestVersion();

    String getNextVersion();
}
