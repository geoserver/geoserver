/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegexCheckPageTest extends GeoServerWicketTestSupport {

    private static final String NAME = "tester";
    private static final String DESCRIPTION = "description";
    private static final String REGEX = ".*";

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
        URLCheckDAO dao = getGeoServerApplication().getBeanOfType(URLCheckDAO.class);
        dao.saveChecks(Collections.emptyList());
    }

    private static void startEmptyPage() {
        tester.startPage(RegexCheckPage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(RegexCheckPage.class);
    }

    @Test
    public void testNewRule() throws Exception {
        fillNewRule(REGEX);
        tester.assertNoErrorMessage();

        URLCheckDAO dao = getGeoServerApplication().getBeanOfType(URLCheckDAO.class);
        List<AbstractURLCheck> checks = dao.getChecks();
        assertEquals(1, checks.size());
        RegexURLCheck check = (RegexURLCheck) checks.get(0);

        assertEquals(NAME, check.getName());
        assertEquals(DESCRIPTION, check.getDescription());
        assertEquals(REGEX, check.getRegex());
        assertTrue(check.isEnabled());
    }

    @Test
    public void testDefaultRegex() throws Exception {
        startEmptyPage();

        FormTester form = tester.newFormTester("form");
        String regex = form.getTextComponentValue("regex");

        assertTrue("query", regex.contains("\\?.*"));
        assertTrue("line", regex.endsWith("$"));
    }

    private static void fillNewRule(String regex) {
        startEmptyPage();

        FormTester form = tester.newFormTester("form");
        form.setValue("name", NAME);
        form.setValue(DESCRIPTION, DESCRIPTION);
        form.setValue("regex", regex);
        form.setValue("enabled", true);
        form.submit("submit");
    }

    @Test
    public void testRegexValidator() throws Exception {
        // invalid expression
        fillNewRule("[.*");
        tester.assertErrorMessages(new String[] {"Invalid regular expression: [.*"});
    }

    @Test
    public void testMandatoryFields() throws Exception {
        startEmptyPage();

        // enabled is provided anyways, description is optional
        FormTester form = tester.newFormTester("form");
        form.setValue("enabled", true);
        form.setValue("regex", null); // has a default value, dodge it
        form.submit("submit");
        tester.assertErrorMessages(
                new String[] {"Field 'Name' is required.", "Field 'regex' is required."});
    }

    @Test
    public void testDuplicateRule() throws Exception {
        startEmptyPage();

        // this one should work
        fillNewRule(REGEX);
        tester.assertNoErrorMessage();

        // this one should fail, same rule again
        fillNewRule(REGEX);

        tester.assertErrorMessages(
                new String[] {"Another rule with the same name already exists: 'tester'"});
    }
}
