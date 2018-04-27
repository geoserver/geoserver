/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class ProcessParameterIOTest2 extends WPSTestSupport {
    private static GenericApplicationContext context = new GenericApplicationContext();
    static WPSResourceManager resourceManager = GeoServerExtensions.bean(WPSResourceManager.class);
    @BeforeClass
    public static void initAppContext() {
        ProcessParameterIO[] factories = {new CSVPPIO(resourceManager),new WFSPPIO.WFS11(), new WFSPPIO.WFS10(), new WFSPPIO.WFS10Alternate(), new WFSPPIO.WFS11Alternate()}; 
        List<ProcessParameterIO> factoriesList = new ArrayList<>();
        factoriesList.addAll(Arrays.asList(factories));
        PPIOFactory testPPIOFactory = () -> factoriesList;
        context.getBeanFactory().registerSingleton("testPPIOFactory", testPPIOFactory);
        context.refresh();
        new GeoServerExtensions().setApplicationContext(context);
    }


    @AfterClass
    public static void destroyAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testGetWFSByMimeType() {
        Parameter p = new Parameter<>("WFS", SimpleFeatureCollection.class);
        ProcessParameterIO ppio = ProcessParameterIO.find(p, context, "text/xml");
        assertNotNull(ppio);
        assertEquals("Wrong PPIO returned", WFSPPIO.WFS10.class, ppio.getClass());
    }

}
