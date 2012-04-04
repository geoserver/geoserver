package org.geoserver.security.web.group;

import java.util.SortedSet;

import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.role.NewRolePage;

public class NewGroupPageTest extends AbstractSecurityWicketTestSupport {

    NewGroupPage page;
    
    
    public void testFill() throws Exception{
        initializeForXML();
        doTestFill();
    }

    protected void doTestFill() throws Exception {
        insertValues();        
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(page=(NewGroupPage) 
                new NewGroupPage(getUserGroupServiceName()).setReturnPage(returnPage));
        tester.assertRenderedPage(NewGroupPage.class);
        
        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "testgroup");
        
        assertTrue(((GeoServerUserGroup)page.get("form").getDefaultModelObject()).isEnabled());
        form.setValue("enabled", false);

        
        assertTrue(page.rolePalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

                
        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form=tester.newFormTester("form");                
        form.setValue("name", "ROLE_NEW");
        form.submit("save");
        
        
        // assign the new role to the new group
        form=tester.newFormTester("form");
        tester.assertRenderedPage(NewGroupPage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());
        
        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewGroupPage.class);
        
        // now save
        form=tester.newFormTester("form");
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        tester.assertErrorMessages(new String[0]);
        
        GeoServerUserGroup group = ugService.getGroupByGroupname("testgroup");
        assertNotNull(group);
        assertFalse(group.isEnabled());
        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("testgroup");
        assertEquals(1,roleList.size());
        assertEquals("ROLE_NEW",roleList.iterator().next().getAuthority());
        
    }
    
    public void testGroupNameConflict() throws Exception {
        initializeForXML();
        insertValues();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(page=(NewGroupPage) 
            new NewGroupPage(getUserGroupServiceName()).setReturnPage(returnPage));
        
        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "group1");
        form.submit("save");
        
        assertTrue(testErrorMessagesWithRegExp(".*group1.*"));
        tester.getMessages(FeedbackMessage.ERROR);
        tester.assertRenderedPage(NewGroupPage.class);
    }

    public void testInvalidWorkflow() throws Exception{
        initializeForXML();
        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        boolean fail = true;
        try {
            tester.startPage(page=(NewGroupPage) 
                new NewGroupPage(getROUserGroupServiceName()).setReturnPage(returnPage));
        } catch (RuntimeException ex) {
            fail = false;
        }
        if (fail)
            fail("No runtime exception for read only UserGroupService");
    }
    
    public void testReadOnlyRoleService() throws Exception{
        initializeForXML();
        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(page=(NewGroupPage) 
            new NewGroupPage(getUserGroupServiceName()).setReturnPage(returnPage));
        assertFalse(page.rolePalette.isEnabled());
        
        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "testgroup");
        form.submit("save");
        
        GeoServerUserGroup group = ugService.getGroupByGroupname("testgroup");
        assertNotNull(group);
        assertTrue(group.isEnabled());
        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("testgroup");
        assertEquals(0,roleList.size());
    }

   
}
