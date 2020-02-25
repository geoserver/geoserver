/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.PublishedInfo;

/**
 * A thread local variable for a {@link PublishedInfo} that was specified as part of an ows request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalPublished {

    /** Key for extended request property */
    public static final String KEY = "localPublished";

    /** the layer thread local */
    static ThreadLocal<PublishedInfo> published = new ThreadLocal<PublishedInfo>();

    public static void set(PublishedInfo p) {
        published.set(p);
    }

    public static PublishedInfo get() {
        return published.get();
    }

    public static void remove() {
        published.remove();
    }
}
