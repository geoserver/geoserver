package org.geoserver.wfs.xslt;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetFeatureXSLTTest extends WFSTestSupport {

    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        File dd = dataDirectory.getDataDirectoryRoot();
        File wfs = new File(dd, "wfs");
        File transform = new File(wfs, "transform");
        if (transform.exists()) {
            FileUtils.deleteDirectory(transform);
        }
        assertTrue(transform.mkdirs());
        FileUtils.copyDirectory(new File("src/test/resources/org/geoserver/wfs/xslt"), transform);
    }
    

    public void testGetCapabilities() throws Exception {
        // force the list of output formats provided by the xslt output format to be updated 
        XSLTOutputFormatUpdater updater = (XSLTOutputFormatUpdater) applicationContext.getBean("xsltOutputFormatUpdater");
        updater.run();

        // now we can run the request
        Document dom = getAsDOM("wfs?service=wfs&version=1.1.0&request=GetCapabilities");
        // print(dom);
        
        XMLAssert.assertXpathEvaluatesTo("1", 
                "count(//ows:Operation[@name='GetFeature']/ows:Parameter[@name = 'outputFormat']/ows:Value[text() = 'HTML'])", dom);
        XMLAssert.assertXpathEvaluatesTo("1", 
                "count(//ows:Operation[@name='GetFeature']/ows:Parameter[@name = 'outputFormat' and ows:Value = 'text/html; subtype=xslt'])", dom);
    }


    public void testGeneralOutput() throws Exception {
        Document d = getAsDOM("wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS)
                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        // two features
        XMLAssert.assertXpathEvaluatesTo("2", "count(//h2)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//h2[text() = 'Buildings.1107531701010'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//h2[text() = 'Buildings.1107531701011'])", d);

        // check the first
        XMLAssert
                .assertXpathEvaluatesTo(
                        "113",
                        "//h2[text() = 'Buildings.1107531701010']/following-sibling::table/tr[td='cite:FID']/td[2]",
                        d);
        XMLAssert
                .assertXpathEvaluatesTo(
                        "123 Main Street",
                        "//h2[text() = 'Buildings.1107531701010']/following-sibling::table/tr[td='cite:ADDRESS']/td[2]",
                        d);
    }
    

    public void testHeaders() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS)
                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        
        assertEquals("text/html; subtype=xslt", response.getContentType());
        assertEquals("inline; filename=Buildings.html", response.getHeader("Content-Disposition"));
    }
    

    public void testHeadersTwoLayers() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) 
                + "," + getLayerId(MockData.LAKES)
                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        
        assertEquals("text/html; subtype=xslt", response.getContentType());
        assertEquals("inline; filename=Buildings_Lakes.html", response.getHeader("Content-Disposition"));
    }


    public void testLayerSpecific() throws Exception {
        Document d = getAsDOM("wfs?request=GetFeature&typename=" + getLayerId(MockData.BRIDGES)
                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        // just one features
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'ID: Bridges.1107531599613'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'FID: 110'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'Name: Cam Bridge'])", d);
    }
    

    public void testMimeType() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wfs?request=GetFeature&typename=" + getLayerId(MockData.BRIDGES)
                + "&version=1.0.0&service=wfs&outputFormat=HTML");
        assertEquals("text/html", response.getContentType());
        
        Document d = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));

        // just one features
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'ID: Bridges.1107531599613'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'FID: 110'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'Name: Cam Bridge'])", d);
    }
    

    public void testLayerSpecificOnOtherLayer() throws Exception {
        Document d = getAsDOM("wfs?request=GetFeature&typename=" + getLayerId(MockData.BASIC_POLYGONS)
                + "&version=1.1.0&service=wfs&outputFormat=HTML");
        
        checkOws10Exception(d, "InvalidParameterValue", "typeName");
    }


    public void testIncompatibleMix() throws Exception {
        Document d = getAsDOM("wfs?request=GetFeature&typename=" + getLayerId(MockData.BRIDGES)
                + "," + getLayerId(MockData.BUILDINGS)
                + "&version=1.1.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        checkOws10Exception(d, "InvalidParameterValue", "typeName");
    }
}
