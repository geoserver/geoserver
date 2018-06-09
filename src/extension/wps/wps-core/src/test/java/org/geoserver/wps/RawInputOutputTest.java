/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Map;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.process.Processors;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class RawInputOutputTest extends WPSTestSupport {

    private static final int MAX_WAIT_FOR_ASYNCH = 60;

    static {
        Processors.addProcessFactory(RawProcess.getFactory());
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("ani", "http://geoserver.org/wps/animation");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // disable entity resolver as it won't let the tests run in IntelliJ if also GeoTools is
        // loaded...
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setXmlExternalEntitiesEnabled(true);
        getGeoServer().save(global);
    }

    @Test
    public void testGetCapabilities() throws Exception {
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        // print(d);
        // the process was not blacklisted
        assertEquals("1", xp.evaluate("count(//wps:Process[ows:Identifier='gs:Raw'])", d));
    }

    @Test
    public void testDescribeProcess() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:Raw");
        // print(d);

        // check inputs
        String inputBase = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";
        assertXpathEvaluatesTo(
                "application/json",
                inputBase + "/Input[1]/ComplexData/Default/Format/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "application/json",
                inputBase + "/Input[1]/ComplexData/Supported/Format[1]/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "text/xml",
                inputBase + "/Input[1]/ComplexData/Supported/Format[2]/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "0", "count(" + inputBase + "/Input[ows:Identifier='outputMimeType'])", d);

        // check outputs
        String outputBase = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        assertXpathEvaluatesTo(
                "application/json",
                outputBase + "/Output/ComplexOutput/Default/Format/MimeType",
                d);
        assertXpathEvaluatesTo(
                "application/json",
                outputBase + "/Output/ComplexOutput/Supported/Format[1]/MimeType",
                d);
        assertXpathEvaluatesTo(
                "text/xml", outputBase + "/Output/ComplexOutput/Supported/Format[2]/MimeType", d);
    }

    @Test
    public void testExecuteSynchDocument() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Raw</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>data</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA[ABCDE]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='false'>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        // System.out.println(xml);

        Document d = postAsDOM("wps", xml);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessSucceeded", d);

        // TODO: the binary outputs are not encoded right now, add a test when they are
    }

    @Test
    public void testExecuteSynchRaw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Raw</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>data</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA[ABCDE]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/xml\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        // System.out.println(xml);

        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        // System.out.println(response.getContentType());
        assertEquals("text/xml", response.getContentType());
        assertEquals("ABCDE", response.getContentAsString());
    }

    @Test
    public void testExecuteEmbeddedXML() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Raw</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>data</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"text/xml\" xmlns:ani=\"http://geoserver.org/wps/animation\">"
                        + "  <ani:Layer>\n"
                        + "    <ani:Name>test</ani:Name>\n"
                        + "    <ani:Parameter key=\"CQL_FILTER\">sun_elevation%3C51</ani:Parameter>\n"
                        + "  </ani:Layer>\n"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/xml\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        // System.out.println(xml);

        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // the elements are there and the namespace is recognizable
        assertXpathEvaluatesTo("test", "//ani:Layer/ani:Name", dom);
        assertXpathEvaluatesTo("CQL_FILTER", "//ani:Layer/ani:Parameter/@key", dom);
        assertXpathEvaluatesTo("sun_elevation%3C51", "//ani:Layer/ani:Parameter", dom);
    }

    @Test
    public void testExecuteAsynch() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Raw</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>data</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA[ABCDE]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='true' status='true'>"
                        + "<wps:Output asReference='true'>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        // System.out.println(xml);

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:ProcessAccepted", dom);
        String fullStatusLocation = xp.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);

        long start = System.currentTimeMillis();
        long wait;
        while ((wait = (System.currentTimeMillis() - start) / 1000) < 60) {
            dom = getAsDOM(statusLocation);
            // print(dom);
            // are we still waiting for termination?
            if (xp.getMatchingNodes("//wps:Status/wps:ProcessAccepted", dom).getLength() > 0
                    || xp.getMatchingNodes("//wps:Status/wps:ProcessStarted", dom).getLength() > 0
                    || xp.getMatchingNodes("//wps:Status/wps:ProcessQueued", dom).getLength() > 0) {
                Thread.sleep(100);
            } else {
                break;
            }
        }
        if (wait > 60) {
            throw new Exception(
                    "Waited for the process to complete more than " + MAX_WAIT_FOR_ASYNCH);
        }

        // print(dom);
        assertEquals(1, xp.getMatchingNodes("//wps:Status/wps:ProcessSucceeded", dom).getLength());
        String fullReference =
                xp.evaluate(
                        "//wps:ProcessOutputs/wps:Output[ows:Identifier='result']/wps:Reference/@href",
                        dom);
        String reference = fullReference.substring(fullReference.indexOf('?') - 3);

        MockHttpServletResponse response = getAsServletResponse(reference);
        assertEquals("application/json", response.getContentType());
        assertEquals("ABCDE", response.getContentAsString());
    }

    @Test
    public void testReturnNull() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Raw</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>data</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/json\"><![CDATA[ABCDE]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>returnNull</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>true</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";
        // System.out.println(xml);

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        checkValidationErrors(dom);
        assertEquals(1, xp.getMatchingNodes("//wps:Status/wps:ProcessSucceeded", dom).getLength());
        assertEquals(
                "1",
                xp.evaluate(
                        "count(//wps:ProcessOutputs/wps:Output[ows:Identifier='result']/wps:Data/wps:ComplexData)",
                        dom));
        assertEquals(
                "0",
                xp.evaluate(
                        "count(//wps:ProcessOutputs/wps:Output[ows:Identifier='result']/wps:Data/wps:ComplexData/*)",
                        dom));
    }
}
