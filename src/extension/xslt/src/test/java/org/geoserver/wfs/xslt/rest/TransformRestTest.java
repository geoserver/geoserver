/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.wfs.xslt.XSLTTestSupport;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class TransformRestTest extends XSLTTestSupport {

    private XpathEngine xpath;
    private TransformRepository repository;

    @Before
    public void prepare() throws IOException {
        xpath = XMLUnit.newXpathEngine();
        repository = (TransformRepository) applicationContext.getBean("transformRepository");

        File dd = testData.getDataDirectoryRoot();
        File wfs = new File(dd, "wfs");
        File transform = new File(wfs, "transform");
        deleteDirectory(transform);
        assertTrue(transform.mkdirs());
        FileUtils.copyDirectory(new File("src/test/resources/org/geoserver/wfs/xslt"), transform);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("h", "http://www.w3.org/1999/xhtml");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Test
    public void testListHTML() throws Exception {
        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/transforms.html");
        // print(d);

        assertEquals("1", xpath.evaluate("count(//h:h2)", d));
        assertEquals("XSLT transformations:", xpath.evaluate("/h:html/h:body/h:h2", d));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/services/wfs/transforms/general.html",
                xpath.evaluate(("//h:li[h:a = 'general']/h:a/@href"), d));
    }

    @Test
    public void testListXML() throws Exception {
        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/transforms.xml");
        // print(d);

        assertEquals("3", xpath.evaluate("count(//transform)", d));
        assertEquals(
                "http://localhost:8080/geoserver"
                        + RestBaseController.ROOT_PATH
                        + "/services/wfs/transforms/general.xml",
                xpath.evaluate("//transform[name='general']/atom:link/@href", d));
    }

    @Test
    public void testGetTransformHTML() throws Exception {
        Document d =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/transforms/general.html");
        // print(d);

        assertEquals(
                "Source format: \"text/xml; subtype=gml/2.1.2\"", xpath.evaluate("//h:li[1]", d));
        assertEquals("Output format: \"text/html; subtype=xslt\"", xpath.evaluate("//h:li[2]", d));
        assertEquals("File extension: \"html\"", xpath.evaluate("//h:li[3]", d));
        assertEquals("XSLT transform: \"general.xslt\"", xpath.evaluate("//h:li[4]", d));
    }

    @Test
    public void testGetTransformXML() throws Exception {
        Document d =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wfs/transforms/general.xml");
        // print(d);

        assertEquals("text/xml; subtype=gml/2.1.2", xpath.evaluate("//sourceFormat", d));
        assertEquals("text/html; subtype=xslt", xpath.evaluate("//outputFormat", d));
        assertEquals("html", xpath.evaluate("//fileExtension", d));
        assertEquals("general.xslt", xpath.evaluate("//xslt", d));
    }

    @Test
    public void testPostXML() throws Exception {
        String xml =
                "<transform>\n"
                        + "  <name>buildings</name>\n"
                        + //
                        "  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n"
                        + //
                        "  <outputFormat>text/html</outputFormat>\n"
                        + //
                        "  <fileExtension>html</fileExtension>\n"
                        + //
                        "  <xslt>buildings.xslt</xslt>\n"
                        + //
                        "  <featureType>\n"
                        + //
                        "    <name>cite:Buildings</name>\n"
                        + //
                        "  </featureType>\n"
                        + "</transform>\n";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/transforms", xml);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                ""
                                        + RestBaseController.ROOT_PATH
                                        + "/services/wfs/transforms/buildings"));

        TransformInfo info = repository.getTransformInfo("buildings");
        assertNotNull(info);
    }

    @Test
    public void testPostXSLT() throws Exception {
        String xslt =
                FileUtils.readFileToString(
                        new File("src/test/resources/org/geoserver/wfs/xslt/general2.xslt"),
                        "UTF-8");

        // test for missing params
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/transforms?name=general2",
                        xslt,
                        "application/xslt+xml");
        assertEquals(400, response.getStatus());

        // now pass all
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/services/wfs/transforms?name=general2&sourceFormat=gml&outputFormat=HTML&outputMimeType=text/html",
                        xslt,
                        "application/xslt+xml");
        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                ""
                                        + RestBaseController.ROOT_PATH
                                        + "/services/wfs/transforms/general2"));

        TransformInfo info = repository.getTransformInfo("general2");
        assertNotNull(info);
        assertEquals("gml", info.getSourceFormat());
        assertEquals("HTML", info.getOutputFormat());
        assertEquals("text/html", info.getOutputMimeType());
    }

    @Test
    public void testPutXML() throws Exception {
        // let's change the output format
        String xml =
                "<transform>\n"
                        + "  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n"
                        + "  <outputFormat>text/html</outputFormat>\n"
                        + "  <fileExtension>html</fileExtension>\n"
                        + "  <xslt>general.xslt</xslt>\n"
                        + "</transform>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/transforms/general",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        TransformInfo info = repository.getTransformInfo("general");
        assertEquals("text/html", info.getOutputFormat());
    }

    @Test
    public void testPutXSLT() throws Exception {
        String xslt =
                FileUtils.readFileToString(
                        new File("src/test/resources/org/geoserver/wfs/xslt/general2.xslt"),
                        "UTF-8");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/transforms/general",
                        xslt,
                        "application/xslt+xml");
        assertEquals(200, response.getStatus());

        TransformInfo info = repository.getTransformInfo("general");
        String actual;
        try (InputStream is = repository.getTransformSheet(info)) {
            actual = IOUtils.toString(is, "UTF-8");
        }

        assertEquals(xslt, actual);
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wfs/transforms/general");
        assertEquals(200, response.getStatus());

        TransformInfo info = repository.getTransformInfo("general");
        assertNull(info);
    }
}
