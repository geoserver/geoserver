package org.geoserver.security.web.role;

import java.util.Iterator;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;

public class NewRolePageTest extends AbstractSecurityWicketTestSupport {

    NewRolePage page;
     

    public void testFill() throws Exception{
        initializeForXML();
        doTestFill();
    }
    

    protected void doTestFill() throws Exception {
        
        insertValues();        
        
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
                        
        tester.startPage(page=(NewRolePage) 
            new NewRolePage(getRoleServiceName()).setReturnPage(returnPage));
        
        tester.assertRenderedPage(NewRolePage.class);
        
        
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "ROLE_TEST");
        
        int index =-1;
        for (String name : ((DropDownChoice<String>)page.get("form:parent")).getChoices()) {
            index++;
            if ("ROLE_AUTHENTICATED".equals(name))
                break;
        }
        assertTrue (index >=0);
        form.select("parent", index);
        
        
        //tester.executeAjaxEvent("form:properties:add", "onclick");
        //form = tester.newFormTester("form");
        //print(tester.getLastRenderedPage(),true,true);
        
        //form.setValue("properties:container:list:0:key", "bbox");
        //form.setValue("properties:container:list:0:value", "10 10 20 20");
                
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.assertErrorMessages(new String[0]);
        
        GeoServerRole role = gaService.getRoleByName("ROLE_TEST");
        assertNotNull(role);
        //assertEquals(1,role.getProperties().size());
        //assertEquals("10 10 20 20",role.getProperties().get("bbox"));
        GeoServerRole parentRole = gaService.getParentRole(role);
        assertNotNull(parentRole);
        assertEquals("ROLE_AUTHENTICATED",parentRole.getAuthority());
        
    }
    
    public void testRoleNameConflict() throws Exception {
        initializeForXML();
        insertValues();        
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
        tester.startPage(page=(NewRolePage) 
            new NewRolePage(getRoleServiceName()).setReturnPage(returnPage));
        
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "ROLE_WFS");
        form.submit("save");
        
        assertTrue(testErrorMessagesWithRegExp(".*ROLE_WFS.*"));
        tester.getMessages(FeedbackMessage.ERROR);
        tester.assertRenderedPage(NewRolePage.class);
    }

    public void testInvalidWorkflow() throws Exception{
        initializeForXML();
        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRORoleServiceName());
        boolean fail = true;
        try {
            tester.startPage(page=(NewRolePage) 
                new NewRolePage(getRORoleServiceName()).setReturnPage(returnPage));
        } catch (RuntimeException ex) {
            fail = false;
        }
        if (fail)
            fail("No runtime exception for read only RoleService");
    }

}
