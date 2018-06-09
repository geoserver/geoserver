/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Watches a files last modified date to determine when a file has been changed.
 *
 * <p>Client code using this class should call {@link #isModified()} to determine if the file has
 * changed since the last check, and {@link #read()} to read the contents of the file and update the
 * last check timestamp.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FileWatcher<T> {
    protected Resource resource;
    private long lastModified = Long.MIN_VALUE;
    private long lastCheck;
    private boolean stale;

    public FileWatcher(Resource resource) {
        this.resource = resource;
    }

    public FileWatcher(File file) {
        this.resource = Files.asResource(file);
    }

    public File getFile() {
        return resource.file();
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Reads the file updating the last check timestamp.
     *
     * <p>Subclasses can override {@link #parseFileContents(InputStream)} to do something when the
     * file is read.
     *
     * @return parsed file contents
     */
    public T read() throws IOException {
        T result = null;

        if (resource.getType() == Type.RESOURCE) {
            InputStream is = null;

            try {
                is = resource.in();
                result = parseFileContents(is);

                lastModified = resource.lastmodified();
                lastCheck = System.currentTimeMillis();
                stale = false;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        return result;
    }

    /**
     * Parses the contents of the file being watched.
     *
     * <p>Subclasses should override.
     */
    protected T parseFileContents(InputStream in) throws IOException {
        return null;
    }

    /** Determines if the underlying file has been modified since the last check. */
    public boolean isModified() {
        long now = System.currentTimeMillis();
        if ((now - lastCheck) > 1000) {
            lastCheck = now;
            stale =
                    (resource.getType() != Type.UNDEFINED)
                            && (resource.lastmodified() != lastModified);
        }
        return stale;
    }

    /**
     * Method to set the last modified time stamp. Clients synchronized with the actual file content
     * and knowing the last modified time stamp can avoid unnecessary reload operations
     *
     * @param lastModified last modified time
     */
    public void setKnownLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
