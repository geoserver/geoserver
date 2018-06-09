/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.ows.LocalPublished;

/**
 * Transfers the {@link LocalPublished} management to another thread
 *
 * @author Andrea Aime - GeoSolutions
 */
public class LocalPublishedThreadLocalTransfer implements ThreadLocalTransfer {

    public static final String KEY = LocalPublished.class.getName() + "#threadLocal";

    @Override
    public void collect(Map<String, Object> storage) {
        PublishedInfo pi = LocalPublished.get();
        storage.put(KEY, pi);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        PublishedInfo pi = (PublishedInfo) storage.get(KEY);
        LocalPublished.set(pi);
    }

    @Override
    public void cleanup() {
        LocalPublished.remove();
    }
}
