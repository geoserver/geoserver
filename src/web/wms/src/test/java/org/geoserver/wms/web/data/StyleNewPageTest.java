/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerApplication;
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
    public void testCopyFormat() {
        FormTester form = tester.newFormTester("styleForm");
        // Select the ZIP format
        form.select("context:panel:format", new StyleFormatsModel().getObject().indexOf("zip"));
        tester.executeAjaxEvent("styleForm:context:panel:format", "change");
        // Select the SLD style to copy
        form.select("context:panel:existingStyles", 0);
        tester.executeAjaxEvent("styleForm:context:panel:existingStyles", "change");
        // Verify that the format is ZIP
        tester.assertModelValue("styleForm:context:panel:format", "zip");
        // Copy the SLD style
        tester.clickLink("styleForm:context:panel:copy", true);
        // Verify that the format is SLD on the server
        tester.assertModelValue("styleForm:context:panel:format", "sld");
        // Verify that the format is SLD in the response page
        String doc = tester.getLastResponse().getDocument();
        TagTester tag = TagTester.createTagByAttribute(doc, "name", "context:panel:format");
        tag = tag.getChild("selected", "selected");
        assertEquals("sld", tag.getAttribute("value"));
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

    @Test
    public void testInsertImage() throws Exception {
        // create some fake images
        GeoServerDataDirectory dd =
                GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
        dd.getStyles().get("somepicture.png").out().close();
        dd.getStyles().get("otherpicture.jpg").out().close();
        dd.getStyles().get("vector.svg").out().close();

        // since we don't have code mirror available in the test environment, we are kind of limited
        // we'll make the tool bar visible to test the dialog anyway
        tester.getComponentFromLastRenderedPage(
                        "styleForm:styleEditor:editorContainer:toolbar", false)
                .setVisible(true);

        tester.assertComponent(
                "styleForm:styleEditor:editorContainer:toolbar:custom-buttons:1", AjaxLink.class);
        tester.clickLink("styleForm:styleEditor:editorContainer:toolbar:custom-buttons:1");
        tester.assertComponent(
                "dialog:dialog:content:form:userPanel", AbstractStylePage.ChooseImagePanel.class);
        tester.assertComponent("dialog:dialog:content:form:userPanel:image", DropDownChoice.class);
        tester.assertInvisible("dialog:dialog:content:form:userPanel:display");
        @SuppressWarnings("unchecked")
        List<? extends String> choices =
                ((DropDownChoice<String>)
                                tester.getComponentFromLastRenderedPage(
                                        "dialog:dialog:content:form:userPanel:image"))
                        .getChoices();
        assertEquals(4, choices.size());
        assertEquals("otherpicture.jpg", choices.get(1));
        assertEquals("somepicture.png", choices.get(2));
        assertEquals("vector.svg", choices.get(3));

        FormTester formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.select("userPanel:image", 2);

        tester.executeAjaxEvent("dialog:dialog:content:form:userPanel:image", "change");
        tester.assertVisible("dialog:dialog:content:form:userPanel:display");

        formTester.submit("submit");

        // we can at least test that the right javascript code is there
        Pattern pattern =
                Pattern.compile(
                        "replaceSelection\\('<ExternalGraphic "
                                + "xmlns=\"http://www.opengis.net/sld\" "
                                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\">\\\\n"
                                + "<OnlineResource xlink:type=\"simple\" xlink:href=\""
                                + "(.*)\" />\\\\n"
                                + "<Format>(.*)</Format>\\\\n"
                                + "</ExternalGraphic>\\\\n'\\)");
        Matcher matcher = pattern.matcher(tester.getLastResponse().getDocument());
        assertTrue(matcher.find());
        assertEquals("somepicture.png", matcher.group(1));
        assertEquals("image/png", matcher.group(2));

        // test uploading
        tester.clickLink("styleForm:styleEditor:editorContainer:toolbar:custom-buttons:1");
        formTester = tester.newFormTester("dialog:dialog:content:form");
        org.apache.wicket.util.file.File file =
                new org.apache.wicket.util.file.File(
                        getClass().getResource("GeoServer_75.png").getFile());
        formTester.setFile("userPanel:upload", file, "image/png");
        formTester.submit("submit");

        assertTrue(Resources.exists(dd.getStyles().get("GeoServer_75.png")));

        matcher = pattern.matcher(tester.getLastResponse().getDocument());
        assertTrue(matcher.find());
        assertEquals("GeoServer_75.png", matcher.group(1));
        assertEquals("image/png", matcher.group(2));
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
