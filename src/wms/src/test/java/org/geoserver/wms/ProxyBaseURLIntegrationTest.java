/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class ProxyBaseURLIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /** Do the links in getcaps respect the proxy base url? */
    @Test
    public void testProxyBaseUrl() throws Exception {
        // setup the proxy base
        final String proxyBaseUrl = "http://localhost/proxy";
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setProxyBaseUrl(proxyBaseUrl);
        getGeoServer().save(gs);

        // setup the wms online resource
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        String geoserverSite = "http://www.geoserver.org";
        wms.setOnlineResource(geoserverSite);
        getGeoServer().save(wms);

        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1");
        // print(dom);

        String serviceOnlineRes = "/WMT_MS_Capabilities/Service/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(geoserverSite, serviceOnlineRes, dom);

        String getCapsGet =
                "/WMT_MS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getCapsGet, dom);

        String getCapsPost =
                "/WMT_MS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Post/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getCapsPost, dom);

        String getMapGet =
                "/WMT_MS_Capabilities/Capability/Request/GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getMapGet, dom);

        String getFeatureInfoGet =
                "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getFeatureInfoGet, dom);

        String getFeatureInfoPost =
                "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/DCPType/HTTP/Post/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getFeatureInfoPost, dom);

        String describeLayerGet =
                "/WMT_MS_Capabilities/Capability/Request/DescribeLayer/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", describeLayerGet, dom);

        String getLegentGet =
                "/WMT_MS_Capabilities/Capability/Request/GetLegendGraphic/DCPType/HTTP/Get/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo(proxyBaseUrl + "/wms?SERVICE=WMS&", getLegentGet, dom);
    }
}
