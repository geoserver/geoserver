package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HTMLGetComplexFeatureResponseTest extends TemplateComplexTestSupport {

    private static final String MF_HTML = "HTMLMappedFeature";
    private static final String MF_HTML_PARAM = "&" + MF_HTML + "=true";

    private static final String MF_HTML_JSONLD = "HTMLMappedFeatureJSONLD";
    private static final String MF_HTML_PARAM_JSONLD = "&" + MF_HTML_JSONLD + "=true";

    private static final String MF_JSON_LD_TEMPLATE = "MappedFeatureJSONLDForHTML";

    private static final String MF_JSON_LD_PARAM = "&" + MF_JSON_LD_TEMPLATE + "=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "HTMLMappedFeature.xhtml";
        setUpTemplate(
                "requestParam('" + MF_HTML + "')='true'",
                SupportedFormat.HTML,
                templateMappedFeature,
                MF_HTML,
                ".xhtml",
                "gsml",
                mappedFeature);

        String htmlWithJSONLD = "HTMLMappedFeatureWithJSONLD.xhtml";
        setUpTemplate(
                "requestParam('" + MF_HTML_JSONLD + "')='true'",
                SupportedFormat.HTML,
                htmlWithJSONLD,
                MF_HTML_JSONLD,
                ".xhtml",
                "gsml",
                mappedFeature);

        String jsonLDMappedFeature = "ManagedMappedFeatureJSONLD.json";
        setUpTemplate(
                "requestParam('" + MF_JSON_LD_TEMPLATE + "')='true'",
                SupportedFormat.JSONLD,
                jsonLDMappedFeature,
                MF_JSON_LD_TEMPLATE,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void getMappedFeature() throws Exception {
        org.jsoup.nodes.Document doc =
                getAsJSoup(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text/html"
                                + MF_HTML_PARAM);
        assertEquals(1, doc.select("script").size());
        assertHTMLResult(doc);
    }

    @Test
    public void getMappedFeatureHTMLWithJSONLD() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text/html"
                                + MF_HTML_PARAM_JSONLD
                                + MF_JSON_LD_PARAM);

        assertXpathCount(2, "//html/head/script", doc);
        NodeList list = doc.getElementsByTagName("script");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            Node type = node.getAttributes().getNamedItem("type");
            if (type != null && type.getTextContent().equals("application/ld+json")) {
                JSONObject jsonLD = (JSONObject) JSONSerializer.toJSON(node.getTextContent());
                Object context = jsonLD.get("@context");
                checkContext(context);
                assertNotNull(context);
                JSONArray features = (JSONArray) jsonLD.get("features");
                assertEquals(5, features.size());
                for (int j = 0; j < features.size(); j++) {
                    JSONObject feature = (JSONObject) features.get(i);
                    checkMappedFeatureJSON(feature);
                }
            }
        }

        assertHTMLResultXPath(doc);
    }

    @Test
    public void getMappedFeatureHTMLWithJSONLDTemplateMissing() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text/html"
                                + MF_HTML_PARAM_JSONLD);
        assertTrue(resp.getContentAsString().contains("Unable to find a JSON-LD template"));
    }

    @Test
    public void testEscaping() throws Exception {
        org.jsoup.nodes.Document doc =
                getAsJSoup(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature"
                                + "&outputFormat=text/html"
                                + MF_HTML_PARAM);
        assertFalse(doc.select("li:contains(60°)").isEmpty());
        assertHTMLResult(doc);
    }

    private void assertHTMLResultXPath(Document doc) {
        assertXpathCount(1, "//html/head/style", doc);
        assertXpathCount(5, "//html/body/ul/li[./span = 'MappedFeature']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul[./li = 'mf1']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul[./li = 'mf2']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul[./li = 'mf3']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul[./li = 'mf4']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul[./li = 'mf5']", doc);

        assertXpathCount(1, "//html/body/ul/li/ul/li/ul[./li = 'GUNTHORPE FORMATION']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul[./li = 'MERCIA MUDSTONE GROUP']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul[./li = 'CLIFTON FORMATION']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul[./li = 'MURRADUC BASALT']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul[./li = 'IDONTKNOW']", doc);

        assertXpathCount(5, "//html/body/ul/li/ul/li[./span = 'Shape']", doc);

        assertXpathCount(4, "//html/body/ul/li/ul/li[./span = 'Specifications']", doc);
        assertXpathCount(4, "//html/body/ul/li/ul/li/ul/li[./span = 'Geologic Unit']", doc);
        assertXpathCount(4, "//html/body/ul/li/ul/li/ul/li/ul/li[./span = 'Purpose']", doc);
        assertXpathCount(4, "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = 'instance']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = 'New Group']", doc);
        assertXpathCount(1, "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = '-Xy']", doc);
        assertXpathCount(
                1, "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = 'Yaugher Volcanic Group']", doc);
        assertXpathCount(
                2,
                "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = 'Yaugher Volcanic Group 1']",
                doc);
        assertXpathCount(
                2,
                "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = 'Yaugher Volcanic Group 2']",
                doc);
        assertXpathCount(3, "//html/body/ul/li/ul/li/ul/li/ul/li/ul[./li = '-Py']", doc);
        assertXpathCount(
                6, "//html/body/ul/li/ul/li/ul/li/ul/li[./span = 'Composition Parts']", doc);
        assertXpathCount(6, "//html/body/ul/li/ul/li/ul/li/ul/li/ul/li[./span = 'Part']", doc);
        assertXpathCount(
                6, "//html/body/ul/li/ul/li/ul/li/ul/li/ul/li/ul/li[./span = 'Role']", doc);
        assertXpathCount(
                5,
                "//html/body/ul/li/ul/li/ul/li/ul/li/ul/li/ul/li/ul[./li = 'interbedded component']",
                doc);
        assertXpathCount(
                1,
                "//html/body/ul/li/ul/li/ul/li/ul/li/ul/li/ul/li/ul[./li = 'fictitious component']",
                doc);
    }

    private void assertHTMLResult(org.jsoup.nodes.Document doc) {
        assertEquals(1, doc.select("style").size());
        assertEquals(5, doc.select("span.caret:contains(MappedFeature)").size());
        assertEquals(1, doc.select("li ul li:contains(mf1)").size());
        assertEquals(1, doc.select("li ul li:contains(mf2)").size());
        assertEquals(1, doc.select("li ul li:contains(mf3)").size());
        assertEquals(1, doc.select("li ul li:contains(mf4)").size());
        assertEquals(1, doc.select("li ul li:contains(mf5)").size());

        assertEquals(1, doc.select("ul li ul li ul li:contains(GUNTHORPE FORMATION)").size());
        assertEquals(1, doc.select("ul li ul li ul li:contains(MERCIA MUDSTONE GROUP)").size());
        assertEquals(1, doc.select("ul li ul li ul li:contains(CLIFTON FORMATION)").size());
        assertEquals(1, doc.select("ul li ul li ul li:contains(MURRADUC BASALT)").size());
        assertEquals(1, doc.select("ul li ul li ul li:contains(IDONTKNOW)").size());

        assertEquals(5, doc.select("span:contains(Shape)").size());

        assertEquals(4, doc.select("ul li ul li span:contains(Specifications)").size());
        assertEquals(4, doc.select("ul li ul li span:contains(Geologic Unit)").size());
        assertEquals(4, doc.select("ul li ul li span:contains(Purpose)").size());
        assertEquals(4, doc.select("ul li ul li ul li ul li ul li:contains(instance)").size());
        assertEquals(1, doc.select("ul li ul li ul li ul li ul li:contains(New Group)").size());
        assertEquals(1, doc.select("ul li ul li ul li ul li ul li:contains(-Xy)").size());

        assertEquals(
                "Yaugher Volcanic Group",
                doc.select(
                                "ul li ul li ul li ul li ul li:contains(Yaugher Volcanic Group):first-child")
                        .get(0)
                        .text());
        assertEquals(
                2,
                doc.select("ul li ul li ul li ul li ul li:contains(Yaugher Volcanic Group 1)")
                        .size());
        assertEquals(
                2,
                doc.select("ul li ul li ul li ul li ul li:contains(Yaugher Volcanic Group 2)")
                        .size());

        assertEquals(3, doc.select("ul li ul li ul li ul li ul li:contains(-Py)").size());
        assertEquals(
                6, doc.select("ul li ul li ul li ul li span:contains(Composition Parts)").size());
        assertEquals(6, doc.select("ul li ul li ul li ul li ul li span:contains(Part)").size());
        assertEquals(6, doc.select("ul li ul li ul li ul li ul li span:contains(Role)").size());
        assertEquals(
                5,
                doc.select(
                                "ul li ul li ul li ul li ul li ul li ul li:contains(interbedded component)")
                        .size());
        assertEquals(
                1,
                doc.select(
                                "ul li ul li ul li ul li ul li ul li ul li:contains(fictitious component)")
                        .size());
    }
}
