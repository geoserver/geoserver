/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class StyleEditPageNoLayersTest extends GeoServerWicketTestSupport {

    StyleInfo buildingsStyle;
    StyleEditPage edit;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();

        buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        if (buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = catalog.getStyleByName("BuildingsNew");
            if (si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                catalog.save(si);
            }
            buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        }
        // Clear all layers
        catalog.getLayers().forEach(catalog::remove);

        edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
    }

    @Test
    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();

        tester.debugComponentTrees();
        tester.assertComponent("styleForm:context:panel:name", TextField.class);
        tester.assertComponent(
                "styleForm:styleEditor:editorContainer:editorParent:editor", TextArea.class);
    }

    @Test
    public void testPublishingTab() {

        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:1:link", "click");
        tester.assertErrorMessages(
                new String[] {"Cannot show Publishing options: No Layers available."});
    }

    @Test
    public void testLayerPreviewTab() {

        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:2:link", "click");
        tester.assertErrorMessages(
                new String[] {"Cannot show Layer Preview: No Layers available."});
    }

    @Test
    public void testLayerAttributesTab() {

        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:3:link", "click");
        tester.assertErrorMessages(
                new String[] {"Cannot show Attribute Preview: No Layers available."});
    }
}
