/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.util.URLs;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class TemplateRestControllerTest extends CatalogRESTTestSupport {

    private static final String XHTML_TEMPLATE =
            "<gft:Template>\n"
                    + "  <ul>\n"
                    + "    <li><span>MeteoStations</span>\n"
                    + "      <ul>\n"
                    + "        <li><span >Code</span>\n"
                    + "          <ul>\n"
                    + "            <li>\n"
                    + "              $${strConcat('Station_',st:code)}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li><span>Name</span>\n"
                    + "          <ul>\n"
                    + "            <li>\n"
                    + "              ${st:common_name}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li><span>Geometry</span>\n"
                    + "          <ul>\n"
                    + "            <li>\n"
                    + "              ${st:position}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "      </ul>\n"
                    + "    </li>\n"
                    + "  </ul>\n"
                    + "</gft:Template>";

    private static final String XHTML_TEMPLATE_2 =
            "<gft:Template>\n"
                    + "  <ul>\n"
                    + "    <li><span>MeteoStations</span>\n"
                    + "      <ul>\n"
                    + "        <li><span>Name</span>\n"
                    + "          <ul>\n"
                    + "            <li>\n"
                    + "              ${st:common_name}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li><span>Geometry</span>\n"
                    + "          <ul>\n"
                    + "            <li>\n"
                    + "              ${st:position}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "      </ul>\n"
                    + "    </li>\n"
                    + "  </ul>\n"
                    + "</gft:Template>";

    private static final String JSON_TEMPLATE =
            "{"
                    + "  \"@context\": {"
                    + "    \"gsp\": \"http://www.opengis.net/ont/geosparql#\","
                    + "    \"sf\": \"http://www.opengis.net/ont/sf#\","
                    + "    \"schema\": \"https://schema.org/\","
                    + "    \"dc\": \"http://purl.org/dc/terms/\","
                    + "    \"Feature\": \"gsp:Feature\","
                    + "    \"FeatureCollection\": \"schema:Collection\","
                    + "    \"Point\": \"sf:Point\","
                    + "    \"wkt\": \"gsp:asWKT\","
                    + "    \"features\": {"
                    + "      \"@container\": \"@set\","
                    + "      \"@id\": \"schema:hasPart\""
                    + "    },"
                    + "    \"geometry\": \"sf:geometry\","
                    + "    \"description\": \"dc:description\","
                    + "    \"title\": \"dc:title\","
                    + "    \"name\": \"schema:name\""
                    + "  },"
                    + "  \"type\": \"FeatureCollection\","
                    + "  \"features\": ["
                    + "    {"
                    + "      \"$source\": \"cite:NamedPlaces\""
                    + "    },"
                    + "    {"
                    + "      \"id\": \"${cite:FID}\","
                    + "      \"@type\": ["
                    + "        \"Feature\","
                    + "        \"cite:NamedPlaces\","
                    + "        \"http://vocabulary.odm2.org/samplingfeaturetype/namedplaces\""
                    + "      ],"
                    + "      \"name\": \"${cite:NAME}\","
                    + "      \"geometry\": {"
                    + "        \"@type\": \"MultiPolygon\","
                    + "        \"wkt\": \"$${toWKT(xpath('cite:the_geom'))}\""
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String JSON_TEMPLATE_2 =
            "{"
                    + "  \"type\": \"FeatureCollection\","
                    + "  \"features\": ["
                    + "    {"
                    + "      \"$source\": \"cite:NamedPlaces\""
                    + "    },"
                    + "    {"
                    + "      \"id\": \"${cite:FID}\","
                    + "      \"@type\": ["
                    + "        \"Feature\","
                    + "        \"cite:NamedPlaces\","
                    + "        \"http://vocabulary.odm2.org/samplingfeaturetype/namedplaces\""
                    + "      ],"
                    + "      \"name\": \"${cite:NAME}\","
                    + "      \"geometry\": {"
                    + "        \"@type\": \"MultiPolygon\","
                    + "        \"wkt\": \"$${toWKT(xpath('cite:the_geom'))}\""
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String GML_TEMPLATE =
            "<gft:Template>\n"
                    + "<gft:Options>\n"
                    + "  <gft:Namespaces xmlns:topp=\"http://www.openplans.org/topp\"/>\n"
                    + "  <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"/>\n"
                    + "</gft:Options>\n"
                    + "  <topp:states gml:id=\"${@id}\">\n"
                    + "  \t<topp:name code=\"${STATE_ABBR}\">${STATE_NAME}</topp:name>\n"
                    + "  \t<topp:region>${SUB_REGION}</topp:region>\n"
                    + "    <topp:population>${PERSONS}</topp:population>\n"
                    + "    <topp:males>${MALE}</topp:males>\n"
                    + "  \t<topp:females>${FEMALE}</topp:females>\n"
                    + "  \t<topp:active_population>${WORKERS}</topp:active_population>\n"
                    + "  \t<topp:wkt_geom>$${toWKT(the_geom)}</topp:wkt_geom>\n"
                    + "  </topp:states>\n"
                    + "</gft:Template>";

    private static final String GML_TEMPLATE_2 =
            "<gft:Template>\n"
                    + "<gft:Options>\n"
                    + "  <gft:Namespaces xmlns:topp=\"http://www.openplans.org/topp\"/>\n"
                    + "  <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"/>\n"
                    + "</gft:Options>\n"
                    + "  <topp:states gml:id=\"${@id}\">\n"
                    + "  \t<topp:name code=\"${STATE_ABBR}\">${STATE_NAME}</topp:name>\n"
                    + "  \t<topp:region>${SUB_REGION}</topp:region>\n"
                    + "  \t<topp:wkt_geom>$${toWKT(the_geom)}</topp:wkt_geom>\n"
                    + "  </topp:states>\n"
                    + "</gft:Template>";

    @Test
    public void testPostGetPutGetDeleteJson() throws Exception {
        try {
            MockHttpServletResponse response =
                    postAsServletResponse(
                            RestBaseController.ROOT_PATH + "/featurestemplates?templateName=foo",
                            JSON_TEMPLATE,
                            MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_TEMPLATE.trim(), response.getContentAsString());
            response =
                    putAsServletResponse(
                            RestBaseController.ROOT_PATH + "/featurestemplates/foo",
                            JSON_TEMPLATE_2,
                            MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_TEMPLATE_2.trim(), response.getContentAsString());

            response =
                    deleteAsServletResponse(
                            RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("foo"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostGetPutGetDeleteXML() throws Exception {
        try {
            MockHttpServletResponse response =
                    postAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featurestemplates?templateName=foo2",
                            GML_TEMPLATE,
                            MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(200, response.getStatus());
            assertEquals(GML_TEMPLATE.trim(), response.getContentAsString());
            response =
                    putAsServletResponse(
                            RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates/foo2",
                            GML_TEMPLATE_2,
                            MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(200, response.getStatus());
            assertEquals(GML_TEMPLATE_2.trim(), response.getContentAsString());

            response =
                    deleteAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("cdf:foo2"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostGetPutGetDeleteXHTML() throws Exception {
        try {
            MockHttpServletResponse response =
                    postAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates?templateName=foo3",
                            XHTML_TEMPLATE,
                            MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
            assertEquals(200, response.getStatus());
            assertEquals(XHTML_TEMPLATE.trim(), response.getContentAsString());
            response =
                    putAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3",
                            XHTML_TEMPLATE_2,
                            MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
            assertEquals(200, response.getStatus());
            assertEquals(XHTML_TEMPLATE_2.trim(), response.getContentAsString());

            response =
                    deleteAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("cdf:Fifteen:foo3"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testZip() throws Exception {
        try {
            URL url = getClass().getResource("test-template.zip");
            byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            MockHttpServletResponse response =
                    postAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates",
                            bytes,
                            MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template");
            assertEquals(200, response.getStatus());
            assertEquals(
                    unzip("test-template.zip", getClass()).trim(),
                    response.getContentAsString().trim());
            url = getClass().getResource("test-template2.zip");
            bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            response =
                    putAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template",
                            bytes,
                            MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            response =
                    getAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template");
            assertEquals(200, response.getStatus());
            assertEquals(
                    unzip("test-template2.zip", getClass()).trim(), response.getContentAsString());

            response =
                    deleteAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("cdf:Fifteen:test-template"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    private String unzip(String resourceName, Class<?> context) {
        InputStream object = context.getResourceAsStream(resourceName);
        try {
            File tempDir = Files.createTempDirectory("_template").toFile();

            org.geoserver.util.IOUtils.decompress(object, tempDir);

            File file = tempDir.listFiles()[0];
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return new String(bytes, Charset.defaultCharset());
        } catch (Exception e) {
            LOGGER.severe("Error processing the template zip (PUT): " + e.getMessage());
            throw new RestException(
                    "Error processing the template", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Test
    public void testFindAll() throws Exception {
        newTemplateInfo("jsontemplate", "json", null, null, JSON_TEMPLATE);
        newTemplateInfo("gmltemplate", "xml", "cdf", null, GML_TEMPLATE);
        newTemplateInfo("xhtmltemplate", "xhtml", "cdf", "Fifteen", XHTML_TEMPLATE);

        newTemplateInfo("jsontemplate2", "json", "cdf", "Fifteen", JSON_TEMPLATE_2);
        newTemplateInfo("gmltemplate2", "xml", null, null, GML_TEMPLATE_2);
        newTemplateInfo("xhtmltemplate2", "xhtml", "cdf", null, XHTML_TEMPLATE_2);
        JSON result =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates.json",
                        200);
        JSONObject object = ((JSONObject) result).getJSONObject("templatesInfo");
        JSONArray array = object.getJSONArray("templates");
        assertEquals(2, array.size());
        assertInfos(array);

        result =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates.json",
                        200);

        object = ((JSONObject) result).getJSONObject("templatesInfo");
        array = object.getJSONArray("templates");
        assertEquals(2, array.size());
        assertInfos(array);

        result = getAsJSON(RestBaseController.ROOT_PATH + "/featurestemplates.json", 200);

        object = ((JSONObject) result).getJSONObject("templatesInfo");
        array = object.getJSONArray("templates");
        assertEquals(2, array.size());
        assertInfos(array);
    }

    private void assertInfos(JSONArray array) {
        for (int i = 0; i < array.size(); i++) {
            JSONObject info = array.getJSONObject(i);
            assertNotNull(info.getString("name"));
            assertNotNull(info.getString("fileType"));
            assertNotNull(info.getString("location"));
        }
    }

    private TemplateInfo newTemplateInfo(
            String name,
            String extension,
            String workspace,
            String featureType,
            String rawTemplate) {
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName(name);
        info.setExtension(extension);
        info.setWorkspace(workspace);
        info.setFeatureType(featureType);
        TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, rawTemplate);
        return info;
    }

    @Test
    public void testUpdatedCacheOnPut() throws Exception {

        try {
            // create template and adds it to the featureType
            TemplateInfo info =
                    newTemplateInfo(
                            "testJsonTemplateCache", "json", null, null, "{\"static\":\"value\"}");
            FeatureTypeInfo fifteen = getCatalog().getFeatureTypeByName("cite", "NamedPlaces");
            TemplateRuleService service = new TemplateRuleService(fifteen);
            TemplateRule templateRule = new TemplateRule();
            templateRule.setTemplateName(info.getFullName());
            templateRule.setTemplateIdentifier(info.getIdentifier());
            templateRule.setOutputFormat(SupportedFormat.GEOJSON);
            service.saveRule(templateRule);

            // ask the loader for the template to be sure the cache load it.
            Request request = new Request();
            request.setOutputFormat(TemplateIdentifier.JSON.getOutputFormat());
            Dispatcher.REQUEST.set(request);
            RootBuilder rootBuilder =
                    TemplateLoader.get()
                            .getTemplate(fifteen, TemplateIdentifier.JSON.getOutputFormat());
            StaticBuilder builder =
                    (StaticBuilder) rootBuilder.getChildren().get(0).getChildren().get(0);
            String value = builder.getStaticValue().textValue();
            assertEquals("value", value);

            // replace the template with a new one.
            putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/featurestemplates/testJsonTemplateCache",
                    "{\"differentStatic\":\"differentValue\"}",
                    MediaType.APPLICATION_JSON_VALUE);

            // reload the template. The new builder tree should represent the new template.
            rootBuilder =
                    TemplateLoader.get()
                            .getTemplate(fifteen, TemplateIdentifier.JSON.getOutputFormat());
            assertNotNull(rootBuilder);
            builder = (StaticBuilder) rootBuilder.getChildren().get(0).getChildren().get(0);
            value = builder.getStaticValue().textValue();
            assertEquals("differentValue", value);

            // delete the template
            deleteAsServletResponse(
                    RestBaseController.ROOT_PATH + "/featurestemplates/testJsonTemplateCache");
        } finally {
            Dispatcher.REQUEST.set(null);
        }
    }
}
