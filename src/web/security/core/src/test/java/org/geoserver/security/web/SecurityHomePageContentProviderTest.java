/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.geoserver.security.SecurityConfigDiagnostics;
import org.geoserver.security.SecurityConfigDiagnostics.ComponentType;
import org.geoserver.security.SecurityConfigDiagnostics.DisabledComponent;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.web.GeoServerApplication;
import org.junit.Test;

public class SecurityHomePageContentProviderTest extends AbstractSecurityWicketTestSupport {

    @Test
    public void testIsEmbeddedDataDirectoryTrue() {
        // the data directory will be created in target
        GeoServerApplication app = new GeoServerApplication();
        app.setApplicationContext(applicationContext);
        app.setServletContext(new MockServletContext(app, "target"));
        assertTrue(SecurityHomePageContentProvider.isEmbeddedDataDirectory(app));
    }

    @Test
    public void testIsEmbeddedDataDirectoryFalse() {
        // the data directory will be created in target
        GeoServerApplication app = new GeoServerApplication();
        app.setApplicationContext(applicationContext);
        app.setServletContext(new MockServletContext(app, "src"));
        assertFalse(SecurityHomePageContentProvider.isEmbeddedDataDirectory(app));
    }

    @Test
    public void testMasterPasswordMessageWithLoginDisabled() throws Exception {
        checkMasterPasswordMessage(false);
    }

    @Test
    public void testMasterPasswordMessageWithLoginEnabled() throws Exception {
        checkMasterPasswordMessage(true);
    }

    private void checkMasterPasswordMessage(boolean loginEnabled) throws Exception {
        MasterPasswordProviderConfig masterPasswordConfig = getSecurityManager()
                .loadMasterPassswordProviderConfig(
                        getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(loginEnabled);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);
        tester.startComponentInPage(new SecurityHomePageContentProvider().getPageBodyComponent("swp"));
        tester.assertComponent("swp", SecurityHomePageContentProvider.SecurityWarningsPanel.class);
        tester.assertVisible("swp:mpmessage");
        tester.assertVisible("swp:mplink");
    }

    /**
     * Regression guard against XSS in the disabled-components notice: the persisted name/alias of a disabled component
     * is attacker-controlled (crafted data directory), so it must be rendered HTML-escaped. The
     * {@code disabledComponent} label relies on Wicket's default escaping (unlike its siblings it does NOT call
     * setEscapeModelStrings(false)); this test fails if a future edit makes that markup render the value raw.
     */
    @Test
    public void testDisabledComponentNameIsHtmlEscaped() throws Exception {
        // record a disabled component whose persisted name carries an HTML/script payload
        SecurityConfigDiagnostics diagnostics = getSecurityManager().getConfigDiagnostics();
        diagnostics.clear();
        diagnostics.addDisabledComponent(new DisabledComponent(
                ComponentType.AUTHENTICATION_FILTER,
                "<script>alert('xss')</script>",
                "<b>evilAlias</b>",
                null,
                "Filter class is not available: <img src=x onerror=alert(1)>"));

        tester.startComponentInPage(new SecurityHomePageContentProvider().getPageBodyComponent("swp"));
        tester.assertComponent("swp", SecurityHomePageContentProvider.SecurityWarningsPanel.class);

        String html = tester.getLastResponseAsString();
        // the warning is shown...
        assertTrue("the disabled-components notice should be rendered", html.contains("could not be loaded"));
        // ...but the payload is escaped, never emitted as live markup
        assertFalse(
                "the disabled component name must be HTML-escaped, not rendered as a live <script> tag",
                html.contains("<script>alert('xss')</script>"));
        assertFalse(
                "the disabled component reason must be HTML-escaped, not rendered as a live <img> tag",
                html.contains("<img src=x onerror=alert(1)>"));
        // the escaped form is present instead
        assertTrue(
                "the escaped script payload should appear in the output",
                html.contains("&lt;script&gt;alert(&#039;xss&#039;)&lt;/script&gt;")
                        || html.contains("&lt;script&gt;"));
    }
}
