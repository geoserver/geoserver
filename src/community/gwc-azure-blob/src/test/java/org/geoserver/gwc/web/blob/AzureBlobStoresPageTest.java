/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geowebcache.azure.AzureBlobStoreInfo;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.FileBlobStoreInfo;
import org.geowebcache.layer.TileLayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Modified Test for the BlobStoresPage, but now with a Azure BlobStore */
public class AzureBlobStoresPageTest extends GeoServerWicketTestSupport {

    private static final String ID_DUMMY1 = "zzz";
    private static final String ID_DUMMY2 = "yyy";

    @Before
    public void before() {
        login();
    }

    @After
    public void after() {
        logout();
    }

    public BlobStoreInfo dummyStore1() {
        FileBlobStoreInfo config = new FileBlobStoreInfo(ID_DUMMY1);
        config.setFileSystemBlockSize(1024);
        config.setBaseDirectory("/tmp");
        return config;
    }

    public BlobStoreInfo dummyStore2() throws Exception {
        AzureBlobStoreInfo config = new AzureBlobStoreInfo();
        Field id = BlobStoreInfo.class.getDeclaredField("name");
        id.setAccessible(true);
        id.set(config, ID_DUMMY2);
        config.setContainer("container");
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

        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("storesPanel");

        assertEquals(blobStores.size(), table.getDataProvider().size());
        assertTrue(getStoresFromTable(table).contains(dummy1));

        BlobStoreInfo dummy2 = dummyStore2();
        GWC.get().addBlobStore(dummy2);

        assertEquals(blobStores.size() + 1, table.getDataProvider().size());
        assertTrue(getStoresFromTable(table).contains(dummy2));

        // sort descending on type, Azure blob store should be on bottom
        tester.clickLink("storesPanel:listContainer:sortableLinks:1:header:link", true);
        tester.clickLink("storesPanel:listContainer:sortableLinks:1:header:link", true);
        assertEquals(dummy2, getStoresFromTable(table).get(1));

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

    @Test
    public void testDelete() throws Exception {
        BlobStoresPage page = new BlobStoresPage();
        tester.startPage(page);

        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("storesPanel");

        BlobStoreInfo dummy1 = dummyStore1();
        GWC.get().addBlobStore(dummy1);

        assertTrue(GWC.get().getBlobStores().contains(dummy1));

        // sort descending on id
        tester.clickLink("storesPanel:listContainer:sortableLinks:0:header:link", true);
        tester.clickLink("storesPanel:listContainer:sortableLinks:0:header:link", true);

        // select
        CheckBox selector =
                ((CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "storesPanel:listContainer:items:1:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");

        assertEquals(1, table.getSelection().size());
        assertEquals(dummy1, table.getSelection().get(0));

        // click delete
        tester.clickLink("headerPanel:removeSelected", true);

        assertFalse(GWC.get().getBlobStores().contains(dummy1));

        // with layer
        GWC.get().addBlobStore(dummy1);
        assertTrue(GWC.get().getBlobStores().contains(dummy1));
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId(ID_DUMMY1);
        assertEquals(ID_DUMMY1, layer.getBlobStoreId());
        GWC.get().save(layer);

        // sort descending on id
        tester.clickLink("storesPanel:listContainer:sortableLinks:0:header:link", true);
        tester.clickLink("storesPanel:listContainer:sortableLinks:0:header:link", true);

        // select
        // super.print(page, false, false, true);
        selector =
                ((CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "storesPanel:listContainer:items:2:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");

        // click delete
        assertEquals(1, table.getSelection().size());
        assertEquals(dummy1, table.getSelection().get(0));

        ModalWindow w =
                (ModalWindow) tester.getComponentFromLastRenderedPage("confirmDeleteDialog:dialog");
        assertFalse(w.isShown());
        tester.clickLink("headerPanel:removeSelected", true);
        assertTrue(w.isShown());

        // confirm
        GeoServerDialog dialog =
                (GeoServerDialog) tester.getComponentFromLastRenderedPage("confirmDeleteDialog");
        dialog.submit(new AjaxRequestHandler(tester.getLastRenderedPage()));

        assertFalse(GWC.get().getBlobStores().contains(dummy1));
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertNull(layer.getBlobStoreId());
    }

    public List<BlobStoreInfo> getStoresFromTable(GeoServerTablePanel table) {
        List<BlobStoreInfo> result = new ArrayList<BlobStoreInfo>();
        Iterator it = table.getDataProvider().iterator(0, table.size());
        while (it.hasNext()) {
            result.add((BlobStoreInfo) it.next());
        }
        return result;
    }
}
