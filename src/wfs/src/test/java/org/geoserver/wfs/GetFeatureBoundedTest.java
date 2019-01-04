/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

public class GetFeatureBoundedTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testCloseIterators() throws Exception {
        // build a wfs response with an iterator that will mark if close has been called, or not
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.POLYGONS));
        FeatureSource fs = fti.getFeatureSource(null, null);
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fs.getFeatures();
        final AtomicInteger openIterators = new AtomicInteger(0);

        SimpleFeatureCollection decorated =
                new org.geotools.feature.collection.DecoratingSimpleFeatureCollection(fc) {
                    @Override
                    public SimpleFeatureIterator features() {
                        openIterators.incrementAndGet();
                        final SimpleFeature f = DataUtilities.first(delegate);
                        return new SimpleFeatureIterator() {

                            @Override
                            public SimpleFeature next() throws NoSuchElementException {
                                return f;
                            }

                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public void close() {
                                openIterators.decrementAndGet();
                            }
                        };
                    }
                };

        FeatureBoundsFeatureCollection fbc =
                new FeatureBoundsFeatureCollection(decorated, decorated.getSchema());
        FeatureIterator<?> i = fbc.features();
        i.close();

        assertEquals(0, openIterators.get());
    }
}
