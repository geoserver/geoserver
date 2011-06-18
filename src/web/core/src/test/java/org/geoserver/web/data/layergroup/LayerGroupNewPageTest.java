package org.geoserver.web.data.layergroup;

import org.apache.wicket.util.tester.FormTester;

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
        form.submit();
        
        // should not work, duplicate provided, so we remain
        // in the same page
        tester.assertRenderedPage(LayerGroupNewPage.class);
        // System.out.println(tester.getMessages(FeedbackMessage.ERROR));
        tester.assertErrorMessages(new String[] {"A layer group named lakes already exists", "Field 'Bounds' is required."});
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
