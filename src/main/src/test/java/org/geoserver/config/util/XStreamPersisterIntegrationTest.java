/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests XStreamPersister integration with other beans in the app context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class XStreamPersisterIntegrationTest extends GeoServerSystemTestSupport {

    private XStreamPersister persister;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Before
    public void setupPersister() {
        persister = new XStreamPersister();
        persister.setEncryptPasswordFields(true);
    }

    @Test
    public void testWmsStorePasswordEncryption() throws Exception {
        WMSStoreInfo wms = buildWmsStore();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(wms, out);
        Document dom = dom(new ByteArrayInputStream(out.toByteArray()));
        // print(dom);

        // check password has been encrypted
        XMLAssert.assertXpathExists("/wmsStore/password", dom);
        XMLAssert.assertXpathNotExists("/wmsStore[password = 'password']", dom);
        XMLAssert.assertXpathExists("/wmsStore[starts-with(password, 'crypt1:')]", dom);

        WMSStoreInfo loaded =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded.getPassword());
    }

    @Test
    public void testWmsStoreBackwardsCompatibility() throws Exception {
        WMSStoreInfo wms = buildWmsStore();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // save with no encryption
        persister.setEncryptPasswordFields(false);
        persister.save(wms, out);
        Document dom = dom(new ByteArrayInputStream(out.toByteArray()));
        print(dom);

        // check password has not been encrypted
        XMLAssert.assertXpathExists("/wmsStore/password", dom);
        XMLAssert.assertXpathExists("/wmsStore[password = 'password']", dom);

        // load back with a password encrypting persister, should fall back reading plain text
        // password
        persister.setEncryptPasswordFields(true);
        WMSStoreInfo loaded =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded.getPassword());

        // just to be thorough test also loading with no password encryption
        persister.setEncryptPasswordFields(false);
        WMSStoreInfo loaded2 =
                persister.load(new ByteArrayInputStream(out.toByteArray()), WMSStoreInfo.class);
        assertEquals("password", loaded2.getPassword());
    }

    private WMSStoreInfo buildWmsStore() {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        WMSStoreInfo wms = cFactory.createWebMapServer();
        wms.setName("bar");
        wms.setWorkspace(ws);
        wms.setCapabilitiesURL("http://fake.host/wms?request=GetCapabilities&service=wms");
        wms.setUsername("user");
        wms.setPassword("password");

        return wms;
    }
}
