/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.w3c.dom.Document;

public class WFSDisabledTest extends WFSTestSupport {

    @Rule public final EnvironmentVariables enviromentVariables = new EnvironmentVariables();

    @Test
    public void testDisabledServiceResponse() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEnabled(false);
        getGeoServer().save(wfs);

        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testEnabledServiceResponse() throws Exception {
        enableWFS();

        Document doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=getCapabilities");
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Tests WFS service disabled on layer-resource */
    @Test
    public void testLayerDisabledServiceResponse() throws Exception {
        enableWFS();
        String layerName = "cite:RoadSegments";
        LayerInfo linfo = getCatalog().getLayerByName(layerName);
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(true);
        ri.setDisabledServices(new ArrayList<>(Arrays.asList("WFS")));
        getCatalog().save(ri);
        getCatalog().save(linfo);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + layerName
                                + "&version=1.0.0&service=wfs");
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
    }

    /** Tests WFS service disabled on layer-resource, by environment variable */
    @Test
    public void testLayerEnvDisabledServiceResponse() throws Exception {
        enableWFS();
        enviromentVariables.set("org.geoserver.service.disabled", "WFS,WPS");
        String layerName = "cite:RoadSegments";
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + layerName
                                + "&version=1.0.0&service=wfs");
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
        enviromentVariables.clear("org.geoserver.service.disabled");
    }

    /** Tests WFS service enabled on layer-resource */
    @Test
    public void testLayerEnabledServiceResponse() throws Exception {
        enableWFS();
        String layerName = "cite:RoadSegments";
        LayerInfo linfo = getCatalog().getLayerByName(layerName);
        ResourceInfo ri = linfo.getResource();
        ri.setServiceConfiguration(false);
        ri.setDisabledServices(new ArrayList<>());
        getCatalog().save(ri);
        getCatalog().save(linfo);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + layerName
                                + "&version=1.0.0&service=wfs");
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
    }

    private void enableWFS() {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEnabled(true);
        getGeoServer().save(wfs);
    }
}
