/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.external.FileService;

public class FileReferenceImpl implements FileReference {

    private FileService service;

    private String latestVersion;

    private String nextVersion;

    public FileReferenceImpl(FileService service, String latestVersion, String nextVersion) {
        this.service = service;
        this.latestVersion = latestVersion;
        this.nextVersion = nextVersion;
    }

    @Override
    public String getLatestVersion() {
        return latestVersion;
    }

    @Override
    public String getNextVersion() {
        return nextVersion;
    }

    @Override
    public FileService getService() {
        return service;
    }
}
