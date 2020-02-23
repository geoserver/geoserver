/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Objects;

/**
 * Cache configuration object for WMS remote styles. Allows enabling the cache and setting the size,
 * in terms of entries and single entry size.
 *
 * @author maurobartolomeoli@gmail.com
 */
public class CacheConfiguration implements Cloneable {

    private boolean enabled;
    private int maxEntries = 1000;
    private long maxEntrySize = 50 * 1024;

    public CacheConfiguration() {
        super();
    }

    public CacheConfiguration(boolean enabled) {
        super();
        this.enabled = enabled;
    }

    public CacheConfiguration(boolean enabled, int maxEntries, long maxEntrySize) {
        super();
        this.enabled = enabled;
        this.maxEntries = maxEntries;
        this.maxEntrySize = maxEntrySize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Enables / disables caching. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    /** Sets max # of entries in cache. */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public long getMaxEntrySize() {
        return maxEntrySize;
    }

    /** Sets max entry size in bytes. */
    public void setMaxEntrySize(long maxEntrySize) {
        this.maxEntrySize = maxEntrySize;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CacheConfiguration) {
            CacheConfiguration other = (CacheConfiguration) obj;
            return other.enabled == enabled
                    && other.maxEntries == maxEntries
                    && other.maxEntrySize == maxEntrySize;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxEntries, maxEntrySize);
    }

    public Object clone() {
        return new CacheConfiguration(enabled, maxEntries, maxEntrySize);
    }
}
