/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.s3.Access;
import org.geowebcache.s3.S3BlobStoreInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the BlobStorePage with S3 BlobStore Panel
 *
 * @author Niels Charlier
 */
public class S3BlobStorePageTest extends GeoServerWicketTestSupport {

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
        assertEquals("File BlobStore", typeOfBlobStore.getChoices().get(0).toString());
        assertEquals("S3 BlobStore", typeOfBlobStore.getChoices().get(1).toString());

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "0");

        tester.assertVisible("blobConfigContainer:blobStoreForm");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);

        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", S3BlobStorePanel.class);
    }

    @Test
    public void testNew() throws ConfigurationException {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "myblobstore");
        formTester.setValue("enabled", false);
        formTester.setValue("blobSpecificPanel:bucket", "mybucket");
        formTester.setValue("blobSpecificPanel:awsAccessKey", "myaccesskey");
        formTester.setValue("blobSpecificPanel:awsSecretKey", "mysecretkey");
        formTester.select("blobSpecificPanel:accessType", 1);
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        List<BlobStoreInfo> blobStores = GWC.get().getBlobStores();
        BlobStoreInfo config = blobStores.get(0);
        assertTrue(config instanceof S3BlobStoreInfo);
        assertEquals("myblobstore", config.getName());
        assertEquals("mybucket", ((S3BlobStoreInfo) config).getBucket());
        assertEquals("myaccesskey", ((S3BlobStoreInfo) config).getAwsAccessKey());
        assertEquals("mysecretkey", ((S3BlobStoreInfo) config).getAwsSecretKey());
        assertEquals(50, ((S3BlobStoreInfo) config).getMaxConnections().intValue());
        assertEquals("PRIVATE", ((S3BlobStoreInfo) config).getAccess().toString());

        GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
    }

    @Test
    public void testNewAccessPublic() throws ConfigurationException {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "myblobstore");
        formTester.setValue("enabled", false);
        formTester.setValue("blobSpecificPanel:bucket", "mybucket");
        formTester.setValue("blobSpecificPanel:awsAccessKey", "myaccesskey");
        formTester.setValue("blobSpecificPanel:awsSecretKey", "mysecretkey");
        formTester.select("blobSpecificPanel:accessType", 0);
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        List<BlobStoreInfo> blobStores = GWC.get().getBlobStores();
        BlobStoreInfo config = blobStores.get(0);
        assertTrue(config instanceof S3BlobStoreInfo);
        assertEquals("myblobstore", config.getName());
        assertEquals("mybucket", ((S3BlobStoreInfo) config).getBucket());
        assertEquals("myaccesskey", ((S3BlobStoreInfo) config).getAwsAccessKey());
        assertEquals("mysecretkey", ((S3BlobStoreInfo) config).getAwsSecretKey());
        assertEquals(50, ((S3BlobStoreInfo) config).getMaxConnections().intValue());
        assertEquals("PUBLIC", ((S3BlobStoreInfo) config).getAccess().toString());

        GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
    }

    @Test
    public void testModify() throws Exception {
        S3BlobStoreInfo sconfig = new S3BlobStoreInfo();
        Field id = BlobStoreInfo.class.getDeclaredField("name");
        id.setAccessible(true);
        id.set(sconfig, "myblobstore");
        sconfig.setMaxConnections(50);
        sconfig.setBucket("mybucket");
        sconfig.setAwsAccessKey("myaccesskey");
        sconfig.setAwsSecretKey("mysecretkey");
        sconfig.setAccess(Access.PRIVATE);
        GWC.get().addBlobStore(sconfig);
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId("myblobstore");
        GWC.get().save(layer);

        BlobStorePage page = new BlobStorePage(sconfig);

        tester.startPage(page);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", S3BlobStorePanel.class);

        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("name", "yourblobstore");
        formTester.setValue("blobSpecificPanel:bucket", "yourbucket");
        formTester.select("blobSpecificPanel:accessType", 0);
        formTester.submit();
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        BlobStoreInfo config = GWC.get().getBlobStores().get(0);
        assertTrue(config instanceof S3BlobStoreInfo);
        assertEquals("yourblobstore", config.getId());
        assertEquals("yourbucket", ((S3BlobStoreInfo) config).getBucket());
        assertEquals("PUBLIC", ((S3BlobStoreInfo) config).getAccess().toString());

        // test updated id!
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertEquals("yourblobstore", layer.getBlobStoreId());

        GWC.get().removeBlobStores(Collections.singleton("yourblobstore"));
    }
}
