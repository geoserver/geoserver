/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * Allows plugins to participate in the {@link GeoServerApplication} initialization and change some of its properties
 * before the application is fully initialized.
 */
public interface GeoServerApplicationInitializer {

    /**
     * Called during {@link GeoServerApplication} initialization, before the application is fully initialized.
     * Implementations can change some of the application properties at this stage, for example by changing the page
     * store (useful for clustering, the default page store is disk based).
     *
     * @param application The {@link GeoServerApplication} being initialized
     */
    void init(GeoServerApplication application);
}
