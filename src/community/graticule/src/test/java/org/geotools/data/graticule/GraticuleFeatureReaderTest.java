/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.process.vector.GraticuleLabelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class GraticuleFeatureReaderTest extends GraticuleLabelTestSupport {

    @Test
    public void testReader() throws Exception {
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                store.getFeatureReader(new Query("Graticule_10_30"), null)) {
            Assert.assertNotNull(reader);
            double[] counts = new double[steps.size()];
            while (reader.hasNext()) {
                SimpleFeature f = reader.next();
                int level = (int) f.getAttribute("level");
                counts[level]++;

                // check the sequence value is properly computed
                Double value = (Double) f.getAttribute(LineFeatureBuilder.VALUE_ATTRIBUTE_NAME);
                Boolean horizontal = (Boolean) f.getAttribute(LineFeatureBuilder.HORIZONTAL);
                String sequence = (String) f.getAttribute(LineFeatureBuilder.SEQUENCE);

                if (horizontal) {
                    if (value.equals(-90d)) {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_START, sequence);
                    } else if (value.equals(90d)) {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_END, sequence);
                    } else {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_MID, sequence);
                    }
                } else {
                    if (value.equals(-180d)) {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_START, sequence);
                    } else if (value.equals(180d)) {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_END, sequence);
                    } else {
                        Assert.assertEquals(LineFeatureBuilder.SEQUENCE_MID, sequence);
                    }
                }
            }
            Assert.assertEquals(56.0, counts[0], 0.00001);
            Assert.assertEquals(20.0, counts[1], 0.00001);
        }
    }
}
