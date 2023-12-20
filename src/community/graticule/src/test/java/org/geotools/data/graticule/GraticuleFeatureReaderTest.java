/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.process.vector.GraticuleLabelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class GraticuleFeatureReaderTest extends GraticuleLabelTestSupport {

    @Test
    public void testReader() throws Exception {
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                store.getFeatureReader(new Query(), null)) {
            Assert.assertNotNull(reader);
            double[] counts = new double[steps.size()];
            while (reader.hasNext()) {
                SimpleFeature f = reader.next();
                int level = (int) f.getAttribute("level");
                counts[level]++;
            }
            Assert.assertEquals(36.0, counts[0], 0.00001);
            Assert.assertEquals(20.0, counts[1], 0.00001);
        }
    }
}
