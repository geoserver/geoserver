/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.wicket.Component;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class ConfirmRemovalPanelTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle(
                catalog.getWorkspaceByName(MockData.CITE_PREFIX),
                "lakes",
                "Lakes.sld",
                SystemTestData.class,
                catalog);
    }

    void setupPanel(final CatalogInfo... roots) {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new ConfirmRemovalPanel(id, roots);
                            }
                        }));
    }

    @Test
    public void testRemoveWorkspace() {
        setupPanel(getCatalog().getWorkspaceByName(MockData.CITE_PREFIX));

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("form:panel:removedObjects:storesRemoved:stores", "cite");
        tester.assertLabel("form:panel:removedObjects:stylesRemoved:styles", "lakes");

        String layers =
                tester.getComponentFromLastRenderedPage(
                                "form:panel:removedObjects:layersRemoved:layers")
                        .getDefaultModelObjectAsString();
        String[] layerArray = layers.split(", ");
        DataStoreInfo citeStore = getCatalog().getStoreByName("cite", DataStoreInfo.class);
        List<FeatureTypeInfo> typeInfos =
                getCatalog().getResourcesByStore(citeStore, FeatureTypeInfo.class);
        assertEquals(typeInfos.size(), layerArray.length);
    }

    @Test
    public void testRemoveLayer() {
        setupPanel(getCatalog().getLayerByName(getLayerId(MockData.BUILDINGS)));

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        // damn wicket tester, it does not have a assertHidden...
        assertFalse(
                tester.getLastRenderedPage()
                        .get("form:panel:removedObjects:storesRemoved")
                        .isVisible());
        assertFalse(tester.getLastRenderedPage().get("form:panel:modifiedObjects").isVisible());
    }

    @Test
    public void testRemoveStyle() {
        setupPanel(getCatalog().getStyleByName(MockData.BUILDINGS.getLocalPart()));

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        // damn wicket tester, it does not have a assertHidden...
        assertFalse(
                tester.getLastRenderedPage()
                        .get("form:panel:removedObjects:storesRemoved")
                        .isVisible());

        tester.assertLabel("form:panel:modifiedObjects:layersModified:layers", "Buildings");
    }
}
