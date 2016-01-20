package org.geoserver.community.css.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class CssDemoTest extends GeoServerWicketTestSupport {

    @Before
    public void setup() {
        login();
    }

    @Before
    public void removeFooStyle() {
        Catalog cat = getCatalog();
        StyleInfo foo = cat.getStyleByName("foo");
        if (foo != null) {
            cat.remove(foo);
        }
    }

    @Test
    public void testBasicLayout() {
        tester.startPage(CssDemoPage.class);
        tester.assertRenderedPage(CssDemoPage.class);
        tester.assertComponent("main-content:context", AjaxTabbedPanel.class);
        tester.assertComponent("main-content:context:panel", SLDPreviewPanel.class);
        tester.assertComponent("main-content:change.style", SimpleAjaxLink.class);
        tester.assertComponent("main-content:change.layer", SimpleAjaxLink.class);
        tester.assertComponent("main-content:create.style", AjaxLink.class);
        tester.assertComponent("main-content:associate.styles" , AjaxLink.class);
        tester.assertComponent("main-content:style.editing" , StylePanel.class);
    }

    @Test
    public void testOpenLayersMapPanel() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:1:link");
        tester.assertComponent("main-content:context:panel", OpenLayersMapPanel.class);
    }

    @Test
    public void testSLDPreviewPanel() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:0:link");
        tester.assertComponent("main-content:context:panel", SLDPreviewPanel.class);
    }

    @Test
    public void testStyleChooser() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:change.style:link");
        tester.assertComponent("main-content:popup:content:style.table", GeoServerTablePanel.class);
    }


    @Test
    public void testLayerChooser() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:change.layer:link");
        tester.assertComponent("main-content:popup:content:layer.table", GeoServerTablePanel.class);
    }

    @Test
    public void testDocsPanel() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:3:link");
        tester.assertComponent("main-content:context:panel", DocsPanel.class);
    }

    @Test
    public void testWorkspaceSpecificStyle() {
        // make this style workspace specific
        StyleInfo si = getCatalog().getStyleByName(MockData.BASIC_POLYGONS.getLocalPart());
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.BASIC_POLYGONS.getPrefix());
        si.setWorkspace(ws);
        getCatalog().save(si);

        login();
        PageParameters pp = new PageParameters();
        String prefixedName = getLayerId(MockData.BASIC_POLYGONS);
        pp.add("layer", prefixedName);
        pp.add("style", prefixedName);
        tester.startPage(CssDemoPage.class, pp);
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertRenderedPage(CssDemoPage.class);
        tester.assertModelValue("main-content:change.style:link:label", prefixedName);
        tester.assertModelValue("main-content:change.layer:link:label", prefixedName);
    }

    @Test
    public void testWorkspaceRelativeLinks() throws UnsupportedEncodingException,
            ParserConfigurationException, SAXException, IOException {
        StyleInfo si = getCatalog().getStyleByName(MockData.LAKES.getLocalPart());
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.LAKES.getPrefix());
        si.setWorkspace(ws);

        CssDemoPage page = new CssDemoPage();
        String css = "* { mark: url(\"smiley.png\");  }";
        String sld = page.cssText2sldText(css, si);

        // check the relative reference is still relative
        assertTrue(sld.contains("xlink:href=\"smiley.png\""));
    }

    @Test
    public void testEditPreConfiguredCSSStyle() throws IOException {
        Catalog cat = getCatalog();
        StyleInfo foo = cat.getFactory().createStyle();
        foo.setName("foo");
        foo.setFilename("foo.css");
        foo.setFormat(CssHandler.FORMAT);

        String css = "* { fill: #cccccc }";
        cat.getResourcePool().writeStyle(foo, new ByteArrayInputStream(css.getBytes()));
        cat.add(foo);

        login();
        PageParameters pp = new PageParameters();
        String prefixedName = getLayerId(MockData.BASIC_POLYGONS);
        pp.add("layer", prefixedName);
        pp.add("style", "foo");
        tester.startPage(CssDemoPage.class, pp);
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertRenderedPage(CssDemoPage.class);
        tester.assertModelValue("main-content:change.style:link:label", "foo");
        tester.assertModelValue("main-content:change.layer:link:label", prefixedName);

        // tester.debugComponentTrees();
        FormTester form = tester.newFormTester("main-content:style.editing:style-editor");
        form.setValue("editor:editorContainer:editorParent:editor", "* { stroke: red; }");

        tester.executeAjaxEvent("main-content:style.editing:style-editor:submit", "click");

        BufferedReader reader = cat.getResourcePool().readStyle(foo);
        String content = IOUtils.toString(reader);
        reader.close();
        assertEquals("* { stroke: red; }", content);
    }
}
