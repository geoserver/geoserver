/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.geotools.data.DataUtilities;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Test paging with app-schema.
 * 
 * @author Rini Angreani, CSIRO Mineral Resources Flagship
 */
public class PagingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        FeatureGML32MockData mockData = new FeatureGML32MockData();
        mockData.addStyle("namefilter", "styles/mappedfeaturebyname.sld");
        return mockData;
    }

    @Test
    public void testWfs110GetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&maxFeatures=2&startIndex=2");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf3 and mf4
        assertXpathCount(2, "//gsml:MappedFeature", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf3", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());

        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf4", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
    }

    @Test
    public void testWfs200GetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=2.0.0&typeNames=gsml:MappedFeature&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typeNames=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf2
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature/@gml:id", doc);
    }

    @Test
    public void testGetFeatureDenormalised() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=2.0.0&typeNames=gsml:GeologicUnit&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typeNames=gsml:GeologicUnit response:\n" + prettyString(doc));
        // expecting gu.25682
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.25682", "//gsml:GeologicUnit/@gml:id", doc);
    }

    @Test
    public void testGetFeatureSortBy() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&maxFeatures=2&startIndex=2&sortBy=gsml:specification");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(2, "//gsml:MappedFeature", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf4", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());

        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf1", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
    }

    @Test
    public void testGetFeatureSortByDenormalised() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&outputFormat=gml32&maxFeatures=2&startIndex=0&sortBy=gml:name");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(2, "//gsml:GeologicUnit", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:GeologicUnit").item(0);
        assertEquals("gu.25682", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());

        Node secondNode = doc.getElementsByTagName("gsml:GeologicUnit").item(1);
        assertEquals("gu.25699", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
    }

    @Test
    public void testGetFeatureReproject() {
        Document doc = null;
        doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&srsName=EPSG:4283&bbox=52.5,-1.3,52.51,-1.29&startIndex=1");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }

    @Test
    public void testGetFeatureWithFilter() {
        String xml = //
        "<wfs:GetFeature "
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                + "xsi:schemaLocation=\"" //
                + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                + AbstractAppSchemaMockData.GSML_URI
                + " "
                + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                + "\" "
                + "outputFormat=\"gml32\" "
                + "startIndex=\"1\" "
                + ">"
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                + "                <ogc:Literal>M*</ogc:Literal>"
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                + "            </ogc:PropertyIsLike>" + "        </ogc:Filter>"
                + "    </wfs:Query> " + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);

        xml = //
        "<wfs:GetFeature "
                + "service=\"WFS\" " //
                + "version=\"2.0.0\" " //
                + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                + "xsi:schemaLocation=\"" //
                + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
                + AbstractAppSchemaMockData.GSML_URI
                + " "
                + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                + "\" " + "startIndex=\"0\" count=\"1\" " + ">"
                + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" + "        <fes:Filter>"
                + "            <fes:PropertyIsEqualTo>"
                + "                <fes:Literal>-Py</fes:Literal>"
                + "                <fes:ValueReference>gml:name</fes:ValueReference>"
                + "            </fes:PropertyIsEqualTo>" + "        </fes:Filter>"
                + "    </wfs:Query> " + "</wfs:GetFeature>";
        validate(xml);

        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.25678", "//gsml:GeologicUnit/@gml:id", doc);
    }

    // TODO: will enable this with post filtering changes
    // @Test
    // public void testGetFeatureWithFilter() {
    // String xml = //
    // "<wfs:GetFeature "
    // + "service=\"WFS\" " //
    // + "version=\"1.1.0\" " //
    // + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
    // + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
    // + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
    // + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
    // + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
    // + "xsi:schemaLocation=\"" //
    // + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
    // + AbstractAppSchemaMockData.GSML_URI
    // + " "
    // + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
    // + "\" "
    // + "outputFormat=\"gml32\" "
    // + "startIndex=\"1\" "
    // + ">"
    // + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
    // + "        <ogc:Filter>"
    // + "            <ogc:PropertyIsEqualTo>"
    // + "                <ogc:Literal>Yaugher Volcanic Group 2</ogc:Literal>"
    // + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>"
    // + "            </ogc:PropertyIsEqualTo>"
    // + "        </ogc:Filter>"
    // + "    </wfs:Query> "
    // + "</wfs:GetFeature>";
    //
    // Document doc = postAsDOM("wfs", xml);
    // LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
    // assertXpathCount(1, "//gsml:MappedFeature", doc);
    // assertXpathEvaluatesTo("mf3", "//gsml:MappedFeature/@gml:id", doc);
    // xml = //
    // "<wfs:GetFeature "
    // + "service=\"WFS\" " //
    // + "version=\"2.0.0\" " //
    // + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
    // + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
    // + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
    // + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
    // + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
    // + "xsi:schemaLocation=\"" //
    // + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
    // + AbstractAppSchemaMockData.GSML_URI
    // + " "
    // + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
    // + "\" "
    // + "startIndex=\"0\" count=\"1\" "
    // + ">"
    // + "    <wfs:Query typeNames=\"gsml:MappedFeature\">"
    // + "        <fes:Filter>"
    // + "            <fes:PropertyIsEqualTo>"
    // + "                <fes:Literal>Yaugher Volcanic Group 2</fes:Literal>"
    // + "                <fes:ValueReference>gsml:specification/gsml:GeologicUnit/gml:name</fes:ValueReference>"
    // + "            </fes:PropertyIsEqualTo>"
    // + "        </fes:Filter>"
    // + "    </wfs:Query> "
    // + "</wfs:GetFeature>";
    // validate(xml);
    //
    // doc = postAsDOM("wfs", xml);
    // LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
    // assertXpathCount(1, "//gsml:MappedFeature", doc);
    // assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature/@gml:id", doc);
    // }
    //
    @Test
    public void testGetFeatureWithCSVFormat() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=2.0.0&typeNames=gsml:MappedFeature&count=1&startIndex=1&outputFormat=csv");

        // check the mime type
        assertEquals("text/csv", resp.getContentType());

        // check the content disposition
        assertEquals("attachment; filename=MappedFeature.csv",
                resp.getHeader("Content-Disposition"));

        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = CSVOutputFormatTest.readLines(resp.getOutputStreamContent());

        // we should have one header line and then all the features in that feature type
        assertEquals(2, lines.size());

        assertEquals("mf2", lines.get(1)[0]);
    }

    @Test
    public void testGetMap() throws IOException {
        InputStream is = getBinary("wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=namefilter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png&startIndex=1");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap", imageBuffer, Color.WHITE);
        ImageAssert
                .assertEquals(
                        DataUtilities.urlToFile(getClass().getResource(
                                "/test-data/img/mappedfeature.png")), imageBuffer, 10);
    }

}
