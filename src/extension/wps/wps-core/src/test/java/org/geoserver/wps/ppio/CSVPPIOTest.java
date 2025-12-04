/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ian */
public class CSVPPIOTest extends WPSTestSupport {

    private InputStream is;
    private WPSResourceManager resourceManager;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-noargs.xml");
    }

    @Before
    public void prepareInputStream() throws IOException {
        resourceManager = GeoServerExtensions.bean(WPSResourceManager.class);

        is = SystemTestData.class.getResourceAsStream("states.csv");
        assertNotNull(is);
    }

    @After
    public void cleanup() throws IOException {
        if (is != null) {
            is.close();
        }
    }

    /** Test method for {@link org.geoserver.wps.ppio.WFSPPIO#decode(java.io.InputStream)}. */
    @Test
    public void testDecodeInputStream() throws Exception {
        SimpleFeatureCollection states = (SimpleFeatureCollection) new CSVPPIO(resourceManager).decode(is);

        assertEquals("Wrong number of states", 51, states.size());
        assertEquals("Wrong number of columns", 9, states.getSchema().getAttributeCount());

        Filter filter = CQL.toFilter("State = 'Alabama'");

        SimpleFeatureCollection alabama = states.subCollection(filter);

        assertEquals("inc1995 wrong", 19683, DataUtilities.first(alabama).getAttribute("inc1995"));
    }

    @Test
    public void testEncodeOutputStream() throws Exception {

        SimpleFeatureCollection states = (SimpleFeatureCollection) new CSVPPIO(resourceManager).decode(is);

        assertEquals("Wrong number of states", 51, states.size());

        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        new CSVPPIO(resourceManager).encode(states, os);

        String csv = os.toString();

        BufferedReader r = new BufferedReader(new StringReader(csv));
        String line;
        int lines = 0;
        while ((line = r.readLine()) != null) {
            String[] attribs = line.split(",");
            if (lines == 0) {
                assertEquals("State", attribs[0]);
                assertEquals(attribs[1], "inc1980");
                assertEquals("inc2000", attribs[4]);
                assertEquals(attribs[8], "inc2012");
            }
            if (attribs[0].equalsIgnoreCase("Tennessee")) {
                // Tennessee,7711,15903,21800,25946,28455,32172,34089,37678
                assertEquals("7711", attribs[1]);
                assertEquals("37678", attribs[8]);
            }
            lines++;
        }
        assertEquals("Wrong number of lines", 52, lines);
    }

    @Test
    public void testEncodeOutputStreamWithGeometries() throws Exception {

        SimpleFeatureType type = DataUtilities.createType(
                "Locations",
                "geom:Point:srid=4326,area:MultiPolygon:srid=4326,path:LineString:srid=4326,name:String,population:Integer,last_census_date:Date,density:Double,visited:Boolean,null_field:Float");

        DefaultFeatureCollection fc = new DefaultFeatureCollection("locations", type);

        WKTReader2 wktReader = new WKTReader2();
        fc.add(SimpleFeatureBuilder.build(
                type,
                new Object[] {
                    wktReader.read("POINT (9.19 45.46)"),
                    wktReader.read("MULTIPOLYGON (((9.13 45.52, 9.13 45.42, 9.30 45.42, 9.30 45.52, 9.13 45.52)))"),
                    wktReader.read("LINESTRING (9.18 45.48, 9.19 45.47, 9.20 45.46)"),
                    "Milan",
                    1378000,
                    "2025-01-01T00:00:00",
                    Double.MAX_VALUE,
                    true,
                    null
                },
                null));

        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        new CSVPPIO(resourceManager).encode(fc, os);

        try (FileOutputStream fos = new FileOutputStream("file.csv")) {
            fos.write(os.toByteArray());
        }

        String csv = os.toString();

        BufferedReader r = new BufferedReader(new StringReader(csv));

        String header = r.readLine();
        assertEquals("geom,area,path,name,population,last_census_date,density,visited,null_field", header);

        String firstLine = r.readLine();
        assertEquals(
                "POINT (9.19 45.46),"
                        + "\"MULTIPOLYGON (((9.13 45.52, 9.13 45.42, 9.3 45.42, 9.3 45.52, 9.13 45.52)))\","
                        + "\"LINESTRING (9.18 45.48, 9.19 45.47, 9.2 45.46)\","
                        + "Milan,1378000,2025-01-01T00:00:00Z,1.7976931348623157E308,true,",
                firstLine);
    }
}
