/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.WPSException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.data.util.SubProgressListener;
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

    Map<String, InputProvider> providers = new LinkedHashMap<String, InputProvider>();

    Map<String, Object> values = new HashMap<String, Object>();

    boolean parsed = false;

    ProgressListener listener = DEFAULT_LISTENER;

    public LazyInputMap(Map<String, InputProvider> providers) {
        this.providers = providers;
    }

    public Object get(Object key) {
        // make sure we just kill the process is a dismiss happened
        if (listener.isCanceled()) {
            throw new ProcessDismissedException(listener);
        }
        // lazy parse inputs
        parseInputs();
        // return the value
        return values.get(key);
    }

    private void parseInputs() {
        // we want to (try to) actually parse stuff just once
        if (parsed) {
            return;
        }
        parsed = true;

        // count long parses
        int totalSteps = 0;
        for (InputProvider provider : providers.values()) {
            totalSteps += provider.longStepCount();
        }

        listener.started();
        float stepsSoFar = 0;
        for (InputProvider provider : providers.values()) {
            listener.setTask(
                    new SimpleInternationalString(
                            "Retrieving/parsing process input: " + provider.getInputId()));
            try {
                // force parsing
                float providerLongSteps = provider.longStepCount();
                ProgressListener subListener;
                if (providerLongSteps > 0) {
                    subListener =
                            new SubProgressListener(
                                    listener,
                                    (stepsSoFar / totalSteps) * 100,
                                    (providerLongSteps / totalSteps) * 100);
                } else {
                    subListener = new NullProgressListener();
                }
                stepsSoFar += providerLongSteps;
                subListener.started();
                subListener.progress(0);
                Object value = provider.getValue(subListener);
                values.put(provider.getInputId(), value);
            } catch (Exception e) {
                listener.exceptionOccurred(e);
                if (e instanceof WPSException) {
                    throw (WPSException) e;
                }
                throw new WPSException(
                        "Failed to retrieve value for input " + provider.getInputId(), e);
            }
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

    public int longStepCount() {
        int count = 0;
        for (InputProvider provider : providers.values()) {
            count += provider.longStepCount();
        }
        return count;
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

    /** The listener will be informed of the parse progress, when it happens */
    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }
}
