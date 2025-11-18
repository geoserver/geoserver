/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.security.impl.UserProfilePropertyNames.FIRST_NAME;
import static org.geoserver.security.impl.UserProfilePropertyNames.LAST_NAME;
import static org.geoserver.security.impl.UserProfilePropertyNames.PREFERRED_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class LoggedInUserLabelTest extends GeoServerWicketTestSupport {

    @Test
    public void testUsernameMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setLoggedInUserDisplayMode(UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode.USERNAME);
        gs.save(global);

        loginAsAdmin();

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("admin", loggedInLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testPreferredUsernameMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setLoggedInUserDisplayMode(UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode.PREFERRED_USERNAME);
        gs.save(global);

        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().put(PREFERRED_USERNAME, "administrator");
        login(user, "", "role");

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("administrator", loggedInLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testFullNameMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setLoggedInUserDisplayMode(UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode.FULL_NAME);
        gs.save(global);

        /* first and last names are present */
        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().putAll(Map.of(FIRST_NAME, "Great Benevolent", LAST_NAME, "Dictator Admin"));
        login(user, "", "role");

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("Great Benevolent Dictator Admin", loggedInLabel.getDefaultModelObjectAsString());

        /* only first name is present */
        user = new GeoServerUser("user");
        user.getProperties().put(FIRST_NAME, "Admin");
        login(user, "", "role");

        loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("Admin", loggedInLabel.getDefaultModelObjectAsString());

        /* only last name is present */
        user = new GeoServerUser("user");
        user.getProperties().put(LAST_NAME, "Istrator");
        login(user, "", "role");

        loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("Istrator", loggedInLabel.getDefaultModelObjectAsString());

        /* last name is blank */
        user = new GeoServerUser("user");
        user.getProperties().putAll(Map.of(FIRST_NAME, "Admin", LAST_NAME, "     "));
        login(user, "", "role");

        loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("Admin", loggedInLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testFallbackMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setLoggedInUserDisplayMode(UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode.FALLBACK);
        gs.save(global);

        /* first and last names are present */
        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().putAll(Map.of(FIRST_NAME, "Great", LAST_NAME, "Admin"));
        login(user, "", "role");

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("Great Admin", loggedInLabel.getDefaultModelObjectAsString());

        /* first and last names are not present, fallback to preferred username */
        user = new GeoServerUser("user");
        user.getProperties().put(PREFERRED_USERNAME, "administrator");
        login(user, "", "role");

        loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("administrator", loggedInLabel.getDefaultModelObjectAsString());

        /* Neither first nor last name is present and no preferred username, fallback to username */
        user = new GeoServerUser("user");
        login(user, "", "role");

        loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("user", loggedInLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testDefault() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setLoggedInUserDisplayMode(UserDetailsDisplaySettingsInfo.LoggedInUserDisplayMode.PREFERRED_USERNAME);
        gs.save(global);

        loginAsAdmin();

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("admin", loggedInLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testAuthenticationIsNotAGeoServerUser() {

        login("user", "pwd", "role");

        LoggedInUserLabel loggedInLabel = new LoggedInUserLabel("label");
        assertEquals("user", loggedInLabel.getDefaultModelObjectAsString());
    }
}
