/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geoserver.wps.WPSException;

/**
 * A map using input providers internally, allows for deferred execution of the input parsing
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class LazyInputMap extends AbstractMap<String, Object> {

    Map<String, InputProvider> providers = new HashMap<String, InputProvider>();

    public LazyInputMap(Map<String, InputProvider> providers) {
        this.providers = providers;
    }

    public Object get(Object key) {
        InputProvider provider = providers.get((String) key);
        if (provider == null) {
            return null;
        } else {
            try {
                return provider.getValue();
            } catch (Exception e) {
                throw new WPSException("Failed to retrieve value for input "
                        + provider.getInputId(), e);
            }
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, InputProvider>> entries = providers.entrySet();
        Set<Entry<String, Object>> result = new HashSet<Map.Entry<String, Object>>();
        for (Entry<String, InputProvider> entry : entries) {
            result.add(new DeferredEntry(entry.getKey(), entry.getValue()));
        }
        return result;
    }
    /**
     * The retrieved input percentage, as a number between 0 and 100
     * @return
     */
    public float getRetrievedInputPercentage() {
        if(providers.size() == 0) {
            return 100.0f;
        }
        
        int  count = 0;
        for (InputProvider provider: providers.values()) {
            if(provider.resolved()) {
                count++;
            }
        }
        return 100f * count / providers.size();
    }
    
    public boolean longParse() {
        for (InputProvider provider: providers.values()) {
            if(provider.longParse()) {
                return true;
            }
        }
        return false;
    }

    public class DeferredEntry implements Entry<String, Object> {

        private String key;

        private InputProvider provider;

        public DeferredEntry(String key, InputProvider provider) {
            this.key = key;
            this.provider = provider;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            try {
                return provider.getValue();
            } catch (Exception e) {
                throw new WPSException("Failed to retrieve value for input "
                        + provider.getInputId());
            }
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

    }

    
}
