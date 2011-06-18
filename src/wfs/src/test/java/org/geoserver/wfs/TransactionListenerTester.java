package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;

public class TransactionListenerTester implements TransactionListener {
    List events = new ArrayList();
    List features = new ArrayList();
    
    public void clear() {
        events.clear();
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        events.add(event);
        features.addAll(DataUtilities.list(event.getAffectedFeatures()));
    }
    
    
    
}
