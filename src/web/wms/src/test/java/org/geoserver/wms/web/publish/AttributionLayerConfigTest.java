/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class AttributionLayerConfigTest extends GeoServerWicketTestSupport {

    public <T extends PublishedInfo> void testPublished(final IModel<T> publishedInfoModel) {
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 6999752257807054508L;

                            public Component buildComponent(String id) {
                                return new AttributionLayerConfigPanel(id, publishedInfoModel);
                            }
                        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:wms.attribution.logo", TextField.class);

        // check setting something else works
        String target = "http://example.com/";
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:wms.attribution.logo", target);
        ft.submit();
        tester.assertModelValue("form:panel:wms.attribution.logo", target);
    }

    @Test
    public void testLayer() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        testPublished(new Model<LayerInfo>(layer));
    }

    @Test
    public void testLayerGroup() {
        final LayerGroupInfo layerGroup = getCatalog().getFactory().createLayerGroup();
        testPublished(new Model<LayerGroupInfo>(layerGroup));
    }
}
