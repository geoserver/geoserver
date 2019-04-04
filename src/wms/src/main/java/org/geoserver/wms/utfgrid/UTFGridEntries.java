/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.util.HashMap;
import java.util.Map;
import org.opengis.feature.Feature;

class UTFGridEntries {

    static class UTFGridEntry {

        int value;

        int key = -1;

        Feature feature;

        public UTFGridEntry(int value, Feature feature) {
            super();
            this.value = value;
            this.feature = feature;
        }

        public int getValue() {
            return value;
        }

        public Feature getFeature() {
            return feature;
        }

        public int getKey() {
            return key;
        }

        public void setKey(int key) {
            this.key = key;
        }
    }

    Map<String, UTFGridEntry> entryMap = new HashMap<>();

    int value = 1;

    int getKeyForFeature(Feature feature) {
        String id = feature.getIdentifier().getID();
        UTFGridEntry entry = entryMap.get(id);
        if (entry == null) {
            entry = new UTFGridEntry(value++, feature);
            entryMap.put(id, entry);
        }
        return entry.getValue();
    }

    Map<Integer, UTFGridEntry> getEntryMap() {
        Map<Integer, UTFGridEntry> result = new HashMap<>();
        for (UTFGridEntry entry : entryMap.values()) {
            result.put(entry.getValue(), entry);
        }

        return result;
    }
}
