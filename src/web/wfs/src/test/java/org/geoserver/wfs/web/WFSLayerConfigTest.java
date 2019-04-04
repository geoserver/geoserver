/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.web.publish.WFSLayerConfig;
import org.junit.Test;

/** Contains tests related with WFS specific publishing configuration options * */
public final class WFSLayerConfigTest extends GeoServerWicketTestSupport {

    @Test
    public void testEncodeMeasuresCheckbox() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        Model<LayerInfo> model = new Model<>(layer);
        FormTestPage page =
                new FormTestPage((ComponentBuilder) id -> new WFSLayerConfig(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the checkbox is available
        tester.assertComponent("form:panel:encodeMeasures", CheckBox.class);
        // unselect the checkbox, no measures should be selected
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:encodeMeasures", false);
        ft.submit();
        tester.assertModelValue("form:panel:encodeMeasures", false);
    }

    @Test
    public void testForceDecimalCheckbox() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        Model<LayerInfo> model = new Model<>(layer);
        FormTestPage page =
                new FormTestPage((ComponentBuilder) id -> new WFSLayerConfig(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the checkbox is available
        tester.assertComponent(
                "form:panel:forcedDecimalBorder:forcedDecimalBorder_body:forcedDecimal",
                CheckBox.class);
        // unselect the checkbox, no measures should be selected
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:forcedDecimalBorder:forcedDecimalBorder_body:forcedDecimal", true);
        ft.submit();
        tester.assertModelValue(
                "form:panel:forcedDecimalBorder:forcedDecimalBorder_body:forcedDecimal", true);
    }

    @Test
    public void testPadWithZerosCheckbox() {
        // get a test layer and instantiate the model
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        Model<LayerInfo> model = new Model<>(layer);
        FormTestPage page =
                new FormTestPage((ComponentBuilder) id -> new WFSLayerConfig(id, model));
        // let's start the page and check that the components are correctly instantiated
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        // check that the checkbox is available
        tester.assertComponent(
                "form:panel:padWithZerosBorder:padWithZerosBorder_body:padWithZeros",
                CheckBox.class);
        // unselect the checkbox, no measures should be selected
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:padWithZerosBorder:padWithZerosBorder_body:padWithZeros", true);
        ft.submit();
        tester.assertModelValue(
                "form:panel:padWithZerosBorder:padWithZerosBorder_body:padWithZeros", true);
    }
}
