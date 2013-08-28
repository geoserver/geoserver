package org.geoserver.community.css.web;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;

public class CssDemoTest extends GeoServerWicketTestSupport {
    @Test
    public void testBasicLayout() {
        login();
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
        login();
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:1:link");
        tester.assertComponent("main-content:context:panel", OpenLayersMapPanel.class);
    }

    @Test
    public void testSLDPreviewPanel() {
        login();
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:0:link");
        tester.assertComponent("main-content:context:panel", SLDPreviewPanel.class);
    }

    @Test 
    public void testStyleChooser() {
        login();
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:change.style");
        tester.assertComponent("main-content:popup:content:style.table", GeoServerTablePanel.class);
    }


    @Test
    public void testLayerChooser() {
        login();
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:change.layer");
        tester.assertComponent("main-content:popup:content:layer.table", GeoServerTablePanel.class);
    }

    @Test
    public void testDocsPanel() {
        login();
        tester.startPage(CssDemoPage.class);
        tester.clickLink("main-content:context:tabs-container:tabs:3:link");
        tester.assertComponent("main-content:context:panel", DocsPanel.class);
    }
}
