package org.geoserver.wms.wms_1_3;

import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class CapabilitiesAllowedRequestTypesTest extends WMSTestSupport {

  @Override
  protected void registerNamespaces(Map<String, String> namespaces) {
    namespaces.put("wms", "http://www.opengis.net/wms");
    namespaces.put("ows", "http://www.opengis.net/ows");
  }

  @Test
  public void testGetMapAllowedRequestTypes() throws Exception {
    Document doc =
        getAsDOM(
            "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
            true);
    XpathEngine xpath = XMLUnit.newXpathEngine();

    // Verify that both GET and POST are advertised for GetMap requests.
    NodeList requestTypeNodes =
        xpath.getMatchingNodes(
            "wms:WMS_Capabilities/wms:Capability/wms:Request/wms:GetMap/wms:DCPType/wms:HTTP/wms:Get",
            doc);
    Assert.assertEquals(1, requestTypeNodes.getLength());

    requestTypeNodes =
        xpath.getMatchingNodes(
            "wms:WMS_Capabilities/wms:Capability/wms:Request/wms:GetMap/wms:DCPType/wms:HTTP/wms:Post",
            doc);
    Assert.assertEquals(1, requestTypeNodes.getLength());
  }
}