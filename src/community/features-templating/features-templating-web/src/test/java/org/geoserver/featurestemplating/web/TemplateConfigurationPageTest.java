package org.geoserver.featurestemplating.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;
import org.geoserver.featurestemplating.configuration.TemplateService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.junit.Test;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class TemplateConfigurationPageTest extends GeoServerWicketTestSupport {

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

    private static final String HTML_TEMPLATE =
            "<gft:Template>\n"
                    + "  <gft:Options>\n"
                    + "    <style>\n"
                    + "      <![CDATA[\n"
                    + "ul, #myUL {\n"
                    + "  list-style-type: none;\n"
                    + "}\n"
                    + "\n"
                    + "#myUL {\n"
                    + "  margin: 0;\n"
                    + "  padding: 0;\n"
                    + "}\n"
                    + "\n"
                    + ".caret {\n"
                    + "  cursor: pointer;\n"
                    + "  -webkit-user-select: none; /* Safari 3.1+ */\n"
                    + "  -moz-user-select: none; /* Firefox 2+ */\n"
                    + "  -ms-user-select: none; /* IE 10+ */\n"
                    + "  user-select: none;\n"
                    + "}\n"
                    + "\n"
                    + ".caret::before {\n"
                    + "  content: \"\\25B6\";\n"
                    + "  color: black;\n"
                    + "  display: inline-block;\n"
                    + "  margin-right: 6px;\n"
                    + "}\n"
                    + "\n"
                    + ".caret-down::before {\n"
                    + "  -ms-transform: rotate(90deg); /* IE 9 */\n"
                    + "  -webkit-transform: rotate(90deg); /* Safari */'\n"
                    + "  transform: rotate(90deg);  \n"
                    + "}\n"
                    + "\n"
                    + ".nested {\n"
                    + "  display: none;\n"
                    + "}\n"
                    + "\n"
                    + ".active {\n"
                    + "  display: block;\n"
                    + "}\n"
                    + "]]>\n"
                    + "    </style>\n"
                    + "    <script>\n"
                    + "      <![CDATA[\n"
                    + "  window.onload = function() {\n"
                    + "  var toggler = document.getElementsByClassName(\"caret\");\n"
                    + "  for (let item of toggler){\n"
                    + "    item.addEventListener(\"click\", function() {\n"
                    + "    this.parentElement.querySelector(\".nested\").classList.toggle(\"active\");\n"
                    + "    this.classList.toggle(\"caret-down\");\n"
                    + "  });\n"
                    + "  }\n"
                    + "  }\n"
                    + "]]>\n"
                    + "    </script>\n"
                    + "  </gft:Options>\n"
                    + "  <ul id=\"myUL\">\n"
                    + "    <li><span class=\"caret\">MeteoStations</span>\n"
                    + "      <ul class=\"nested\">\n"
                    + "        <li><span class=\"caret\">Code</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li>\n"
                    + "              $${strConcat('Station_',st:code)}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li><span class=\"caret\">Name</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li>\n"
                    + "              ${st:common_name}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li><span class=\"caret\">Geometry</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li>\n"
                    + "              ${st:position}\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li gft:isCollection=\"true\" gft:source=\"st:meteoObservations/st:MeteoObservationsFeature\" gft:filter=\"xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'temperature'\">\n"
                    + "          <span class=\"caret\">Temperature</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li><span class=\"caret\">Time</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "            <li><span class=\"caret\">Value</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li gft:isCollection=\"true\" gft:source=\"st:meteoObservations/st:MeteoObservationsFeature\" gft:filter=\"xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'pressure'\">\n"
                    + "          <span class=\"caret\">Pressure</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li><span class=\"caret\">Time</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "            <li><span class=\"caret\">Value</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "        <li gft:isCollection=\"true\" gft:source=\"st:meteoObservations/st:MeteoObservationsFeature\" gft:filter=\"xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'wind speed'\">\n"
                    + "          <span class=\"caret\">Wind Speed</span>\n"
                    + "          <ul class=\"nested\">\n"
                    + "            <li><span class=\"caret\">Time</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "            <li><span class=\"caret\">Value</span>\n"
                    + "              <ul class=\"nested\">\n"
                    + "                <li>\n"
                    + "                  ${st:time}\n"
                    + "                </li>\n"
                    + "              </ul>\n"
                    + "            </li>\n"
                    + "          </ul>\n"
                    + "        </li>\n"
                    + "      </ul>\n"
                    + "    </li>\n"
                    + "  </ul>\n"
                    + "</gft:Template>";

    private static final String GML_STATIC_TEMPLATE =
            "<gft:Template>\n"
                    + "<gft:Options>\n"
                    + "  <gft:Namespaces xmlns:topp=\"http://www.openplans.org/topp\"/>\n"
                    + "  <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"/>\n"
                    + "</gft:Options>\n"
                    + "  <theFeature gml:id=\"idVal\">\n"
                    + "  \t<name>name</name>\n"
                    + "  </theFeature>\n"
                    + "</gft:Template>";

    @Test
    public void testNew() {
        try {
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(new TemplateInfo()), true));
            FormTester form = tester.newFormTester("theForm");
            form.select("tabbedPanel:panel:extension", 2);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:extension", "change");
            form.select("tabbedPanel:panel:workspace", 2);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:workspace", "change");
            form.select("tabbedPanel:panel:featureTypeInfo", 8);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:featureTypeInfo", "change");
            form.setValue("templateEditor:editorContainer:editorParent:editor", JSON_TEMPLATE);
            form.setValue("tabbedPanel:panel:templateName", "testJsonLDTemplate");
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);

            OutputFormatsDropDown outputFormatsDropDown =
                    (OutputFormatsDropDown)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:outputFormats");
            // only JSON-LD and GEOJSON should be available
            assertEquals(2, outputFormatsDropDown.getChoices().size());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);
            DropDownChoice ws =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:workspaces");
            // template is Local to feature type no need to select ws
            assertFalse(ws.isEnabled());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);

            DropDownChoice ft =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:featureTypes");
            // template is Local to feature type no need to select ft
            assertFalse(ft.isEnabled());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            form.submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testNew2() {
        try {
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(new TemplateInfo()), true));
            FormTester form = tester.newFormTester("theForm");
            form.select("tabbedPanel:panel:extension", 0);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:extension", "change");
            form.setValue("templateEditor:editorContainer:editorParent:editor", GML_TEMPLATE);
            form.setValue("tabbedPanel:panel:templateName", "testGMLTemplate");
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);

            OutputFormatsDropDown outputFormatsDropDown =
                    (OutputFormatsDropDown)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:outputFormats");
            // only GML should be available
            assertEquals(1, outputFormatsDropDown.getChoices().size());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);

            // template is global need to select ws
            form2.select("workspaces", 2);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:previewForm:workspaces", "change");
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);

            // template is global need to select feature type
            form2.select("featureTypes", 0);

            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            form.submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testEdit() {
        TemplateInfo info = new TemplateInfo();
        info.setWorkspace("cite");
        info.setFeatureType("NamedPlaces");
        info.setTemplateName("testJsonLDTemplate");
        info.setExtension("json");
        info = TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, JSON_TEMPLATE);
        try {
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(info), false));
            tester.assertModelValue("theForm:tabbedPanel:panel:templateName", "testJsonLDTemplate");
            tester.assertModelValue("theForm:tabbedPanel:panel:extension", "json");
            tester.assertModelValue("theForm:tabbedPanel:panel:workspace", "cite");
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            tester.newFormTester("theForm").submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testEdit2() {
        TemplateInfo info = new TemplateInfo();
        info.setWorkspace("cite");
        info.setTemplateName("testGMLTemplate");
        info.setExtension("xml");
        info = TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, GML_TEMPLATE);
        try {
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(info), false));
            tester.assertModelValue("theForm:tabbedPanel:panel:templateName", "testGMLTemplate");

            tester.assertModelValue("theForm:tabbedPanel:panel:extension", "xml");
            tester.assertModelValue("theForm:tabbedPanel:panel:workspace", "cite");
            tester.assertModelValue("theForm:tabbedPanel:panel:featureTypeInfo", null);
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            DropDownChoice ws =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:workspaces");
            // template is Local to feature type no need to select ws
            assertFalse(ws.isEnabled());

            DropDownChoice ft =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:featureTypes");
            // template is not Local to feature type is needed select ft
            assertTrue(ft.isEnabled());

            form2.select("featureTypes", 0);

            tester.newFormTester("theForm").submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testHTMLTemplate() {
        try {
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(new TemplateInfo()), true));
            FormTester form = tester.newFormTester("theForm");
            form.select("tabbedPanel:panel:extension", 1);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:extension", "change");
            form.select("tabbedPanel:panel:workspace", 2);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:workspace", "change");
            form.select("tabbedPanel:panel:featureTypeInfo", 8);
            tester.executeAjaxEvent("theForm:tabbedPanel:panel:featureTypeInfo", "change");
            form.setValue("templateEditor:editorContainer:editorParent:editor", HTML_TEMPLATE);
            form.setValue("tabbedPanel:panel:templateName", "testJsonLDTemplate");
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);

            OutputFormatsDropDown outputFormatsDropDown =
                    (OutputFormatsDropDown)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:outputFormats");
            // only JSON-LD and GEOJSON should be available
            assertEquals(1, outputFormatsDropDown.getChoices().size());
            assertEquals(SupportedFormat.HTML, outputFormatsDropDown.getChoices().get(0));
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);
            DropDownChoice ws =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:workspaces");
            // template is Local to feature type no need to select ws
            assertFalse(ws.isEnabled());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);

            DropDownChoice ft =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:featureTypes");
            // template is Local to feature type no need to select ft
            assertFalse(ft.isEnabled());
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            form.submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    @Test
    public void testPreviewRequestCanSendJsessionId() {
        // tests that the http client requesting a wfs preview
        // is able to send the JSESSIONID cookie to authenticate the wfs request according to the
        // user
        // preview the template fromUI.
        TemplateInfo info = new TemplateInfo();
        info.setWorkspace("cite");
        info.setTemplateName("testGMLTemplate");
        info.setExtension("xml");
        info = TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, GML_STATIC_TEMPLATE);
        try {
            login();
            MockHttpServletRequest request =
                    (MockHttpServletRequest) GeoServerApplication.get().servletRequest();
            RequestAttributes requestAttributes = new ServletRequestAttributes(request);
            RequestContextHolder.setRequestAttributes(requestAttributes);
            tester.startPage(new TemplateConfigurationPage(new Model<>(info), false));
            tester.assertModelValue("theForm:tabbedPanel:panel:templateName", "testGMLTemplate");

            tester.assertModelValue("theForm:tabbedPanel:panel:extension", "xml");
            tester.assertModelValue("theForm:tabbedPanel:panel:workspace", "cite");
            tester.assertModelValue("theForm:tabbedPanel:panel:featureTypeInfo", null);
            tester.clickLink("theForm:tabbedPanel:tabs-container:tabs:1:link");
            FormTester form2 = tester.newFormTester("theForm:tabbedPanel:panel:previewForm");
            form2.select("outputFormats", 0);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:workspaces", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:featureTypes", DropDownChoice.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:previewArea", CodeMirrorEditor.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:preview", AjaxSubmitLink.class);
            tester.assertComponent(
                    "theForm:tabbedPanel:panel:previewForm:validate", AjaxSubmitLink.class);
            DropDownChoice ws =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:workspaces");
            // template is Local to feature type no need to select ws
            assertFalse(ws.isEnabled());

            DropDownChoice ft =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:previewForm:featureTypes");
            // template is not Local to feature type is needed select ft
            assertTrue(ft.isEnabled());
            form2.select("featureTypes", 0);

            // checks that the JSESSIONID has been picked up by the HttpClient.
            TemplatePreviewPanel previewPanel =
                    (TemplatePreviewPanel)
                            tester.getComponentFromLastRenderedPage("theForm:tabbedPanel:panel");
            List<Cookie> httpCookies = getCookiesFromPreviewRequest(previewPanel);
            Cookie cookie =
                    httpCookies.stream()
                            .filter(c -> c.getName().equals("JSESSIONID"))
                            .findFirst()
                            .get();
            assertNotNull(cookie);
            assertEquals(requestAttributes.getSessionId(), cookie.getValue());

            tester.newFormTester("theForm").submit("save");

            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);
        } finally {
            TemplateInfoDAO.get().deleteAll();
        }
    }

    private List<Cookie> getCookiesFromPreviewRequest(TemplatePreviewPanel previewPanel) {
        CookieStore cookieStore =
                new PropertyModel<CookieStore>(previewPanel.buildHttpClient(), "cookieStore")
                        .getObject();
        return cookieStore.getCookies();
    }

    @Test
    public void testCacheCleanedOnTemplateUpdate() throws ExecutionException {
        try {
            // save a template
            TemplateInfo info = new TemplateInfo();
            info.setWorkspace("cite");
            info.setFeatureType("NamedPlaces");
            info.setTemplateName("testJsonLDTemplate");
            info.setExtension("json");
            TemplateService service = new TemplateService();
            service.saveOrUpdate(info, JSON_TEMPLATE);

            // create rule and add it to a FeatureType
            TemplateRule rule = new TemplateRule();
            rule.setTemplateIdentifier(info.getIdentifier());
            rule.setTemplateName(info.getFullName());
            rule.setOutputFormat(SupportedFormat.JSONLD);
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName("cite", "NamedPlaces");
            TemplateRuleService ruleService = new TemplateRuleService(fti);
            ruleService.saveRule(rule);

            // request the template to make user the cache load it
            Request request = new Request();
            request.setOutputFormat(TemplateIdentifier.JSONLD.getOutputFormat());
            Dispatcher.REQUEST.set(request);

            RootBuilder builder =
                    TemplateLoader.get()
                            .getTemplate(fti, TemplateIdentifier.JSONLD.getOutputFormat());
            assertNotNull(builder);
            IteratingBuilder iteratingBuilder = (IteratingBuilder) builder.getChildren().get(0);
            assertNotNull(iteratingBuilder.getStrSource());

            // from UI move the template to the ws folder and change its content
            login();
            tester.startPage(new TemplateConfigurationPage(new Model<>(info), false));
            tester.assertModelValue("theForm:tabbedPanel:panel:templateName", "testJsonLDTemplate");
            tester.assertModelValue("theForm:tabbedPanel:panel:extension", "json");
            tester.assertModelValue("theForm:tabbedPanel:panel:workspace", "cite");
            DropDownChoice dropDownChoice =
                    (DropDownChoice)
                            tester.getComponentFromLastRenderedPage(
                                    "theForm:tabbedPanel:panel:featureTypeInfo");
            dropDownChoice.setModelObject(null);
            FormTester ft = tester.newFormTester("theForm");
            ft.setValue(
                    "templateEditor:editorContainer:editorParent:editor", "{\"static\":\"value\"}");
            ft.submit("save");
            tester.assertNoErrorMessage();
            tester.assertRenderedPage(TemplateInfoPage.class);

            // retry to get the template. The builder tree should now be modified accordingly to the
            // new template content.
            RootBuilder rootBuilder =
                    TemplateLoader.get()
                            .getTemplate(fti, TemplateIdentifier.JSONLD.getOutputFormat());
            assertNotNull(rootBuilder);
            CompositeBuilder compositeBuilder = (CompositeBuilder) rootBuilder.getChildren().get(0);
            assertNull(compositeBuilder.getStrSource());
        } finally {
            TemplateInfoDAO.get().deleteAll();
            Dispatcher.REQUEST.set(null);
        }
    }
}
