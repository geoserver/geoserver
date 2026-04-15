/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.TagTester;
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
        List<String> scripts = TagTester.createTags(
                        tester.getLastResponseAsString(), tag -> tag.getName().equalsIgnoreCase("script"), false)
                .stream()
                .map(tag -> tag.getAttribute("src"))
                .collect(Collectors.toList());
        String regex =
                "^.*/" + CachedLayersPage.class.getName() + "/" + CachedLayersPage.class.getSimpleName() + ".*\\.js$";
        assertThat(scripts, hasItem(matchesRegex(regex)));
    }

    @Test
    public void testLayerGroupLink() {
        GWC gwc = GWC.get();
        TileLayer tileLayer = gwc.getTileLayerByName(NATURE_GROUP);
        assertNotNull(tileLayer);

        tester.startComponentInPage(
                new ConfigureCachedLayerAjaxLink("test", new TileLayerDetachableModel(tileLayer.getName()), null));
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
        URLMangler testMangler = (base, path, map, type) -> {
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
        assertPreviewLinks("http://localhost/context/cgf/gwc/demo/cgf");
    }

    @Test
    public void testManglePreviewLink() {
        // Mimic a Proxy URL mangler
        URLMangler testMangler = (base, path, map, type) -> {
            base.setLength(0);
            base.append("http://rewrite/");
        };
        extensions.singleton("testMangler", testMangler, URLMangler.class);
        assertPreviewLinks("http://rewrite/gwc/demo/cgf");
    }

    private static void assertPreviewLinks(String url) {
        CachedLayersPage page = new CachedLayersPage();
        tester.startPage(page);
        List<TagTester> tags = TagTester.createTags(
                tester.getLastResponseAsString(),
                tag -> {
                    String value = tag.getAttributes().getString("value");
                    return value != null && value.startsWith(url);
                },
                false);
        assertEquals("Incorrect number of preview links starting with " + url, 20, tags.size());
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
        tester.clickLink("dialog:dialog:modal:overlay:dialog:content:content:form:submit", true);

        tester.assertNoErrorMessage();
    }

    @Test
    public void testCachingImages() {
        // test that icons are rendered as CSS icon elements (gs-icon-*), not img tags
        tester.startPage(CachedLayersPage.class);
        tester.assertRenderedPage(CachedLayersPage.class);
        tester.clickLink("table:navigatorBottom:navigator:next", true);
        List<TagTester> icons = TagTester.createTags(
                tester.getLastResponseAsString(),
                tag -> tag.getName().equalsIgnoreCase("i")
                        && tag.getAttribute("class") != null
                        && tag.getAttribute("class").toString().contains("gs-icon"),
                false);
        assertThat(icons, not(empty()));
        icons.stream()
                .map(icon -> icon.getAttribute("class"))
                .forEach(cls -> assertThat(cls, containsString("gs-icon")));
    }

    @Test
    public void testWorkspaceFilter() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");

        tester.startPage(new CachedLayersPage(params));
        tester.assertRenderedPage(CachedLayersPage.class);

        // Cast to DataView instead of AbstractPageableView
        tester.assertComponent("table:listContainer:items", DataView.class);
        DataView<?> dataView = (DataView<?>) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        // Ensure the table isn't empty
        assertThat((int) dataView.getItemCount(), org.hamcrest.Matchers.greaterThan(0));

        // Iterate through the actual layer models backing the table rows
        Iterator<?> it = dataView.getDataProvider().iterator(0, dataView.getItemCount());
        while (it.hasNext()) {
            TileLayer layer = (TileLayer) it.next();
            // Assert every single layer in the table belongs to the 'cite' workspace
            assertThat(layer.getName(), org.hamcrest.Matchers.startsWith("cite:"));
        }
    }

    @Test
    public void testEmptyWorkspaceParameter() {
        PageParameters params = new PageParameters();
        params.add("workspace", ""); // Empty parameter

        tester.startPage(new CachedLayersPage(params));
        tester.assertRenderedPage(CachedLayersPage.class);

        DataView<?> dataView = (DataView<?>) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean foundCgf = false;
        boolean foundCite = false;

        Iterator<?> it = dataView.getDataProvider().iterator(0, dataView.getItemCount());
        while (it.hasNext()) {
            TileLayer layer = (TileLayer) it.next();
            if (layer.getName().startsWith("cgf:")) foundCgf = true;
            if (layer.getName().startsWith("cite:")) foundCite = true;
        }

        // Assert that layers from multiple different workspaces are present in the table
        assertThat("Should contain 'cgf:' layers", foundCgf, org.hamcrest.Matchers.is(true));
        assertThat("Should contain 'cite:' layers", foundCite, org.hamcrest.Matchers.is(true));
    }

    @Test
    public void testNonExistentWorkspaceFilter() {
        PageParameters params = new PageParameters();
        params.add("workspace", "nonexistent_ws");

        tester.startPage(new CachedLayersPage(params));
        tester.assertRenderedPage(CachedLayersPage.class);

        DataView<?> dataView = (DataView<?>) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        // Assert the data provider returned exactly 0 rows
        assertEquals("Table should have 0 rows for a non-existent workspace", 0, dataView.getItemCount());
    }
}
