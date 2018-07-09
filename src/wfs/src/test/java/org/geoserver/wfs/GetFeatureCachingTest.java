/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.wfs.v2_0.GetFeatureTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Test making sure GetFeature still works when the caching machinery is off (since most test
 * datasets will actually be cached in memory)
 */
public class GetFeatureCachingTest extends GetFeatureTest {

    @BeforeClass
    public static void enableCaching() {
        Assert.assertEquals(0, FeatureSizeFeatureCollection.DEFAULT_CACHE_SIZE);
        FeatureSizeFeatureCollection.setFeatureCacheLimit(16);
    }

    @AfterClass
    public static void disableCaching() {
        FeatureSizeFeatureCollection.setFeatureCacheLimit(
                FeatureSizeFeatureCollection.DEFAULT_CACHE_SIZE);
    }
}
