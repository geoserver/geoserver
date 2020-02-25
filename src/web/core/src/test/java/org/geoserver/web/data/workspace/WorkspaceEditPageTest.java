/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.security.AccessDataRuleInfoManager;
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
        form.setValue("tabs:panel:uri", "");
        form.submit("save");

        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertModelValue("form:tabs:panel:name", MockData.CITE_PREFIX);
        tester.assertModelValue("form:tabs:panel:uri", MockData.CITE_URI);
    }

    @Test
    public void testValidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("save");

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testInvalidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:uri", "not a valid uri");
        form.submit("save");

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
        form.setValue("tabs:panel:uri", newNsURI);
        form.submit("save");
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
        form.setValue("tabs:panel:default", true);
        form.submit("save");
        tester.assertNoErrorMessage();

        assertEquals(MockData.CITE_PREFIX, getCatalog().getDefaultWorkspace().getName());
    }

    @Test
    public void testEnableSettings() throws Exception {
        GeoServer gs = getGeoServer();

        assertNull(gs.getSettings(citeWorkspace));

        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:settings:enabled", true);
        form.submit("save");

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
                "tabs:panel:settings:settingsContainer:otherSettings:localWorkspaceIncludesPrefix",
                false);
        form.submit("save");

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
                        "tabs:panel:settings:settingsContainer:otherSettings:proxyBaseUrl"));
        form.setValue("tabs:panel:settings:enabled", false);
        form.submit("save");

        assertNull(gs.getSettings(citeWorkspace));
    }

    @Test
    public void testDisablingIsolatedWorkspace() {
        // create two workspaces with the same namespace, one of them is isolated
        createWorkspace("test_a1", "http://www.test_a.org", false);
        createWorkspace("test_a2", "http://www.test_a.org", true);
        // edit the second workspace to make it non isolated, this should fail
        updateWorkspace("test_a2", "test_a2", "http://www.test_a.org", false);
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertErrorMessages(
                new String[] {"Namespace with URI 'http://www.test_a.org' already exists."});
        // edit the first workspace and make it isolated
        updateWorkspace("test_a1", "test_a1", "http://www.test_a.org", true);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // edit the second workspace to make it non isolated
        updateWorkspace("test_a2", "test_a2", "http://www.test_a.org", false);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // check that the catalog contains the expected objects
        Catalog catalog = getCatalog();
        // validate the first workspace
        assertThat(catalog.getWorkspaceByName("test_a1"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_a1").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_a1"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_a1").getURI(), is("http://www.test_a.org"));
        assertThat(catalog.getNamespaceByPrefix("test_a1").isIsolated(), is(true));
        // validate the second workspace
        assertThat(catalog.getWorkspaceByName("test_a2"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_a2").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByPrefix("test_a2"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_a2").getURI(), is("http://www.test_a.org"));
        assertThat(catalog.getNamespaceByPrefix("test_a2").isIsolated(), is(false));
        // validate the global namespace, i.e. non isolated namespace
        assertThat(catalog.getNamespaceByURI("http://www.test_a.org").getPrefix(), is("test_a2"));
    }

    @Test
    public void testUpdatingIsolatedWorkspaceName() {
        // create two workspaces with the same namespace, one of them is isolated
        createWorkspace("test_b1", "http://www.test_b.org", false);
        createWorkspace("test_b2", "http://www.test_b.org", true);
        // change second workspace name and try to make non isolated, this should fail
        updateWorkspace("test_b2", "test_b3", "http://www.test_b.org", false);
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertErrorMessages(
                new String[] {"Namespace with URI 'http://www.test_b.org' already exists."});
        // check that the catalog contains the expected objects
        Catalog catalog = getCatalog();
        // validate the first workspace
        assertThat(catalog.getWorkspaceByName("test_b1"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_b1").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByPrefix("test_b1"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b1").getURI(), is("http://www.test_b.org"));
        assertThat(catalog.getNamespaceByPrefix("test_b1").isIsolated(), is(false));
        // validate the second workspace
        assertThat(catalog.getWorkspaceByName("test_b2"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_b2").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_b2"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b2").getURI(), is("http://www.test_b.org"));
        assertThat(catalog.getNamespaceByPrefix("test_b2").isIsolated(), is(true));
        // validate the global namespace, i.e. non isolated namespace
        assertThat(catalog.getNamespaceByURI("http://www.test_b.org").getPrefix(), is("test_b1"));
        // assert that no workspace with the updated name exists
        assertThat(catalog.getWorkspaceByName("test_b3"), nullValue());
    }

    /**
     * Helper method that creates a workspace and add it to the catalog. This method will first
     * create the namespace and then the workspace.
     *
     * @param prefix name of the workspace and prefix of the namespace
     * @param namespaceUri URI fo the namespace associated to the workspace
     * @param isolated TRUE if the created workspace and namespace should be considered isolated
     */
    private void createWorkspace(String prefix, String namespaceUri, boolean isolated) {
        Catalog catalog = getCatalog();
        // create the namespace
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setPrefix(prefix);
        namespace.setURI(namespaceUri);
        namespace.setIsolated(isolated);
        catalog.add(namespace);
        // create the workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(prefix);
        workspace.setIsolated(isolated);
        catalog.add(workspace);
    }

    /**
     * Helper method that edits an workspace and submits the editions.
     *
     * @param name new workspace name
     * @param namespace new workspace namespace URI
     * @param isolated TRUE if the workspace should be isolated, otherwise false
     */
    private void updateWorkspace(
            String originalName, String name, String namespace, boolean isolated) {
        // make sure the form is initiated
        WorkspaceInfo originalWorkspace = getCatalog().getWorkspaceByName(originalName);
        tester.startPage(new WorkspaceEditPage(originalWorkspace));
        // get the workspace creation form
        FormTester form = tester.newFormTester("form");
        // fill the form with the provided values
        form.setValue("tabs:panel:name", name);
        form.setValue("tabs:panel:uri", namespace);
        form.setValue("tabs:panel:isolated", isolated);
        // submit the form
        form.submit("save");
    }

    @Test
    public void testSecurityTabLoad() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "abc");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertComponent("form:tabs:panel:listContainer", WebMarkupContainer.class);
        tester.assertComponent("form:tabs:panel:listContainer:selectAll", CheckBox.class);
        tester.assertComponent("form:tabs:panel:listContainer:rules", ListView.class);
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testUpdatesWsSecurityRules() throws IOException {
        AccessDataRuleInfoManager ruleMan = new AccessDataRuleInfoManager();
        try {
            final Catalog catalog = getCatalog();
            final List<DataStoreInfo> storesInitial =
                    catalog.getStoresByWorkspace(citeWorkspace, DataStoreInfo.class);

            final NamespaceInfo citeNamespace =
                    catalog.getNamespaceByPrefix(citeWorkspace.getName());
            for (DataStoreInfo store : storesInitial) {
                assertEquals(
                        citeNamespace.getURI(), store.getConnectionParameters().get("namespace"));
            }
            assertTrue(ruleMan.getResourceRule(citeWorkspace.getName(), citeWorkspace).isEmpty());
            FormTester form = tester.newFormTester("form");
            tester.clickLink("form:tabs:tabs-container:tabs:1:link");
            form.setValue("tabs:panel:listContainer:rules:0:admin", true);
            tester.clickLink("form:save");
            assertTrue(ruleMan.getResourceRule(citeWorkspace.getName(), citeWorkspace).size() == 1);
            tester.assertNoErrorMessage();

        } finally {
            ruleMan.removeAllResourceRules(citeWorkspace.getName(), citeWorkspace);
            assertTrue(ruleMan.getResourceRule(citeWorkspace.getName(), citeWorkspace).isEmpty());
        }
    }

    @Test
    public void testWsSecurityRulesUI() throws IOException {
        AccessDataRuleInfoManager ruleMan = new AccessDataRuleInfoManager();
        try {
            final Catalog catalog = getCatalog();
            final List<DataStoreInfo> storesInitial =
                    catalog.getStoresByWorkspace(citeWorkspace, DataStoreInfo.class);

            final NamespaceInfo citeNamespace =
                    catalog.getNamespaceByPrefix(citeWorkspace.getName());
            for (DataStoreInfo store : storesInitial) {
                assertEquals(
                        citeNamespace.getURI(), store.getConnectionParameters().get("namespace"));
            }
            DataAccessRule ruleLayer =
                    new DataAccessRule(
                            citeWorkspace.getName(), "Forests", AccessMode.READ, "ADMIN");
            DataAccessRule ruleWS =
                    new DataAccessRule(
                            citeWorkspace.getName(), DataAccessRule.ANY, AccessMode.ADMIN, "ADMIN");
            Set<DataAccessRule> news = new HashSet<>();
            news.add(ruleLayer);
            news.add(ruleWS);
            ruleMan.saveRules(new HashSet<DataAccessRule>(), news);
            assertTrue(ruleMan.getResourceRule(citeWorkspace.getName(), citeWorkspace).size() == 1);
            tester.clickLink("form:tabs:tabs-container:tabs:1:link");
            CheckBox checkboxFalse =
                    (CheckBox)
                            tester.getComponentFromLastRenderedPage(
                                    "form:tabs:panel:listContainer:rules:0:read");
            CheckBox checkboxTrue =
                    (CheckBox)
                            tester.getComponentFromLastRenderedPage(
                                    "form:tabs:panel:listContainer:rules:0:admin");
            assertFalse(checkboxFalse.getModelObject().booleanValue());
            assertTrue(checkboxTrue.getModelObject().booleanValue());

        } finally {
            ruleMan.removeAllResourceRules(citeWorkspace.getName(), citeWorkspace);
            assertTrue(ruleMan.getResourceRule(citeWorkspace.getName(), citeWorkspace).isEmpty());
        }
    }
}
