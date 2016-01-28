/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.config.LogEvent;
import org.geogig.geoserver.config.LogEvent.Severity;
import org.geogig.geoserver.config.LogStore;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class SecurityLoggerTestIntegrationTest extends GeoServerSystemTestSupport {

    private String BASE_URL;

    @Rule
    public GeoGigTestData geogigData = new GeoGigTestData();

    private LogStore logStore;

    private String repoURL;

    /**
     * Override to avoid creating default geoserver test data
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing
    }

    @Before
    public void before() throws Exception {
        // protected void onSetUp(SystemTestData testData) throws Exception {

        geogigData.init()//
                .config("user.name", "gabriel")//
                .config("user.email", "gabriel@test.com")//
                .createTypeTree("lines", "geom:LineString:srid=4326")//
                .createTypeTree("points", "geom:Point:srid=4326")//
                .add()//
                .commit("created type trees")//
                .get();

        RepositoryManager repositoryManager = RepositoryManager.get();

        RepositoryInfo info = new RepositoryInfo();
        repoURL = geogigData.repoDirectory().getAbsolutePath();
        info.setLocation(repoURL);
        info = repositoryManager.save(info);

        BASE_URL = "/geogig/" + info.getId();

        logStore = GeoServerExtensions.bean(LogStore.class);
        assertNotNull(logStore);
    }

    private void login() throws Exception {
        super.login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRemoteAdd() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        System.out.println(dom.toString());
        // <response><success>true</success><name>upstream</name></response>
        assertXpathEvaluatesTo("true", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertEquals(entries.toString(), 2, entries.size());
        LogEvent first = entries.get(1);

        assertEquals(Severity.DEBUG, first.getSeverity());
        assertEquals("anonymous", first.getUser());
        assertEquals(repoURL, first.getRepositoryURL());
        assertTrue(first.getMessage(), first.getMessage().contains("Remote add:"));
        assertTrue(first.getMessage(), first.getMessage().contains("name='upstream'"));

        LogEvent second = entries.get(0);
        assertEquals(Severity.INFO, second.getSeverity());
        assertEquals("anonymous", second.getUser());
        assertEquals(repoURL, second.getRepositoryURL());
        assertTrue(first.getMessage(), second.getMessage().contains("Remote add success"));
        assertTrue(first.getMessage(), second.getMessage().contains("name='upstream'"));
    }

    @Test
    public void testRemoteAddExisting() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        assertXpathEvaluatesTo("true", "/response/success", dom);

        dom = getAsDOM(url);
        assertXpathEvaluatesTo("false", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertTrue(entries.toString(), entries.size() > 0);
        LogEvent last = entries.get(0);

        assertEquals(Severity.ERROR, last.getSeverity());
        assertEquals("anonymous", last.getUser());
        assertEquals(repoURL, last.getRepositoryURL());
        assertTrue(last.getMessage(), last.getMessage().contains("Remote add failed"));
        assertTrue(last.getMessage(), last.getMessage().contains("name='upstream'"));
        assertTrue(last.getMessage(), last.getMessage().contains("REMOTE_ALREADY_EXISTS"));
    }

    @Test
    public void testUserLogged() throws Exception {
        login();
        super.setRequestAuth("admin", "geoserver");

        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        assertXpathEvaluatesTo("true", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertTrue(entries.size() > 0);
        for (LogEvent e : entries) {
            assertEquals("admin", e.getUser());
        }
    }
}
