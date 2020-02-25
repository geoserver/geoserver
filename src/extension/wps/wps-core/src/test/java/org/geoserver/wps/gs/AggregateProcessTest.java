/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class AggregateProcessTest extends WPSTestSupport {

    @Test
    public void testSum() throws Exception {
        String xml = aggregateCall("Sum");

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("-111.0", "/AggregationResults/Sum", dom);
    }

    @Test
    public void testMin() throws Exception {
        String xml = aggregateCall("Min");

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("-900.0", "/AggregationResults/Min", dom);
    }

    @Test
    public void testMax() throws Exception {
        String xml = aggregateCall("Max");

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("300.0", "/AggregationResults/Max", dom);
    }

    @Test
    public void testAverage() throws Exception {
        String xml = aggregateCall("Average");

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("-22.2", "/AggregationResults/Average", dom);
    }

    @Test
    public void testStdDev() throws Exception {
        String xml = aggregateCall("StdDev");

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(
                xpath.evaluate("/AggregationResults/StandardDeviation", dom)
                        .matches("442\\.19380.*"));
    }

    @Test
    public void testNonRawOutput() throws Exception {
        String xml = aggregateCall("StdDev", false);
        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//AggregationResults/*)", dom);
    }

    private String aggregateCall(String function) {
        return aggregateCall(function, true);
    }

    private String aggregateCall(String function, boolean rawOutput) {
        return aggregateCall(function, rawOutput, "text/xml", false);
    }

    private String aggregateCall(
            String function, boolean rawOutput, String mimeType, boolean groupBy) {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Aggregate</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getWFSQueryLayerId(MockData.PRIMITIVEGEOFEATURE)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>aggregationAttribute</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>intProperty</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>function</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>"
                        + function
                        + "</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n";
        if (groupBy) {
            xml +=
                    "    <wps:Input>\n"
                            + "      <ows:Identifier>groupByAttributes</ows:Identifier>\n"
                            + "      <wps:Data>\n"
                            + "        <wps:LiteralData>name</wps:LiteralData>\n"
                            + "      </wps:Data>\n"
                            + "    </wps:Input>\n";
        }
        xml += "  </wps:DataInputs>\n" + " <wps:ResponseForm>\n";
        if (rawOutput) {
            xml +=
                    "    <wps:RawDataOutput mimeType=\""
                            + mimeType
                            + "\">\n"
                            + "      <ows:Identifier>result</ows:Identifier>\n"
                            + "    </wps:RawDataOutput>\n";
        } else {
            xml +=
                    "    <wps:Output mimeType=\""
                            + mimeType
                            + "\">\n"
                            + "      <ows:Identifier>result</ows:Identifier>\n"
                            + "    </wps:Output>\n";
        }

        xml += "  </wps:ResponseForm>\n" + "</wps:Execute>";
        return xml;
    }

    @Test
    public void testAllOneByOne() throws Exception {
        String xml = callAll(false);

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("5", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("-111.0", "/AggregationResults/Sum", dom);
        assertXpathEvaluatesTo("-900.0", "/AggregationResults/Min", dom);
        assertXpathEvaluatesTo("300.0", "/AggregationResults/Max", dom);
        assertXpathEvaluatesTo("-22.2", "/AggregationResults/Average", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(
                xpath.evaluate("/AggregationResults/StandardDeviation", dom)
                        .matches("442\\.19380.*"));
    }

    @Test
    public void testAllSinglePass() throws Exception {
        String xml = callAll(true);

        Document dom = postAsDOM(root(), xml);
        // print(dom);
        assertXpathEvaluatesTo("5", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("-111.0", "/AggregationResults/Sum", dom);
        assertXpathEvaluatesTo("-900.0", "/AggregationResults/Min", dom);
        assertXpathEvaluatesTo("300.0", "/AggregationResults/Max", dom);
        assertXpathEvaluatesTo("-22.2", "/AggregationResults/Average", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(
                xpath.evaluate("/AggregationResults/StandardDeviation", dom)
                        .matches("442\\.19380.*"));
    }

    private String callAll(boolean singlePass) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:Aggregate</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                + "            <wfs:Query typeName=\""
                + getWFSQueryLayerId(MockData.PRIMITIVEGEOFEATURE)
                + "\"/>\n"
                + "          </wfs:GetFeature>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>aggregationAttribute</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>intProperty</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>function</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>Min</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>function</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>Max</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>function</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>Average</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>function</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>Sum</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>function</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>StdDev</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>singlePass</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>"
                + singlePass
                + "</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";
    }

    @Test
    public void testSumAsJson() throws Exception {
        String xml = aggregateCall("Sum", true, "application/json", false);
        JSONObject result = executeJsonRequest(xml);
        assertTrue(result.size() == 4);
        String aggregationAttribute = (String) result.get("AggregationAttribute");
        JSONArray aggregationFunctions = (JSONArray) result.get("AggregationFunctions");
        JSONArray groupByAttributes = (JSONArray) result.get("GroupByAttributes");
        JSONArray aggregationResults = (JSONArray) result.get("AggregationResults");
        assertTrue(aggregationAttribute.equals("intProperty"));
        assertTrue(aggregationFunctions.size() == 1);
        assertTrue(aggregationFunctions.get(0).equals("Sum"));
        assertTrue(groupByAttributes.size() == 0);
        assertTrue(aggregationResults.size() == 1);
        JSONArray sumResult = (JSONArray) aggregationResults.get(0);
        assertTrue(sumResult.size() == 1);
        assertTrue(sumResult.get(0).equals(-111L));
    }

    @Test
    public void testSumWithGroupBy() throws Exception {
        String xml = aggregateCall("Sum", true, "text/xml", true);
        Document dom = postAsDOM(root(), xml);
        assertXpathEvaluatesTo("1", "count(/AggregationResults/*)", dom);
        assertXpathEvaluatesTo("5", "count(/AggregationResults/GroupByResult/*)", dom);
    }

    @Test
    public void testSumAsJsonWithGroupBy() throws Exception {
        String xml = aggregateCall("Sum", true, "application/json", true);
        JSONObject result = executeJsonRequest(xml);
        assertTrue(result.size() == 4);
        String aggregationAttribute = (String) result.get("AggregationAttribute");
        JSONArray aggregationFunctions = (JSONArray) result.get("AggregationFunctions");
        JSONArray groupByAttributes = (JSONArray) result.get("GroupByAttributes");
        JSONArray aggregationResults = (JSONArray) result.get("AggregationResults");
        assertTrue(aggregationAttribute.equals("intProperty"));
        assertTrue(aggregationFunctions.size() == 1);
        assertTrue(aggregationFunctions.get(0).equals("Sum"));
        assertTrue(groupByAttributes.size() == 1);
        assertTrue(groupByAttributes.get(0).equals("name"));
        assertTrue(aggregationResults.size() == 5);
    }

    protected JSONObject executeJsonRequest(String wpsRequest) throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(getWorkspaceAndServicePath(), wpsRequest);
        String content = response.getContentAsString();
        assertFalse(content.isEmpty());
        Object jsonObject = new JSONParser().parse(content);
        assertNotNull(jsonObject);
        return (JSONObject) jsonObject;
    }

    protected String getWorkspaceAndServicePath() {
        return "wps?";
    }

    protected String getWFSQueryLayerId(QName layerName) {
        return getLayerId(layerName);
    }

    /**
     * Extends AggregateProcessTest to support local workspace resolution cases
     *
     * @author Geosolutions
     */
    public static class LocalWSTest extends AggregateProcessTest {

        @Override
        protected String getWorkspaceAndServicePath() {
            return MockData.PRIMITIVEGEOFEATURE.getPrefix() + "/wps?";
        }

        @Override
        protected String root() {
            return MockData.PRIMITIVEGEOFEATURE.getPrefix() + "/wps?";
        }

        @Override
        protected String getWFSQueryLayerId(QName layerName) {
            return layerName.getLocalPart();
        }
    }
}
