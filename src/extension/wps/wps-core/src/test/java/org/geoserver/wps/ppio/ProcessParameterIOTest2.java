/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.List;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.Parameter;
import org.junit.AfterClass;
import org.junit.Test;

public class ProcessParameterIOTest2 extends WPSTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-noargs.xml");
    }

    @AfterClass
    public static void destroyAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testGetWFSByMimeType() {
        Parameter p = new Parameter<>("WFS", WFSPPIO.class);
        ProcessParameterIO ppio = ProcessParameterIO.find(p, null, "text/xml");
        // System.out.println(ppio);
    }
}
