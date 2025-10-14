/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.junit.After;
import org.junit.Test;

public class ConditionTest extends AbstractWicketMetadataTest {

    @Override
    protected boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs10Coverages();
        return true;
    }

    @After
    @Override
    public void stop() throws Exception {
        restoreLayers();
        super.stop();
    }

    @Test
    public void testNoFeatureCatalogueForRasters() {
        // this test does not run reliably on GitHub actions, cause unclear
        assumeFalse(GeoServerSystemTestSupport.isGitHubAction());

        login();
        LayerInfo raster = geoServer.getCatalog().getLayerByName(MockData.USA_WORLDIMG.getLocalPart());
        ResourceConfigurationPage page = new ResourceConfigurationPage(raster, false);
        tester.startPage(page);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs")).setSelectedTab(4);

        MarkupContainer c = (MarkupContainer) tester.getComponentFromLastRenderedPage(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items");
        tester.submitForm("publishedinfo");
        assertEquals(14, c.size());
        logout();
    }
}
