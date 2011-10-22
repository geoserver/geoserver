/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv;

import java.io.IOException;

import net.opengis.wfs.TransactionType;

import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.Transaction;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.postgis.VersionedPostgisDataStore;
import org.springframework.context.ApplicationContext;

/**
 * Extends the base transaction to handle extended versioning elements
 *
 * @author Andrea Aime, TOPP
 */
public class VersioningTransaction extends Transaction {
    public VersioningTransaction(WFSInfo wfs, Catalog catalog, ApplicationContext context) {
        super(wfs, catalog, context);
    }

    protected DefaultTransaction getDatastoreTransaction(TransactionType request)
        throws IOException {
        DefaultTransaction transaction = new DefaultTransaction();
        // use handle as the log messages
        String username = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null) {
            Object principal = authentication.getPrincipal();
            if(principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername(); 
            }
        }
        transaction.putProperty(VersionedPostgisDataStore.AUTHOR, username);
        transaction.putProperty(VersionedPostgisDataStore.MESSAGE, request.getHandle());

        return transaction;
    }

    
}
