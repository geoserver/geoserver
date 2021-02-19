/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

/** @author prushfor */
public class MapMLGetFeatureOutputFormatTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        HashMap<String, String> m = new HashMap<>();
        m.put("html", "http://www.w3.org/1999/xhtml/");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);

        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(false);
        getGeoServer().save(wfs);
    }

    @Test
    public void testMapMLOutputFormatLinks() throws Exception {
        String licenseLink = "https://example.org/license/";
        String licenseTitle = "Test License Title";
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@title)", doc);

        FeatureTypeInfo layerInfo = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.put("mapml.licenseLink", licenseLink);
        layerMeta.put("mapml.licenseTitle", licenseTitle);
        getCatalog().save(layerInfo);

        layerMeta = getFeatureTypeInfo(SystemTestData.FIFTEEN).getMetadata();
        assertTrue(layerMeta.containsKey("mapml.licenseLink"));
        assertTrue(layerMeta.containsKey("mapml.licenseTitle"));
        assertTrue(layerMeta.get("mapml.licenseLink").toString().equalsIgnoreCase(licenseLink));
        assertTrue(layerMeta.get("mapml.licenseTitle").toString().equalsIgnoreCase(licenseTitle));

        doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo(licenseLink, "//html:link[@rel='license']/@href", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:link[@rel='license']/@title", doc);

        layerInfo = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        layerMeta = layerInfo.getMetadata();
        layerMeta.remove("mapml.licenseLink");
        layerMeta.put("mapml.licenseTitle", "Test License Title");
        getCatalog().save(layerInfo);

        doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:link[@rel='license']/@title", doc);
    }

    @Test
    public void testMapMLOutputFormatCoordinates() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");
        vars.put("crs", "EPSG:3857");

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
    }
    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @param query A map representing kvp to be used by the request.
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path, HashMap<String, String> query)
            throws Exception {
        MockHttpServletRequest request = createRequest(path, query);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }
}
