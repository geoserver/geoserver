/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Simple testing aid adding a given delay to each FeatureIterator.next() call
 *
 * @author Andrea Aime
 */
class DelayedFeatureCollection extends DecoratingSimpleFeatureCollection {

    private long featureDelay;

    protected DelayedFeatureCollection(SimpleFeatureCollection delegate, long featureDelay) {
        super(delegate);
        this.featureDelay = featureDelay;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new DecoratingSimpleFeatureIterator(super.features()) {
            @Override
            public SimpleFeature next() throws NoSuchElementException {
                try {
                    Thread.sleep(featureDelay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.next();
            }
        };
    }
}
