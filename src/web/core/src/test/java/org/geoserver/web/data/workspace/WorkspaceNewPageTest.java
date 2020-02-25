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
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.security.AccessDataRuleInfoManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class WorkspaceNewPageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(WorkspaceNewPage.class);
        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();

        tester.assertComponent("form:tabs:panel:name", TextField.class);
        tester.assertComponent("form:tabs:panel:uri", TextField.class);
    }

    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "test");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }

    @Test
    public void testValid() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "abc");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.setValue("tabs:panel:default", "true");
        form.submit("submit");
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        assertEquals("abc", getCatalog().getDefaultWorkspace().getName());
    }

    @Test
    public void testInvalidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "def");
        form.setValue("tabs:panel:uri", "not a valid uri");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Invalid URI syntax: not a valid uri"});
    }

    @Test
    public void testInvalidName() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "default");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {"Invalid workspace name: \"default\" is a reserved keyword"});
    }

    @Test
    public void testDuplicateURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "def");
        form.setValue("tabs:panel:uri", MockTestData.CITE_URI);
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {
                    "Namespace with URI '" + MockTestData.CITE_URI + "' already exists."
                });

        // Make sure the workspace doesn't get added if the namespace fails
        assertNull(getCatalog().getWorkspaceByName("def"));
        assertNull(getCatalog().getNamespaceByPrefix("def"));
    }

    @Test
    public void testDuplicateName() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", MockTestData.CITE_PREFIX);
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {
                    "Workspace named '" + MockTestData.CITE_PREFIX + "' already exists."
                });
    }

    @Test
    public void addIsolatedWorkspacesWithSameNameSpace() {
        Catalog catalog = getCatalog();
        // create the first workspace
        createWorkspace("test_a", "http://www.test.org", false);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // check that the correct objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_a"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_a").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByPrefix("test_a"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_a").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByURI("http://www.test.org"), notNullValue());
        // try to create non isolated workspace with the same namespace
        createWorkspace("test_b", "http://www.test.org", false);
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {"Namespace with URI 'http://www.test.org' already exists."});
        // check that no objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_b"), nullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b"), nullValue());
        assertThat(catalog.getNamespaceByURI("http://www.test.org"), notNullValue());
        assertThat(catalog.getNamespaceByURI("http://www.test.org").getPrefix(), is("test_a"));
        // create isolated workspace with the same namespace
        createWorkspace("test_b", "http://www.test.org", true);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // check that no objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_b"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_b").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_b"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_b").getURI(), is("http://www.test.org"));
        assertThat(catalog.getNamespaceByURI("http://www.test.org").getPrefix(), is("test_a"));
        assertThat(catalog.getNamespaceByURI("http://www.test.org").isIsolated(), is(false));
    }

    /**
     * Helper method that submits a new workspace using the provided parameters.
     *
     * @param name workspace name
     * @param namespace workspace namespace URI
     * @param isolated TRUE if the workspace should be isolated, otherwise false
     */
    private void createWorkspace(String name, String namespace, boolean isolated) {
        // make sure the form is initiated
        init();
        // get the workspace creation form
        FormTester form = tester.newFormTester("form");
        // fill the form with the provided values
        form.setValue("tabs:panel:name", name);
        form.setValue("tabs:panel:uri", namespace);
        form.setValue("tabs:panel:isolated", isolated);
        // submit the form
        form.submit("submit");
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
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testCreateWsWithAccessRules() throws IOException {
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        WorkspaceInfo wsInfo = null;
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "cba");
        form.setValue("tabs:panel:uri", "http://www.geoserver2.org");
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form.setValue("tabs:panel:listContainer:rules:0:admin", true);
        form.submit("submit");
        tester.assertNoErrorMessage();
        wsInfo = getCatalog().getWorkspaceByName("cba");
        assertEquals("cba", wsInfo.getName());
        assertTrue(manager.getResourceRule(wsInfo.getName(), wsInfo).size() == 1);
    }

    @Test
    public void testSecurityTabInactiveWithNoDeafaultAccessManager() {
        TestResourceAccessManager manager = new TestResourceAccessManager();
        SecureCatalogImpl oldSc = (SecureCatalogImpl) GeoServerExtensions.bean("secureCatalog");
        SecureCatalogImpl sc =
                new SecureCatalogImpl(getCatalog(), manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        applicationContext.getBeanFactory().destroyBean("secureCatalog");
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);
        tester.startPage(WorkspaceNewPage.class);
        try {
            tester.newFormTester("form");
            TabbedPanel tabs = (TabbedPanel) tester.getComponentFromLastRenderedPage("form:tabs");
            assertTrue(tabs.getTabs().size() == 1);
        } finally {
            applicationContext.getBeanFactory().destroyBean("secureCatalog");
            GeoServerExtensionsHelper.clear();
            GeoServerExtensionsHelper.singleton("secureCatalog", oldSc, SecureCatalogImpl.class);
        }
    }
}
