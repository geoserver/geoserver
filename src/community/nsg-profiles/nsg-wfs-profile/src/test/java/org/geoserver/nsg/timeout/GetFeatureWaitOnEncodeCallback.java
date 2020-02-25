/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GetFeatureWaitOnEncodeCallback extends AbstractDispatcherCallback
        implements ExtensionPriority {

    long delaySeconds = 0;
    int delayAfterFeatures = 0;

    class DelayFeatureCollection extends DecoratingSimpleFeatureCollection {

        protected DelayFeatureCollection(
                FeatureCollection<SimpleFeatureType, SimpleFeature> delegate) {
            super(delegate);
        }

        @Override
        public SimpleFeatureIterator features() {
            return new DelayFeatureIterator(super.features());
        }
    }

    class DelayFeatureIterator extends DecoratingSimpleFeatureIterator {

        int count = 0;

        public DelayFeatureIterator(SimpleFeatureIterator iterator) {
            super(iterator);
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            if (count == delayAfterFeatures) {
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            count++;
            return super.next();
        }
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        if (delaySeconds > 0 && result instanceof FeatureCollectionResponse) {
            FeatureCollectionResponse featureCollectionResponse =
                    (FeatureCollectionResponse) result;
            List<FeatureCollection> collections = featureCollectionResponse.getFeatures();
            List<FeatureCollection> wrappers =
                    collections
                            .stream()
                            .map(fc -> new DelayFeatureCollection((SimpleFeatureCollection) fc))
                            .collect(Collectors.toList());

            featureCollectionResponse.setFeatures(wrappers);
        }

        return super.operationExecuted(request, operation, result);
    }

    @Override
    public int getPriority() {
        // make sure it's the first to catch the results
        return Integer.MIN_VALUE;
    }
}
