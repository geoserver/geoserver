/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class CoverageStoreEditPageTest extends GeoServerWicketTestSupport {

    CoverageStoreInfo coverageStore;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void init() throws IOException {
        login();

        coverageStore =
                getCatalog()
                        .getStoreByName(
                                MockData.TASMANIA_BM.getLocalPart(), CoverageStoreInfo.class);
        if (coverageStore == null) {
            // revert the bluemable modified change
            Catalog cat = getCatalog();
            CoverageStoreInfo c = cat.getCoverageStoreByName("BlueMarbleModified");
            if (c != null) {
                c.setName("BlueMarble");
                cat.save(c);
            }
            coverageStore =
                    getCatalog()
                            .getStoreByName(
                                    MockData.TASMANIA_BM.getLocalPart(), CoverageStoreInfo.class);
        }
        tester.startPage(new CoverageStoreEditPage(coverageStore.getId()));
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("rasterStoreForm:storeType", "GeoTIFF");
        tester.assertModelValue(
                "rasterStoreForm:namePanel:border:border_body:paramValue", "BlueMarble");
    }

    @Test
    public void testChangeName() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:border_body:paramValue", "BlueMarbleModified");
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StorePage.class);
        assertNotNull(getCatalog().getStoreByName("BlueMarbleModified", CoverageStoreInfo.class));
    }

    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:border_body:paramValue", null);
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Data Source Name' is required."});
    }

    /**
     * Test that changing a datastore's workspace updates the datastore's "namespace" parameter as
     * well as the namespace of its previously configured resources
     */
    @Test
    public void testWorkspaceSyncsUpWithNamespace() {
        final Catalog catalog = getCatalog();

        final FormTester formTester = tester.newFormTester("rasterStoreForm");

        final String wsDropdownPath =
                "rasterStoreForm:workspacePanel:border:border_body:paramValue";

        tester.assertModelValue(wsDropdownPath, catalog.getWorkspaceByName(MockData.WCS_PREFIX));

        // select the fifth item in the drop down, which is the cdf workspace
        formTester.select("workspacePanel:border:border_body:paramValue", 2);

        // weird on this test I need to both call form.submit() and also simulate clicking on the
        // ajax "save" link for the model to be updated. On a running geoserver instance it works ok
        // though
        formTester.submit();

        final boolean isAjax = true;
        tester.clickLink("rasterStoreForm:save", isAjax);

        // did the save finish normally?
        tester.assertRenderedPage(StorePage.class);

        CoverageStoreInfo store = catalog.getCoverageStore(coverageStore.getId());
        WorkspaceInfo workspace = store.getWorkspace();
        assertFalse(MockData.WCS_PREFIX.equals(workspace.getName()));

        // was the namespace for the datastore resources updated?
        List<CoverageInfo> resourcesByStore;
        resourcesByStore = catalog.getResourcesByStore(store, CoverageInfo.class);

        assertTrue(resourcesByStore.size() > 0);

        for (CoverageInfo cv : resourcesByStore) {
            assertEquals(
                    "Namespace for " + cv.getName() + " was not updated",
                    workspace.getName(),
                    cv.getNamespace().getPrefix());
        }
    }

    @Test
    public void testEditDetached() throws Exception {
        final Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        new CatalogBuilder(catalog).updateCoverageStore(store, coverageStore);
        assertNull(store.getId());

        try {
            tester.startPage(new CoverageStoreEditPage(store));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("rasterStoreForm");
            form.setValue("namePanel:border:border_body:paramValue", "foo");
            form.submit();
            tester.clickLink("rasterStoreForm:save");
            tester.assertNoErrorMessage();

            assertNotNull(store.getId());
            assertEquals("foo", store.getName());
            assertNotNull(catalog.getStoreByName(coverageStore.getName(), CoverageStoreInfo.class));
            assertNotNull(catalog.getStoreByName("foo", CoverageStoreInfo.class));
        } finally {
            catalog.remove(store);
        }
    }

    @Test
    public void testCoverageStoreEdit() throws Exception {
        final Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        new CatalogBuilder(catalog).updateCoverageStore(store, coverageStore);
        assertNull(store.getId());

        try {
            tester.startPage(new CoverageStoreEditPage(store));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("rasterStoreForm");
            form.setValue("namePanel:border:border_body:paramValue", "foo");
            form.submit();
            tester.clickLink("rasterStoreForm:save");
            tester.assertNoErrorMessage();

            assertNotNull(store.getId());

            CoverageStoreInfo expandedStore = catalog.getResourcePool().clone(store, true);

            assertNotNull(expandedStore.getId());
            assertNotNull(expandedStore.getCatalog());

            catalog.validate(expandedStore, false).throwIfInvalid();
        } finally {
            catalog.remove(store);
        }
    }
}
