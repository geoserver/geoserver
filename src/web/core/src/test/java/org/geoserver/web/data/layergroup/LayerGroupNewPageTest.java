/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class LayerGroupNewPageTest extends LayerGroupBaseTest {

    @Before
    public void doLogin() {
        login();
    }

    @Test
    public void testMissingName() {
        LayerGroupNewPage page = new LayerGroupNewPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        FormTester form = tester.newFormTester("form");
        form.submit();
        
        // should not work, no name provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required.", "Field 'Bounds' is required."});
    }
    
    @Test
    public void testMissingCRS() {
        LayerGroupNewPage page = new LayerGroupNewPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "lakes");
        form.setValue("bounds:minX", "-180");
        form.setValue("bounds:minY", "-90");
        form.setValue("bounds:maxX", "180");
        form.setValue("bounds:maxY", "90");

        page.lgEntryPanel.getEntries().add(
            new LayerGroupEntry(getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        form.submit("save");

        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupNewPage.class);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message = tester.getMessages(FeedbackMessage.ERROR).get(0).toString();
        assertTrue(message.contains("Bounds"));
    }
    
    @Test
    public void testDuplicateName() {
        LayerGroupNewPage page = new LayerGroupNewPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "lakes");
        form.setValue("bounds:minX", "0");
        form.setValue("bounds:minY", "0");
        form.setValue("bounds:maxX", "0");
        form.setValue("bounds:maxY", "0");
        form.setValue("bounds:crsContainer:crs:srs", "EPSG:4326");

        page.lgEntryPanel.getEntries().add(
            new LayerGroupEntry(getCatalog().getLayerByName(getLayerId(MockData.LAKES)), null));
        form.submit("save");

        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupNewPage.class);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        assertTrue(tester.getMessages(FeedbackMessage.ERROR).get(0).toString()
            .endsWith("Layer group named 'lakes' already exists"));
    }
    
    @Test
    public void testNewName() {
        LayerGroupNewPage page = new LayerGroupNewPage();
        // print(page, false, false);
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "newGroup");
        form.submit();
        
        // should work, we switch to the edit page
        tester.assertRenderedPage(LayerGroupNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Bounds' is required."});
    }
    
    @Test
    public void testLayerLink() {
        
        LayerGroupNewPage page = new LayerGroupNewPage();
        // Create the new page
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        // Click on the link
        tester.clickLink("form:layers:addLayer");
        tester.assertNoErrorMessage();
        // Ensure that the Layer List page is rendered correctly
        tester.assertComponent("form:layers:popup:content:listContainer:items", DataView.class);
        // Get the DataView containing the Layer List
        DataView dataView = (DataView) page.lgEntryPanel.get("popup:content:listContainer:items");
        // Ensure that the Row count is equal to the Layers in the Catalog
        Catalog catalog = getGeoServerApplication().getCatalog();
        
        int layerCount = catalog.count(LayerInfo.class, Filter.INCLUDE);
        int rowCount = dataView.getRowCount();
        
        assertEquals(layerCount, rowCount);
    }
}
