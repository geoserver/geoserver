/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import static org.geoserver.kml.utils.KmlCentroidOptions.CLIP;
import static org.geoserver.kml.utils.KmlCentroidOptions.CONTAIN;
import static org.geoserver.kml.utils.KmlCentroidOptions.SAMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

public class KmlCentroidBuilderTest {

    Geometry cShapeGeom;

    @Before
    public void setUp() throws Exception {
        cShapeGeom =
                new WKTReader()
                        .read(
                                "POLYGON ((-112.534433451864 43.8706532611928,-112.499157652296 44.7878240499628,-99.6587666095152 44.7878240499628,-99.7242788087131 43.2155312692142,-111.085391877449 43.099601544023,-110.744593363875 36.1862602686501,-98.6760836215473 35.9436771582516,-98.7415958207452 33.5197257879307,-111.77852346112 33.9783111823157,-111.758573671673 34.6566040234952,-113.088767445077 34.7644575726901,-113.023255245879 43.8706532611928,-112.534433451864 43.8706532611928))");
    }

    @Test
    public void testSampleForPoint() throws Exception {
        Geometry g = cShapeGeom;

        KmlCentroidOptions opts1 =
                KmlCentroidOptions.create(ImmutableMap.of(CONTAIN, "true", SAMPLE, "2"));
        KmlCentroidOptions opts2 =
                KmlCentroidOptions.create(ImmutableMap.of(CONTAIN, "true", SAMPLE, "10"));

        KmlCentroidBuilder builder = new KmlCentroidBuilder();

        Coordinate c = builder.geometryCentroid(g, null, opts1);
        assertFalse(g.contains(g.getFactory().createPoint(c)));

        c = builder.geometryCentroid(g, null, opts2);
        assertTrue(g.contains(g.getFactory().createPoint(c)));
    }

    @Test
    public void testClip() {
        Geometry g = cShapeGeom;
        KmlCentroidOptions opts1 = KmlCentroidOptions.create(ImmutableMap.of());
        KmlCentroidOptions opts2 = KmlCentroidOptions.create(ImmutableMap.of(CLIP, "true"));
        opts2.isClip();

        KmlCentroidBuilder builder = new KmlCentroidBuilder();

        Coordinate c = builder.geometryCentroid(g, null, opts1);
        assertFalse(g.contains(g.getFactory().createPoint(c)));

        Envelope bbox =
                new Envelope(
                        -106.603059724489, -103.655010760585, 34.6334331742943, 36.9918723454173);
        c = builder.geometryCentroid(g, bbox, opts2);
        assertTrue(g.contains(g.getFactory().createPoint(c)));
    }

    @Test
    public void testCaseInsensitivity() {
        KmlCentroidOptions opts =
                KmlCentroidOptions.create(
                        ImmutableMap.of(
                                CONTAIN.toUpperCase(),
                                "true",
                                CLIP.toUpperCase(),
                                "true",
                                SAMPLE.toUpperCase(),
                                "12"));
        assertTrue(opts.isContain());
        assertTrue(opts.isClip());
        assertEquals(12, opts.getSamples());
    }
}
