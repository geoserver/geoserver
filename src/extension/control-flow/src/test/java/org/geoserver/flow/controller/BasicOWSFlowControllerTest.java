package org.geoserver.flow.controller;

import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.ows.Request;

/**
 * This test just checks the basic OWS flow controller matches requests as expecte, for a
 * concurrency test see {@link GlobalFlowControllerTest}
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class BasicOWSFlowControllerTest extends AbstractFlowControllerTest {

    public void testMatchService() {
        BasicOWSController controller = new BasicOWSController("WMS", 1);
        assertFalse(controller.matchesRequest(buildRequest("WFS", "GetFeature", "GML")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GetMap", "image/png")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GetFeatureInfo", "image/png")));
    }
    
    public void testMatchServiceRequest() {
        BasicOWSController controller = new BasicOWSController("WMS", "GetMap", 1);
        assertFalse(controller.matchesRequest(buildRequest("WFS", "GetFeature", "GML")));
        assertTrue(controller.matchesRequest(buildRequest("WMS", "GETMAP", "image/png")));
        assertFalse(controller.matchesRequest(buildRequest("WMS", "GetFeatureInfo", "image/png")));
    }
    
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
