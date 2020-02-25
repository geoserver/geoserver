/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.wps.WPSException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.data.util.SubProgressListener;
import org.opengis.util.ProgressListener;

/**
 * A InputProvider that handles a list of simple providers (used for multi-valued inputs)
 *
 * @author Andrea Aime - GeoSolutions
 */
class ListInputProvider implements InputProvider {

    List<InputProvider> providers;

    String inputId;

    List<Object> value;

    int maxItems;

    public ListInputProvider(InputProvider provider, int maxItems) {
        this.providers = new ArrayList<InputProvider>();
        this.providers.add(provider);
        this.inputId = provider.getInputId();
        this.maxItems = maxItems;
    }

    @Override
    public Object getValue(ProgressListener listener) throws Exception {
        float totalSteps = longStepCount();
        float stepsSoFar = 0;
        if (value == null) {
            // check we are not going above the limit
            if (maxItems > 0 && providers.size() > maxItems) {
                throw new WPSException(
                        "Too many values for input " + getInputId() + ", the max is " + maxItems,
                        "NoApplicableCode",
                        getInputId());
            }

            value = new ArrayList<Object>();
            for (InputProvider provider : providers) {
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
                totalSteps += providerLongSteps;
                Object pv = provider.getValue(subListener);
                value.add(pv);
            }
        }
        return value;
    }

    @Override
    public String getInputId() {
        return inputId;
    }

    public void add(InputProvider provider) {
        this.providers.add(provider);
    }

    @Override
    public boolean resolved() {
        return value != null;
    }

    @Override
    public int longStepCount() {
        int count = 0;
        for (InputProvider ip : providers) {
            count += ip.longStepCount();
        }

        return count;
    }
}
