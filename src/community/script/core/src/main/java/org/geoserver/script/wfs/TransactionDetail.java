package org.geoserver.script.wfs;

import javax.xml.namespace.QName;

import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geotools.feature.FeatureCollection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

        public Entry (TransactionEventType type, FeatureCollection features) {
            this.type = type;
            this.features = features;
        }
    }
}
