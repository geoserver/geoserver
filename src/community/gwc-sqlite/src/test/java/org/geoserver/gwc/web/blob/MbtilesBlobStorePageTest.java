/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.sqlite.MbtilesInfo;
import org.junit.Test;

/** Test for the BlobStorePage with the MBTiles blob store panel. */
public class MbtilesBlobStorePageTest extends GeoServerWicketTestSupport {

    @Test
    public void testOpeningTheBlobStoresPage() {

        // opening the blob stores page
        BlobStorePage page = new BlobStorePage();
        tester.startPage(page);
        tester.assertRenderedPage(BlobStorePage.class);

        // let's see if we have the correct components instantiated
        tester.assertComponent("selector", Form.class);
        tester.assertComponent("selector:typeOfBlobStore", DropDownChoice.class);
        tester.assertComponent("blobConfigContainer", MarkupContainer.class);

        // the blob store form should not be visible
        tester.assertInvisible("blobConfigContainer:blobStoreForm");

        // we should have two types of blob stores available (file and mbtiles)
        DropDownChoice typeOfBlobStore =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage("selector:typeOfBlobStore");
        assertEquals(2, typeOfBlobStore.getChoices().size());
        assertEquals("File BlobStore", typeOfBlobStore.getChoices().get(0).toString());
        assertEquals("MBTiles BlobStore", typeOfBlobStore.getChoices().get(1).toString());

        // let's select the file store
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "0");
        // the blob store form should be visible now
        tester.assertVisible("blobConfigContainer:blobStoreForm");
        // and the form should be the file blob store one
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);
        // let's select the mbtiles store
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");
        // the form should be the mbtiles blob store one
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", MbtilesBlobStorePanel.class);
    }

    @Test
    public void testCreatingNewBlobStore() throws ConfigurationException {

        // opening the blob stores page
        BlobStorePage page = new BlobStorePage();
        tester.startPage(page);

        // selecting the mbtiles blob store type
        executeAjaxEventBehavior("selector:typeOfBlobStore", "change", "1");

        // let's fill the blob store form with some custom values
        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        String storeId = UUID.randomUUID().toString();
        formTester.setValue("name", storeId);
        formTester.setValue("enabled", false);
        formTester.setValue(
                "blobSpecificPanel:rootDirectory:border:border_body:paramValue", "/tmp/gwc");
        formTester.setValue(
                "blobSpecificPanel:templatePath", "{grid}/{layer}/{params}/tiles-{z}.sqlite");
        formTester.setValue("blobSpecificPanel:rowRangeCount", "1500");
        formTester.setValue("blobSpecificPanel:columnRangeCount", "500");
        formTester.setValue("blobSpecificPanel:poolSize", "2000");
        formTester.setValue("blobSpecificPanel:poolReaperIntervalMs", "1000");
        formTester.setValue("blobSpecificPanel:eagerDelete", "true");
        formTester.setValue("blobSpecificPanel:useCreateTime", "true");
        formTester.setValue(
                "blobSpecificPanel:mbtilesMetadataDirectory:border:border_body:paramValue",
                "/tmp/gwc/mbtilesMetadata");
        // submit the form
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        // checking if a store with the correct options was instantiated
        MbtilesInfo configuration = findStore(storeId);
        assertThat(configuration, notNullValue());
        assertThat(configuration.getRootDirectory(), is("/tmp/gwc"));
        assertThat(configuration.getTemplatePath(), is("{grid}/{layer}/{params}/tiles-{z}.sqlite"));
        assertThat(configuration.getRowRangeCount(), is(1500L));
        assertThat(configuration.getColumnRangeCount(), is(500L));
        assertThat(configuration.getPoolSize(), is(2000L));
        assertThat(configuration.getPoolReaperIntervalMs(), is(1000L));
        assertThat(configuration.eagerDelete(), is(true));
        assertThat(configuration.useCreateTime(), is(true));
        assertThat(configuration.getMbtilesMetadataDirectory(), is("/tmp/gwc/mbtilesMetadata"));

        // removing the created store
        GWC.get().removeBlobStores(Collections.singleton(storeId));
    }

    @Test
    public void testModifyingAnExistingStore() throws Exception {

        // creating an mbtiles store (with the default values)
        MbtilesInfo originalConfiguration = new MbtilesInfo();
        originalConfiguration.setRootDirectory("/tmp/gwc");
        String storeId = UUID.randomUUID().toString();
        // the setId method has package only visibility, so we set the value by reflection
        Field id = BlobStoreInfo.class.getDeclaredField("name");
        id.setAccessible(true);
        id.set(originalConfiguration, storeId);
        // associate the store with a layer (it will be used to test store id update)
        GWC.get().addBlobStore(originalConfiguration);
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId(storeId);
        GWC.get().save(layer);

        // open the bob store page with the previously created store configuration
        BlobStorePage page = new BlobStorePage(originalConfiguration);
        tester.startPage(page);
        tester.assertVisible("blobConfigContainer:blobStoreForm");
        tester.assertComponent(
                "blobConfigContainer:blobStoreForm:blobSpecificPanel", MbtilesBlobStorePanel.class);

        // let's update some configuration values
        assertThat(findStore(storeId), notNullValue());
        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        String updatedStoreId = UUID.randomUUID().toString();
        formTester.setValue("name", updatedStoreId);
        formTester.setValue(
                "blobSpecificPanel:templatePath",
                "{grid}/{layer}/{params}/{style}/tiles-{z}.sqlite");
        // submit the changes
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "click");

        // checking if the store was correctly updated
        assertThat(findStore(storeId), nullValue());
        MbtilesInfo configuration = findStore(updatedStoreId);
        assertThat(configuration, notNullValue());
        assertThat(
                configuration.getTemplatePath(),
                is("{grid}/{layer}/{params}/{style}/tiles-{z}.sqlite"));

        // test that the store id updated was correctly propagated
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertThat(layer.getBlobStoreId(), is(updatedStoreId));

        // remove the created store
        GWC.get().removeBlobStores(Collections.singleton(updatedStoreId));
    }

    /** Helper method that finds a GWC store by is id. */
    private MbtilesInfo findStore(String storeId) {
        List<BlobStoreInfo> configurations = GWC.get().getBlobStores();
        for (BlobStoreInfo candidateConfiguration : configurations) {
            if (candidateConfiguration instanceof MbtilesInfo
                    && candidateConfiguration.getId().equals(storeId)) {
                return (MbtilesInfo) candidateConfiguration;
            }
        }
        return null;
    }
}
