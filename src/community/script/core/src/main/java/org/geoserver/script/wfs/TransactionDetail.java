/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wfs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import javax.xml.namespace.QName;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geotools.feature.FeatureCollection;

public class TransactionDetail {

    Multimap<QName, Entry> entries;

    public TransactionDetail() {
        entries = ArrayListMultimap.create();
    }

    public void update(TransactionEvent event) {
        entries.put(event.getLayerName(), new Entry(event.getType(), event.getAffectedFeatures()));
    }

    public Multimap<QName, Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        public TransactionEventType type;
        public FeatureCollection features;

        public Entry(TransactionEventType type, FeatureCollection features) {
            this.type = type;
            this.features = features;
        }
    }
}
