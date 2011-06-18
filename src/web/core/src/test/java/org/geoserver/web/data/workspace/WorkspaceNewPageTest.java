package org.geoserver.web.data.workspace;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;

public class WorkspaceNewPageTest extends GeoServerWicketTestSupport {
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
        tester.startPage(WorkspaceNewPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:uri", TextField.class);
    }
    
    public void testNameRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "http://www.geoserver.org");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }
    
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "test");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }
    
    public void testValid() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "abc");
        form.setValue("uri", "http://www.geoserver.org");
        form.setValue("default", "true");
        form.submit();
    
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        
        assertEquals("abc", getCatalog().getDefaultWorkspace().getName());
    }
    
    public void testInvalidURI()  {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "def");
        form.setValue("uri", "not a valid uri");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Invalid URI syntax: not a valid uri"});
    }
}
