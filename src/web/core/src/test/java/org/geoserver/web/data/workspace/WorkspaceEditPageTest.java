/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceEditPageTest extends GeoServerWicketTestSupport {

    private WorkspaceInfo citeWorkspace;

    @Before
    public void init() {
        login();
        citeWorkspace = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);

        GeoServer gs = getGeoServer();
        SettingsInfo s = gs.getSettings(citeWorkspace);
        if (s != null) {
            gs.remove(s);
        }
        NamespaceInfo citeNS = getCatalog().getNamespaceByPrefix(MockData.CITE_PREFIX);
        citeNS.setURI(MockData.CITE_URI);
        getCatalog().save(citeNS);

        tester.startPage(new WorkspaceEditPage(citeWorkspace));
    }

    @Test
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "");
        form.submit();

        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertModelValue("form:name", MockData.CITE_PREFIX);
        tester.assertModelValue("form:uri", MockData.CITE_URI);
    }

    @Test
    public void testValidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "http://www.geoserver.org");
        form.submit();

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testInvalidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("uri", "not a valid uri");
        form.submit();

        tester.assertRenderedPage(WorkspaceEditPage.class);
        List messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        assertEquals(
                "Invalid URI syntax: not a valid uri",
                ((ValidationErrorFeedback) messages.get(0)).getMessage());
    }

    /**
     * See GEOS-3322, upon a namespace URI change the datastores connection parameter shall be
     * changed accordingly
     */
    @Test
    public void testUpdatesDataStoresNamespace() {
        final Catalog catalog = getCatalog();
        final List<DataStoreInfo> storesInitial =
                catalog.getStoresByWorkspace(citeWorkspace, DataStoreInfo.class);

        final NamespaceInfo citeNamespace = catalog.getNamespaceByPrefix(citeWorkspace.getName());

        for (DataStoreInfo store : storesInitial) {
            assertEquals(citeNamespace.getURI(), store.getConnectionParameters().get("namespace"));
        }

        FormTester form = tester.newFormTester("form");
        final String newNsURI = "http://www.geoserver.org/changed";
        form.setValue("uri", newNsURI);
        form.submit();
        tester.assertNoErrorMessage();

        List<DataStoreInfo> storesChanged =
                catalog.getStoresByWorkspace(citeWorkspace, DataStoreInfo.class);
        for (DataStoreInfo store : storesChanged) {
            assertEquals(newNsURI, store.getConnectionParameters().get("namespace"));
        }
    }

    @Test
    public void testDefaultCheckbox() {
        assertFalse(getCatalog().getDefaultWorkspace().getName().equals(MockData.CITE_PREFIX));

        FormTester form = tester.newFormTester("form");
        form.setValue("default", "true");
        form.submit();
        tester.assertNoErrorMessage();

        assertEquals(MockData.CITE_PREFIX, getCatalog().getDefaultWorkspace().getName());
    }

    @Test
    public void testEnableSettings() throws Exception {
        GeoServer gs = getGeoServer();

        assertNull(gs.getSettings(citeWorkspace));

        FormTester form = tester.newFormTester("form");
        form.setValue("settings:enabled", true);
        form.submit();

        tester.assertNoErrorMessage();
        assertNotNull(gs.getSettings(citeWorkspace));
    }

    @Test
    public void testLocalworkspaceRemovePrefix() throws Exception {
        GeoServer gs = getGeoServer();
        SettingsInfo settings = gs.getFactory().createSettings();
        settings.setLocalWorkspaceIncludesPrefix(true);
        settings.setWorkspace(citeWorkspace);
        gs.add(settings);

        assertNotNull(gs.getSettings(citeWorkspace));
        tester.startPage(new WorkspaceEditPage(citeWorkspace));
        tester.assertRenderedPage(WorkspaceEditPage.class);

        FormTester form = tester.newFormTester("form");
        form.setValue(
                "settings:settingsContainer:otherSettings:localWorkspaceIncludesPrefix", false);
        form.submit();

        assertEquals(false, settings.isLocalWorkspaceIncludesPrefix());
    }

    @Test
    public void testDisableSettings() throws Exception {
        GeoServer gs = getGeoServer();
        SettingsInfo settings = gs.getFactory().createSettings();
        settings.setProxyBaseUrl("http://foo.org");
        settings.setWorkspace(citeWorkspace);
        gs.add(settings);

        assertNotNull(gs.getSettings(citeWorkspace));
        tester.startPage(new WorkspaceEditPage(citeWorkspace));
        tester.assertRenderedPage(WorkspaceEditPage.class);

        FormTester form = tester.newFormTester("form");
        assertEquals(
                "http://foo.org",
                form.getTextComponentValue(
                        "settings:settingsContainer:otherSettings:proxyBaseUrl"));
        form.setValue("settings:enabled", false);
        form.submit();

        assertNull(gs.getSettings(citeWorkspace));
    }
}
