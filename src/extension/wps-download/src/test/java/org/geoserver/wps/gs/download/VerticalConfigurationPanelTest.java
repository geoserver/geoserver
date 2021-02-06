/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.geoserver.data.test.CiteTestData.WCS_PREFIX;
import static org.geoserver.web.wps.VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY;
import static org.junit.Assert.assertNotNull;

import javax.xml.namespace.QName;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wps.VerticalCRSConfigurationPanel;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Before;
import org.junit.Test;

public class VerticalConfigurationPanelTest extends GeoServerWicketTestSupport {

    private static QName FLOAT = new QName(WPSTestSupport.WCS_URI, "float", WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(FLOAT, "float.zip", null, getCatalog());
    }

    @Before
    public void setUpInternal() throws Exception {
        // Creating models
        LayerInfo layerInfo = getCatalog().getLayerByName("float");
        ResourceInfo resource = layerInfo.getResource();
        // Add Element to MetadataMap
        MetadataMap metadata = resource.getMetadata();
        if (!metadata.containsKey(VERTICAL_CRS_KEY)) {
            metadata.put(VERTICAL_CRS_KEY, "TEST");
        }
        getCatalog().save(layerInfo);
    }

    @Test
    public void testComponent() {
        login();
        CoverageInfo coverage = getCatalog().getCoverageByName("float");
        Model<CoverageInfo> model = new Model<>(coverage);

        FormTestPage page = new FormTestPage(id -> new VerticalCRSConfigurationPanel(id, model));
        tester.startPage(page);

        tester.assertComponent("form:panel", VerticalCRSConfigurationPanel.class);
        MarkupContainer container =
                (MarkupContainer) tester.getComponentFromLastRenderedPage("form:panel:verticalCRS");
        assertNotNull(container);
    }
}
