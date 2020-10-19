/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;

public class GeoJSONPPIOTest extends WPSTestSupport {
    private InputStream is;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-noargs.xml");
    }

    @Before
    public void prepareInputStream() throws IOException {
        is = SystemTestData.class.getResourceAsStream("states.json");
        assertNotNull(is);
    }

    @After
    public void cleanup() throws IOException {
        if (is != null) {
            is.close();
        }
    }

    @Test
    public void testEncodeGeometries() throws Exception {
        WKTReader reader = new WKTReader();
        Point point = (Point) reader.read("POINT(1.123456789 2.0)");

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        new GeoJSONPPIO.Geometries(gs).encode(point, os);
        String output = os.toString();
        assertEquals(output, "{\"type\":\"Point\",\"coordinates\":[1.12345679,2]}");

        int dec = global.getSettings().getNumDecimals();
        global.getSettings().setNumDecimals(4);
        getGeoServer().save(global);

        ByteArrayOutputStream os2 = new ByteArrayOutputStream(1024);
        new GeoJSONPPIO.Geometries(gs).encode(point, os2);
        String output2 = os2.toString();
        assertEquals(output2, "{\"type\":\"Point\",\"coordinates\":[1.1235,2]}");

        global.getSettings().setNumDecimals(dec);
        getGeoServer().save(global);
    }

    /** Test method for {@link WFSPPIO#decode(InputStream)}. */
    @Test
    public void testDecodeInputStream() throws Exception {
        SimpleFeatureCollection states =
                (SimpleFeatureCollection) new GeoJSONPPIO.FeatureCollections().decode(is);

        assertEquals("Wrong number of states", 49, states.size());
        assertEquals("Wrong number of columns", 23, states.getSchema().getAttributeCount());

        Filter filter = CQL.toFilter("STATE_NAME = 'Alabama'");

        SimpleFeatureCollection alabama = states.subCollection(filter);

        assertEquals("Persons", 4040587.0, DataUtilities.first(alabama).getAttribute("PERSONS"));
    }

    @Test
    public void testEncodeOutputStream() throws Exception {
        SimpleFeatureCollection states =
                (SimpleFeatureCollection) new GeoJSONPPIO.FeatureCollections().decode(is);

        assertEquals("Wrong number of states", 49, states.size());

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        new GeoJSONPPIO.FeatureCollections(gs).encode(states, os);

        String json = os.toString();

        BufferedReader r = new BufferedReader(new StringReader(json));
        String line;
        int lines = 0;
        while ((line = r.readLine()) != null) {
            String[] state = line.split("]]]]}");

            String[] coords = state[0].split(",");
            // The test data has 6 digits of precision.
            // The default number of digits given by GeoServer is 8.
            assertEquals("[-88.087883", coords[7]);
            assertEquals("[-88.311707", coords[9]);
            assertEquals("37.420292]", coords[14]);

            String[] attribs = state[2].split(",");
            assertEquals(attribs[2], "\"STATE_FIPS\":\"29\"");
            assertEquals(attribs[3], "\"SUB_REGION\":\"W N Cen\"");
            assertEquals(attribs[6], "\"WATER_KM\":2100.115");
            assertEquals(attribs[9], "\"HOUSHOLD\":1961206");

            String[] attribs2 = state[4].split(",");
            assertEquals(attribs2[2], "\"STATE_FIPS\":\"40\"");
            assertEquals(attribs2[3], "\"SUB_REGION\":\"W S Cen\"");
            assertEquals(attribs2[6], "\"WATER_KM\":3170.998");
            assertEquals(attribs2[9], "\"HOUSHOLD\":1206135");
            lines++;
        }
        assertEquals("Block line", 1, lines);

        int dec = global.getSettings().getNumDecimals();
        global.getSettings().setNumDecimals(2);
        getGeoServer().save(global);

        ByteArrayOutputStream os2 = new ByteArrayOutputStream(1024);
        new GeoJSONPPIO.FeatureCollections(gs).encode(states, os2);

        String json2 = os2.toString();

        String[] state = json2.split("]]]]}");
        String[] coords = state[0].split(",");
        // This part of the test shows reducing the precision from 8 to 4.
        assertEquals("[-88.09", coords[7]);
        assertEquals("[-88.31", coords[9]);
        assertEquals("37.42]", coords[14]);

        // Resetting the number of decimals so that the next test will pass.
        global.getSettings().setNumDecimals(dec);
        getGeoServer().save(global);
    }
}
