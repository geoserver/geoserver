/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.util.List;
import java.util.Map;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class UserTablePanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testProfileColumnsHidden() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings().setShowProfileColumnsInUserList(false);
        gs.save(global);

        UserTablePanel userTablePanel = new UserTablePanel("users", null, new UserListProvider(null) {
            @Override
            protected List<GeoServerUser> getItems() {
                return List.of(new GeoServerUser("user"));
            }
        });

        tester.startComponentInPage(userTablePanel);

        String firstUserProperties = "users:listContainer:items:1:itemProperties";

        // check rendering in users table
        tester.assertLabel(firstUserProperties + ":0:component:link:label", "user");
        tester.assertComponent(firstUserProperties + ":1:component:img", org.geoserver.web.wicket.CachingImage.class);
        tester.assertLabel(firstUserProperties + ":2:component", "");
        // no more columns
        tester.assertNotExists(firstUserProperties + ":3:component");
    }

    @Test
    public void testProfileColumnsShown() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings().setShowProfileColumnsInUserList(true);
        gs.save(global);

        GeoServerUser user = new GeoServerUser("user");
        user.getProperties()
                .putAll(Map.of(
                        "first_name", "Iam",
                        "last_name", "Theuser",
                        "preferred_username", "iamtheuser",
                        "email", "user@example.com"));

        UserTablePanel userTablePanel = new UserTablePanel("users", null, new UserListProvider(null) {
            @Override
            protected List<GeoServerUser> getItems() {
                return List.of(user);
            }
        });

        tester.startComponentInPage(userTablePanel);

        String tableItemProperties = "users:listContainer:items:1:itemProperties";

        // check rendering in users table
        tester.assertLabel(tableItemProperties + ":0:component:link:label", "user");
        tester.assertComponent(tableItemProperties + ":1:component:img", org.geoserver.web.wicket.CachingImage.class);
        tester.assertComponent(tableItemProperties + ":2:component:img", org.geoserver.web.wicket.CachingImage.class);
        tester.assertLabel(tableItemProperties + ":3:component", "Iam");
        tester.assertLabel(tableItemProperties + ":4:component", "Theuser");
        tester.assertLabel(tableItemProperties + ":5:component", "iamtheuser");
        tester.assertLabel(tableItemProperties + ":6:component", "example.com");
    }

    @Test
    public void testClickWontRevealMaskedEmail() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        UserDetailsDisplaySettingsInfo userDetailsDisplaySettings = global.getUserDetailsDisplaySettings();
        userDetailsDisplaySettings.setShowProfileColumnsInUserList(true);
        userDetailsDisplaySettings.setRevealEmailAtClick(false);
        gs.save(global);

        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().put("email", "user@example.com");

        UserTablePanel userTablePanel = new UserTablePanel("users", null, new UserListProvider(null) {
            @Override
            protected List<GeoServerUser> getItems() {
                return List.of(user);
            }
        });

        tester.startComponentInPage(userTablePanel);

        String emailColumnPath = "users:listContainer:items:1:itemProperties:6:component";

        // check masked email
        tester.assertLabel(emailColumnPath, "example.com");
        // check absence of full email
        tester.assertContainsNot("user@example.com");

        // click on masked email
        tester.executeAjaxEvent(emailColumnPath, "click");

        // masked email has not been revealed
        tester.assertContainsNot("user@example.com");
    }

    @Test
    public void testClickRevealsMaskedEmail() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        UserDetailsDisplaySettingsInfo userDetailsDisplaySettings = global.getUserDetailsDisplaySettings();
        userDetailsDisplaySettings.setShowProfileColumnsInUserList(true);
        userDetailsDisplaySettings.setRevealEmailAtClick(true);
        gs.save(global);

        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().put("email", "user@example.com");

        UserTablePanel userTablePanel = new UserTablePanel("users", null, new UserListProvider(null) {
            @Override
            protected List<GeoServerUser> getItems() {
                return List.of(user);
            }
        });

        tester.startComponentInPage(userTablePanel);

        String emailColumnPath = "users:listContainer:items:1:itemProperties:6:component";

        // check masked email
        tester.assertLabel(emailColumnPath, "example.com");
        // check absence of full email
        tester.assertContainsNot("user@example.com");

        // click on masked email
        tester.executeAjaxEvent(emailColumnPath, "click");

        // masked email has been revealed
        tester.assertContains("user@example.com");
    }

    @Test
    public void testSearchWithMaskedEmail() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        UserDetailsDisplaySettingsInfo userDetailsDisplaySettings = global.getUserDetailsDisplaySettings();
        userDetailsDisplaySettings.setShowProfileColumnsInUserList(true);
        gs.save(global);

        GeoServerUser user = new GeoServerUser("user");
        user.getProperties().put("email", "masked_email@example.com");

        UserTablePanel userTablePanel = new UserTablePanel("users", null, new UserListProvider(null) {
            @Override
            protected List<GeoServerUser> getItems() {
                return List.of(user);
            }
        });

        tester.startComponentInPage(userTablePanel);

        // check masked email
        tester.assertLabel("users:listContainer:items:1:itemProperties:6:component", "example.com");

        String searchForm = "users:filterForm";
        FormTester form = tester.newFormTester(searchForm);

        // search for non-existent term
        form.setValue("filter", "ABC");
        tester.executeAjaxEvent(searchForm + ":submit", "click");

        // check no result
        tester.assertNotExists("users:listContainer:items:1");

        // search with masked email local part
        form.setValue("filter", "masked_email");
        tester.executeAjaxEvent(searchForm + ":submit", "click");

        // check result
        tester.assertExists("users:listContainer:items:2");
        tester.assertLabel("users:listContainer:items:2:itemProperties:0:component:link:label", "user");
    }
}
