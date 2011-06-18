package org.geoserver.web.security.catalog;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerWicketTestSupport;

public class CatalogModePageTest extends GeoServerWicketTestSupport {

    private DataAccessRuleDAO dao;

    private CatalogMode catalogMode;

    @Override
    protected void setUpInternal() throws Exception {
        dao = DataAccessRuleDAO.get();
        catalogMode = dao.getMode();
        login();
        tester.startPage(CatalogModePage.class);
    }

    public void testRenders() throws Exception {
        tester.assertRenderedPage(CatalogModePage.class);
    }

    public void testDefaultCatalogMode() throws Exception {
        tester.assertRenderedPage(CatalogModePage.class);
        assertEquals("HIDE", tester.getComponentFromLastRenderedPage("catalogModeForm:catalogMode")
                .getDefaultModelObject().toString());
    }

    public void testEditCatalogMode() throws Exception {
        tester.assertRenderedPage(CatalogModePage.class);
        
        // simple test 
        assertFalse(("CHALLENGE".equals(tester.getComponentFromLastRenderedPage(
                "catalogModeForm:catalogMode").getDefaultModelObject())));
        
        // edit catalogMode value
        final FormTester form = tester.newFormTester("catalogModeForm");

        form.select("catalogMode", 1);

        form.getForm().visitChildren(RadioChoice.class, new IVisitor() {
            public Object component(final Component component) {
                if (component.getId().equals("catalogMode")) {
                    ((RadioChoice) component).onSelectionChanged();
                }
                return CONTINUE_TRAVERSAL;
            }
        });

        assertEquals("MIXED", tester.getComponentFromLastRenderedPage(
                "catalogModeForm:catalogMode").getDefaultModelObject().toString());

    }

}
