package org.geoserver.community.css.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Before;
import org.junit.Test;

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
        tester.assertComponent("main-content:change.style", AjaxLink.class);
        tester.assertComponent("main-content:change.layer", AjaxLink.class);
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
        tester.clickLink("main-content:change.style");
        tester.assertComponent("main-content:popup:content:style.table", GeoServerTablePanel.class);
    }


    @Test
    public void testLayerChooser() {
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:change.layer");
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
        pp.put("layer", prefixedName);
        pp.put("style", prefixedName);
        tester.startPage(CssDemoPage.class, pp);
        tester.assertRenderedPage(CssDemoPage.class);
        tester.assertModelValue("main-content:style.name", prefixedName);
        tester.assertModelValue("main-content:layer.name", prefixedName);
    }

    @Test
    public void testWorkspaceRelativeLinks() {
        StyleInfo si = getCatalog().getStyleByName(MockData.LAKES.getLocalPart());
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.LAKES.getPrefix());
        si.setWorkspace(ws);

        CssDemoPage page = new CssDemoPage();
        String css = "* { mark: url(\"smiley.png\");  }";
        String sld = page.cssText2sldText(css, si);

        // check the reference is workspace specific (see http://jira.codehaus.org/browse/GEOS-6229
        // though, that should be a better fix)
        // System.out.println(sld);
        assertTrue(sld.contains("workspaces/cite/styles/smiley.png"));
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
        pp.put("layer", prefixedName);
        pp.put("style", "foo");
        tester.startPage(CssDemoPage.class, pp);
        tester.assertRenderedPage(CssDemoPage.class);
        tester.assertModelValue("main-content:style.name", "foo");
        tester.assertModelValue("main-content:layer.name", prefixedName);

        // tester.debugComponentTrees();
        FormTester form = tester.newFormTester("main-content:style.editing:style-editor");
        form.setValue("editor", "* { stroke: red; }");

        tester.executeAjaxEvent("main-content:style.editing:style-editor:submit", "onclick");

        BufferedReader reader = cat.getResourcePool().readStyle(foo);
        String content = IOUtils.toString(reader);
        reader.close();
        assertEquals("* { stroke: red; }", content);
    }
}
