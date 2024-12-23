/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.protocol.http.mock.MockServletContext;
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
}
