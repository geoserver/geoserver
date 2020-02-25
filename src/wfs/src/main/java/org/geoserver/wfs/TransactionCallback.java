/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;

/**
 * A transaction plugin is able to listen to a transaction evolution, perform checks and throw
 * exceptions, alter transaction requests, as well as
 */
public interface TransactionCallback extends ExtensionPriority, TransactionListener {
    /** Check/alter the transaction request elements */
    TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException;

    /** Say the last word before we actually commit the transaction */
    void beforeCommit(TransactionRequest request) throws WFSException;

    /**
     * Notification the transaction ended
     *
     * @param request the originating transaction request
     * @param result {@code null} if {@code committed == false}, the transaction result object to be
     *     sent back to the client otherwise.
     * @param committed true if the transaction was successful, false if the transaction was aborted
     *     for any reason
     */
    void afterTransaction(
            TransactionRequest request, TransactionResponse result, boolean committed);

    @Override
    default int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
