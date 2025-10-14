/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.web.AbstractListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Test;

public class DataSecurityPageTest extends AbstractListPageTest<DataAccessRule> {

    @Override
    protected Page listPage(PageParameters params) {
        return new DataSecurityPage();
    }

    @Override
    protected Page newPage(Object... params) {
        return new NewDataAccessRulePage();
    }

    @Override
    protected Page editPage(Object... params) {
        if (params.length == 0)
            return new EditDataAccessRulePage(new DataAccessRule(
                    "it.geosolutions", "layer.dots", AccessMode.READ, Collections.singleton("ROLE_ABC")));
        else return new EditDataAccessRulePage((DataAccessRule) params[0]);
    }

    @Override
    protected Property<DataAccessRule> getEditProperty() {
        return DataAccessRuleProvider.RULEKEY;
    }

    @Override
    protected boolean checkEditForm(String objectString) {
        String[] array = objectString.split("\\.");
        return array[0].equals(
                        tester.getComponentFromLastRenderedPage("form:root").getDefaultModelObject())
                && array[1].equals(tester.getComponentFromLastRenderedPage("form:layerContainer:layerAndLabel:layer")
                        .getDefaultModelObject());
    }

    @Override
    protected String getSearchString() throws Exception {
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (MockData.CITE_PREFIX.equals(rule.getRoot())
                    && MockData.BRIDGES.getLocalPart().equals(rule.getLayer())) return rule.getKey();
        }
        return null;
    }

    @Override
    protected void simulateDeleteSubmit() throws Exception {

        DataAccessRuleDAO.get().reload();
        assertFalse(DataAccessRuleDAO.get().getRules().isEmpty());

        SelectionDataRuleRemovalLink link = (SelectionDataRuleRemovalLink) getRemoveLink();
        Method m = link.delegate.getClass().getDeclaredMethod("onSubmit", AjaxRequestTarget.class, Component.class);
        m.invoke(link.delegate, null, null);

        DataAccessRuleDAO.get().reload();
        // if there are no rules, DataAccessRuleDAO.loadRules adds two basic rules
        assertEquals(2, DataAccessRuleDAO.get().getRules().size());
    }

    @Test
    public void testDefaultCatalogMode() throws Exception {
        tester.startPage(DataSecurityPage.class);
        tester.assertRenderedPage(DataSecurityPage.class);
        assertEquals(
                "HIDE",
                tester.getComponentFromLastRenderedPage("otherSettingsForm:catalogMode")
                        .getDefaultModelObject()
                        .toString());
    }

    @Test
    public void testEditCatalogMode() throws Exception {
        tester.startPage(DataSecurityPage.class);
        tester.assertRenderedPage(DataSecurityPage.class);

        // simple test
        assertNotEquals(
                "CHALLENGE",
                tester.getComponentFromLastRenderedPage("otherSettingsForm:catalogMode")
                        .getDefaultModelObject());

        // edit catalogMode value
        final FormTester form = tester.newFormTester("otherSettingsForm");

        form.select("catalogMode", 1);

        assertEquals(
                "MIXED",
                tester.getComponentFromLastRenderedPage("otherSettingsForm:catalogMode")
                        .getDefaultModelObject()
                        .toString());
    }

    @Test
    public void testSandbox() throws Exception {
        // setup a sandbox
        File sandbox = new File("./target/sandbox").getCanonicalFile();
        sandbox.mkdirs();
        File notThere = new File("./target/notThere").getCanonicalFile();

        tester.startPage(DataSecurityPage.class);
        tester.assertRenderedPage(DataSecurityPage.class);

        // test non existing sandbox
        FormTester form = tester.newFormTester("otherSettingsForm");
        form.setValue("sandboxContainer:sandbox:border:border_body:paramValue", notThere.getAbsolutePath());
        form.submit("save");
        tester.assertErrorMessages("The sandbox directory does not exist");
        tester.clearFeedbackMessages();

        // test existing sandbox
        form = tester.newFormTester("otherSettingsForm");
        form.setValue("sandboxContainer:sandbox:border:border_body:paramValue", sandbox.getAbsolutePath());
        form.submit("save");
        tester.assertNoErrorMessage();

        DataAccessRuleDAO dao = GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        assertEquals(sandbox.getAbsolutePath().replace("\\", "/"), dao.getFilesystemSandbox());
    }
}
