/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.*;


import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.ows.Request;
import org.junit.Test;

/**
 * This test just checks the basic OWS flow controller matches requests as expecte, for a
 * concurrency test see {@link GlobalFlowControllerTest}
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class BasicOWSFlowControllerTest extends AbstractFlowControllerTest {

    @Test
    public void testMatchService() {
        BasicOWSController controller = new BasicOWSController("WMS", 1);
        assertFalse(controller.matchesRequest(buildRequest("WFS", "GetFeature", "GML")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GetMap", "image/png")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GetFeatureInfo", "image/png")));
    }
    
    @Test
    public void testMatchServiceRequest() {
        BasicOWSController controller = new BasicOWSController("WMS", "GetMap", 1);
        assertFalse(controller.matchesRequest(buildRequest("WFS", "GetFeature", "GML")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GETMAP", "image/png")));
        assertFalse(controller.matchesRequest(buildRequest("WMS", "GetFeatureInfo", "image/png")));
    }
    
    @Test
    public void testMatchServiceRequestOutputFormat() {
        BasicOWSController controller = new BasicOWSController("WMS", "GetMap", "image/png", 1);
        assertFalse(controller.matchesRequest(buildRequest("WFS", "GetFeature", "GML")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GETMAP", "image/png")));
        assertFalse(controller.matchesRequest(buildRequest("WMS", "GETMAP", "application/pdf")));
        assertFalse(controller.matchesRequest(buildRequest("WMS", "GetFeatureInfo", "image/png")));
    }

    Request buildRequest(String service, String request, String outputFormat) {
        Request r = new Request();
        r.setService(service);
        r.setRequest(request);
        r.setOutputFormat(outputFormat);

        return r;
    }
}
