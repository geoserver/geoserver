/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.web.InternationalStringPanel;
import org.geoserver.web.data.resource.MetadataLinkEditor;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.util.InternationalString;

public class LayerGroupEditPageTest extends LayerGroupBaseTest {

    @Test
    public void testComputeBounds() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(new PageParameters().add("group", "lakes"));
        tester.startPage(page);
        // print(page, true, false);

        tester.assertRenderedPage(LayerGroupEditPage.class);
        // remove the first and second elements
        // tester.clickLink("form:layers:layers:listContainer:items:1:itemProperties:4:component:link");
        // the regenerated list will have ids starting from 4
        // tester.clickLink("form:layers:layers:listContainer:items:4:itemProperties:4:component:link");
        // manually regenerate bounds
        tester.clickLink("publishedinfo:tabs:panel:generateBounds");
        // print(page, true, true);
        // submit the form
        tester.submitForm("publishedinfo");

        // For the life of me I cannot get this test to work... and I know by direct UI inspection
        // that
        // the page works as expected...
        //        FeatureTypeInfo bridges =
        // getCatalog().getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);
        //        assertEquals(getCatalog().getLayerGroupByName("lakes").getBounds(),
        // bridges.getNativeBoundingBox());
    }

    @Test
    public void testComputeBoundsFromCRS() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(new PageParameters().add("group", "lakes"));
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);

        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");
        tester.clickLink("publishedinfo:tabs:panel:generateBoundsFromCRS", true);
        tester.assertComponentOnAjaxResponse("publishedinfo:tabs:panel:bounds");
        Component ajaxComponent =
                tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:bounds");
        assert (ajaxComponent instanceof EnvelopePanel);
        EnvelopePanel envPanel = (EnvelopePanel) ajaxComponent;
        assertEquals(
                ((DecimalTextField) envPanel.get("minX")).getModelObject(), Double.valueOf(-180.0));
        assertEquals(
                ((DecimalTextField) envPanel.get("minY")).getModelObject(), Double.valueOf(-90.0));
        assertEquals(
                ((DecimalTextField) envPanel.get("maxX")).getModelObject(), Double.valueOf(180.0));
        assertEquals(
                ((DecimalTextField) envPanel.get("maxY")).getModelObject(), Double.valueOf(90.0));
    }

    @Before
    public void doLogin() {
        login();
    }

    @Test
    public void testMissingName() {
        LayerGroupEditPage page = new LayerGroupEditPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        FormTester form = tester.newFormTester("publishedinfo");
        form.submit();

        // should not work, no name provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupEditPage.class);
        tester.assertErrorMessages(
                new String[] {"Field 'Name' is required.", "Field 'Bounds' is required."});
    }

    @Test
    public void testMissingCRS() {
        LayerGroupEditPage page = new LayerGroupEditPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "lakes");
        form.setValue("tabs:panel:bounds:minX", "-180");
        form.setValue("tabs:panel:bounds:minY", "-90");
        form.setValue("tabs:panel:bounds:maxX", "180");
        form.setValue("tabs:panel:bounds:maxY", "90");

        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        form.submit("save");

        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupEditPage.class);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message = tester.getMessages(FeedbackMessage.ERROR).get(0).toString();
        assertTrue(message.contains("Bounds"));
    }

    @Test
    public void testDuplicateName() {
        LayerGroupEditPage page = new LayerGroupEditPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "lakes");
        form.setValue("tabs:panel:bounds:minX", "0");
        form.setValue("tabs:panel:bounds:minY", "0");
        form.setValue("tabs:panel:bounds:maxX", "1");
        form.setValue("tabs:panel:bounds:maxY", "1");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");

        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        form.submit("save");

        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupEditPage.class);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        assertTrue(
                tester.getMessages(FeedbackMessage.ERROR)
                        .get(0)
                        .toString()
                        .endsWith("Layer group named 'lakes' already exists"));
    }

    @Test
    public void testNewName() {
        LayerGroupEditPage page = new LayerGroupEditPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "newGroup");
        form.submit();

        // should work, we switch to the edit page
        tester.assertRenderedPage(LayerGroupEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Bounds' is required."});
    }

    @Test
    public void testLayerLink() {

        LayerGroupEditPage page = new LayerGroupEditPage();
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addLayer");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent(
                "publishedinfo:tabs:panel:layers:popup:content:listContainer:items",
                DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView =
                (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();

        int layerCount = catalog.count(LayerInfo.class, Filter.INCLUDE);
        int rowCount = (int) dataView.getRowCount();

        assertEquals(layerCount, rowCount);
    }

    @Test
    public void testStyleGroupLink() {

        LayerGroupEditPage page = new LayerGroupEditPage();
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addStyleGroup");
        tester.assertNoErrorMessage();
        // Ensure that the Style Group List page is rendered correctly
        tester.assertComponent(
                "publishedinfo:tabs:panel:layers:popup:content:listContainer:items",
                DataView.class);
        // Get the DataView containing the Style Group List
        DataView<?> dataView =
                (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the style in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();

        int styleCount = catalog.count(StyleInfo.class, Filter.INCLUDE);
        int rowCount = (int) dataView.getRowCount();

        assertEquals(styleCount, rowCount);
    }

    @Test
    public void testLayerLinkWithWorkspace() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(
                        new PageParameters().add("workspace", "cite").add("group", "bridges"));
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addLayer");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent(
                "publishedinfo:tabs:panel:layers:popup:content:listContainer:items",
                DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView =
                (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();

        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
        final Filter filter =
                ff.equal(
                        ff.property("resource.store.workspace.id"),
                        ff.literal(catalog.getWorkspaceByName("cite").getId()),
                        true);

        int layerCount = catalog.count(LayerInfo.class, filter);
        int rowCount = (int) dataView.getRowCount();

        assertEquals(layerCount, rowCount);
    }

    @Test
    public void testLayerGroupLinkWithWorkspace() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(
                        new PageParameters().add("workspace", "cite").add("group", "bridges"));
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addLayerGroup");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent(
                "publishedinfo:tabs:panel:layers:popup:content:listContainer:items",
                DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView =
                (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();

        int layerGroupCount = catalog.getLayerGroupsByWorkspace("cite").size();
        int rowCount = (int) dataView.getRowCount();

        assertEquals(layerGroupCount, rowCount);
    }

    @Test
    public void testMetadataLinks() {
        LayerGroupEditPage page = new LayerGroupEditPage();
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);

        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent("publishedinfo:tabs:panel:metadataLinks", MetadataLinkEditor.class);

        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "lakes");
        form.setValue("tabs:panel:bounds:minX", "0");
        form.setValue("tabs:panel:bounds:minY", "0");
        form.setValue("tabs:panel:bounds:maxX", "1");
        form.setValue("tabs:panel:bounds:maxY", "1");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");

        tester.executeAjaxEvent("publishedinfo:tabs:panel:metadataLinks:addlink", "click");

        form.setValue(
                "tabs:panel:metadataLinks:container:table:links:0:urlBorder:urlBorder_body:metadataLinkURL",
                "http://test.me");
        tester.executeAjaxEvent("publishedinfo:tabs:panel:metadataLinks:addlink", "click");

        LayerGroupInfo info = page.getPublishedInfo();
        assertEquals(2, info.getMetadataLinks().size());
        assertEquals("http://test.me", info.getMetadataLinks().get(0).getContent());
    }

    @Test
    public void testKeywords() {
        // create a new layer group page
        LayerGroupEditPage page = new LayerGroupEditPage();
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // check that keywords editor panel was rendered
        tester.assertComponent("publishedinfo:tabs:panel:keywords", KeywordsEditor.class);
        // add layer group entries
        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        // add layer group mandatory parameters
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "keywords-layer-group");
        form.setValue("tabs:panel:bounds:minX", "-180");
        form.setValue("tabs:panel:bounds:minY", "-90");
        form.setValue("tabs:panel:bounds:maxX", "180");
        form.setValue("tabs:panel:bounds:maxY", "90");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");
        // add a keyword
        form.setValue("tabs:panel:keywords:newKeyword", "keyword1");
        form.setValue("tabs:panel:keywords:lang", "en");
        form.setValue("tabs:panel:keywords:vocab", "vocab1");
        tester.executeAjaxEvent("publishedinfo:tabs:panel:keywords:addKeyword", "click");
        // add another keyword
        form.setValue("tabs:panel:keywords:newKeyword", "keyword2");
        form.setValue("tabs:panel:keywords:lang", "pt");
        form.setValue("tabs:panel:keywords:vocab", "vocab2");
        tester.executeAjaxEvent("publishedinfo:tabs:panel:keywords:addKeyword", "click");
        // save the layer group
        form = tester.newFormTester("publishedinfo");
        form.submit("save");
        // get the create layer group from the catalog
        LayerGroupInfo layerGroup = getCatalog().getLayerGroupByName("keywords-layer-group");
        assertThat(layerGroup, notNullValue());
        // check the keywords
        List<KeywordInfo> keywords = layerGroup.getKeywords();
        assertThat(keywords, notNullValue());
        assertThat(keywords.size(), is(2));
        // check that the first keyword is present
        assertElementExist(
                keywords,
                (keyword) -> {
                    assertThat(keyword, notNullValue());
                    return Objects.equals(keyword.getValue(), "keyword1")
                            && Objects.equals(keyword.getLanguage(), "en")
                            && Objects.equals(keyword.getVocabulary(), "vocab1");
                });
        // check that the second keyword is present
        assertElementExist(
                keywords,
                (keyword) -> {
                    assertThat(keyword, notNullValue());
                    return Objects.equals(keyword.getValue(), "keyword2")
                            && Objects.equals(keyword.getLanguage(), "pt")
                            && Objects.equals(keyword.getVocabulary(), "vocab2");
                });
    }

    @Test
    public void testHTTPCaches() {
        // create a new layer group page
        LayerGroupEditPage page = new LayerGroupEditPage();
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // check that keywords editor panel was rendered
        // tester.assertComponent("publishedinfo:tabs:panel:keywords", KeywordsEditor.class);
        // add layer group entries
        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        // add layer group mandatory parameters
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:name", "httpcaches-layer-group");
        form.setValue("tabs:panel:bounds:minX", "-180");
        form.setValue("tabs:panel:bounds:minY", "-90");
        form.setValue("tabs:panel:bounds:maxX", "180");
        form.setValue("tabs:panel:bounds:maxY", "90");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");

        tester.clickLink("publishedinfo:tabs:tabs-container:tabs:1:link");

        form = tester.newFormTester("publishedinfo");

        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:1:content:cacheAgeMax", TextField.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:1:content:cachingEnabled", CheckBox.class);

        form.setValue("tabs:panel:theList:1:content:cachingEnabled", "on");
        form.setValue("tabs:panel:theList:1:content:cacheAgeMax", "1234");

        // save the layer group
        form.submit("save");

        tester.assertNoErrorMessage();

        // get the create layer group from the catalog
        LayerGroupInfo layerGroup = getCatalog().getLayerGroupByName("httpcaches-layer-group");
        assertThat(layerGroup, notNullValue());
        assertEquals(
                Integer.valueOf(1234),
                layerGroup.getMetadata().get(FeatureTypeInfo.CACHE_AGE_MAX, Integer.class));
        assertTrue(layerGroup.getMetadata().get(FeatureTypeInfo.CACHING_ENABLED, Boolean.class));
    }

    @Test
    public void testStyleGroup() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(new PageParameters().add("group", "styleGroup"));
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);

        // save the layer group
        FormTester form = tester.newFormTester("publishedinfo");
        form.submit("save");
        // We should be back to the list page
        tester.assertRenderedPage(LayerGroupPage.class);
    }

    /** Checks that an element that match the provided matcher exists. */
    private <T> void assertElementExist(List<T> elements, Function<T, Boolean> matcher) {
        boolean found = false;
        for (T element : elements) {
            if (matcher.apply(element)) {
                // element found
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
    }

    @Test
    public void testGroupManyLayers() throws Exception {
        String groupName = "many-lakes";
        buildManyLakes(groupName);

        LayerGroupEditPage page =
                new LayerGroupEditPage(new PageParameters().add("group", groupName));
        tester.startPage(page);
        // print(tester.getLastRenderedPage(), true, true, true);

        // check we have all the expected components showing up
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:layers:layers:listContainer:items:50");
        assertNotNull(component);
    }

    private void buildManyLakes(String groupName) throws Exception {
        Catalog catalog = getCatalog();
        String lakes = MockData.LAKES.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName(groupName);
        for (int i = 0; i < 50; i++) {
            lg.getLayers().add(catalog.getLayerByName(lakes));
        }
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    @Test
    public void testApply() {
        LayerGroupEditPage page =
                new LayerGroupEditPage(
                        new PageParameters().add("workspace", "cite").add("group", "bridges"));
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Update the title
        FormTester ft = tester.newFormTester("publishedinfo");
        ft.setValue("tabs:panel:bounds:minX", "0");
        ft.setValue("tabs:panel:bounds:minY", "0");
        ft.setValue("tabs:panel:bounds:minZ", "0");
        ft.setValue("tabs:panel:bounds:maxX", "1");
        ft.setValue("tabs:panel:bounds:maxY", "1");
        ft.setValue("tabs:panel:bounds:maxZ", "1");
        String newTitle = "A test title";
        ft.setValue("tabs:panel:titleAndAbstract:title", newTitle);
        ft.submit("apply");
        // no errors, and page is still the same
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(LayerGroupEditPage.class);

        // check the title was updated
        assertEquals(newTitle, getCatalog().getLayerGroupByName("cite:bridges").getTitle());
    }

    @Test
    public void testInternationalContent() {
        // create a new layer group page
        LayerGroupEditPage page = new LayerGroupEditPage();
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // check that keywords editor panel was rendered
        tester.assertComponent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle",
                InternationalStringPanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract",
                InternationalStringPanel.class);

        // add layer group entries
        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        // add layer group mandatory parameters
        FormTester form = tester.newFormTester("publishedinfo");

        // enable i18n for title
        form.setValue("tabs:panel:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        form.select(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle:container:addNew",
                "click");
        form.select(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");

        // enable i18n for abstract
        form.setValue("tabs:panel:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        form.select(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        form.select(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");

        // set mandatory fields
        form = tester.newFormTester("publishedinfo");

        form.setValue("tabs:panel:name", "international-layer-group");
        form.setValue("tabs:panel:bounds:minX", "-180");
        form.setValue("tabs:panel:bounds:minY", "-90");
        form.setValue("tabs:panel:bounds:maxX", "180");
        form.setValue("tabs:panel:bounds:maxY", "90");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");
        form.submit("save");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testEmptyLangInternationalContent() {
        // create a new layer group page
        LayerGroupEditPage page = new LayerGroupEditPage();
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // check that keywords editor panel was rendered
        tester.assertComponent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle",
                InternationalStringPanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract",
                InternationalStringPanel.class);

        // add layer group entries
        page.lgEntryPanel
                .getEntries()
                .add(
                        new LayerGroupEntry(
                                getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        // add layer group mandatory parameters
        FormTester form = tester.newFormTester("publishedinfo");

        // enable i18n for title
        form.setValue("tabs:panel:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        form.select(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);

        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        // enable i18n for abstract
        form.setValue("tabs:panel:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox", true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        form.select(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);

        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:titleAndAbstract:internationalAbstract:container:addNew",
                "click");

        form = tester.newFormTester("publishedinfo");

        // set the titles to i18n fields
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "empty lang international title");
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract");
        form.setValue(
                "tabs:panel:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "empty lang international abstract");

        // set mandatory fields
        form.setValue("tabs:panel:name", "bridges-i18n-layer-group");
        form.setValue("tabs:panel:bounds:minX", "-180");
        form.setValue("tabs:panel:bounds:minY", "-90");
        form.setValue("tabs:panel:bounds:maxX", "180");
        form.setValue("tabs:panel:bounds:maxY", "90");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");
        form.submit("save");
        tester.assertNoErrorMessage();
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("bridges-i18n-layer-group");
        InternationalString i18nTitle = groupInfo.getInternationalTitle();
        assertEquals("empty lang international title", i18nTitle.toString(null));
        InternationalString i18nAbstract = groupInfo.getInternationalAbstract();
        assertEquals("empty lang international abstract", i18nAbstract.toString(null));
    }

    @Test
    public void testLayerGroupStyle() {
        // test layerGroup Style UI.
        LayerGroupInfo groupInfo = null;
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(
                            new PageParameters().add("workspace", "cite").add("group", "bridges"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);

            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:addNew", "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:addLayer",
                    "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:popup:content:listContainer:items:1:itemProperties:0:component:link",
                    "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:addLayerGroup",
                    "click");

            FormTester ft = tester.newFormTester("publishedinfo");
            ft.setValue("tabs:panel:bounds:minX", "0");
            ft.setValue("tabs:panel:bounds:minY", "0");
            ft.setValue("tabs:panel:bounds:minZ", "0");
            ft.setValue("tabs:panel:bounds:maxX", "1");
            ft.setValue("tabs:panel:bounds:maxY", "1");
            ft.setValue("tabs:panel:bounds:maxZ", "1");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupStyleName",
                    "LgStyleTest");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("cite:bridges");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(1, styles.size());
            LayerGroupStyle groupStyle = styles.get(0);
            assertEquals(1, groupStyle.getLayers().size());
            assertEquals(1, groupStyle.getStyles().size());
            assertEquals("LgStyleTest", groupStyle.getName().getName());
        } finally {
            if (groupInfo != null) {
                groupInfo.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(groupInfo);
            }
        }
    }

    @Test
    public void testLayerGroupStyle2() throws Exception {
        // test LayerGroupStyle with nested group.
        buildLayerGroup("testLgStyles");

        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testLgStyles");
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(new PageParameters().add("group", "testLgStyles"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);

            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:addNew", "click");

            // add a nested LayerGroup
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:addLayerGroup",
                    "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:popup:content:listContainer:items:3:itemProperties:0:component:link",
                    "click");

            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:addLayer",
                    "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:popup:content:listContainer:items:1:itemProperties:0:component:link",
                    "click");

            FormTester ft = tester.newFormTester("publishedinfo");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupStyleName",
                    "anotherLgStyleTest");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("testLgStyles");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(1, styles.size());
            LayerGroupStyle theStyle = styles.get(0);
            assertEquals(2, theStyle.getLayers().size());
            assertEquals(2, theStyle.getStyles().size());
            assertNull(theStyle.getStyles().get(0));
            assertEquals("anotherLgStyleTest", theStyle.getName().getName());
        } finally {
            if (groupInfo != null) {
                getCatalog().remove(groupInfo);
            }
        }
    }

    @Test
    public void testLayerGroupStyleSelection() throws Exception {
        // tests the possibility to select a layerGroupStyle as a style for an entry.
        buildLayerGroup("testLgStyles-2");
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testLgStyles-2");

        // get the a LayerGroup that will be added as an entry and adds two styles.
        LayerGroupInfo nestedGroup = getCatalog().getLayerGroupByName("nestedLayerGroup");
        LayerGroupStyle nestedGroupStyle = new LayerGroupStyleImpl();
        StyleInfo styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("nestedGroupStyle");
        nestedGroupStyle.setName(styleName);
        nestedGroupStyle.getStyles().add(getCatalog().getStyleByName("BasicPolygons"));
        nestedGroupStyle.getLayers().add(getCatalog().getLayerByName("cite:BasicPolygons"));
        nestedGroup.getLayerGroupStyles().add(nestedGroupStyle);

        LayerGroupStyle nestedGroupStyle2 = new LayerGroupStyleImpl();
        StyleInfo styleName2 = new StyleInfoImpl(getCatalog());
        styleName2.setName("nestedGroupStyle2");
        nestedGroupStyle2.setName(styleName2);
        nestedGroupStyle2.getStyles().add(null);
        nestedGroupStyle2.getLayers().add(getCatalog().getLayerByName(getLayerId(MockData.LAKES)));
        nestedGroup.getLayerGroupStyles().add(nestedGroupStyle2);
        getCatalog().save(nestedGroup);

        // create a style for the containing group
        LayerGroupStyle groupStyleTest = new LayerGroupStyleImpl();
        styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("styleWithNestedGroup");
        groupStyleTest.setName(styleName);
        groupStyleTest.getLayers().add(nestedGroup);
        groupStyleTest.getStyles().add(null);
        groupInfo.getLayerGroupStyles().add(groupStyleTest);
        getCatalog().save(groupInfo);

        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(new PageParameters().add("group", "testLgStyles-2"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);

            // change from default style for the nested layerGroup
            FormTester ft = tester.newFormTester("publishedinfo");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:layers:listContainer:items:1:itemProperties:4:component:checkbox",
                    false);
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:layers:listContainer:items:1:itemProperties:4:component:checkbox",
                    "change");
            // click on the style name to open the style selector.
            tester.clickLink(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:layers:listContainer:items:2:itemProperties:5:component:link");

            // select the LayerGroupStyle.
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:popup:content:listContainer:items:2:itemProperties:0:component:link",
                    "click");

            // forces the model of the default style checkbox to be set to false
            // to avoid that the recreation of the FormTester loose the already set value.
            // this seems to happens in Linux and MacOs
            CheckBox checkBox =
                    (CheckBox)
                            tester.getComponentFromLastRenderedPage(
                                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupEntryPanel:layers:listContainer:items:3:itemProperties:4:component:checkbox");
            checkBox.setModelObject(false);
            ft = tester.newFormTester("publishedinfo");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("testLgStyles-2");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(1, styles.size());
            LayerGroupStyle theStyle = styles.get(0);
            assertEquals(1, theStyle.getLayers().size());
            assertEquals(1, theStyle.getStyles().size());
            assertEquals("nestedGroupStyle2", theStyle.getStyles().get(0).getName());
        } finally {
            if (nestedGroup != null) {
                nestedGroup.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(nestedGroup);
            }
            if (groupInfo != null) {
                getCatalog().remove(groupInfo);
            }
        }
    }

    @Test
    public void testRemoveStyleBtn() throws Exception {
        // add many LayerGroupStyle form to the UI and then remove  them.
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("nestedLayerGroup");
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("nestedGroupStyle");
        groupStyle.setName(styleName);
        groupStyle.getStyles().add(getCatalog().getStyleByName("BasicPolygons"));
        groupStyle.getLayers().add(getCatalog().getLayerByName("cite:BasicPolygons"));
        groupInfo.getLayerGroupStyles().add(groupStyle);
        getCatalog().save(groupInfo);
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(new PageParameters().add("group", "nestedLayerGroup"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);
            // Click on the link

            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:addNew", "click");
            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:addNew", "click");

            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:2:layerGroupStylePanel:remove",
                    "click");

            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:1:layerGroupStylePanel:remove",
                    "click");
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:remove",
                    "click");

            FormTester ft = tester.newFormTester("publishedinfo");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("nestedLayerGroup");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(0, styles.size());
        } finally {
            if (groupInfo != null) {
                groupInfo.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(groupInfo);
            }
        }
    }

    @Test
    public void testCopyGroupDefaultStyle() throws Exception {
        // tests the copy style UI functionality.
        buildLayerGroup("testLgStylesCopyDef", LayerGroupInfo.Mode.SINGLE);
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testLgStylesCopyDef");
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(
                            new PageParameters().add("group", "testLgStylesCopyDef"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);
            FormTester ft = tester.newFormTester("publishedinfo");
            ft.select("tabs:panel:layerGroupStyles:availableStyles", 0);
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:availableStyles", "change");
            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:copy", "click");
            ft = tester.newFormTester("publishedinfo");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupStyleName",
                    "copiedStyleLg");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("testLgStylesCopyDef");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(1, styles.size());
            LayerGroupStyle groupStyle1 = styles.get(0);
            assertEquals(groupStyle1.getLayers(), groupInfo.getLayers());
            assertEquals(groupStyle1.getStyles(), groupInfo.getStyles());
        } finally {
            if (groupInfo != null) {
                groupInfo.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(groupInfo);
            }
        }
    }

    @Test
    public void testCopyGroupStyle() throws Exception {
        // tests the copy style UI functionality.
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("nestedLayerGroup");
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("nestedGroupStyle");
        groupStyle.setName(styleName);
        groupStyle.getStyles().add(getCatalog().getStyleByName("BasicPolygons"));
        groupStyle.getLayers().add(getCatalog().getLayerByName("cite:BasicPolygons"));
        groupInfo.getLayerGroupStyles().add(groupStyle);
        getCatalog().save(groupInfo);
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(new PageParameters().add("group", "nestedLayerGroup"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);
            FormTester ft = tester.newFormTester("publishedinfo");
            ft.select("tabs:panel:layerGroupStyles:availableStyles", 1);
            tester.executeAjaxEvent(
                    "publishedinfo:tabs:panel:layerGroupStyles:availableStyles", "change");
            tester.executeAjaxEvent("publishedinfo:tabs:panel:layerGroupStyles:copy", "click");
            ft = tester.newFormTester("publishedinfo");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:1:layerGroupStylePanel:layerGroupStyleName",
                    "copiedStyleLg");
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName("nestedLayerGroup");
            List<LayerGroupStyle> styles = groupInfo.getLayerGroupStyles();
            assertEquals(2, styles.size());
            LayerGroupStyle groupStyle1 = styles.get(0);
            LayerGroupStyle groupStyle2 = styles.get(1);
            assertEquals(groupStyle1.getLayers(), groupStyle2.getLayers());
            assertEquals(groupStyle1.getStyles(), groupStyle2.getStyles());
        } finally {
            if (groupInfo != null) {
                groupInfo.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(groupInfo);
            }
        }
    }

    @Test
    public void testLayerGroupStyleVisibility() throws Exception {
        // check that if the mode is one that not support styles the LayerGroupStyle panel is not
        // visible.
        // check also that visibility switch when mode changes.
        buildLayerGroup("testLgStylesNamed", LayerGroupInfo.Mode.NAMED);
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testLgStylesNamed");
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(new PageParameters().add("group", groupInfo.getName()));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);

            assertNull(
                    tester.getComponentFromLastRenderedPage(
                            "publishedinfo:tabs:panel:layerGroupStyles"));

            FormTester ft = tester.newFormTester("publishedinfo");
            // select mode single
            ft.select("tabs:panel:mode", 0);
            tester.executeAjaxEvent("publishedinfo:tabs:panel:mode", "change");

            // now visible
            assertNotNull(
                    tester.getComponentFromLastRenderedPage(
                            "publishedinfo:tabs:panel:layerGroupStyles"));

        } finally {
            if (groupInfo != null) {
                getCatalog().remove(groupInfo);
            }
        }
    }

    private void buildLayerGroup(String groupName) throws Exception {
        buildLayerGroup(groupName, LayerGroupInfo.Mode.SINGLE);
    }

    private void buildLayerGroup(String groupName, LayerGroupInfo.Mode mode) throws Exception {
        Catalog catalog = getCatalog();
        String lakes = MockData.BASIC_POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName(groupName);
        lg.setMode(mode);
        lg.getLayers().add(catalog.getLayerByName(lakes));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    @Test
    public void testLayerGroupStyleEditingIsolation() throws Exception {
        buildLayerGroup("testLgStylesEditing", LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        LayerGroupInfo groupInfo = getCatalog().getLayerGroupByName("testLgStylesEditing");
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo styleName = new StyleInfoImpl(getCatalog());
        styleName.setName("editingGroupStyle");
        groupStyle.setName(styleName);
        groupStyle.getStyles().add(getCatalog().getStyleByName("BasicPolygons"));
        groupStyle.getLayers().add(getCatalog().getLayerByName("cite:BasicPolygons"));
        groupInfo.getLayerGroupStyles().add(groupStyle);
        getCatalog().save(groupInfo);
        try {
            LayerGroupEditPage page =
                    new LayerGroupEditPage(
                            new PageParameters().add("group", "testLgStylesEditing"));
            // Create the new page
            tester.startPage(page);
            tester.assertRenderedPage(LayerGroupEditPage.class);
            // Click on the link
            FormTester ft = tester.newFormTester("publishedinfo");
            ft.setValue(
                    "tabs:panel:layerGroupStyles:listContainer:styleList:0:layerGroupStylePanel:layerGroupStyleName",
                    "changeName");

            groupInfo = getCatalog().getLayerGroupByName(groupInfo.prefixedName());
            String savedName = groupInfo.getLayerGroupStyles().get(0).getName().getName();
            assertNotEquals("changeName", savedName);
            assertEquals("editingGroupStyle", savedName);
            ft.submit("save");
            tester.assertNoErrorMessage();
            groupInfo = getCatalog().getLayerGroupByName(groupInfo.prefixedName());
            savedName = groupInfo.getLayerGroupStyles().get(0).getName().getName();
            assertEquals("changeName", savedName);
        } finally {
            if (groupInfo != null) {
                groupInfo.setLayerGroupStyles(new ArrayList<>());
                getCatalog().save(groupInfo);
            }
        }
    }
}
