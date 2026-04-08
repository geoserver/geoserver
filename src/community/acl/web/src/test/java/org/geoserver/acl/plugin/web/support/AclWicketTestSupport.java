/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.support;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.After;
import org.junit.Before;

public abstract class AclWicketTestSupport extends GeoServerWicketTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    @Before
    public void beforeEach() throws IOException {
        login();
    }

    @After
    public void clearRules() {
        AdminRuleAdminService adminService = adminService();
        adminService.getAll().map(AdminRule::getId).forEach(adminService::delete);
    }

    protected AdminRuleAdminService adminService() {
        return ApplicationContextSupport.getBeanOfType(AdminRuleAdminService.class);
    }

    protected RuleAdminService ruleService() {
        return ApplicationContextSupport.getBeanOfType(RuleAdminService.class);
    }

    protected FormPage createFormPage(Component c) {
        return new FormPage(c);
    }

    protected FormTester createFormTester(FormPage page) {
        return tester.newFormTester(page.getPathToForm());
    }

    @SuppressWarnings("serial")
    protected static class FormPage extends WebPage implements IMarkupResourceStreamProvider {

        private final Form<Void> form;
        private final Component c;

        private FormPage(final Component c) {
            this.c = c;
            add(form = new Form<>("form"));
            form.add(c);
        }

        public String getPathToForm() {
            return form.getPageRelativePath();
        }

        @Override
        public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass) {
            return new StringResourceStream("<html><body>"
                    + "<form wicket:id='"
                    + form.getId()
                    + "'><span wicket:id='"
                    + c.getId()
                    + "'/></form>"
                    + "</body></html>");
        }
    }
}
