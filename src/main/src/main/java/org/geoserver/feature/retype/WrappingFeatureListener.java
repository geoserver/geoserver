/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;

/**
 * A FeatureListener wrapper that changes the feature source, to be used in
 * wrapping feature sources
 */
public class WrappingFeatureListener implements FeatureListener {

    RetypingFeatureSource source;
    FeatureListener listener;

    public WrappingFeatureListener(RetypingFeatureSource source, FeatureListener listener) {
        this.source = source;
        this.listener = listener;
    }

    public void changed(FeatureEvent featureEvent) {
        FeatureEvent retyped = new FeatureEvent(source, featureEvent.getEventType(), featureEvent
                .getBounds());
        listener.changed(retyped);
    }

}
