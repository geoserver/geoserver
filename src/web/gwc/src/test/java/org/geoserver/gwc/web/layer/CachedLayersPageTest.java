/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geowebcache.layer.TileLayer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CachedLayersPageTest extends GeoServerWicketTestSupport {
    private static final Method getReplaceModelMethod;
    /*
     * Yes, we do need to use reflection here as the stupid wicket model is private and has no setter which
     * makes it hard to test!
     * See https://cwiki.apache.org/confluence/display/WICKET/Testing+Pages
     */
    static {
        try {
            getReplaceModelMethod = AttributeModifier.class.getDeclaredMethod("getReplaceModel");
            getReplaceModelMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Rule
    public GeoServerExtensionsHelper.ExtensionsHelperRule extensions =
            new GeoServerExtensionsHelper.ExtensionsHelperRule();

    protected static final String NATURE_GROUP = "nature";

    @Before
    public void loginBefore() {
        super.login();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // setup a layer group
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if (lakes != null && forests != null) {
            group.setName(NATURE_GROUP);
            group.getLayers().add(lakes);
            group.getLayers().add(forests);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
        }
    }

    @Test
    public void testPageLoad() {
        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        tester.assertRenderedPage(CachedLayersPage.class);

        // print(page, true, true);
    }

    @Test
    public void testLayerGroupLink() {
        GWC gwc = GWC.get();
        TileLayer tileLayer = gwc.getTileLayerByName(NATURE_GROUP);
        assertNotNull(tileLayer);

        tester.startComponentInPage(
                new ConfigureCachedLayerAjaxLink(
                        "test", new TileLayerDetachableModel(tileLayer.getName()), null));
        // tester.debugComponentTrees();
        tester.executeAjaxEvent("test:link", "click");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(LayerGroupEditPage.class);
    }

    @Test
    public void testNoMangleSeedLink() {

        // Don't add a mangler

        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        tester.assertModelValue(
                "table:listContainer:items:1:itemProperties:7:component:seedLink",
                "http://localhost/context/gwc/rest/seed/cgf:Polygons");
    }

    @Test
    public void testMangleSeedLink() {
        // Mimic a Proxy URL mangler
        URLMangler testMangler =
                (base, path, map, type) -> {
                    base.setLength(0);
                    base.append("http://rewrite/");
                };
        extensions.singleton("testMangler", testMangler, URLMangler.class);

        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        tester.assertModelValue(
                "table:listContainer:items:1:itemProperties:7:component:seedLink",
                "http://rewrite/gwc/rest/seed/cgf:Polygons");
    }

    @Test
    public void testNoManglePreviewLink() {

        // Don't add a mangler

        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        // print(page, true, true);
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "table:listContainer:items:1:itemProperties:6:component:menu");
        List<AttributeModifier> attr = component.getBehaviors(AttributeModifier.class);
        try {
            IModel<?> model = (IModel<?>) getReplaceModelMethod.invoke(attr.get(0));
            assertTrue(
                    "Unmangled names fail",
                    model.getObject().toString().contains("http://localhost/context/gwc/demo/cgf"));
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testManglePreviewLink() {
        // Mimic a Proxy URL mangler
        URLMangler testMangler =
                (base, path, map, type) -> {
                    base.setLength(0);
                    base.append("http://rewrite/");
                };
        extensions.singleton("testMangler", testMangler, URLMangler.class);

        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "table:listContainer:items:1:itemProperties:6:component:menu");
        List<AttributeModifier> attr = component.getBehaviors(AttributeModifier.class);
        try {
            IModel<?> model = (IModel<?>) getReplaceModelMethod.invoke(attr.get(0));

            assertTrue(
                    "Mangled names fail",
                    model.getObject().toString().contains("http://rewrite/gwc/demo/cgf"));
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGWCClean() {
        // This asserts GWC integration from GUI
        CachedLayersPage page = new CachedLayersPage();

        // load tiles layer list page
        tester.startPage(page);
        tester.assertRenderedPage(CachedLayersPage.class);

        //        // click on the Empty All Link
        tester.clickLink("headerPanel:clearGwcLink", true);

        // assert the dialog shows up
        tester.assertVisible("dialog");

        // click submit
        tester.clickLink("dialog:dialog:content:form:submit", true);

        tester.assertNoErrorMessage();
    }

    @Test
    public void testAutoTileCachingTabSelection() {
        // this tests asserts that when a layer is clicked on Tile Layer page
        // the resulting navigation page should have the 'Tile Caching' tab selected by itself
        CachedLayersPage page = new CachedLayersPage();

        // load tiles layer list page
        tester.startPage(page);
        tester.assertRenderedPage(CachedLayersPage.class);

        // click on the first layer
        tester.clickLink("table:listContainer:items:1:itemProperties:1:component:link", true);

        // UI should navigate to ResourceConfiguration Page with Tiles tab selected
        tester.assertComponent("publishedinfo:tabs:panel", LayerCacheOptionsTabPanel.class);
    }
}
