/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.web.AbstractConfirmRemovalPanelTest;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class ConfirmRemovalServicePanelTest
        extends AbstractConfirmRemovalPanelTest<ServiceAccessRule> {
    private static final long serialVersionUID = 1L;

    @Test
    public void testRemoveRule() throws Exception {
        initializeForXML();
        removeObject();
    }

    @Override
    protected void setupPanel(final List<ServiceAccessRule> roots) {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {
                                return new ConfirmRemovalServicePanel(id, roots) {
                                    @Override
                                    protected IModel<String> canRemove(ServiceAccessRule data) {
                                        SelectionServiceRemovalLink link =
                                                new SelectionServiceRemovalLink("XXX", null, null);
                                        return link.canRemove(data);
                                    }

                                    private static final long serialVersionUID = 1L;
                                };
                            }
                        }));
    }

    @Override
    protected ServiceAccessRule getRemoveableObject() throws Exception {
        for (ServiceAccessRule rule : ServiceAccessRuleDAO.get().getRules()) {
            if ("wms".equals(rule.getService()) && "GetMap".equals(rule.getMethod())) return rule;
        }
        return null;
    }

    @Override
    protected ServiceAccessRule getProblematicObject() throws Exception {
        return null;
    }

    @Override
    protected String getProblematicObjectRegExp() throws Exception {
        return null;
    }

    @Override
    protected String getRemoveableObjectRegExp() throws Exception {
        ServiceAccessRule rule = getRemoveableObject();
        return ".*"
                + rule.getService()
                + ".*"
                + rule.getMethod()
                + ".*"
                + "ROLE_AUTHENTICATED"
                + ".*";
    }
}
