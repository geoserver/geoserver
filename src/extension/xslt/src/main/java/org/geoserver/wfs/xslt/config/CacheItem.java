/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import java.io.File;

/**
 * A cache item for a resource loaded from a file. Helps checking if the cached item is up to date,
 * and avoids flooding the file system with excessive IO requests by preventing the up-to-date check
 * to be hitting the file system too often (at most once per second)
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 * @param <T>
 */
class CacheItem<T extends Object> {

    static final long MIN_INTERVALS_CHECK = 1000;

    T item;

    long lastModified;

    long lastChecked;

    public CacheItem(T item, File sourceFile) {
        this.item = item;
        this.lastModified = sourceFile.lastModified();
    }

    public T getItem() {
        return item;
    }

    public boolean isUpToDate(File file) {
        long now = System.currentTimeMillis();
        if (now - lastChecked < MIN_INTERVALS_CHECK) {
            return true;
        } else {
            lastChecked = now;
            long actualLastModified = file.lastModified();
            return actualLastModified == lastModified;
        }
    }
}
