/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.web;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class RasterAttributeTableConfigTest extends GeoServerWicketTestSupport {

    QName RAT = new QName(MockTestData.CITE_URI, "rat", MockTestData.CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addRasterLayer(RAT, "rat.tiff", "tiff", null, getClass(), getCatalog());
        GeoServerDataDirectory dd = getDataDirectory();
        Resource aux = dd.get("rat", "rat.tiff.aux.xml");
        try (InputStream is = getClass().getResourceAsStream("rat.tiff.aux.xml");
                OutputStream os = aux.out()) {
            IOUtils.copy(is, os);
            os.close();
        }
    }

    @Before
    public void startComponent() {
        FormTestPage page = new FormTestPage((ComponentBuilder) id -> {
            LayerInfo layer = getCatalog().getLayerByName(getLayerId(RAT));
            return new RasterAttributeTableConfig(id, new Model<>(layer));
        });

        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        String id = FormTestPage.FORM + ":" + FormTestPage.PANEL;
        tester.assertComponent(id, RasterAttributeTableConfig.class);
        tester.assertVisible(id);
    }

    @Test
    public void testRatPanelRenders() throws Exception {
        // check table is rendered and first entry is correct
        String itemProperties = "form:panel:rat:listContainer:items:1:itemProperties";
        tester.assertModelValue(itemProperties + ":0:component", "1.000000023841858");
        tester.assertModelValue(itemProperties + ":1:component", "1.200000023841858");
        tester.assertModelValue(itemProperties + ":2:component", "green");
    }

    @Test
    public void testGenerateSLD() throws Exception {
        FormTester form = tester.newFormTester("form");
        String styleToolbar = "panel:styleToolbar";
        String styleToolbarFull = "form:" + styleToolbar;
        form.select(styleToolbar + ":bands", 0);
        tester.executeAjaxEvent(styleToolbarFull + ":bands", "change");
        form.select(styleToolbar + ":classifications", 0);
        tester.executeAjaxEvent(styleToolbarFull + ":classifications", "change");
        tester.assertNoErrorMessage();
        String styleName = "rat_b0_test";
        tester.assertModelValue(styleToolbarFull + ":styleName", styleName);
        form.submit(styleToolbar + ":createStyles");
        tester.assertInfoMessages("Created style cite:rat_b0_test");

        // actual style contents checked elsewhere
        StyleInfo style = getCatalog().getStyleByName(styleName);
        assertNotNull(style);
    }
}
