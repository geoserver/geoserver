/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.List;
import org.geotools.data.DataUtilities;
import org.opengis.feature.Feature;

public class TransactionListenerTester implements TransactionListener {
    List<TransactionEvent> events = new ArrayList<>();
    List<Feature> features = new ArrayList<>();

    public void clear() {
        events.clear();
        features.clear();
    }

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        events.add(event);
        features.addAll(DataUtilities.list(event.getAffectedFeatures()));
    }
}
