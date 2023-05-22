/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.url;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.Icon;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLChecksPageTest extends GeoServerWicketTestSupport {

    private static final RegexURLCheck TEST_RULE =
            new RegexURLCheck("test", "just a test", "^http://geoserver.org/.*$");
    private static final String ALL_GEOTOOLS_REGEX = "^http://geotools.org/.*$";

    @BeforeClass
    public static void setLanguage() {
        // for error message tests
        Locale.setDefault(Locale.ENGLISH);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed, faster execution
    }

    @Before
    public void setup() throws IOException {
        login();
        URLCheckDAO dao = getDao();
        dao.saveChecks(Collections.emptyList());
    }

    private URLCheckDAO getDao() {
        URLCheckDAO dao = getGeoServerApplication().getBeanOfType(URLCheckDAO.class);
        return dao;
    }

    @Test
    public void testEmptyPage() throws Exception {
        tester.startPage(URLChecksPage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(URLChecksPage.class);
    }

    private void startWithOneRule() {
        URLCheckDAO dao = getDao();
        dao.save(TEST_RULE);

        tester.startPage(URLChecksPage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(URLChecksPage.class);
    }

    @Test
    public void testRenderOneRule() throws Exception {
        startWithOneRule();

        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:0:component:link:label",
                TEST_RULE.getName());
        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:1:component",
                TEST_RULE.getDescription());
        tester.assertLabel(
                "table:listContainer:items:1:itemProperties:2:component", TEST_RULE.getRegex());
        tester.assertComponent(
                "table:listContainer:items:1:itemProperties:3:component", Icon.class);
    }

    @Test
    public void testDeleteRule() throws Exception {
        startWithOneRule();

        // select the rule
        CheckBox selection =
                (CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "table:listContainer:items:1:selectItemContainer:selectItem");
        selection.setModelObject(true);

        // click remove link
        tester.clickLink("removeSelected", true);
        // confirm on dialog
        tester.clickLink("dialog:dialog:content:form:submit");

        assertThat(getDao().getChecks(), empty());
    }

    @Test
    public void testAddNewRuleWorkflow() throws Exception {
        // start empty
        tester.startPage(URLChecksPage.class);

        // click new rule link
        tester.clickLink("addNew", true);

        // confirm we switched onto the single rule page
        tester.assertRenderedPage(RegexCheckPage.class);

        // fill in the form
        FormTester form = tester.newFormTester("form");
        form.setValue("name", TEST_RULE.getName());
        form.setValue("description", TEST_RULE.getDescription());
        form.setValue("regex", TEST_RULE.getRegex());
        form.setValue("enabled", TEST_RULE.isEnabled());
        form.submit("submit");
        tester.assertNoErrorMessage();

        // confirm we switched back to the list page
        tester.assertRenderedPage(URLChecksPage.class);

        // the rule has been added
        assertEquals(Arrays.asList(TEST_RULE), getDao().getChecks());
    }

    @Test
    public void testEditRuleWorkflow() throws Exception {
        startWithOneRule();

        // click on the rule edit link
        tester.clickLink("table:listContainer:items:1:itemProperties:0:component:link");

        // confirm we switched onto the single rule page
        tester.assertRenderedPage(RegexCheckPage.class);

        // check it's filled with the expected contents
        tester.assertModelValue("form:name", TEST_RULE.getName());
        tester.assertModelValue("form:description", TEST_RULE.getDescription());
        tester.assertModelValue("form:regex", TEST_RULE.getRegex());
        tester.assertModelValue("form:enabled", TEST_RULE.isEnabled());

        // update values in the form
        FormTester form = tester.newFormTester("form");
        form.setValue("regex", ALL_GEOTOOLS_REGEX);
        form.submit("submit");
        tester.assertNoErrorMessage();

        // confirm we switched back to the list page
        tester.assertRenderedPage(URLChecksPage.class);

        /* the rule has been saved */
        List<AbstractURLCheck> checks = getDao().getChecks();
        assertEquals(1, checks.size());
        RegexURLCheck savedCheck = (RegexURLCheck) checks.get(0);
        assertEquals(TEST_RULE.getName(), savedCheck.getName());
        assertEquals(TEST_RULE.getDescription(), savedCheck.getDescription());
        assertEquals(ALL_GEOTOOLS_REGEX, savedCheck.getRegex());
        assertEquals(TEST_RULE.isEnabled(), savedCheck.isEnabled());
    }

    @Test
    public void testEditCancelWorkflow() throws Exception {
        startWithOneRule();

        // click on the rule edit link
        tester.clickLink("table:listContainer:items:1:itemProperties:0:component:link");

        // confirm we switched onto the single rule page
        tester.assertRenderedPage(RegexCheckPage.class);

        // check it's filled with the expected contents
        tester.assertModelValue("form:name", TEST_RULE.getName());
        tester.assertModelValue("form:description", TEST_RULE.getDescription());
        tester.assertModelValue("form:regex", TEST_RULE.getRegex());
        tester.assertModelValue("form:enabled", TEST_RULE.isEnabled());

        // update values in the form
        FormTester form = tester.newFormTester("form");
        form.setValue("regex", ALL_GEOTOOLS_REGEX);
        form.submit(); // should not make the page switch
        tester.assertNoErrorMessage();

        // now cancel the edit
        tester.clickLink("form:cancel");

        // confirm we switched back to the list page
        tester.assertRenderedPage(URLChecksPage.class);

        // the rule has _not_ been saved
        List<AbstractURLCheck> checks = getDao().getChecks();
        assertEquals(1, checks.size());
        RegexURLCheck savedCheck = (RegexURLCheck) checks.get(0);
        assertEquals(TEST_RULE.getName(), savedCheck.getName());
        assertEquals(TEST_RULE.getDescription(), savedCheck.getDescription());
        assertEquals(TEST_RULE.getRegex(), savedCheck.getRegex());
        assertEquals(TEST_RULE.isEnabled(), savedCheck.isEnabled());
    }
}
