/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.*;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.junit.Before;
import org.junit.Test;

/** Unit test for evaluating the Root Directory TextField for the RESTUploadPathMapper. */
public class RESTPanelTest extends GeoServerWicketTestSupport {

    /** Workspace info object called "cite" */
    private WorkspaceInfo citeWorkspace;

    @Before
    public void init() {
        // Login
        login();
        // Selection of the workspace
        citeWorkspace = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        // Removing the settings associated to the workspace
        GeoServer gs = getGeoServer();
        SettingsInfo s = gs.getSettings(citeWorkspace);
        if (s != null) {
            gs.remove(s);
        }
        NamespaceInfo citeNS = getCatalog().getNamespaceByPrefix(MockData.CITE_PREFIX);
        citeNS.setURI(MockData.CITE_URI);
        getCatalog().save(citeNS);
    }

    @Test
    public void testRootSettingOnWorkSpace() {
        // Opening the selected page
        tester.startPage(new WorkspaceEditPage(citeWorkspace));
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();
        // Setting of the "settings" parameter to true
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:settings:enabled", true);
        form.submit("save");
        // Check if no error has been found
        tester.assertNoErrorMessage();
        // Get GeoServer object for retrieving the settings associated to the workspace
        GeoServer gs = getGeoServer();
        assertNotNull(gs.getSettings(citeWorkspace));
        // Reload the page
        tester.startPage(new WorkspaceEditPage(citeWorkspace));
        tester.assertRenderedPage(WorkspaceEditPage.class);

        // Selection of the root directory
        String root = getTestData().getDataDirectoryRoot().getAbsolutePath();
        // Set the root directory
        FormTester form2 = tester.newFormTester("form");
        form2.setValue(
                "tabs:panel:settings:settingsContainer:otherSettings:extensions:0:content:rootdir",
                root);
        form2.submit("save");
        // Check if no error has been found
        tester.assertNoErrorMessage();
        // Control if the defined root has been correctly set
        assertEquals(
                gs.getSettings(citeWorkspace).getMetadata().get(RESTUtils.ROOT_KEY, String.class),
                root);
    }

    @Test
    public void testRootSettingOnGlobal() {
        // Opening the selected page
        tester.startPage(new GlobalSettingsPage());
        tester.assertRenderedPage(GlobalSettingsPage.class);
        tester.assertNoErrorMessage();

        // Get GeoServer object for searching the global settings
        GeoServer gs = getGeoServer();

        // Selection of the root directory
        String root = getTestData().getDataDirectoryRoot().getAbsolutePath();
        // Set the root directory
        FormTester form = tester.newFormTester("form");
        form.setValue("extensions:0:content:rootdir", root);
        form.submit("submit");
        // Check if no error has been found
        tester.assertNoErrorMessage();
        // Control if the defined root has been correctly set
        assertEquals(
                gs.getGlobal().getSettings().getMetadata().get(RESTUtils.ROOT_KEY, String.class),
                root);
    }

    @Test
    public void testQuietSettingOnGlobal() {
        // Opening the selected page
        tester.startPage(new GlobalSettingsPage());
        tester.assertRenderedPage(GlobalSettingsPage.class);
        tester.assertNoErrorMessage();

        // Get GeoServer object for searching the global settings
        GeoServer gs = getGeoServer();

        // Set the root directory
        FormTester form = tester.newFormTester("form");
        form.setValue("extensions:0:content:quiet", false);
        form.submit("submit");
        // Check if no error has been found
        tester.assertNoErrorMessage();
        // Control if the defined root has been correctly set
        assertEquals(
                gs.getGlobal()
                        .getSettings()
                        .getMetadata()
                        .get(RESTUtils.QUIET_ON_NOT_FOUND_KEY, Boolean.class),
                false);

        // Opening the selected page
        tester.startPage(new GlobalSettingsPage());
        tester.assertRenderedPage(GlobalSettingsPage.class);
        tester.assertNoErrorMessage();

        // Get GeoServer object for searching the global settings
        gs = getGeoServer();

        // Set the root directory
        form = tester.newFormTester("form");
        form.setValue("extensions:0:content:quiet", true);
        form.submit("submit");
        // Check if no error has been found
        tester.assertNoErrorMessage();
        // Control if the defined root has been correctly set
        assertEquals(
                gs.getGlobal()
                        .getSettings()
                        .getMetadata()
                        .get(RESTUtils.QUIET_ON_NOT_FOUND_KEY, Boolean.class),
                true);
    }
}
