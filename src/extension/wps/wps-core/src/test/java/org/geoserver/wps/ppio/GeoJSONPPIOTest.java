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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

public class GeoJSONPPIOTest extends WPSTestSupport {
    private InputStream is;
    static final double EPS = 1e-5;

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
    public void testDecodeGeometries() throws Exception {
        GeoJSONPPIO ppio = new GeoJSONPPIO.Geometries(getGeoServer());
        String string = "{\"type\":\"Point\",\"coordinates\":[1.123456789,2]}";
        Point point1 = (Point) ppio.decode(string);
        assertEquals(1.123456789, point1.getX(), EPS);
        assertEquals(2.0, point1.getY(), EPS);
        ByteArrayInputStream input = new ByteArrayInputStream(string.getBytes());
        Point point2 = (Point) ppio.decode(input);
        assertEquals(1.123456789, point2.getX(), EPS);
        assertEquals(2.0, point2.getY(), EPS);
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
        JSONObject fc = (JSONObject) JSONSerializer.toJSON(json);
        JSONArray features = fc.getJSONArray("features");
        JSONObject state0 = features.getJSONObject(0);
        // random tests on the first state ordinates ...
        JSONArray state0Ordinates =
                state0.getJSONObject("geometry")
                        .getJSONArray("coordinates")
                        .getJSONArray(0)
                        .getJSONArray(0);
        assertEquals(-88.087883, state0Ordinates.getJSONArray(1).getDouble(0), EPS);
        assertEquals(-88.311707, state0Ordinates.getJSONArray(2).getDouble(0), EPS);
        assertEquals(37.420292, state0Ordinates.getJSONArray(4).getDouble(1), EPS);
        // checking the 10th state attributes...
        JSONObject state10 = features.getJSONObject(9);
        JSONObject state10p = state10.getJSONObject("properties");
        assertEquals("Missouri", state10p.getString("STATE_NAME"));
        assertEquals("29", state10p.getString("STATE_FIPS"));
        assertEquals("W N Cen", state10p.getString("SUB_REGION"));
        assertEquals("2100.115", state10p.getString("WATER_KM"));
        assertEquals("1961206", state10p.getString("HOUSHOLD"));
        // checking the 11th state attributes...
        JSONObject state11 = features.getJSONObject(11);
        JSONObject state11p = state11.getJSONObject("properties");
        assertEquals("Oklahoma", state11p.getString("STATE_NAME"));
        assertEquals("40", state11p.getString("STATE_FIPS"));
        assertEquals("W S Cen", state11p.getString("SUB_REGION"));
        assertEquals("3170.998", state11p.getString("WATER_KM"));
        assertEquals("1206135", state11p.getString("HOUSHOLD"));

        int dec = global.getSettings().getNumDecimals();
        global.getSettings().setNumDecimals(2);
        getGeoServer().save(global);
        try {

            ByteArrayOutputStream os2 = new ByteArrayOutputStream(1024);
            new GeoJSONPPIO.FeatureCollections(gs).encode(states, os2);

            String json2 = os2.toString();
            JSONObject fc2 = (JSONObject) JSONSerializer.toJSON(json2);
            JSONArray features2 = fc2.getJSONArray("features");
            state0 = features2.getJSONObject(0);
            state0Ordinates =
                    state0.getJSONObject("geometry")
                            .getJSONArray("coordinates")
                            .getJSONArray(0)
                            .getJSONArray(0);
            assertEquals(-88.09, state0Ordinates.getJSONArray(1).getDouble(0), EPS);
            assertEquals(-88.31, state0Ordinates.getJSONArray(2).getDouble(0), EPS);
            assertEquals(37.42, state0Ordinates.getJSONArray(4).getDouble(1), EPS);
        } finally {
            global.getSettings().setNumDecimals(dec);
            getGeoServer().save(global);
        }
    }
}
