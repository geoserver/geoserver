/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import java.io.Closeable;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;

/** A closeable iterator that returns the properties needed to build a vector tile feature */
public interface VTIterator extends Closeable {

    /** Checks if there is a new feature Checks if there is a new feature */
    boolean hasNext();

    /** Moves to the next feature */
    VTFeature next();

    @Override
    void close();

    /**
     * Retrieves an iterator optimized for the target GeoTools {@link FeatureIterator}
     *
     * @param delegate the delegate iterator
     * @param coalesce whether features with the same attributes should be coalesced into one (assumes features have
     *     been sorted already)
     * @return A {@link VTIterator} wrapping the provided {@link FeatureIterator}
     */
    static VTIterator getIterator(FeatureIterator<?> delegate, boolean coalesce) {
        VTIterator result;
        if (delegate instanceof SimpleFeatureIterator) {
            result = new SimpleVTIterator((SimpleFeatureIterator) delegate);
        } else {
            result = new ComplexVTIterator(delegate);
        }

        if (coalesce) result = new CoalescingVTIterator(result);

        return result;
    }
}
