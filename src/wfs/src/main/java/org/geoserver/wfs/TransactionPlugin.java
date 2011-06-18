/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs.TransactionType;


/**
 * A transaction plugin is able to listen to a transaction evolution, perform
 * checks and throw exceptions, alter transaction requests, as well as
 */
public interface TransactionPlugin extends TransactionListener {
    /**
     * Check/alter the transaction request elements
     */
    TransactionType beforeTransaction(TransactionType request)
        throws WFSException;

    /**
     * Say the last word before we actually commit the transaction
     */
    void beforeCommit(TransactionType request) throws WFSException;

    /**
     * Notification the transaction ended
     *
     * @param committed
     *            true if the transaction was successful, false if the
     *            transaction was aborted for any reason
     */
    void afterTransaction(TransactionType request, boolean committed);

    /**
     * Aspects gets called in a specific order. State your priority, the higher
     * the number, the earlier the plugin will be processed.
     */
    int getPriority();
}
