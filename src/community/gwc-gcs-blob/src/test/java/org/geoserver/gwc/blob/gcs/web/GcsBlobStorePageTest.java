/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.blob.gcs.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.blob.BlobStorePage;
import org.geoserver.gwc.web.blob.FileBlobStorePanel;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.blobstore.gcs.GoogleCloudStorageBlobStoreInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test for the {@link BlobStorePage} with {@link GcsBlobStorePanel} */
public class GcsBlobStorePageTest extends GeoServerWicketTestSupport {

    @Before
    public void before() {
        login();
    }

    @After
    public void after() {
        // Clean up any blob stores that may have been created
        try {
            GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        try {
            GWC.get().removeBlobStores(Collections.singleton("yourblobstore"));
        } catch (Exception e) {
            // Ignore if doesn't exist
        }
        logout();
    }

    @Test
    public void testPage() {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        tester.assertRenderedPage(BlobStorePage.class);

        tester.assertComponent("selector", Form.class);
        tester.assertComponent("selector:typeOfBlobStore", DropDownChoice.class);
        tester.assertComponent("blobConfigContainer", MarkupContainer.class);

        tester.assertInvisible("blobConfigContainer:blobStoreForm");

        DropDownChoice typeOfBlobStore =
                (DropDownChoice) tester.getComponentFromLastRenderedPage("selector:typeOfBlobStore");
        assertEquals(2, typeOfBlobStore.getChoices().size());
        assertEquals("File BlobStore", typeOfBlobStore.getChoices().get(0).toString());
        assertEquals(
                "Google Cloud Storage BlobStore",
                typeOfBlobStore.getChoices().get(1).toString());

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "0");
        tester.assertComponent("blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");
        tester.assertComponent("blobConfigContainer:blobStoreForm:blobSpecificPanel", GcsBlobStorePanel.class);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
    }

    @Test
    public void testNew() throws ConfigurationException {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        // alphabetical order: File, Google Cloud Storage
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "myblobstore");
        formTester.setValue("enabled", false);
        formTester.setValue("blobSpecificPanel:bucket", "myBucket");
        formTester.setValue("blobSpecificPanel:prefix", "test");
        formTester.setValue("blobSpecificPanel:projectId", "myProject");
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        List<BlobStoreInfo> blobStores = GWC.get().getBlobStores();
        BlobStoreInfo config = blobStores.get(0);
        assertTrue(config instanceof GoogleCloudStorageBlobStoreInfo);
        assertEquals("myblobstore", config.getName());
        GoogleCloudStorageBlobStoreInfo gcsConfig = (GoogleCloudStorageBlobStoreInfo) config;
        assertEquals("myBucket", gcsConfig.getBucket());
        assertEquals("test", gcsConfig.getPrefix());
        assertEquals("myProject", gcsConfig.getProjectId());

        GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
    }

    @Test
    public void testModify() throws Exception {
        GoogleCloudStorageBlobStoreInfo sconfig = new GoogleCloudStorageBlobStoreInfo();
        sconfig.setEnabled(false);
        sconfig.setName("myblobstore");
        sconfig.setBucket("myBucket");
        sconfig.setPrefix("test");
        sconfig.setProjectId("myProject");
        GWC.get().addBlobStore(sconfig);
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId("myblobstore");
        GWC.get().save(layer);

        BlobStorePage page = new BlobStorePage(sconfig);

        tester.startPage(page);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
        tester.assertComponent("blobConfigContainer:blobStoreForm:blobSpecificPanel", GcsBlobStorePanel.class);

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "yourblobstore");
        formTester.setValue("blobSpecificPanel:bucket", "yourBucket");
        formTester.submit();
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        BlobStoreInfo config = GWC.get().getBlobStores().get(0);
        assertTrue(config instanceof GoogleCloudStorageBlobStoreInfo);
        assertEquals("yourblobstore", config.getId());
        GoogleCloudStorageBlobStoreInfo gcsConfig = (GoogleCloudStorageBlobStoreInfo) config;
        assertEquals("yourBucket", gcsConfig.getBucket());

        // test updated id!
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertEquals("yourblobstore", layer.getBlobStoreId());

        GWC.get().removeBlobStores(Collections.singleton("yourblobstore"));
    }
}
