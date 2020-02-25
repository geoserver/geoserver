/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

/** Implemented by classes needing to listen to datastore change events during a WFS Transaction */
public interface TransactionListener {
    /**
     * Check/alter feature collections and filters before a change hits the datastores
     *
     * <p><b>Note</b> that caution should be exercised when relying on feature identifiers from a
     * {@link TransactionEventType#POST_INSERT} event. Depending on the type of store those
     * identifiers may be reliable. Essentially they can only be relied upon in the case of a
     * spatial dbms (such as PostGIS) is being used.
     */
    void dataStoreChange(TransactionEvent event) throws WFSException;
}
