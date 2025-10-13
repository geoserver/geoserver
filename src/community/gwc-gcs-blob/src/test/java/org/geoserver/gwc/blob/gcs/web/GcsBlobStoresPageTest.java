/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.blob.gcs.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.blob.BlobStorePage;
import org.geoserver.gwc.web.blob.BlobStoresPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.FileBlobStoreInfo;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test for the {@link BlobStoresPage}, with a GCS BlobStore */
public class GcsBlobStoresPageTest extends GeoServerWicketTestSupport {

    private static final String ID_DUMMY1 = "zzz";
    private static final String ID_DUMMY2 = "yyy";

    @Before
    public void before() {
        login();
    }

    @After
    public void after() {
        // Clean up any blob stores that may have been created
        try {
            GWC.get().removeBlobStores(Collections.singleton(ID_DUMMY1));
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            GWC.get().removeBlobStores(Collections.singleton(ID_DUMMY2));
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        logout();
    }

    public BlobStoreInfo dummyStore1() {
        FileBlobStoreInfo config = new FileBlobStoreInfo(ID_DUMMY1);
        config.setFileSystemBlockSize(1024);
        config.setBaseDirectory("/tmp");
        return config;
    }

    public BlobStoreInfo dummyStore2() throws Exception {
        GoogleCloudStorageBlobStoreInfo config = new GoogleCloudStorageBlobStoreInfo();
        Field id = BlobStoreInfo.class.getDeclaredField("name");
        id.setAccessible(true);
        id.set(config, ID_DUMMY2);
        config.setBucket("test-bucket");
        return config;
    }

    @Test
    public void testPage() {
        BlobStoresPage page = new BlobStoresPage();

        tester.startPage(page);
        tester.assertRenderedPage(BlobStoresPage.class);

        tester.assertComponent("storesPanel", GeoServerTablePanel.class);
        tester.assertComponent("confirmDeleteDialog", GeoServerDialog.class);

        tester.assertComponent("headerPanel:addNew", AjaxLink.class);
        tester.assertComponent("headerPanel:removeSelected", AjaxLink.class);
    }

    @Test
    public void testBlobStores() throws Exception {
        BlobStoresPage page = new BlobStoresPage();

        BlobStoreInfo dummy1 = dummyStore1();
        GWC.get().addBlobStore(dummy1);

        List<BlobStoreInfo> blobStores = GWC.get().getBlobStores();

        tester.startPage(page);

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<BlobStoreInfo> table =
                (GeoServerTablePanel<BlobStoreInfo>) tester.getComponentFromLastRenderedPage("storesPanel");

        assertEquals(blobStores.size(), table.getDataProvider().size());
        assertTrue(getStoresFromTable(table).contains(dummy1));

        BlobStoreInfo dummy2 = dummyStore2();
        GWC.get().addBlobStore(dummy2);

        assertEquals(blobStores.size() + 1, table.getDataProvider().size());
        assertTrue(getStoresFromTable(table).contains(dummy2));

        // sort descending on type, GCS blob store should be on top (G > F alphabetically)
        tester.clickLink("storesPanel:listContainer:sortableLinks:1:header:link", true);
        tester.clickLink("storesPanel:listContainer:sortableLinks:1:header:link", true);
        assertEquals(dummy2, getStoresFromTable(table).get(0));

        GWC.get().removeBlobStores(Collections.singleton(ID_DUMMY1));
        GWC.get().removeBlobStores(Collections.singleton(ID_DUMMY2));
    }

    @Test
    public void testNew() {
        BlobStoresPage page = new BlobStoresPage();
        tester.startPage(page);

        tester.clickLink("headerPanel:addNew", true);

        tester.assertRenderedPage(BlobStorePage.class);
    }

    public List<BlobStoreInfo> getStoresFromTable(GeoServerTablePanel<? extends BlobStoreInfo> table) {
        List<BlobStoreInfo> result = new ArrayList<>();
        Iterator<? extends BlobStoreInfo> it = table.getDataProvider().iterator(0, table.size());
        while (it.hasNext()) {
            result.add((BlobStoreInfo) it.next());
        }
        return result;
    }
}
