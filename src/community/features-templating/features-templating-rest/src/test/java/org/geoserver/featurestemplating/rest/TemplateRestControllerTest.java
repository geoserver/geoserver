/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.geoserver.platform.resource.Resource;
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
            """
            <gft:Template>
              <ul>
                <li><span>MeteoStations</span>
                  <ul>
                    <li><span >Code</span>
                      <ul>
                        <li>
                          $${strConcat('Station_',st:code)}
                        </li>
                      </ul>
                    </li>
                    <li><span>Name</span>
                      <ul>
                        <li>
                          ${st:common_name}
                        </li>
                      </ul>
                    </li>
                    <li><span>Geometry</span>
                      <ul>
                        <li>
                          ${st:position}
                        </li>
                      </ul>
                    </li>
                  </ul>
                </li>
              </ul>
            </gft:Template>""";

    private static final String XHTML_TEMPLATE_2 =
            """
            <gft:Template>
              <ul>
                <li><span>MeteoStations</span>
                  <ul>
                    <li><span>Name</span>
                      <ul>
                        <li>
                          ${st:common_name}
                        </li>
                      </ul>
                    </li>
                    <li><span>Geometry</span>
                      <ul>
                        <li>
                          ${st:position}
                        </li>
                      </ul>
                    </li>
                  </ul>
                </li>
              </ul>
            </gft:Template>""";

    private static final String JSON_TEMPLATE = "{"
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

    private static final String JSON_TEMPLATE_2 = "{"
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
            """
            <gft:Template>
            <gft:Options>
              <gft:Namespaces xmlns:topp="http://www.openplans.org/topp"/>
              <gft:SchemaLocation xsi:schemaLocation="http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd"/>
            </gft:Options>
              <topp:states gml:id="${@id}">
              	<topp:name code="${STATE_ABBR}">${STATE_NAME}</topp:name>
              	<topp:region>${SUB_REGION}</topp:region>
                <topp:population>${PERSONS}</topp:population>
                <topp:males>${MALE}</topp:males>
              	<topp:females>${FEMALE}</topp:females>
              	<topp:active_population>${WORKERS}</topp:active_population>
              	<topp:wkt_geom>$${toWKT(the_geom)}</topp:wkt_geom>
              </topp:states>
            </gft:Template>""";

    private static final String GML_TEMPLATE_2 =
            """
            <gft:Template>
            <gft:Options>
              <gft:Namespaces xmlns:topp="http://www.openplans.org/topp"/>
              <gft:SchemaLocation xsi:schemaLocation="http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd"/>
            </gft:Options>
              <topp:states gml:id="${@id}">
              	<topp:name code="${STATE_ABBR}">${STATE_NAME}</topp:name>
              	<topp:region>${SUB_REGION}</topp:region>
              	<topp:wkt_geom>$${toWKT(the_geom)}</topp:wkt_geom>
              </topp:states>
            </gft:Template>""";
    public static final String XHTMLTEMPLATE_NAME = "xhtmltemplate";

    @Test
    public void testPostGetPutGetDeleteJson() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/featurestemplates?templateName=foo",
                    JSON_TEMPLATE,
                    MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_TEMPLATE.trim(), response.getContentAsString());
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/featurestemplates/foo",
                    JSON_TEMPLATE_2,
                    MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_TEMPLATE_2.trim(), response.getContentAsString());

            response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/foo");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("foo"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostGetPutGetDeleteXML() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates?templateName=foo2",
                    GML_TEMPLATE,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(200, response.getStatus());
            assertEquals(GML_TEMPLATE.trim(), response.getContentAsString());
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates/foo2",
                    GML_TEMPLATE_2,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(200, response.getStatus());
            assertEquals(GML_TEMPLATE_2.trim(), response.getContentAsString());

            response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates/foo2");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("cdf:foo2"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostErrorMessage() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/stations/featurestemplates?templateName=foo2",
                    GML_TEMPLATE,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(404, response.getStatus());
            assertEquals("FeatureType stations not found", response.getContentAsString());
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostGetPutGetDeleteXHTML() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates?templateName=foo3",
                    XHTML_TEMPLATE,
                    MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
            assertEquals(200, response.getStatus());
            assertEquals(XHTML_TEMPLATE.trim(), response.getContentAsString());
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3",
                    XHTML_TEMPLATE_2,
                    MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
            assertEquals(200, response.getStatus());
            assertEquals(XHTML_TEMPLATE_2.trim(), response.getContentAsString());

            response = deleteAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3");
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
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates",
                    bytes,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH
                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template");
            assertEquals(200, response.getStatus());
            assertEquals(
                    unzip("test-template.zip", getClass()).trim(),
                    response.getContentAsString().trim());
            url = getClass().getResource("test-template2.zip");
            bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template",
                    bytes,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH
                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template");
            assertEquals(200, response.getStatus());
            assertEquals(unzip("test-template2.zip", getClass()).trim(), response.getContentAsString());

            response = deleteAsServletResponse(RestBaseController.ROOT_PATH
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
            throw new RestException("Error processing the template", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Test
    public void testFindAll() throws Exception {
        newTemplateInfo("jsontemplate", "json", null, null, JSON_TEMPLATE);
        newTemplateInfo("gmltemplate", "xml", "cdf", null, GML_TEMPLATE);
        newTemplateInfo(XHTMLTEMPLATE_NAME, "xhtml", "cdf", "Fifteen", XHTML_TEMPLATE);

        newTemplateInfo("jsontemplate2", "json", "cdf", "Fifteen", JSON_TEMPLATE_2);
        newTemplateInfo("gmltemplate2", "xml", null, null, GML_TEMPLATE_2);
        newTemplateInfo("xhtmltemplate2", "xhtml", "cdf", null, XHTML_TEMPLATE_2);
        JSON result = getAsJSON(
                RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates.json", 200);
        JSONObject object = ((JSONObject) result).getJSONObject("templatesInfo");
        JSONArray array = object.getJSONArray("templates");
        assertEquals(2, array.size());
        assertInfos(array);

        result = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/cdf/featurestemplates.json", 200);

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
            String name, String extension, String workspace, String featureType, String rawTemplate) {
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
            TemplateInfo info = newTemplateInfo("testJsonTemplateCache", "json", null, null, "{\"static\":\"value\"}");
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
                    TemplateLoader.get().getTemplate(fifteen, TemplateIdentifier.JSON.getOutputFormat());
            StaticBuilder builder = (StaticBuilder)
                    rootBuilder.getChildren().get(0).getChildren().get(0);
            String value = builder.getStaticValue().textValue();
            assertEquals("value", value);

            // replace the template with a new one.
            putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/featurestemplates/testJsonTemplateCache",
                    "{\"differentStatic\":\"differentValue\"}",
                    MediaType.APPLICATION_JSON_VALUE);

            // reload the template. The new builder tree should represent the new template.
            rootBuilder = TemplateLoader.get().getTemplate(fifteen, TemplateIdentifier.JSON.getOutputFormat());
            assertNotNull(rootBuilder);
            builder = (StaticBuilder)
                    rootBuilder.getChildren().get(0).getChildren().get(0);
            value = builder.getStaticValue().textValue();
            assertEquals("differentValue", value);

            // delete the template
            deleteAsServletResponse(RestBaseController.ROOT_PATH + "/featurestemplates/testJsonTemplateCache");
        } finally {
            Dispatcher.REQUEST.set(null);
        }
    }

    @Test
    public void testGetByFeatureType() throws Exception {
        try {
            newTemplateInfo("jsontemplate", "json", null, null, JSON_TEMPLATE);
            newTemplateInfo("gmltemplate", "xml", "cdf", null, GML_TEMPLATE);
            newTemplateInfo(XHTMLTEMPLATE_NAME, "xhtml", "cdf", "Fifteen", XHTML_TEMPLATE);
            MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH
                    + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/" + XHTMLTEMPLATE_NAME + ".xml");
            assertEquals(200, response.getStatus());
            assertEquals(XHTML_TEMPLATE.trim(), response.getContentAsString());
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostByFeatureType() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates?templateName=foo2",
                    GML_TEMPLATE,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
            assertEquals("foo2", response.getContentAsString());
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPostZipByFeatureType() throws Exception {
        try {
            URL url = getClass().getResource("test-template.zip");
            byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates",
                    bytes,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            TemplateInfo templateInfo = TemplateInfoDAO.get().findByFullName("cdf:Fifteen:test-template");
            assertNotNull(templateInfo);
            assertEquals(
                    unzip("test-template.zip", getClass()).trim(),
                    getTemplateContentByFullName(templateInfo.getFullName()).orElseGet(() -> null));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPutByFeatureType() throws Exception {
        TemplateInfoDAO.get().deleteAll();
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates?templateName=foo3",
                    XHTML_TEMPLATE,
                    MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());

            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/foo3",
                    XHTML_TEMPLATE_2,
                    MediaType.APPLICATION_XHTML_XML_VALUE);
            assertEquals(201, response.getStatus());
            getTemplateContentByFullName("cdf:Fifteen:foo3")
                    .ifPresentOrElse(
                            content -> assertEquals(XHTML_TEMPLATE_2.trim(), content),
                            () -> assertNull("Template not found"));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPutZipByFeatureType() throws Exception {
        try {
            URL url = getClass().getResource("test-template.zip");
            byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates",
                    bytes,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            url = getClass().getResource("test-template2.zip");
            bytes = FileUtils.readFileToByteArray(URLs.urlToFile(url));
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH
                            + "/workspaces/cdf/featuretypes/Fifteen/featurestemplates/test-template",
                    bytes,
                    MediaTypeExtensions.APPLICATION_ZIP_VALUE);
            assertEquals(201, response.getStatus());
            TemplateInfo templateInfo = TemplateInfoDAO.get().findByFullName("cdf:Fifteen:test-template");
            assertNotNull(templateInfo);
            assertEquals(
                    unzip("test-template2.zip", getClass()).trim(),
                    getTemplateContentByFullName(templateInfo.getFullName()).orElseGet(() -> null));
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    private Optional<String> getTemplateContentByFullName(String templateName) {
        TemplateInfo templateInfo = TemplateInfoDAO.get().findByFullName(templateName);
        if (templateInfo == null) {
            return Optional.empty();
        }
        Resource resource = TemplateFileManager.get().getTemplateResource(templateInfo);
        if (resource.getType() != Resource.Type.RESOURCE) {
            throw new IllegalArgumentException("Template with fullName " + templateInfo.getFullName() + " not found");
        }
        try {
            return Optional.of(StringUtils.toEncodedString(resource.getContents(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
