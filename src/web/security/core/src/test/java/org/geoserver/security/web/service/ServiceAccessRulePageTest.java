/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.web.AbstractListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class ServiceAccessRulePageTest extends AbstractListPageTest<ServiceAccessRule> {

    protected Page listPage(PageParameters params) {
        return new ServiceAccessRulePage();
    }

    protected Page newPage(Object... params) {
        return new NewServiceAccessRulePage();
    }

    protected Page editPage(Object... params) {
        if (params.length == 0) {
            return new EditServiceAccessRulePage(new ServiceAccessRule());
        } else return new EditServiceAccessRulePage((ServiceAccessRule) params[0]);
    }

    @Override
    protected Property<ServiceAccessRule> getEditProperty() {
        return ServiceAccessRuleProvider.RULEKEY;
    }

    @Override
    protected boolean checkEditForm(String objectString) {
        String[] array = objectString.split("\\.");
        return array[0].equals(
                        tester.getComponentFromLastRenderedPage("form:service")
                                .getDefaultModelObject())
                && array[1].equals(
                        tester.getComponentFromLastRenderedPage("form:method")
                                .getDefaultModelObject());
    }

    @Override
    protected String getSearchString() throws Exception {
        for (ServiceAccessRule rule : ServiceAccessRuleDAO.get().getRules()) {
            if ("wms".equals(rule.getService()) && "GetMap".equals(rule.getMethod()))
                return rule.getKey();
        }
        return null;
    }

    @Override
    protected void simulateDeleteSubmit() throws Exception {

        assertTrue(ServiceAccessRuleDAO.get().getRules().size() > 0);

        SelectionServiceRemovalLink link = (SelectionServiceRemovalLink) getRemoveLink();
        Method m =
                link.delegate
                        .getClass()
                        .getDeclaredMethod("onSubmit", AjaxRequestTarget.class, Component.class);
        m.invoke(link.delegate, null, null);

        // TODO, GEOS-5353, Intermittent build failure in ServiceAccessRulePageTest
        // assertEquals(0,ServiceAccessRuleDAO.get().getRules().size());

    }
}
