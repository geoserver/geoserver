/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InterpolationKvpParserTest extends GeoServerSystemTestSupport {

    InterpolationKvpParser parser = new InterpolationKvpParser();

    private String axisPrefix;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{""}, {"http://www.opengis.net/def/axis/OGC/1/"}});
    }

    public InterpolationKvpParserTest(String axisPrefix) {
        this.axisPrefix = axisPrefix;
    }

    @Test
    public void testInvalidValues() throws Exception {
        try {
            parser.parse(":interpolation");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("a:linear,,b:nearest");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("a::linear");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
    }

    private void checkInvalidSyntaxException(OWS20Exception e) {
        assertNotNull(e.getHttpCode());
        assertEquals(400, e.getHttpCode().intValue());
        assertEquals("InvalidEncodingSyntax", e.getCode());
        assertEquals("interpolation", e.getLocator());
    }

    @Test
    public void testUniformValue() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse("http://www.opengis.net/def/interpolation/OGC/1/linear");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                it.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testSingleAxis() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse(
                                axisPrefix
                                        + "latitude:http://www.opengis.net/def/interpolation/OGC/1/linear");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(1, axes.size());
        assertEquals(axisPrefix + "latitude", axes.get(0).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                axes.get(0).getInterpolationMethod());
    }

    @Test
    public void testMultiAxis() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse(
                                axisPrefix
                                        + "latitude:"
                                        + "http://www.opengis.net/def/interpolation/OGC/1/linear,"
                                        + axisPrefix
                                        + "longitude:"
                                        + "http://www.opengis.net/def/interpolation/OGC/1/nearest");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(2, axes.size());
        assertEquals(axisPrefix + "latitude", axes.get(0).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                axes.get(0).getInterpolationMethod());
        assertEquals(axisPrefix + "longitude", axes.get(1).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/nearest",
                axes.get(1).getInterpolationMethod());
    }

    @Test
    public void testParserForVersion() throws Exception {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        KvpParser parser = KvpUtils.findParser("interpolation", "WCS", null, "2.0.0", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), InterpolationKvpParser.class);
        // Version 2.0.1
        parser = KvpUtils.findParser("interpolation", "WCS", null, "2.0.1", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), InterpolationKvpParser.class);
    }
}
