/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Watches a files last modified date to determine when a file has been changed.
 * <p>
 * Client code using this class should call {@link #isModified()} to determine if the file
 * has changed since the last check, and {@link #read()} to read the contents of the file
 * and update the last check timestamp.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class FileWatcher<T> {

    private final File file;
    private long lastModified = Long.MIN_VALUE;
    private long lastCheck;
    private boolean stale;

    public FileWatcher(final File file) {
        this.file = file;
    }
    
    public final File getFile() {
        return file;
    }
    
    /**
     * Reads the file updating the last check timestamp.
     * <p>
     * Subclasses can override {@link #parseFileContents(FileInputStream)} to do something 
     * when the file is read.
     * </p>
     */
    public T read() throws IOException {
        T result = null;
        
        if (file != null && file.exists()) {
            InputStream is = null;

            try {
                is = new FileInputStream(file);
                result = parseFileContents(is);
                
                lastModified = file.lastModified();
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
     * <p>
     * Subclasses should override.
     * </p>
     */
    protected T parseFileContents(final InputStream in) throws IOException {
        return null;
    }
    
    /**
     * Determines if the underlying file has been modified since the last check.
     */
    public final boolean isModified() {
        final long now = System.currentTimeMillis();
        if((now - lastCheck) > 1000) {
            lastCheck = now;
            stale = file != null && file.exists() && (file.lastModified() != lastModified);
        }
        return stale;
    }
    
    /**
     * Method to set the last modified time stamp.
     * Clients synchronized with the actual file
     * content and knowing the last modified time stamp
     * can avoid unnecessary reload operations 
     * 
     * @param lastModified
     */
    public final void setKnownLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

}
