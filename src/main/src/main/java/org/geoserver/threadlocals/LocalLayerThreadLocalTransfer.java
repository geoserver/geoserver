/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.util.Map;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.ows.LocalLayer;

/**
 * Transfers the {@link LocalLayer} management to another thread
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class LocalLayerThreadLocalTransfer implements ThreadLocalTransfer {

    public static final String KEY = LocalLayer.class.getName() + "#threadLocal";
    
    @Override
    public void collect(Map<String, Object> storage) {
        LayerInfo li = LocalLayer.get();
        storage.put(KEY, li);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        LayerInfo li = (LayerInfo) storage.get(KEY);
        LocalLayer.set(li);
    }

    @Override
    public void cleanup() {
        LocalLayer.remove();
    }

}
