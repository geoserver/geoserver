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
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.FeatureTypeInfo;
import org.junit.After;
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
        FeatureTypeInfo ftinfo = getCatalog().getFeatureTypeByName(layerName);
        ftinfo.setServiceConfiguration(true);
        ftinfo.setDisabledServices(new ArrayList<>(Arrays.asList("WFS")));
        getCatalog().save(ftinfo);
        // check GetFeature
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + layerName
                                + "&version=1.0.0&service=wfs");
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
        // check GetCapabilities
        doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=getCapabilities");
        XMLAssert.assertXpathNotExists(
                "//wfs:FeatureTypeList/wfs:FeatureType/wfs:Name[.='" + layerName + "']", doc);
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
        // GetCapabilities
        doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=getCapabilities");
        XMLAssert.assertXpathNotExists(
                "//wfs:FeatureTypeList/wfs:FeatureType/wfs:Name[.='" + layerName + "']", doc);
    }

    @After
    public void clearEnviromentVariables() {
        enviromentVariables.clear("org.geoserver.service.disabled");
    }

    /** Tests WFS service enabled on layer-resource */
    @Test
    public void testLayerEnabledServiceResponse() throws Exception {
        enableWFS();
        String layerName = "cite:RoadSegments";
        FeatureTypeInfo ftinfo = getCatalog().getFeatureTypeByName(layerName);
        ftinfo.setServiceConfiguration(false);
        ftinfo.setDisabledServices(new ArrayList<>());
        getCatalog().save(ftinfo);
        // GetFeature
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + layerName
                                + "&version=1.0.0&service=wfs");
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        // GetCapabilities
        doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=getCapabilities");
        XMLAssert.assertXpathExists(
                "//wfs:FeatureTypeList/wfs:FeatureType/wfs:Name[.='" + layerName + "']", doc);
    }

    private void enableWFS() {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setEnabled(true);
        getGeoServer().save(wfs);
    }
}
