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
import org.geotools.util.NullProgressListener;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

/**
 * A map using input providers internally, allows for deferred execution of the input parsing (it
 * happens in a single shot when the first input is fetched)
 * 
 * @author Andrea Aime - GeoSolutions
 */
class LazyInputMap extends AbstractMap<String, Object> {

    private static ProgressListener DEFAULT_LISTENER = new NullProgressListener();

    Map<String, InputProvider> providers = new HashMap<String, InputProvider>();

    Map<String, Object> values = new HashMap<String, Object>();

    boolean parsed = false;

    ProgressListener listener = DEFAULT_LISTENER;

    public LazyInputMap(Map<String, InputProvider> providers) {
        this.providers = providers;
    }

    public Object get(Object key) {
        parseInputs();
        return values.get(key);
    }
    
    private void parseInputs() {
        // we want to (try to) actually parse stuff just once
        if (parsed) {
            return;
        }
        parsed = true;

        // count long parses
        int longParses = 0;
        for (InputProvider provider: providers.values()) {
            if(provider.longParse()) {
                longParses++;
            }
        }
        float fastParseStep, longParseStep;
        if(longParses > 0) {
            fastParseStep = 1;
            longParseStep = (100f - (providers.size() - longParses)) / longParses;
        } else {
            fastParseStep = 100f / providers.size();
            longParseStep = 0;
        }
        
        listener.started();
        int progress = 0;
        for (InputProvider provider : providers.values()) {
            listener.setTask(new SimpleInternationalString("Retrieving/parsing process input: "
                    + provider.getInputId()));
            try {
                // force parsing
                Object value = provider.getValue();
                values.put(provider.getInputId(), value);
            } catch (Exception e) {
                listener.exceptionOccurred(e);
                throw new WPSException("Failed to retrieve value for input "
                        + provider.getInputId(), e);
            }
            if (provider.longParse()) {
                progress += longParseStep;
            } else {
                progress += fastParseStep;
            }
            listener.progress(progress);
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> result = new HashSet<Map.Entry<String, Object>>();
        for (String key : providers.keySet()) {
            result.add(new DeferredEntry(key));
        }
        return result;
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

        public DeferredEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            parseInputs();
            return values.get(key);
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * The listener will be informed of the parse progress, when it happens
     * @param listener
     */
    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    
}
