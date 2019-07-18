/* (c) 2019  Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.azure.AzureBlobStoreInfo;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test for the BlobStorePage with Azure BlobStore Panel */
public class AzureBlobStorePageTest extends GeoServerWicketTestSupport {

    @Before
    public void before() {
        login();
    }

    @After
    public void after() {
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
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage("selector:typeOfBlobStore");
        assertEquals(2, typeOfBlobStore.getChoices().size());
        assertEquals("Azure BlobStore", typeOfBlobStore.getChoices().get(0).toString());
        assertEquals("File BlobStore", typeOfBlobStore.getChoices().get(1).toString());

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "0");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", AzureBlobStorePanel.class);

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
    }

    @Test
    public void testNew() throws ConfigurationException {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        // alphabetical order: Azure, File
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "0");
        print(page, true, true, true);

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "myblobstore");
        formTester.setValue("enabled", false);
        formTester.setValue("blobSpecificPanel:container", "myContainer");
        formTester.setValue("blobSpecificPanel:accountName", "myAccountName");
        formTester.setValue("blobSpecificPanel:accountKey", "myAccountKey");
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        List<BlobStoreInfo> blobStores = GWC.get().getBlobStores();
        BlobStoreInfo config = blobStores.get(0);
        assertTrue(config instanceof AzureBlobStoreInfo);
        assertEquals("myblobstore", config.getName());
        AzureBlobStoreInfo azureConfig = (AzureBlobStoreInfo) config;
        assertEquals("myContainer", azureConfig.getContainer());
        assertEquals("myAccountName", azureConfig.getAccountName());
        assertEquals("myAccountKey", azureConfig.getAccountKey());
        assertEquals(
                String.valueOf(AzureBlobStoreInfo.DEFAULT_CONNECTIONS),
                azureConfig.getMaxConnections());

        GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
    }

    @Test
    public void testModify() throws Exception {
        AzureBlobStoreInfo sconfig = new AzureBlobStoreInfo();
        Field id = BlobStoreInfo.class.getDeclaredField("name");
        id.setAccessible(true);
        id.set(sconfig, "myblobstore");
        sconfig.setMaxConnections("50");
        sconfig.setContainer("myContainer");
        sconfig.setAccountName("myAccountName");
        sconfig.setAccountKey("myAccountKey");
        GWC.get().addBlobStore(sconfig);
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId("myblobstore");
        GWC.get().save(layer);

        BlobStorePage page = new BlobStorePage(sconfig);

        tester.startPage(page);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", AzureBlobStorePanel.class);

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "yourblobstore");
        formTester.setValue("blobSpecificPanel:container", "yourContainer");
        formTester.submit();
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        BlobStoreInfo config = GWC.get().getBlobStores().get(0);
        assertTrue(config instanceof AzureBlobStoreInfo);
        assertEquals("yourblobstore", config.getId());
        AzureBlobStoreInfo azureConfig = (AzureBlobStoreInfo) config;
        assertEquals("yourContainer", azureConfig.getContainer());

        // test updated id!
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertEquals("yourblobstore", layer.getBlobStoreId());

        GWC.get().removeBlobStores(Collections.singleton("yourblobstore"));
    }
}
