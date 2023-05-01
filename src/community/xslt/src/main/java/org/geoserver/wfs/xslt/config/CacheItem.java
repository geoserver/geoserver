/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import org.geoserver.platform.resource.Resource;

/**
 * A cache item for a resource loaded from a file. Helps checking if the cached item is up to date,
 * and avoids flooding the file system with excessive IO requests by preventing the up-to-date check
 * to be hitting the file system too often (at most once per second)
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
class CacheItem<T extends Object> {

    static final long MIN_INTERVALS_CHECK = 1000;

    T item;

    long lastModified;

    long lastChecked;

    public CacheItem(T item, Resource sourceFile) {
        this.item = item;
        this.lastModified = sourceFile.lastmodified();
    }

    public T getItem() {
        return item;
    }

    public boolean isUpToDate(Resource file) {
        long now = System.currentTimeMillis();
        if (now - lastChecked < MIN_INTERVALS_CHECK) {
            return true;
        } else {
            lastChecked = now;
            long actualLastModified = file.lastmodified();
            return actualLastModified == lastModified;
        }
    }
}
