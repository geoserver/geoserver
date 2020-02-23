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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.AttributeDescriptor;

/** @author ian */
public class WFSPPIOTest extends WPSTestSupport {

    private InputStream is;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-noargs.xml");
    }

    @Before
    public void prepareInputStream() throws IOException {
        GeoServerExtensions.bean(WPSResourceManager.class);

        is = SystemTestData.class.getResourceAsStream("wfs.xml");
        assertNotNull(is);
    }

    @After
    public void cleanup() throws IOException {
        is.close();
    }

    /** Test method for {@link org.geoserver.wps.ppio.WFSPPIO#decode(java.io.InputStream)}. */
    @Test
    public void testDecodeInputStream() throws Exception {
        SimpleFeatureCollection rawTarget =
                (SimpleFeatureCollection) new WFSPPIO.WFS11().decode(is);

        for (AttributeDescriptor ad : rawTarget.getSchema().getAttributeDescriptors()) {
            final String name = ad.getLocalName();
            if ("metaDataProperty".equalsIgnoreCase(name)) {
                fail("this should be deleted");
            }
        }
    }
}
