/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.opengis.filter.Filter;

/**
 * Loads {@link org.geoserver.catalog.CatalogInfo} objects from the catalog, with a timeout and a
 * maximum number of objects to load.
 */
class BoundedCatalogLoader<T extends CatalogInfo> implements Serializable {

    List<T> result = new ArrayList<>();
    boolean boundExceeded = false;
    long residualTime;

    public BoundedCatalogLoader(T item) {
        this.result.add(item);
        this.boundExceeded = false;
    }

    public BoundedCatalogLoader(
            Catalog catalog, Filter filter, Class<T> target, long timeout, int max) {
        // perform a bounded load of the catalog
        // (single call, doing count + list is slower and count cannot be stopped mid-way)
        long limit = System.currentTimeMillis() + timeout;
        try (CloseableIterator<T> search = catalog.list(target, filter, 0, max + 1, null)) {
            while (System.currentTimeMillis() < limit && search.hasNext()) {
                result.add(search.next());
            }
        }
        // check if bounds have been exceeded
        this.residualTime = limit - System.currentTimeMillis();
        boundExceeded = residualTime <= 0 || result.size() > max;
        // if went beyond the limit, remove the last elements
        if (result.size() > max) {
            // creating new list because sublist is not serializable
            result = new ArrayList<>(result.subList(0, max));
        }
    }

    /** Returns the list of elements loaded from the catalog */
    public List<T> getResult() {
        return result;
    }

    /**
     * Returns true if the bounds were exceeded (either timed out, or went beyond maximum count),
     * false otherwise
     */
    public boolean isBoundExceeded() {
        return boundExceeded;
    }

    /** Returns the residual time, that is the time left after the load operation was completed. */
    public long getResidualTime() {
        return residualTime;
    }
}
