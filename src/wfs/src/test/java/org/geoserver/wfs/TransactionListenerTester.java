/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;

public class TransactionListenerTester implements TransactionListener {
    List events = new ArrayList();
    List features = new ArrayList();
    
    public void clear() {
        events.clear();
        features.clear();
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        events.add(event);
        features.addAll(DataUtilities.list(event.getAffectedFeatures()));
    }
    
    
    
}
