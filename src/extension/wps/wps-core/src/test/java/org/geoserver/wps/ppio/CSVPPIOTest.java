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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

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
        SimpleFeatureCollection states =
                (SimpleFeatureCollection) new CSVPPIO(resourceManager).decode(is);

        assertEquals("Wrong number of states", 51, states.size());
        assertEquals("Wrong number of columns", 9, states.getSchema().getAttributeCount());

        Filter filter = CQL.toFilter("State = 'Alabama'");

        SimpleFeatureCollection alabama = states.subCollection(filter);

        assertEquals("inc1995 wrong", 19683, DataUtilities.first(alabama).getAttribute("inc1995"));
    }

    @Test
    public void testEncodeOutputStream() throws Exception {

        SimpleFeatureCollection states =
                (SimpleFeatureCollection) new CSVPPIO(resourceManager).decode(is);

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
                assertEquals(attribs[0], "State");
                assertEquals(attribs[1], "inc1980");
                assertEquals(attribs[4], "inc2000");
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
}
