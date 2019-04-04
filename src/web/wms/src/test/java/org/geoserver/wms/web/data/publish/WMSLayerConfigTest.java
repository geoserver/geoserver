/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.publish.StylesModel;
import org.geoserver.wms.web.publish.WMSLayerConfig;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("serial")
public class WMSLayerConfigTest extends GeoServerWicketTestSupport {

    @Before
    public void resetPondStyle() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(MockData.PONDS.getLocalPart());
        style.setWorkspace(null);
        catalog.save(style);
    }

    @Test
    public void testExisting() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);

        // check selecting something else works
        StyleInfo target = ((List<StyleInfo>) new StylesModel().getObject()).get(0);
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        tester.assertModelValue("form:panel:styles:defaultStyle", target);
    }

    @Test
    public void testNew() {
        final LayerInfo layer = getCatalog().getFactory().createLayer();
        layer.setResource(getCatalog().getFactory().createFeatureType());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        Component layerConfig = page.get("form:panel:styles:defaultStyle");

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);

        // check submitting like this will create errors, there is no selection
        tester.submitForm("form");

        assertTrue(layerConfig.getFeedbackMessages().hasMessage(FeedbackMessage.ERROR));

        // now set something and check there are no messages this time
        page.getSession().getFeedbackMessages().clear();
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        assertFalse(layerConfig.getFeedbackMessages().hasMessage(FeedbackMessage.ERROR));
    }

    @Test
    public void testLegendGraphicURL() throws Exception {
        // force style into ponds workspace
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(MockData.PONDS.getLocalPart());
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.PONDS.getPrefix());
        style.setWorkspace(ws);
        catalog.save(style);

        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, new Model(layer));
                            }
                        });
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.debugComponentTrees();

        Image img =
                (Image)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:styles:defaultStyleLegendGraphic");
        assertNotNull(img);
        assertEquals(1, img.getBehaviors().size());
        assertTrue(img.getBehaviors().get(0) instanceof AttributeModifier);

        AttributeModifier mod = (AttributeModifier) img.getBehaviors().get(0);
        assertTrue(mod.toString().contains("wms?REQUEST=GetLegendGraphic"));
        assertTrue(mod.toString().contains("style=cite:Ponds"));
    }

    @Test
    public void testInterpolationDropDown() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        final Model<LayerInfo> layerModel = new Model<LayerInfo>(layer);

        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WMSLayerConfig(id, layerModel);
                            }
                        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:defaultInterpolationMethod", DropDownChoice.class);

        // By default, no interpolation method is specified
        FormTester ft = tester.newFormTester("form");
        ft.submit();

        tester.assertModelValue("form:panel:defaultInterpolationMethod", null);

        // Select Bicubic interpolation method
        ft = tester.newFormTester("form");
        ft.select("panel:defaultInterpolationMethod", 2);
        ft.submit();

        tester.assertModelValue("form:panel:defaultInterpolationMethod", WMSInterpolation.Bicubic);
    }
}
