package org.geoserver.web.security.data;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.GeoServerWicketTestSupport;

public class NewRulePageTest extends GeoServerWicketTestSupport {

    
    private DataAccessRuleDAO dao;
    private NewDataAccessRulePage page;

    @Override
    protected void setUpInternal() throws Exception {
        dao = DataAccessRuleDAO.get();
        login();
        page = new NewDataAccessRulePage();
        tester.startPage(page);
    }
    
    public void testRenders() {
        tester.assertRenderedPage(NewDataAccessRulePage.class);
    }

    public void testFill() {
        Locale.setDefault(Locale.ENGLISH);
        
        // make sure the recorder is where we think it is, it contains the palette selection
        tester.assertComponent("ruleForm:roles:roles:recorder", Recorder.class);
        
        FormTester form = tester.newFormTester("ruleForm");
        form.select("workspace", page.getWorkspaceNames().indexOf(MockData.CITE_PREFIX));
        form.select("accessMode", 1);
        form.setValue("roles:roles:recorder", "*");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        form.submit("save");
        
        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(DataAccessRulePage.class);
        
        dao.reload();
        assertEquals(1, dao.getRules().size());
        DataAccessRule rule = dao.getRules().get(0);
        assertEquals("cite", rule.getWorkspace());
        assertEquals("*", rule.getLayer());
        assertEquals(AccessMode.WRITE, rule.getAccessMode());
//        // palette and form submit in tester, just does not work... :-(
//        assertEquals(0, rule.getRoles());
//        assertEquals("*", rule.getRoles().iterator().next());
    }
    
    public void testAjaxUpdate() {
        FormTester form = tester.newFormTester("ruleForm");
        form.select("workspace", page.getWorkspaceNames().indexOf(MockData.CITE_PREFIX));
        List<String> layers = page.getLayerNames(MockData.CITE_PREFIX);
        
        tester.executeAjaxEvent("ruleForm:workspace", "onchange");
        assertEquals(layers, ((DropDownChoice) tester.getComponentFromLastRenderedPage("ruleForm:layer")).getChoices());
    }

}

