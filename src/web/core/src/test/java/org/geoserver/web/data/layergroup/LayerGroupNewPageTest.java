package org.geoserver.web.data.layergroup;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;

public class LayerGroupNewPageTest extends LayerGroupBaseTest {
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
    }

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
