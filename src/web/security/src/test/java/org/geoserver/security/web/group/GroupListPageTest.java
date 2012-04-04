package org.geoserver.security.web.group;


import java.lang.reflect.Method;
import java.util.SortedSet;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractTabbedListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class GroupListPageTest extends AbstractTabbedListPageTest<GeoServerUserGroup> {
    protected boolean withRoles=false;
        
    protected AbstractSecurityPage listPage(String serviceName ) {
                
        AbstractSecurityPage result = initializeForUGServiceNamed(serviceName); 
        tester.clickLink(getTabbedPanelPath()+":tabs-container:tabs:2:link", true);
        return result;
    }
    protected AbstractSecurityPage newPage(AbstractSecurityPage page,Object...params) {
        if (params.length==0)
            return new  NewGroupPage(getUserGroupServiceName()).setReturnPage(page);
        else
            return new  NewGroupPage((String) params[0]).setReturnPage(page);
    }
    protected AbstractSecurityPage editPage(AbstractSecurityPage page,Object...params) {
        if (params.length==0) {
            return new  EditGroupPage(
                    getUserGroupServiceName(),
                    new GeoServerUserGroup("dummygroup")).setReturnPage(page);
        }
        if (params.length==1)
            return new  EditGroupPage(
                    getUserGroupServiceName(),
                    (GeoServerUserGroup) params[0]).setReturnPage(page);
        else
            return new  EditGroupPage( (String) params[0],
                    (GeoServerUserGroup) params[1]).setReturnPage(page);
    }


    @Override
    protected String getSearchString() throws Exception{
         GeoServerUserGroup g = ugService.getGroupByGroupname("admins");
         assertNotNull(g);
         return g.getGroupname();
    }


    @Override
    protected Property<GeoServerUserGroup> getEditProperty() {
        return GroupListProvider.GROUPNAME;
    }


    @Override
    protected boolean checkEditForm(String objectString) {
        return objectString.equals( 
                tester.getComponentFromLastRenderedPage("form:groupname").getDefaultModelObject());
    }
    
    public void testReadOnlyService() throws Exception {
        initializeForXML();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertVisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());
        
        activateRORoleService();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());
        
        activateROUGService();
        tester.startPage(listPage(getROUserGroupServiceName()));
        tester.assertInvisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getAddLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
    }
    
    @Override
    protected void simulateDeleteSubmit() throws Exception {
        SelectionGroupRemovalLink link =   
                (SelectionGroupRemovalLink) (withRoles ?  getRemoveLinkWithRoles() : getRemoveLink());
        Method m = link.delegate.getClass().getDeclaredMethod("onSubmit", AjaxRequestTarget.class,Component.class);
        m.invoke(link.delegate, null,null);
        
        SortedSet<GeoServerUserGroup> groups = ugService.getUserGroups();
        assertTrue(groups.size()==0);
        
        if (withRoles)
            assertTrue(gaService.getRolesForGroup("group1").size()==0);
        else    
            assertTrue(gaService.getRolesForGroup("group1").size()==2);
    }

    public void testRemoveWithRoles() throws Exception {
        withRoles=true;
        initializeForXML();
        insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath()+":panel:header:removeSelectedWithRoles");
    }
    @Override
    protected String getTabbedPanelPath() {
        return "panel:panel";
    }
    @Override
    protected String getServiceName() {
        return getUserGroupServiceName();
    }
    
}
