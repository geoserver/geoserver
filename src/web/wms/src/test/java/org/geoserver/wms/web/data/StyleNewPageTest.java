/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.junit.Before;
import org.junit.Test;

public class StyleNewPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Publish the legend.png so we can see it
        java.io.File file = getResourceLoader().createFile("styles", "legend.png");
        getResourceLoader().copyFromClassPath("legend.png", file, getClass());
    }

    @Before
    public void setUp() throws Exception {
        login();
        tester.startPage(StyleNewPage.class);
        // org.geoserver.web.wicket.WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true,
        // true);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertNoErrorMessage();

        tester.assertComponent("styleForm:context:panel:name", TextField.class);
        tester.assertComponent(
                "styleForm:styleEditor:editorContainer:editorParent:editor", TextArea.class);
        tester.assertComponent("styleForm:context:panel:filename", FileUploadField.class);

        // Load the legend
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");

        tester.assertComponent("styleForm:context:panel:legendPanel", ExternalGraphicPanel.class);

        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:width",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:height",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:format",
                TextField.class);

        tester.assertModelValue("styleForm:context:panel:name", "");
    }

    @Test
    public void testUpload() throws Exception {
        FormTester upload = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");

        upload.setFile("context:panel:filename", styleFile, "application/xml");
        tester.clickLink("styleForm:context:panel:upload", true);

        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("styleForm:styleEditor", sld);
    }

    @Test
    public void testPreviewNoLegendSLD() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "previewsld");
        form.setValue("context:panel:format", "sld");
        form.submit();
        tester.executeAjaxEvent("styleForm:context:panel:preview", "click");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testPreviewNoLegendZIP() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "previewzip");
        form.setValue("context:panel:format", "zip");
        form.submit();
        tester.executeAjaxEvent("styleForm:context:panel:preview", "click");
        tester.assertErrorMessages(
                "Failed to build legend preview. Check to see if the style is valid.");
    }

    @Test
    public void testNoLegend() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "nolegendtest");
        form.submit();

        tester.assertRenderedPage(StyleNewPage.class);
        tester.executeAjaxEvent("submit", "click");
        tester.assertRenderedPage(StylePage.class);

        StyleInfo style = getCatalog().getStyleByName("nolegendtest");
        assertNotNull(style);
        assertNull(style.getLegend());
    }

    @Test
    public void testLegend() throws Exception {
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");
        // Make sure the fields we are editing actually exist
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:width",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:height",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:format",
                TextField.class);

        FormTester form = tester.newFormTester("styleForm", false);
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "legendtest");
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                "legend.png");
        form.setValue("context:panel:legendPanel:externalGraphicContainer:list:width", "100");
        form.setValue("context:panel:legendPanel:externalGraphicContainer:list:height", "100");
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:format", "image/png");
        form.setValue("context:panel:format", "sld");
        form.submit();
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StyleNewPage.class);
        tester.executeAjaxEvent("submit", "click");
        tester.assertRenderedPage(StylePage.class);

        StyleInfo style = getCatalog().getStyleByName("legendtest");
        assertNotNull(style);
        assertNotNull(style.getLegend());
    }

    @Test
    public void testLegendWrongValues() throws Exception {
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");
        // Make sure the fields we are editing actually exist
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:width",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:height",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:format",
                TextField.class);

        FormTester form = tester.newFormTester("styleForm", false);
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "legendwrongvaluestest");
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                "thisisnotavalidurl");
        form.setValue("context:panel:legendPanel:externalGraphicContainer:list:width", "-1");
        form.setValue("context:panel:legendPanel:externalGraphicContainer:list:height", "-1");
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:format", "image/png");
        form.submit();
        tester.assertErrorMessages(
                "Graphic resource must be a png, gif or jpeg",
                "The value of 'Width' must be at least 0.",
                "The value of 'Height' must be at least 0.");
    }

    @Test
    public void testLegendAutoFillEmpty() throws Exception {
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");
        // Make sure the fields we are editing actually exist
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:autoFill",
                GeoServerAjaxFormLink.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:width",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:height",
                TextField.class);
        tester.assertComponent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:format",
                TextField.class);

        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:list:autoFill",
                "click");
    }

    @Test
    public void testPreviewExternalLegendWithSlash() throws Exception {
        // Set the proxy base URL with an ending slash
        Resource resource = getResourceLoader().get("styles/legend.png");
        String url = resource.file().getParentFile().getParentFile().toURI().toURL().toString();
        if (!url.endsWith("/")) {
            url += '/';
        }
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl(url);
        getGeoServer().save(global);

        // Load legend.png
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");
        FormTester form = tester.newFormTester("styleForm", false);
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                "legend.png");
        tester.executeAjaxEvent("styleForm:context:panel:preview", "click");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testPreviewExternalLegendWithoutSlash() throws Exception {
        // Set the proxy base URL without an ending slash
        Resource resource = getResourceLoader().get("styles/legend.png");
        String url = resource.file().getParentFile().getParentFile().toURI().toURL().toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl(url);
        getGeoServer().save(global);

        // Load legend.png
        tester.executeAjaxEvent(
                "styleForm:context:panel:legendPanel:externalGraphicContainer:showhide:show",
                "click");
        FormTester form = tester.newFormTester("styleForm", false);
        form.setValue(
                "context:panel:legendPanel:externalGraphicContainer:list:onlineResource",
                "legend.png");
        tester.executeAjaxEvent("styleForm:context:panel:preview", "click");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.submit();

        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testMissingStyle() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:name", "test");
        form.submit();

        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'styleEditor' is required."});
    }

    @Test
    public void testNewStyleRepeatedName() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "repeatedname");
        form.submit();
        tester.assertRenderedPage(StyleNewPage.class);

        tester.executeAjaxEvent("submit", "click");
        tester.assertRenderedPage(StylePage.class);

        tester.startPage(StyleNewPage.class);
        form = tester.newFormTester("styleForm");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "repeatedname");
        form.submit();
        tester.assertRenderedPage(StyleNewPage.class);

        tester.assertErrorMessages("Style named 'repeatedname' already exists");
    }

    @Test
    public void testNewStyle() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "test");
        form.submit();

        tester.assertRenderedPage(StyleNewPage.class);
        assertNotNull(getCatalog().getStyleByName("test"));

        tester.executeAjaxEvent("submit", "click");
        tester.assertRenderedPage(StylePage.class);
    }

    @Test
    public void testNewStyleApply() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "test");
        tester.executeAjaxEvent("apply", "click");
        tester.assertRenderedPage(StyleEditPage.class);

        assertNotNull(getCatalog().getStyleByName("test"));
    }

    @Test
    public void testNewStyleSubmit() throws Exception {
        FormTester form = tester.newFormTester("styleForm");
        File styleFile =
                new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", sld);
        form.setValue("context:panel:name", "test");
        tester.executeAjaxEvent("submit", "click");
        tester.assertRenderedPage(StylePage.class);

        assertNotNull(getCatalog().getStyleByName("test"));
    }

    @Test
    public void testNewStyleNoSLD() throws Exception {

        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:name", "test");
        form.submit();

        tester.assertRenderedPage(StyleNewPage.class);
        assertTrue(tester.getMessages(FeedbackMessage.ERROR).size() > 0);
    }

    //    Cannot make this one to work, the sld text area is not filled in the test
    //    and I don't understand why, in the real world it is
    //    public void testValidate() throws Exception {
    //        tester.clickLink("form:sld:validate", false);
    //
    //        tester.assertRenderedPage(StyleNewPage.class);
    //        tester.assertErrorMessages(new String[] {"Invalid style"});
    //    }

}
