/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;

class TimeoutFeatureVisitor implements FeatureVisitor {

    TimeoutVerifier timeoutVerifier;

    FeatureVisitor delegate;

    public TimeoutFeatureVisitor(TimeoutVerifier timeoutVerifier, FeatureVisitor delegate) {
        this.timeoutVerifier = timeoutVerifier;
        this.delegate = delegate;
    }

    @Override
    public void visit(Feature feature) {
        timeoutVerifier.checkTimeout();
        delegate.visit(feature);
    }
}
