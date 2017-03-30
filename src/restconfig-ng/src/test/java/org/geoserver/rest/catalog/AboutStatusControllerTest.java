/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertTrue;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * @author Carlo Cancellieri - GeoSolutions SAS
 */
public class AboutStatusControllerTest extends GeoServerSystemTestSupport {
    
    //TODO: why is AboutStatusController.configurePersister not being called?

    @Test
    public void testGetStatus() throws Exception {
        String xml = getAsString("/restng/about/status.xml");        
        //assertTrue(xml.contains("Available"));
        //assertTrue(xml.contains("Enabled"));
    }

    @Test
    public void testGetSingleModule() throws Exception {
        String xml = getAsString("/restng/about/status/gs-main.xml");
        assertTrue(xml.contains("<name>GeoServer Main</name>"));
        //assertTrue(xml.contains("Enabled"));
    }

    @Test
    public void testMalformedModuleName() throws Exception {
        String xml = getAsString("/restng/about/status/fake1_module.xml");
        assertTrue(xml.contains("No such module"));
    }

}
