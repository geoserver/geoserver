/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.security;

/** EO access limit configuration for Products. */
public interface EOProductAccessLimitInfo extends EOAccessLimitInfo {

    /** The name of the collection this product access limit applies to */
    String getCollection();

    /**
     * Sets the name of the collection this product access limit applies to
     *
     * @param collection
     */
    void setCollection(String collection);
}
