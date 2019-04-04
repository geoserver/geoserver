/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class WCSEOLayerConfigTest extends GeoServerWicketTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, catalog);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpWcs11RasterLayers();
    }

    @Test
    public void testEditPlainTiff() {
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(MockData.TASMANIA_DEM));
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WCSEOLayerConfig(id, new Model(layer));
                            }
                        }));

        // print(tester.getLastRenderedPage(), true, true);
        Component panel = tester.getLastRenderedPage().get("form:panel");
        // the panel must not be visible for this layer
        assertFalse(panel.isVisible());
    }

    @Test
    public void testEditMosaic() {
        // setup the panel with a mosaic
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(WATTEMP));
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WCSEOLayerConfig(id, new Model(layer));
                            }
                        }));

        // print(tester.getLastRenderedPage(), true, true);
        Component panel = tester.getLastRenderedPage().get("form:panel");
        // the panel must be visible for this layer, it's a ,mosaic
        assertTrue(panel.isVisible());

        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:dataset", true);
        ft.submit();

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertModelValue("form:panel:dataset", true);
        assertTrue(
                (boolean)
                        layer.getResource()
                                .getMetadata()
                                .get(WCSEOMetadata.DATASET.key, Boolean.class));
    }
}
