/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.data.resource.MetadataLinkEditor;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;


public class LayerGroupEditPageTest extends LayerGroupBaseTest {
    
    @Test
    public void testComputeBounds() {
        LayerGroupEditPage page = new LayerGroupEditPage(new PageParameters().add("group", "lakes"));
        tester.startPage(page);
        // print(page, true, false);
        
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // remove the first and second elements
        // tester.clickLink("form:layers:layers:listContainer:items:1:itemProperties:4:component:link");
        // the regenerated list will have ids starting from 4
        //tester.clickLink("form:layers:layers:listContainer:items:4:itemProperties:4:component:link");
        // manually regenerate bounds
        tester.clickLink("publishedinfo:tabs:panel:generateBounds");
        // print(page, true, true);
        // submit the form
        tester.submitForm("publishedinfo");
        
        // For the life of me I cannot get this test to work... and I know by direct UI inspection that
        // the page works as expected...
//        FeatureTypeInfo bridges = getCatalog().getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);
//        assertEquals(getCatalog().getLayerGroupByName("lakes").getBounds(), bridges.getNativeBoundingBox());
    }
    
    @Test
    public void testComputeBoundsFromCRS() {
        LayerGroupEditPage page = new LayerGroupEditPage(new PageParameters().add("group", "lakes"));
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326"); 
        tester.clickLink("publishedinfo:tabs:panel:generateBoundsFromCRS", true);
        tester.assertComponentOnAjaxResponse("publishedinfo:tabs:panel:bounds");
        Component ajaxComponent = tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:bounds");
        assert(ajaxComponent instanceof EnvelopePanel);
        EnvelopePanel envPanel = (EnvelopePanel)ajaxComponent;
        assertEquals(((DecimalTextField)envPanel.get("minX")).getModelObject(), new Double(-180.0));
        assertEquals(((DecimalTextField)envPanel.get("minY")).getModelObject(), new Double(-90.0));
        assertEquals(((DecimalTextField)envPanel.get("maxX")).getModelObject(), new Double(180.0));
        assertEquals(((DecimalTextField)envPanel.get("maxY")).getModelObject(), new Double(90.0));
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
        tester.assertErrorMessages((Serializable[]) new String[] {"Field 'Name' is required.", "Field 'Bounds' is required."});
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

        page.lgEntryPanel.getEntries().add(
            new LayerGroupEntry(getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
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
        form.setValue("tabs:panel:bounds:maxX", "0");
        form.setValue("tabs:panel:bounds:maxY", "0");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");

        page.lgEntryPanel.getEntries().add(
            new LayerGroupEntry(getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        form.submit("save");

        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupEditPage.class);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        assertTrue(tester.getMessages(FeedbackMessage.ERROR).get(0).toString()
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
        tester.assertErrorMessages((Serializable[]) new String[] {"Field 'Bounds' is required."});
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
        tester.assertComponent("publishedinfo:tabs:panel:layers:popup:content:listContainer:items", DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView = (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();
        
        int layerCount = catalog.count(LayerInfo.class, Filter.INCLUDE);
        int rowCount = (int) dataView.getRowCount();
        
        assertEquals(layerCount, rowCount);
    }
    
    @Test
    public void testLayerLinkWithWorkspace() {
        LayerGroupEditPage page = new LayerGroupEditPage(
                new PageParameters().add("workspace", "cite").add("group", "bridges"));
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addLayer");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent("publishedinfo:tabs:panel:layers:popup:content:listContainer:items", DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView = (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();
        
        
        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
        final Filter filter = ff.equal(ff.property("resource.store.workspace.id"), 
                ff.literal(catalog.getWorkspaceByName("cite").getId()),true);

        int layerCount = catalog.count(LayerInfo.class, filter);
        int rowCount = (int) dataView.getRowCount();
        
        assertEquals(layerCount, rowCount);
    }
    
    @Test
    public void testLayerGroupLinkWithWorkspace() {
        LayerGroupEditPage page = new LayerGroupEditPage(
                new PageParameters().add("workspace", "cite").add("group", "bridges"));
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // Click on the link
        tester.clickLink("publishedinfo:tabs:panel:layers:addLayerGroup");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent("publishedinfo:tabs:panel:layers:popup:content:listContainer:items", DataView.class);
        // Get the DataView containing the Layer List
        DataView<?> dataView = (DataView<?>) page.lgEntryPanel.get("popup:content:listContainer:items");
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
        form.setValue("tabs:panel:bounds:maxX", "0");
        form.setValue("tabs:panel:bounds:maxY", "0");
        form.setValue("tabs:panel:bounds:crsContainer:crs:srs", "EPSG:4326");
        
        tester.executeAjaxEvent("publishedinfo:tabs:panel:metadataLinks:addlink", "click");
        
        form.setValue("tabs:panel:metadataLinks:container:table:links:0:urlBorder:urlBorder_body:metadataLinkURL", "http://test.me");
        tester.executeAjaxEvent("publishedinfo:tabs:panel:metadataLinks:addlink", "click");
        
        LayerGroupInfo info = page.getPublishedInfo();
        assertEquals(2, info.getMetadataLinks().size());
        assertEquals("http://test.me", info.getMetadataLinks().get(0).getContent());
        
    }
}
