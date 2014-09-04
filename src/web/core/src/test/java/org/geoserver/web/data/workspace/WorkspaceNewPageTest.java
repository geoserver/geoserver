/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.junit.Assert.*;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceNewPageTest extends GeoServerWicketTestSupport {
    
    @Before
    public void init() {
        login();
        tester.startPage(WorkspaceNewPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:uri", TextField.class);
    }
    
    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "http://www.geoserver.org");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }
    
    @Test
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "test");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }
    
    @Test
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
    
    @Test
    public void testInvalidURI()  {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "def");
        form.setValue("uri", "not a valid uri");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Invalid URI syntax: not a valid uri"});
    }
    
    @Test
    public void testInvalidName()  {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "default");
        form.setValue("uri", "http://www.geoserver.org");
        form.submit();
        
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Invalid workspace name: \"default\" is a reserved keyword"});
    }
}
