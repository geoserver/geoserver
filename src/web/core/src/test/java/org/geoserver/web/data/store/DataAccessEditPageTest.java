/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class DataAccessEditPageTest extends GeoServerWicketTestSupport {

    private DataStoreInfo store;

    @Before
    public void init() {
        store = getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        tester.startPage(new DataAccessEditPage(store.getId()));
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(DataAccessEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("dataStoreForm:storeType", "Properties");
        tester.assertModelValue("dataStoreForm:dataStoreNamePanel:border:paramValue", "cite");
        String expectedPath = new File(getTestData().getDataDirectoryRoot(), "cite").getPath();
        tester.assertModelValue(
                "dataStoreForm:parametersPanel:parameters:0:parameterPanel:border:paramValue",
                expectedPath);
    }

    // This is disabled due to bad interactions between the submit link and the form submit
    // I need to reproduce ina stand alone test case and report to the Wicket devs
    // public void testEditName() {
    //        
    // FormTester form = tester.newFormTester("dataStoreForm");
    // prefillForm(form);
    // form.setValue("dataStoreNamePanel:border:paramValue", "citeModified");
    // form.submit();
    // tester.assertNoErrorMessage();
    // tester.clickLink("dataStoreForm:save");
    // tester.assertNoErrorMessage();
    //        
    // tester.assertRenderedPage(StorePage.class);
    // }

    @Test
    public void testNameRequired() {
        
        FormTester form = tester.newFormTester("dataStoreForm");
        form.setValue("dataStoreNamePanel:border:paramValue", null);
        form.setValue("workspacePanel:border:paramValue", "cite");
        form.submit();
        // missing click link , the validation triggers before it

        tester.debugComponentTrees();
        tester.assertRenderedPage(DataAccessEditPage.class);

        List<String> l = Lists.transform(tester.getMessages(FeedbackMessage.ERROR), 
            new Function<Serializable, String>() {
                @Override
                public String apply(Serializable input) {
                    return input.toString();
                }
        });
        assertTrue(l.contains("Field 'Data Source Name' is required."));
        //tester.assertErrorMessages(new String[] { "Field 'Data Source Name' is required." });
    }

    /**
     * Test that changing a datastore's workspace updates the datastore's "namespace" parameter as
     * well as the namespace of its previously configured resources
     * 
     * @REVISIT: this test fails on maven but is ok on eclipse...
     */
    public void _testWorkspaceSyncsUpWithNamespace() {
        final FormTester formTester = tester.newFormTester("dataStoreForm");
        print(tester.getLastRenderedPage(), true, true);
        final String wsDropdownPath = "dataStoreForm:workspacePanel:border:paramValue";
        final String namespaceParamPath = "dataStoreForm:parametersPanel:parameters:1:parameterPanel:paramValue";
        final String directoryParamPath = "dataStoreForm:parametersPanel:parameters:0:parameterPanel:border:paramValue";

        final Catalog catalog = getCatalog();
        tester.assertModelValue(wsDropdownPath, catalog.getWorkspaceByName(MockData.CITE_PREFIX));
        // tester.assertModelValue(namespaceParamPath, getCatalog().getNamespaceByPrefix(
        // MockData.CITE_PREFIX));
        tester.assertModelValue(namespaceParamPath, catalog.getNamespaceByPrefix(
                MockData.CITE_PREFIX).getURI());

        Serializable directory = store.getConnectionParameters().get("directory");
        tester.assertModelValue(directoryParamPath, directory);

        WorkspaceInfo expectedWorkspace = catalog.getWorkspaceByName(MockData.CDF_PREFIX);
        NamespaceInfo expectedNamespace = catalog.getNamespaceByPrefix(MockData.CDF_PREFIX);

        // select the fifth item in the drop down, which is the cdf workspace
        formTester.select("workspacePanel:border:paramValue", 4);
        Component wsDropDown = tester.getComponentFromLastRenderedPage(wsDropdownPath);
        tester.executeAjaxEvent(wsDropDown, "onchange");

        // final String namespaceParamPath =
        // "dataStoreForm:parameters:1:parameterPanel:border:paramValue";

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
        List<FeatureTypeInfo> resourcesByStore = catalog.getResourcesByStore(dataStore,
                FeatureTypeInfo.class);
        for (FeatureTypeInfo ft : resourcesByStore) {
            assertEquals("Namespace for " + ft.getName() + " was not updated", expectedNamespace,
                    ft.getNamespace());
        }
    }

    @Test
    public void testEditDettached() throws Exception {
        final Catalog catalog = getCatalog();
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        new CatalogBuilder(catalog).updateDataStore(ds, store);

        assertNull(ds.getId());
        
        tester.startPage(new DataAccessEditPage(ds));
        tester.assertNoErrorMessage();
    }
}
