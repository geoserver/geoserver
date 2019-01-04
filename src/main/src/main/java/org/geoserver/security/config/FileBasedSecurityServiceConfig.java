/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * Security service configuration object that is persisted in a file.
 *
 * @author christian
 */
public class FileBasedSecurityServiceConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;
    private String fileName;
    private long checkInterval;

    public FileBasedSecurityServiceConfig() {}

    public FileBasedSecurityServiceConfig(FileBasedSecurityServiceConfig other) {
        super(other);
        fileName = other.getFileName();
        checkInterval = other.getCheckInterval();
    }

    /** @return The name of file to persist configuration in. */
    public String getFileName() {
        return fileName;
    }

    /** Sets the name of file to persist configuration in. */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * The time interval, in milliseconds, in which to check the underlying file for changes.
     *
     * @return the check interval in ms.
     * @see #setCheckInterval(long)
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * Sets the time interval, in milliseconds, in which to check the underlying file for changes.
     *
     * <p>This property is typically used in environments (such as a cluster) in which the
     * underlying file may have been modified out of process.
     *
     * <p>A value of > 0 causes {@link FileWatcher} object to be created. A value of <= 0 disables
     * any checking of the underlying file.
     *
     * <p>Hint: the granularity of {@link File} last access time is often a second, values < 1000
     * may not have the desired effect.
     *
     * @param checkInterval The time interval in ms.
     */
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }
}
