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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((latestVersion == null) ? 0 : latestVersion.hashCode());
        result = prime * result + ((nextVersion == null) ? 0 : nextVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FileReferenceImpl other = (FileReferenceImpl) obj;
        if (latestVersion == null) {
            if (other.latestVersion != null) return false;
        } else if (!nextVersion.equals(other.nextVersion)) return false;
        if (service == null) {
            if (other.service != null) return false;
        } else if (!service.equals(other.service)) return false;
        return true;
    }
}
