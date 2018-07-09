/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
public class DXFPPIOTest extends WPSTestSupport {

    DXFPPIO ppio;
    DefaultFeatureCollection features;

    @Before
    public void setUp() {
        ppio = new DXFPPIO();
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 4 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            b.add(gf.createLineString(array));
            b.add(0);
            features.add(b.buildFeature(numFeatures + ""));
        }
    }

    @Test
    public void testEncode() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ppio.encode(features, os);
        assertNotNull(os.toByteArray());
        String dxf = new String(os.toByteArray(), "UTF-8");
        checkSequence(dxf, new String[] {"BLOCKS", "LWPOLYLINE"}, 0);
    }

    @Test
    public void testDecodeString() {
        boolean error = false;
        try {
            ppio.decode("");
        } catch (Exception e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testDecodeObject() {
        boolean error = false;
        try {
            ppio.decode(new Object());
        } catch (Exception e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testDecodeInputStream() {
        boolean error = false;
        try {
            ppio.decode(new ByteArrayInputStream(new byte[] {}));
        } catch (Exception e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testFileExtension() {
        assertEquals("dxf", ppio.getFileExtension());
    }

    private void checkSequence(String dxf, String[] sequence, int pos) {
        for (String item : sequence) {
            pos = dxf.indexOf(item, pos + 1);
            assertTrue(pos != -1);
        }
    }
}
