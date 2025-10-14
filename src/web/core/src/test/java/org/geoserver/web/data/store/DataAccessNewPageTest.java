/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.security.impl.FileSandboxEnforcer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.junit.Test;

/**
 * Test suite for {@link DataAccessNewPage}
 *
 * @author Gabriel Roldan
 */
public class DataAccessNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    private static final String ROLE_CITE = "ROLE_CITE";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // force creation of the FileSanboxEnforcer (beans are lazy loaded in tests, and this
        // one registers itself on the catalog on creation)
        GeoServerExtensions.bean(FileSandboxEnforcer.class, applicationContext);
    }

    private AbstractDataAccessPage startPage() {
        login();
        final String dataStoreFactoryDisplayName = new PropertyDataStoreFactory().getDisplayName();

        final AbstractDataAccessPage page = new DataAccessNewPage(dataStoreFactoryDisplayName);
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    @Test
    public void testInitCreateNewDataStoreInvalidDataStoreFactoryName() {

        final String dataStoreFactoryDisplayName = "_invalid_";
        try {
            login();
            new DataAccessNewPage(dataStoreFactoryDisplayName);
            fail("Expected IAE on invalid datastore factory name");
        } catch (IllegalArgumentException e) {
            // hum.. change the assertion if the text changes in GeoserverApplication.properties...
            // but I still want to assert the reason for failure is the expected one..
            assertTrue(e.getMessage().startsWith("Can't find the factory"));
        }
    }

    /** A kind of smoke test that only asserts the page is rendered when first loaded */
    @Test
    public void testPageRendersOnLoad() {

        final PropertyDataStoreFactory dataStoreFactory = new PropertyDataStoreFactory();
        final String dataStoreFactoryDisplayName = dataStoreFactory.getDisplayName();

        startPage();

        tester.assertLabel("dataStoreForm:storeType", dataStoreFactoryDisplayName);
        tester.assertLabel("dataStoreForm:storeTypeDescription", dataStoreFactory.getDescription());

        tester.assertComponent("dataStoreForm:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testDefaultWorkspace() {

        startPage();

        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();

        tester.assertModelValue("dataStoreForm:workspacePanel:border:border_body:paramValue", defaultWs);

        WorkspaceInfo anotherWs = getCatalog().getFactory().createWorkspace();
        anotherWs.setName("anotherWs");

        getCatalog().add(anotherWs);
        getCatalog().setDefaultWorkspace(anotherWs);
        anotherWs = getCatalog().getDefaultWorkspace();

        startPage();
        tester.assertModelValue("dataStoreForm:workspacePanel:border:border_body:paramValue", anotherWs);
    }

    @Test
    public void testDefaultNamespace() {

        // final String namespacePath =
        // "dataStoreForm:parameters:1:parameterPanel:border:border_body:paramValue";
        final String namespacePath = "dataStoreForm:parametersPanel:parameters:1:parameterPanel:paramValue";

        startPage();

        NamespaceInfo defaultNs = getCatalog().getDefaultNamespace();

        tester.assertModelValue(namespacePath, defaultNs.getURI());
    }

    @Test
    public void testDataStoreParametersAreCreated() {
        startPage();
        List parametersListViewValues = Arrays.asList(new Object[] {"directory", "namespace"});
        tester.assertComponent(
                "dataStoreForm:parametersPanel:parameters", org.apache.wicket.markup.html.list.ListView.class);
        tester.assertModelValue("dataStoreForm:parametersPanel:parameters", parametersListViewValues);
    }

    /**
     * Make sure in case the DataStore has a "namespace" parameter, its value is initialized to the NameSpaceInfo one
     * that matches the workspace
     */
    @Test
    public void testInitCreateNewDataStoreSetsNamespaceParam() {
        final AbstractDataAccessPage page = startPage();

        page.get(null);
        // final NamespaceInfo assignedNamespace = (NamespaceInfo) page.parametersMap
        // .get(AbstractDataAccessPage.NAMESPACE_PROPERTY);
        // final NamespaceInfo expectedNamespace = catalog.getDefaultNamespace();
        //
        // assertEquals(expectedNamespace, assignedNamespace);

    }

    @Test
    public void testGeoPackagePage() {
        final String displayName = new GeoPkgDataStoreFactory().getDisplayName();
        login();
        final AbstractDataAccessPage page = new DataAccessNewPage(displayName);
        tester.startPage(page);

        // tester.debugComponentTrees();
        // the "database" key is the second, should be a file panel
        Component component =
                tester.getComponentFromLastRenderedPage("dataStoreForm:parametersPanel:parameters:1:parameterPanel");
        assertThat(component, instanceOf(FileParamPanel.class));
    }

    @Test
    public void testNewDataStoreSave() {
        startPage();
        FormTester ft = tester.newFormTester("dataStoreForm");

        ft.setValue("parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", "file:cdf");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "cdf2");
        ft.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(NewLayerPage.class);
        DataStoreInfo store = getCatalog().getDataStoreByName("cdf2");
        assertNotNull(store);
        assertEquals("file:cdf", store.getConnectionParameters().get("directory"));
    }

    @Test
    public void testNewDataStoreApply() {
        startPage();
        FormTester ft = tester.newFormTester("dataStoreForm");

        ft.setValue("parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", "file:cdf");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "cdf3");
        ft.submit("apply");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(DataAccessEditPage.class);
        DataStoreInfo store = getCatalog().getDataStoreByName("cdf3");
        assertNotNull(store);
        assertEquals("file:cdf", store.getConnectionParameters().get("directory"));
    }

    @Test
    public void testDisableOnConnFailureCheckbox() {
        String name = "autodisablingStore";
        startPage();
        FormTester ft = tester.newFormTester("dataStoreForm");

        ft.setValue("parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", "file:cdf");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", name);

        Component component =
                tester.getComponentFromLastRenderedPage("dataStoreForm:disableOnConnFailurePanel:paramValue");
        CheckBox checkBox = (CheckBox) component;
        assertFalse(Boolean.valueOf(checkBox.getInput()).booleanValue());

        ft.setValue("disableOnConnFailurePanel:paramValue", true);

        ft.submit("save");

        tester.assertNoErrorMessage();
        DataStoreInfo store = getCatalog().getDataStoreByName(name);
        assertNotNull(store);
        assertTrue(store.isDisableOnConnFailure());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDataStoreNewSandbox() throws Exception {
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
        startPage();
        try {
            login("cite", "pwd", ROLE_CITE);
            FormTester ft = tester.newFormTester("dataStoreForm");

            DropDownChoice<WorkspaceInfo> select =
                    (DropDownChoice<WorkspaceInfo>) tester.getComponentFromLastRenderedPage(
                            "dataStoreForm:workspacePanel:border:border_body:paramValue");
            List<? extends WorkspaceInfo> workspaces = select.getChoices();
            int citeIdx = -1;
            for (int i = 0; i < workspaces.size(); i++) {
                if (MockData.CITE_PREFIX.equals(workspaces.get(i).getName())) {
                    citeIdx = i;
                    break;
                }
            }

            // cannot save, the current location is outside of the sandbox
            String storeName = "cite2";
            String toppPath = toppFolder.getAbsolutePath();
            ft.setValue("dataStoreNamePanel:border:border_body:paramValue", storeName);
            ft.select("workspacePanel:border:border_body:paramValue", citeIdx);
            ft.setValue(
                    "parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", toppPath);
            ft.submit("save");

            List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
            assertEquals(1, messages.size());
            checkSandboxDeniedMessage(messages.get(0).toString(), toppPath);
            tester.clearFeedbackMessages();

            // the error got actually rendered
            checkSandboxDeniedMessage(tester.getLastResponseAsString(), toppPath);

            // now try within the sandbox
            String citePath = citeFolder.getAbsolutePath();
            ft = tester.newFormTester("dataStoreForm");
            ft.setValue("dataStoreNamePanel:border:border_body:paramValue", storeName);
            ft.select("workspacePanel:border:border_body:paramValue", citeIdx);
            ft.setValue(
                    "parametersPanel:parameters:0:parameterPanel:fileInput:border:border_body:paramValue", citePath);
            ft.submit("save");

            // no messages and save worked
            tester.assertNoErrorMessage();
            DataStoreInfo store = getCatalog().getDataStoreByName(storeName);
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
