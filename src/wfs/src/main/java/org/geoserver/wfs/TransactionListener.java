/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;


/**
 * Implemented by classes needing to listen to datastore change events during a
 * WFS Transaction
 */
public interface TransactionListener {
    /**
     * Check/alter feature collections and filters before a change hits the
     * datastores
     */
    void dataStoreChange(TransactionEvent event) throws WFSException;
}
