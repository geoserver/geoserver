/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * A InputProvider that handles a list of simple providers (used for multi-valued inputs)
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class ListInputProvider implements InputProvider {

    List<InputProvider> providers;

    String inputId;

    List<Object> value;

    public ListInputProvider(InputProvider provider) {
        this.providers = new ArrayList<InputProvider>();
        this.providers.add(provider);
        this.inputId = provider.getInputId();
    }

    @Override
    public Object getValue() throws Exception {
        if (value == null) {
            value = new ArrayList<Object>();
            for (InputProvider provider : providers) {
                Object pv = provider.getValue();
                value.add(pv);
            }
            providers = null;
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
    public boolean longParse() {
        for (InputProvider provider : providers) {
            if(provider.longParse()) {
                return true;
            }
        }
        return false;
    }

}
