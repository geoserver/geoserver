/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.TestFeedServer;
import org.geoserver.updateschecker.TestFeeds;
import org.geoserver.updateschecker.UpdatesCheckerConfig;
import org.geoserver.updateschecker.UpdatesCheckerConfigPersister;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerStartupChecker;
import org.geoserver.updateschecker.UpdatesCheckerSuppression;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.HelpLink;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Renders the settings page, the Status section and the landing-page banner in a real GeoServer application, against a
 * local feed server. The running version is forced to 2.28.3 with GS_UPDATES_CHECKER_OVERRIDE.
 */
public class UpdatesCheckerWebTest extends GeoServerWicketTestSupport {

    private static final String FEED = TestFeeds.feed(
            "",
            TestFeeds.series("3.0.x", "stable", "3.0.1", false),
            TestFeeds.series("2.27.x", "maintenance", "2.27.6", false),
            TestFeeds.series("2.28.x", "maintenance", "2.28.5", false));

    private static TestFeedServer feedServer;

    @BeforeClass
    public static void startFeedServer() throws Exception {
        System.setProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY, "2.28.3");
        feedServer = TestFeedServer.startForApplication(FEED);
    }

    @AfterClass
    public static void stopFeedServer() {
        feedServer.stop();
        System.clearProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY);
    }

    @Before
    public void configure() throws InterruptedException {
        awaitStartupCheck();
        UpdatesCheckerConfig config = new UpdatesCheckerConfig();
        persister().save(config);
        // a fresh session has not had its one feed check yet
        tester.getSession().invalidateNow();
        login();
    }

    @After
    public void restoreDefaults() {
        feedServer.setStatus(200);
        feedServer.setBody(FEED);
        persister().save(new UpdatesCheckerConfig());
    }

    /** The application's start makes one background fetch; it must be over before a test counts requests. */
    private static void awaitStartupCheck() throws InterruptedException {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (UpdatesCheckerStartupChecker.THREAD_NAME.equals(thread.getName())) {
                thread.join(10_000);
            }
        }
    }

    private static UpdatesCheckerConfigPersister persister() {
        return GeoServerExtensions.bean(UpdatesCheckerConfigPersister.class);
    }

    private static AbstractDefaultAjaxBehavior firstViewBehavior(Component component) {
        return component.getBehaviors(AbstractDefaultAjaxBehavior.class).get(0);
    }

    @Test
    public void settingsPageShowsFeedUrlAndStatus() {
        tester.startPage(UpdatesCheckerPage.class);
        tester.assertRenderedPage(UpdatesCheckerPage.class);
        tester.assertNoErrorMessage();
        String feedUrl = feedServer.url(TestFeedServer.PATH);
        tester.assertModelValue("form:feedUrl", feedUrl);
        tester.assertVisible("status");
        tester.assertContains("jsDelivr content delivery network");
    }

    @Test
    public void firstViewFetchesOnceThenLaterViewsUseTheCache() {
        tester.startPage(UpdatesCheckerPage.class);
        Component status = tester.getComponentFromLastRenderedPage("status");
        assertFalse(UpdatesCheckerSessionState.isChecked());
        int before = feedServer.getRequests();
        tester.executeBehavior(firstViewBehavior(status));
        assertEquals(before + 1, feedServer.getRequests());
        assertTrue(UpdatesCheckerSessionState.isChecked());
        tester.assertContains("Feed checked");
        tester.assertContains("GeoServer 2.28.5");

        // a later view in the same session draws from the cache: no fetch, and no first-view call
        tester.startPage(UpdatesCheckerPage.class);
        assertEquals(before + 1, feedServer.getRequests());
        assertTrue(tester.getComponentFromLastRenderedPage("status")
                .getBehaviors(AbstractDefaultAjaxBehavior.class)
                .isEmpty());
        tester.assertContains("Feed checked");
        tester.assertContains("GeoServer 2.28.5");
    }

    @Test
    public void checkNowFetchesAndRefreshesStatus() {
        tester.startPage(UpdatesCheckerPage.class);
        int before = feedServer.getRequests();
        tester.clickLink("status:root:checkNow", true);
        assertEquals(before + 1, feedServer.getRequests());
        assertTrue(UpdatesCheckerSessionState.isChecked());
        tester.assertContains("2.28.5");
        // the Status list offers the same actions as the banner
        tester.assertContains("Feed checked");
        tester.assertContains("gs-updates-checker-status-none");
        tester.assertContains("data-clear");
        tester.assertContains("data-template=\"Clear previous \\{0\\} dismissals\"");
        tester.assertContains("data-action=\"remind-24h\"");
        tester.assertContains("data-action=\"dismiss\"");
    }

    @Test
    public void aFailedCheckShowsWhyInTheStatusSection() {
        feedServer.setStatus(500);
        tester.startPage(UpdatesCheckerPage.class);
        tester.clickLink("status:root:checkNow", true);
        tester.assertContains("Check failed: Version feed returned HTTP 500");
    }

    @Test
    public void checkNowWorksEvenWhenDisabled() {
        UpdatesCheckerConfig config = persister().getConfig();
        config.setEnabled(false);
        persister().save(config);
        tester.startPage(UpdatesCheckerPage.class);
        int before = feedServer.getRequests();
        tester.clickLink("status:root:checkNow", true);
        assertEquals(before + 1, feedServer.getRequests());
    }

    @Test
    public void hostileFeedTextIsNeverRenderedAsMarkupOrLinks() {
        String script = "<script>alert(1)</script><img src=x onerror=alert(2)>";
        feedServer.setBody("{\"schema_version\": 1, \"series\": ["
                + "{\"series\": \"2.28.x\", \"phase\": \"maintenance\", \"latest_version\": \"2.28.5\","
                + " \"release_url\": \"javascript:alert(3)\"}],"
                + " \"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\\\"><script>alert(4)</script>\","
                + " \"cve_id\": \"CVE-2026-0001<script>\", \"severity\": \"critical\","
                + " \"summary\": \"" + script.replace("\"", "\\\"") + "\","
                + " \"vulnerable_versions\": [\"< 2.28.5\"], \"patched_versions\": {\"2.28.x\": \"2.28.5\"}}]}");
        tester.startPage(UpdatesCheckerPage.class);
        tester.clickLink("status:root:checkNow", true);
        String html = tester.getLastResponseAsString();
        assertTrue(html, html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html, html.contains("<script>alert"));
        assertFalse(html, html.contains("<img src=x"));
        assertFalse(html, html.contains("javascript:alert"));
        assertFalse(html, html.contains("GHSA-aaaa-bbbb-cccc\""));
        tester.startPage(GeoServerHomePage.class);
        html = tester.getLastResponseAsString();
        assertFalse(html, html.contains("<script>alert"));
        assertFalse(html, html.contains("javascript:alert"));
    }

    @Test
    public void anUncheckableAdvisoryShowsItsSummaryWithItalicAndBoldGuidance() {
        feedServer.setBody(
                "{\"schema_version\": 1, \"series\": ["
                        + "{\"series\": \"2.28.x\", \"phase\": \"archive\", \"latest_version\": \"2.28.5\"}],"
                        + " \"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\","
                        + " \"severity\": \"critical\", \"summary\": \"Unauthenticated <b>injection</b>\","
                        + " \"vulnerable_versions\": [\"df11a650c650b2b5a73fe6c6b0e6d0d8a6e2a8c1\"], \"patched_versions\": {}}]}");
        tester.startPage(UpdatesCheckerPage.class);
        tester.clickLink("status:root:checkNow", true);
        String html = tester.getLastResponseAsString();
        assertTrue(
                html,
                html.contains("CVE-2026-0001 (critical): Unauthenticated &lt;b&gt;injection&lt;/b&gt;. <i>Version"));
        assertTrue(html, html.contains("<i>Version range could not be determined automatically.</i>"));
        assertTrue(html, html.contains("<b>Review manually</b>"));
    }

    @Test
    public void aFlaggedSecurityReleaseIsShownAsHighWithItsAnnouncement() {
        // the forced version is 2.28.3
        feedServer.setBody(TestFeeds.feed(
                "",
                TestFeeds.seriesWithReleases(
                        "2.28.x",
                        "archive",
                        "2.28.5",
                        TestFeeds.release("2.28.5", true, false, TestFeeds.blog("2.28.5")))));
        tester.startPage(UpdatesCheckerPage.class);
        tester.clickLink("status:root:checkNow", true);
        String html = tester.getLastResponseAsString();
        assertTrue(html, html.contains("data-type=\"SECURITY_RELEASE\""));
        assertTrue(html, html.contains("data-severity=\"HIGH\""));
        assertTrue(html, html.contains("data-target-version=\"2.28.5\""));
        assertTrue(html, html.contains("Security release: GeoServer 2.28.5 contains security fixes."));
        assertTrue(html, html.contains("href=\"" + TestFeeds.blog("2.28.5") + "\""));
        assertFalse("the plain patch notice is replaced", html.contains("data-type=\"PATCH_AVAILABLE\""));
    }

    @Test
    public void saveStoresTheForm() {
        tester.startPage(UpdatesCheckerPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("enabled", false);
        form.setValue("logUpdateChecks", false);
        form.submit("submit");
        tester.assertNoErrorMessage();

        UpdatesCheckerConfig saved = persister().getConfig();
        assertFalse(saved.isEnabled());
        assertFalse(saved.isLogUpdateChecks());
    }

    @Test
    public void saveFailureIsReportedOnThePage() throws Exception {
        // a directory where the config file should be makes the write fail, as a read-only data directory would
        File file = new File(
                GeoServerExtensions.bean(GeoServerDataDirectory.class)
                        .get("updates-checker")
                        .dir(),
                "updates-checker.xml");
        assertTrue(file.delete());
        assertTrue(file.mkdir());
        try {
            tester.startPage(UpdatesCheckerPage.class);
            FormTester form = tester.newFormTester("form");
            form.submit("submit");
            tester.assertErrorMessages(
                    "The settings could not be saved. The data directory may be read-only; see the GeoServer log for details.");
        } finally {
            assertTrue(file.delete());
        }
    }

    @Test
    public void changingTheLogCheckboxRefreshesTheDependentOption() {
        tester.startPage(UpdatesCheckerPage.class);
        tester.executeAjaxEvent("form:logUpdateChecks", "change");
        tester.assertNoErrorMessage();
    }

    @Test
    public void cancelDoesNotSave() {
        tester.startPage(UpdatesCheckerPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("enabled", false);
        form.submit("cancel");
        assertTrue(persister().getConfig().isEnabled());
    }

    @Test
    public void logCheckboxDrivesHideVersionNumbers() {
        tester.startPage(UpdatesCheckerPage.class);
        tester.assertEnabled("form:hideVersionNumbersInLogs");
        UpdatesCheckerConfig config = persister().getConfig();
        config.setLogUpdateChecks(false);
        persister().save(config);
        tester.startPage(UpdatesCheckerPage.class);
        tester.assertDisabled("form:hideVersionNumbersInLogs");
    }

    @Test
    public void landingPageShowsBannerToAdministratorAfterFirstView() {
        tester.startPage(GeoServerHomePage.class);
        tester.assertRenderedPage(GeoServerHomePage.class);

        // the first view of the session leaves a placeholder that the AJAX call fills
        UpdatesCheckerAsyncPanel panel = findAsyncPanel();
        assertNotNull(panel);
        tester.executeBehavior(firstViewBehavior(panel));
        tester.assertContains("GeoServer 2.28.5");

        tester.startPage(GeoServerHomePage.class);
        tester.assertContains("gs-updates-checker-notification");
        tester.assertContains("gs-updates-checker-heading");
        tester.assertContains("Check for Updates");
        tester.assertContains("feedbackPanelWARNING");
        tester.assertContains("data-type=\"PATCH_AVAILABLE\"");
    }

    @Test
    public void landingPageHasNoBannerWhenDisabledOrAnonymous() {
        UpdatesCheckerConfig config = persister().getConfig();
        config.setEnabled(false);
        persister().save(config);
        tester.startPage(GeoServerHomePage.class);
        assertNull(findAsyncPanel());

        config.setEnabled(true);
        persister().save(config);
        logout();
        tester.startPage(GeoServerHomePage.class);
        assertNull(findAsyncPanel());
    }

    @Test
    public void theBannerIsOnlyForFullAdministrators() {
        tester.startPage(UpdatesCheckerPage.class);
        assertTrue(UpdatesCheckerBannerFactory.isFullAdministrator());

        logout();
        tester.startPage(GeoServerHomePage.class);
        assertFalse(UpdatesCheckerBannerFactory.isFullAdministrator());
        assertNull(UpdatesCheckerBannerFactory.createIfVisible("x"));
    }

    private static UpdatesCheckerAsyncPanel findAsyncPanel() {
        return (UpdatesCheckerAsyncPanel) tester.getLastRenderedPage()
                .visitChildren(UpdatesCheckerAsyncPanel.class, (component, visit) -> visit.stop(component));
    }
    // ---- suppressed advisories ----------------------------------------------------------------------------------

    private void serveOneAdvisory() {
        // the forced version is 2.28.3
        feedServer.setBody("{\"schema_version\": 1, \"series\": ["
                + "{\"series\": \"2.28.x\", \"phase\": \"stable\", \"latest_version\": \"2.28.3\"}],"
                + "\"advisories\": [{\"ghsa_id\": \"GHSA-aaaa-bbbb-cccc\", \"cve_id\": \"CVE-2026-0001\","
                + "\"severity\": \"critical\", \"summary\": \"Affects 2.28.3\","
                + "\"vulnerable_versions\": [\"< 2.28.4\"], \"patched_versions\": {}}]}");
    }

    @Test
    public void anAdvisoryThatIsNotSuppressedShowsInTheBannerAndTheStatusSection() {
        serveOneAdvisory();
        tester.startPage(UpdatesCheckerPage.class);
        tester.clickLink("status:root:checkNow", true);
        String html = tester.getLastResponseAsString();
        assertTrue(html, html.contains("CVE-2026-0001 (critical): Affects 2.28.3"));
        assertFalse(html, html.contains("Suppressed by the server configuration"));
    }

    @Test
    public void aSuppressedAdvisoryIsLeftOutOfBothButTheStatusSectionSaysSo() {
        serveOneAdvisory();
        System.setProperty(UpdatesCheckerSuppression.PROPERTY, "CVE-2026-0001");
        try {
            tester.startPage(UpdatesCheckerPage.class);
            tester.clickLink("status:root:checkNow", true);
            String html = tester.getLastResponseAsString();
            assertFalse(html, html.contains("Affects 2.28.3"));
            assertTrue(html, html.contains("Suppressed by the server configuration: CVE-2026-0001."));
            assertTrue(html, html.contains("No notifications are active."));

            tester.startPage(GeoServerHomePage.class);
            assertFalse(tester.getLastResponseAsString().contains("Affects 2.28.3"));
        } finally {
            System.clearProperty(UpdatesCheckerSuppression.PROPERTY);
        }
    }

    @Test
    public void theSettingsPageShowsTheSuppressedAdvisoriesButCannotChangeThem() {
        System.setProperty(UpdatesCheckerSuppression.PROPERTY, "GHSA-aaaa-bbbb-cccc CVE-2026-0001");
        try {
            tester.startPage(UpdatesCheckerPage.class);
            tester.assertModelValue("form:suppressedCves", "CVE-2026-0001, GHSA-aaaa-bbbb-cccc");
            String html = tester.getLastResponseAsString();
            assertTrue(html, html.contains("readonly=\"readonly\""));

            FormTester form = tester.newFormTester("form");
            form.setValue("suppressedCves", "CVE-2026-9999");
            form.submit("submit");
            assertEquals(
                    "CVE-2026-0001, GHSA-aaaa-bbbb-cccc",
                    GeoServerExtensions.bean(UpdatesCheckerService.class)
                            .getSuppression()
                            .toString());
        } finally {
            System.clearProperty(UpdatesCheckerSuppression.PROPERTY);
        }
    }

    @Test
    public void withNothingSuppressedTheBoxIsEmpty() {
        tester.startPage(UpdatesCheckerPage.class);
        tester.assertModelValue("form:suppressedCves", "");
    }

    @Test
    public void aHelpLinkExplainsTheSuppressedAdvisories() {
        tester.startPage(UpdatesCheckerPage.class);
        tester.assertComponent("form:suppressedCvesHelp", HelpLink.class);
        tester.clickLink("form:suppressedCvesHelp", true);
        String html = tester.getLastResponseAsString();
        assertTrue(html, html.contains("UPDATES_CHECKER_SUPPRESSED_CVES"));
        assertTrue(html, html.contains("cannot be changed here"));
    }
}
