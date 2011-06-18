package org.geoserver.web.security.service;

import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.GeoServerWicketTestSupport;

public class ServiceAccessRulePageTest extends GeoServerWicketTestSupport {

    private ServiceAccessRuleDAO dao;

    private ServiceAccessRule rule;

    @Override
    protected void setUpInternal() throws Exception {
        dao = ServiceAccessRuleDAO.get();
        dao.getRules();
        rule = new ServiceAccessRule("wms", "GetMap", "ROLE_ADMINISTRATOR");
        dao.addRule(rule);
        login();
        tester.startPage(ServiceAccessRulePage.class);
    }

    public void testRenders() throws Exception {
        tester.assertRenderedPage(ServiceAccessRulePage.class);
    }
    
    public void testEditRule() throws Exception {
        tester.clickLink("table:listContainer:items:1:itemProperties:0:component:link");
        tester.assertRenderedPage(EditServiceAccessRulePage.class);
        assertEquals("GetMap", tester.getComponentFromLastRenderedPage("ruleForm:method")
                .getDefaultModelObject());
    }
    
//    public void testNewRule() throws Exception {
//        tester.clickLink("addRule");
//        tester.assertRenderedPage(NewServiceAccessRulePage.class);
//        assertEquals("*", tester.getComponentFromLastRenderedPage("ruleForm:service")
//                .getModelObject());
//        assertEquals("*", tester.getComponentFromLastRenderedPage("ruleForm:method")
//                .getModelObject());
//    }

}
