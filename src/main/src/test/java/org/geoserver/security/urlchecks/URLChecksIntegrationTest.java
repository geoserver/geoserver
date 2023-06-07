/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.ows.URLCheckerException;
import org.geotools.data.ows.URLCheckers;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class URLChecksIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Before
    public void cleanup() throws Exception {
        Resource config = getConfigurationFile();
        config.delete();
        getDao().reset();
    }

    private Resource getConfigurationFile() {
        return getDataDirectory().getSecurity(URLCheckDAO.CONFIG_FILE_NAME);
    }

    @Test
    public void testOnEmpty() throws Exception {
        URLCheckDAO dao = getDao();
        assertNotNull(dao.getChecks());
        assertThat(dao.getChecks(), empty());
    }

    private static URLCheckDAO getDao() {
        return applicationContext.getBean(URLCheckDAO.class);
    }

    @Test
    public void testDaoSaveRules() throws Exception {
        List<AbstractURLCheck> checks = getSampleChecks(false);

        URLCheckDAO dao = getDao();
        dao.saveChecks(checks);

        Document dom;
        try (InputStream is = getConfigurationFile().in()) {
            dom = dom(is, true);
        }

        // check all three have been serialized successfully with the expected XML structure
        for (int i = 0; i < 3; i++) {
            String path = "/checks/regex[" + (i + 1) + "]/";
            RegexURLCheck check = (RegexURLCheck) checks.get(i);
            assertThat(dom, hasXPath(path + "name", equalTo(check.getName())));
            if (check.getDescription() != null)
                assertThat(dom, hasXPath(path + "description", equalTo(check.getDescription())));
            else assertThat(dom, not(hasXPath(path + "description")));
            assertThat(dom, hasXPath(path + "regex", equalTo(check.getRegex())));
            assertThat(dom, hasXPath(path + "enabled", equalTo(String.valueOf(check.isEnabled()))));
        }
    }

    @Test
    public void testDaoReadRules() throws Exception {
        writeSampleRulesXML(false);

        URLCheckDAO dao = getDao();
        List<AbstractURLCheck> checks = dao.getChecks();

        assertEquals(getSampleChecks(false), checks);
    }

    /** Tests the GeoServer URL checker is plugged in, and is reading from the XML file */
    @Test
    public void testCheckers() throws Exception {
        writeSampleRulesXML(false);
        // in the test context, beans are lazy loaded, force it to load (at runtime they are
        // always eagerly loaded instead)
        assertNotNull(applicationContext.getBean(GeoServerURLChecker.class));

        // invalid references
        assertThrows(URLCheckerException.class, () -> URLCheckers.confirm("http://google.com"));
        assertThrows(URLCheckerException.class, () -> URLCheckers.confirm("http://nyt.com"));
        assertThrows(URLCheckerException.class, () -> URLCheckers.confirm("file:///tmp"));
        assertThrows(URLCheckerException.class, () -> URLCheckers.confirm("file:///data/test.tif"));

        // valid references
        URLCheckers.confirm("https://www.geoserver.org/logo.png");
        URLCheckers.confirm("http://www.geotools.org/sld.xsd");

        // write xml with XML enabled
        writeSampleRulesXML(true);

        // depending on the file system, the last modified date can have a resolution of
        // microseconds to a whopping 2 seconds (FAT32). Rather than waiting, force reload
        getDao().reset();

        URLCheckers.confirm("file:///data/test.tif");
    }

    private void writeSampleRulesXML(boolean dataEnabled) {
        Resource config = getConfigurationFile();
        try (PrintStream os = new PrintStream(config.out(), true, StandardCharsets.UTF_8)) {
            os.println(
                    "<checks>"
                            + "<regex>"
                            + "<name>geoserver</name>"
                            + "<description>Allows access to geoserver.org</description>"
                            + "<regex>https?://.*geoserver.org/.*</regex>"
                            + "<enabled>true</enabled>"
                            + "</regex>"
                            + "<regex>"
                            + "<name>geotools</name>"
                            + "<regex>https?://.*geotools.org/.*</regex>"
                            + "<enabled>true</enabled>"
                            + "</regex>"
                            + "<regex>"
                            + "<name>data</name>"
                            + "<description>Allows access to the data folder</description>"
                            + "<regex>file:///data/.*\\.tif</regex>"
                            + "<enabled>"
                            + dataEnabled
                            + "</enabled>"
                            + "</regex>"
                            + "</checks>");
        }
    }

    private static List<AbstractURLCheck> getSampleChecks(boolean dataEnabled) {
        // prepare 3 checks
        List<AbstractURLCheck> checks = new ArrayList<>();
        RegexURLCheck gsCheck =
                new RegexURLCheck(
                        "geoserver",
                        "Allows access to geoserver.org",
                        "https?://.*geoserver.org/.*");
        checks.add(gsCheck);
        RegexURLCheck gtCheck = new RegexURLCheck("geotools", null, "https?://.*geotools.org/.*");
        checks.add(gtCheck);
        RegexURLCheck fileCheck =
                new RegexURLCheck(
                        "data", "Allows access to the data folder", "file:///data/.*\\.tif");
        fileCheck.setEnabled(dataEnabled);
        checks.add(fileCheck);
        return checks;
    }
}
