/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test WPS execute call's with internal references to WFS.
 *
 * @author Roar Brænden
 */
public class ExecuteInternalWFSTest extends WPSTestSupport {

    private static final String WFS10_SchemaURI = "http://www.opengis.net/wfs";

    private static final String WFS20_SchemaURI = "http://www.opengis.net/wfs/2.0";

    @Test
    public void testGetVersion10() throws Exception {
        mockXmlPostRequest(xml("GET", "1.0"));
    }

    @Test
    public void testPostVersion10() throws Exception {
        mockXmlPostRequest(xml("POST", "1.0"));
    }

    @Test
    public void testPostVersion20() throws Exception {
        mockXmlPostRequest(xml("POST", "2.0"));
    }

    @Test
    public void testGetVersion20() throws Exception {
        mockXmlPostRequest(xml("GET", "2.0"));
    }

    private void mockXmlPostRequest(String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        double area = Double.parseDouble(response.getContentAsString());

        Assert.assertEquals(2.195E-6, area, 1E-9);
    }

    /** Create Reference-element of the WPS call */
    private String xml(String method, String version) {

        String wfsReq = null;
        String wfsUrl = "http://geoserver/wfs";

        if ("POST".equals(method) && "1.0".equals(version)) {
            wfsReq = "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                    + "     <wfs:Query typeName=\"cite:Lakes\">\n"
                    + "     </wfs:Query>\n"
                    + "</wfs:GetFeature>\n";
        } else if ("GET".equals(method) && "1.0".equals(version)) {
            wfsUrl +=
                    "?service=WFS&amp;version=1.0.0&amp;request=GetFeature&amp;outputFormat=GML2&amp;typeName=cite:Lakes";
        } else if ("POST".equals(method) && "2.0".equals(version)) {

            wfsReq = "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" outputFormat=\"GML2\">\n"
                    + "     <wfs:Query typeNames=\"cite:Lakes\">\n"
                    + "     </wfs:Query>\n"
                    + "</wfs:GetFeature>\n";
        } else if ("GET".equals(method) && "2.0".equals(version)) {
            wfsUrl +=
                    "?service=WFS&amp;version=2.0.0&amp;request=GetFeature&amp;outputFormat=GML2&amp;typeNames=cite:Lakes";
        }

        String featuresReference;

        if (wfsReq != null) {
            featuresReference = "                    <wps:Reference mimeType=\"text/xml\" xlink:href=\""
                    + wfsUrl
                    + "\" method=\"POST\">\n"
                    + "                        <wps:Body>\n"
                    + wfsReq
                    + "                        </wps:Body>\n"
                    + "                    </wps:Reference>\n";
        } else {
            featuresReference =
                    "                    <wps:Reference mimeType=\"text/xml\" xlink:href=\"" + wfsUrl + "\" />\n";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"  xmlns:wfs=\""
                + ("1.0".equals(version) ? WFS10_SchemaURI : WFS20_SchemaURI)
                + "\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:cite=\"http://www.opengis.net/cite\""
                + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>geo:area</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>geom</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"application/gml-3.1.1\" xlink:href=\"http://geoserver/wps\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wps:Execute version=\"1.0.0\" service=\"WPS\">\n"
                + "            <ows:Identifier>vec:CollectGeometries</ows:Identifier>\n"
                + "            <wps:DataInputs>\n"
                + "                <wps:Input>\n"
                + "                    <ows:Identifier>features</ows:Identifier>\n"
                + featuresReference
                + "                </wps:Input>\n"
                + "            </wps:DataInputs>\n"
                + "            <wps:ResponseForm>\n"
                + "                <wps:RawDataOutput mimeType=\"application/gml-3.1.1\">\n"
                + "                    <ows:Identifier>result</ows:Identifier>\n"
                + "                </wps:RawDataOutput>\n"
                + "            </wps:ResponseForm>\n"
                + "          </wps:Execute>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>\n";
    }
}
