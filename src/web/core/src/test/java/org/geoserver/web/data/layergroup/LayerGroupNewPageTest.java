/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.*;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;

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
    
    
}
