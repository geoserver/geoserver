/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.security.impl.FileSandboxEnforcer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.postgresql.jdbc.SslMode;

public class DataAccessEditPageTest extends GeoServerWicketTestSupport {

    private static final String ROLE_CITE = "ROLE_CITE";
    private DataStoreInfo store;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // force creation of the FileSanboxEnforcer (beans are lazy loaded in tests, and this
        // one registers itself on the catalog on creation)
        GeoServerExtensions.bean(FileSandboxEnforcer.class, applicationContext);
    }

    @Before
    public void init() {
        login();

        DataStoreInfo dataStore = getCatalog().getDataStoreByName(MockData.CITE_PREFIX);
        if (dataStore == null) {
            // revert the cdf modified change
            Catalog cat = getCatalog();
            DataStoreInfo ds = cat.getDataStoreByName("citeModified");
            if (ds != null) {
                ds.setName(MockData.CITE_PREFIX);
                cat.save(ds);
            }
        }

        store = getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        tester.startPage(new DataAccessEditPage(store.getId()));
    }

    @Test
    public void testLoad() throws IOException {
        tester.assertRenderedPage(DataAccessEditPage.class);
        tester.assertNoErrorMessage();
        print(tester.getLastRenderedPage(), true, true);

        tester.assertLabel("dataStoreForm:storeType", "Properties");
        tester.assertModelValue("dataStoreForm:dataStoreNamePanel:border:border_body:paramValue", "cite");
        String expectedPath = new File(getTestData().getDataDirectoryRoot(), "cite").getCanonicalPath();
        tester.assertModelValue(
                "dataStoreForm:parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue",
                expectedPath);
    }

    @Test
    public void testEditName() {
        FormTester form = tester.newFormTester("dataStoreForm");
        form.setValue("dataStoreNamePanel:border:border_body:paramValue", "citeModified");
        form.submit();
        tester.assertNoErrorMessage();
        tester.clickLink("dataStoreForm:save");
        tester.assertNoErrorMessage();

        tester.assertRenderedPage(StorePage.class);

        assertNotNull(getCatalog().getDataStoreByName("citeModified"));
    }

    @Test
    public void testEditNameApply() {
        FormTester form = tester.newFormTester("dataStoreForm");
        form.setValue("dataStoreNamePanel:border:border_body:paramValue", "citeModified");
        form.submit();
        tester.assertNoErrorMessage();
        tester.clickLink("dataStoreForm:apply");
        tester.assertNoErrorMessage();

        tester.assertRenderedPage(DataAccessEditPage.class);

        assertNotNull(getCatalog().getDataStoreByName("citeModified"));
    }

    @Test
    public void testNameRequired() {

        FormTester form = tester.newFormTester("dataStoreForm");
        form.setValue("dataStoreNamePanel:border:border_body:paramValue", null);
        form.setValue("workspacePanel:border:border_body:paramValue", "cite");
        form.submit();
        // missing click link , the validation triggers before it

        tester.debugComponentTrees();
        tester.assertRenderedPage(DataAccessEditPage.class);

        List<String> l = Lists.transform(tester.getMessages(FeedbackMessage.ERROR), input -> input.toString());
        assertTrue(l.contains("Field 'Data Source Name' is required."));
        // tester.assertErrorMessages(new String[] { "Field 'Data Source Name' is required." });
    }

    /**
     * Test that changing a datastore's workspace updates the datastore's "namespace" parameter as well as the namespace
     * of its previously configured resources @REVISIT: this test fails on maven but is ok on eclipse...
     */
    @Test
    @Ignore
    public void testWorkspaceSyncsUpWithNamespace() {
        final FormTester formTester = tester.newFormTester("dataStoreForm");
        print(tester.getLastRenderedPage(), true, true);
        final String wsDropdownPath = "dataStoreForm:workspacePanel:border:border_body:paramValue";
        final String namespaceParamPath = "dataStoreForm:parametersPanel:parameters:1:parameterPanel:paramValue";
        final String directoryParamPath =
                "dataStoreForm:parametersPanel:parameters:0:parameterPanel:border:border_body:paramValue";

        final Catalog catalog = getCatalog();
        tester.assertModelValue(wsDropdownPath, catalog.getWorkspaceByName(MockData.CITE_PREFIX));
        // tester.assertModelValue(namespaceParamPath, getCatalog().getNamespaceByPrefix(
        // MockData.CITE_PREFIX));
        tester.assertModelValue(
                namespaceParamPath,
                catalog.getNamespaceByPrefix(MockData.CITE_PREFIX).getURI());

        Serializable directory = store.getConnectionParameters().get("directory");
        tester.assertModelValue(directoryParamPath, directory);

        WorkspaceInfo expectedWorkspace = catalog.getWorkspaceByName(MockData.CDF_PREFIX);
        NamespaceInfo expectedNamespace = catalog.getNamespaceByPrefix(MockData.CDF_PREFIX);

        // select the fifth item in the drop down, which is the cdf workspace
        formTester.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown = tester.getComponentFromLastRenderedPage(wsDropdownPath);
        tester.executeAjaxEvent(wsDropDown, "change");

        // final String namespaceParamPath =
        // "dataStoreForm:parameters:1:parameterPanel:border:border_body:paramValue";

        // did the workspace change?
        tester.assertModelValue(wsDropdownPath, expectedWorkspace);
        // did the namespace change accordingly?
        // tester.assertModelValue(namespaceParamPath, expectedNamespace);
        tester.assertModelValue(namespaceParamPath, expectedNamespace.getURI());
        tester.assertModelValue(directoryParamPath, directory);

        // use clickLink to simulate hitting the save button instead of calling
        // formTester.submit(). Otherwise the save action is not called at all
        // print(tester.getLastRenderedPage(), true, true);
        final boolean isAjax = true;
        tester.clickLink("dataStoreForm:save", isAjax);

        // did the save finish normally?
        tester.assertRenderedPage(StorePage.class);

        // was the namespace datastore parameter updated?
        DataStoreInfo dataStore = catalog.getDataStore(store.getId());
        Serializable namespace = dataStore.getConnectionParameters().get("namespace");
        assertEquals(expectedNamespace.getURI(), namespace);

        // was the namespace for the datastore resources updated?
        List<FeatureTypeInfo> resourcesByStore = catalog.getResourcesByStore(dataStore, FeatureTypeInfo.class);
        for (FeatureTypeInfo ft : resourcesByStore) {
            assertEquals("Namespace for " + ft.getName() + " was not updated", expectedNamespace, ft.getNamespace());
        }
    }

    @Test
    public void testEditDettached() throws Exception {
        final Catalog catalog = getCatalog();
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        new CatalogBuilder(catalog).updateDataStore(ds, store);

        assertNull(ds.getId());

        try {
            tester.startPage(new DataAccessEditPage(ds));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("dataStoreForm");
            form.select("workspacePanel:border:border_body:paramValue", 4);
            Component wsDropDown = tester.getComponentFromLastRenderedPage(
                    "dataStoreForm:workspacePanel:border:border_body:paramValue");
            tester.executeAjaxEvent(wsDropDown, "change");
            form.setValue("dataStoreNamePanel:border:border_body:paramValue", "foo");
            form.setValue(
                    "parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", "/foo");
            tester.clickLink("dataStoreForm:save", true);
            tester.assertNoErrorMessage();
            catalog.save(ds);

            assertNotNull(ds.getId());
            assertEquals("foo", ds.getName());
        } finally {
            catalog.remove(ds);
        }
    }

    @Test
    public void testDataStoreEdit() throws Exception {
        final Catalog catalog = getCatalog();
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        new CatalogBuilder(catalog).updateDataStore(ds, store);

        assertNull(ds.getId());

        try {
            tester.startPage(new DataAccessEditPage(ds));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("dataStoreForm");
            form.select("workspacePanel:border:border_body:paramValue", 4);
            Component wsDropDown = tester.getComponentFromLastRenderedPage(
                    "dataStoreForm:workspacePanel:border:border_body:paramValue");
            tester.executeAjaxEvent(wsDropDown, "change");
            form.setValue("dataStoreNamePanel:border:border_body:paramValue", "foo");
            form.setValue(
                    "parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", "/foo");
            tester.clickLink("dataStoreForm:save", true);
            tester.assertNoErrorMessage();
            catalog.save(ds);

            assertNotNull(ds.getId());

            DataStoreInfo expandedStore = catalog.getResourcePool().clone(ds, true);

            assertNotNull(expandedStore.getId());
            assertNotNull(expandedStore.getCatalog());

            catalog.validate(expandedStore, false).throwIfInvalid();
        } finally {
            catalog.remove(ds);
        }
    }

    @Test
    public void testDataStoreEditEnum() throws Exception {
        final Catalog catalog = getCatalog();
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setType("PostGIS");
        ds.getConnectionParameters().put(PostgisNGDataStoreFactory.SSL_MODE.key, "DISABLE");
        new CatalogBuilder(catalog).updateDataStore(ds, store);

        assertNull(ds.getId());

        tester.startPage(new DataAccessEditPage(ds));
        tester.assertNoErrorMessage();
        print(tester.getLastRenderedPage(), true, true);

        // look for the dropdown.. we cannot "identify" it but we can check there is a dropdown
        // with the properly converted enum value
        MarkupContainer container =
                (MarkupContainer) tester.getLastRenderedPage().get("dataStoreForm:parametersPanel:parameters");
        DropDownChoiceParamPanel dropDown = null;
        for (Component component : container) {
            if (component instanceof ListItem && component.get("parameterPanel") instanceof DropDownChoiceParamPanel) {
                DropDownChoiceParamPanel panel = (DropDownChoiceParamPanel) component.get("parameterPanel");
                if (panel.getDefaultModelObject() == SslMode.DISABLE) {
                    dropDown = panel;
                }
            }
        }
        assertNotNull(dropDown);
    }

    @Test
    public void testDataStoreEditSandbox() throws Exception {
        // setup sandbox on file system
        java.io.File sandbox = new java.io.File("./target/sandbox").getCanonicalFile();
        java.io.File citeFolder = new java.io.File(sandbox, MockData.CITE_PREFIX);
        java.io.File toppFolder = new java.io.File(sandbox, "topp"); // this won't be allowed
        citeFolder.mkdirs();
        toppFolder.mkdirs();

        // no need to have test data, the property data store can use an empty folder

        // setup a sandbox by security config
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("filesystemSandbox", sandbox.getAbsolutePath());
        properties.put("cite.*.a", ROLE_CITE);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
        }
        DefaultFileAccessManager fam = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
        fam.reload();

        // login as workspace admin (logout happens as @After in base class)
        login("cite", "pwd", ROLE_CITE);
        try {
            tester.startPage(new DataAccessEditPage(store.getId()));

            // cannot save, the current location is outside of the sanbox
            FormTester form = tester.newFormTester("dataStoreForm");
            String toppPath = toppFolder.getAbsolutePath();
            String fileInputPath =
                    "parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue";
            form.setValue(fileInputPath, toppPath);
            form.submit();
            tester.clickLink("dataStoreForm:save", true);

            List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
            assertEquals(1, messages.size());
            checkSandboxDeniedMessage(messages.get(0).toString(), toppPath);
            tester.clearFeedbackMessages();

            // the error got actually rendered
            checkSandboxDeniedMessage(tester.getLastResponseAsString(), toppPath);

            // now try within the sandbox
            form = tester.newFormTester("dataStoreForm");
            String citePath = citeFolder.getAbsolutePath();
            form.setValue(fileInputPath, citePath);
            form.submit();
            tester.clickLink("dataStoreForm:save", true);

            // no messages and save worked
            tester.assertNoErrorMessage();
            DataStoreInfo store = getCatalog().getDataStoreByName(MockData.CITE_PREFIX);
            assertEquals(
                    "file://" + citePath.replace("\\", "/"),
                    store.getConnectionParameters().get("directory"));
        } finally {
            layerSecurity.delete();
            fam.reload();
        }
    }

    private static void checkSandboxDeniedMessage(String message, String toppPath) {
        assertThat(
                message,
                allOf(
                        containsString("Access to "),
                        containsString(toppPath),
                        containsString(" denied by file sandboxing")));
    }
}
