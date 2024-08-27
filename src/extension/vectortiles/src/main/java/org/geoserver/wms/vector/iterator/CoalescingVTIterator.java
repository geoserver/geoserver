/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import java.util.NoSuchElementException;
import org.geotools.geometry.jts.GeometryCollector;

/**
 * A {@link VTIterator} that merges the geometries of two or more subsequent {@link VTFeature} that
 * do share the same attributes
 */
public class CoalescingVTIterator implements VTIterator {
    PushBackVTIterator delegate;
    VTFeature curr;

    public CoalescingVTIterator(VTIterator delegate) {
        this.delegate = new PushBackVTIterator(delegate);
    }

    private void mergeFeatures() {
        if (!delegate.hasNext()) return;

        this.curr = delegate.next();

        GeometryCollector collector = null;
        while (delegate.hasNext()) {
            VTFeature next = delegate.next();
            if (curr.getProperties().equals(next.getProperties())) {
                if (collector == null) {
                    collector = new GeometryCollector();
                    collector.add(curr.getGeometry());
                }
                collector.add(next.getGeometry());
            } else {
                // will process in the next round
                delegate.pushBack();
                break;
            }
        }

        // did merge happen?
        if (collector != null) {
            curr.setGeometry(collector.collect());
        }
    }

    @Override
    public boolean hasNext() {
        if (curr != null) return true;
        mergeFeatures();
        return curr != null;
    }

    @Override
    public VTFeature next() {
        if (!hasNext()) throw new NoSuchElementException();

        VTFeature result = curr;
        curr = null;
        return result;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
