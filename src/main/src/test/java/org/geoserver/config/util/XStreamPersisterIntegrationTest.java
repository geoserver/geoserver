/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
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
        GeoServerExtensionsHelper.setIsSpringContext(false);
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

    @Test
    @SuppressWarnings("deprecation")
    public void testProxyUseHeaderMigrationOnRead() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("global.xml")) {
            GeoServerInfoImpl info = persister.load(is, GeoServerInfoImpl.class);
            assertTrue(info.isUseHeadersProxyURL());
            assertTrue(info.getSettings().isUseHeadersProxyURL());
            assertNull(info.getUseHeadersProxyURLRaw());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProxyUseHeaderMigrationOnWrite() throws Exception {
        GeoServerInfoImpl info = new GeoServerInfoImpl();
        info.setUseHeadersProxyURLRaw(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        persister.save(info, bos);
        String xml = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        Document doc = XMLUnit.buildTestDocument(xml);
        // the setting has been migrated on write
        XMLAssert.assertXpathNotExists("/global/useHeadersProxyURL", doc);
        XMLAssert.assertXpathEvaluatesTo("true", "/global/settings/useHeadersProxyURL", doc);
    }
}
