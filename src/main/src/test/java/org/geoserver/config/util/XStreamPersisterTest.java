/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.util.XStreamPersister.CRSConverter;
import org.geoserver.config.util.XStreamPersister.SRSConverter;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.measure.Measure;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.referencing.wkt.UnformattableObjectException;
import org.geotools.util.NumberRange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import si.uom.SI;

public class XStreamPersisterTest {

    GeoServerFactory factory;
    CatalogFactory cfactory;
    XStreamPersister persister;

    @Before
    public void init() {
        factory = new GeoServerImpl().getFactory();
        persister = new XStreamPersisterFactory().createXMLPersister();
    }

    @Test
    public void testGlobal() throws Exception {
        GeoServerInfo g1 = factory.createGlobal();
        g1.setAdminPassword("foo");
        g1.setAdminUsername("bar");
        g1.getSettings().setCharset("ISO-8859-1");

        ContactInfo contact = factory.createContact();
        g1.getSettings().setContact(contact);
        contact.setAddress("123");
        contact.setAddressCity("Victoria");
        contact.setAddressCountry("Canada");
        contact.setAddressPostalCode("V1T3T8");
        contact.setAddressState("BC");
        contact.setAddressType("house");
        contact.setContactEmail("bob@acme.org");
        contact.setContactFacsimile("+1 250 123 4567");
        contact.setContactOrganization("Acme");
        contact.setContactPerson("Bob");
        contact.setContactPosition("hacker");
        contact.setContactVoice("+1 250 765 4321");

        g1.getSettings().setNumDecimals(2);
        g1.getSettings().setOnlineResource("http://acme.org");
        g1.getSettings().setProxyBaseUrl("http://proxy.acme.org");
        g1.getSettings().setSchemaBaseUrl("http://schemas.acme.org");

        g1.getSettings().setTitle("Acme's GeoServer");
        g1.setUpdateSequence(123);
        g1.getSettings().setVerbose(true);
        g1.getSettings().setVerboseExceptions(true);
        g1.getMetadata().put("one", Integer.valueOf(1));
        g1.getMetadata().put("two", Double.valueOf(2.2));

        ByteArrayOutputStream out = out();

        persister.save(g1, out);

        GeoServerInfo g2 = persister.load(in(out), GeoServerInfo.class);
        assertEquals(g1, g2);

        Document dom = dom(in(out));
        assertEquals("global", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testLogging() throws Exception {
        LoggingInfo logging = factory.createLogging();

        logging.setLevel("CRAZY_LOGGING");
        logging.setLocation("some/place/geoserver.log");
        logging.setStdOutLogging(true);

        ByteArrayOutputStream out = out();
        persister.save(logging, out);

        LoggingInfo logging2 = persister.load(in(out), LoggingInfo.class);
        assertEquals(logging, logging2);

        Document dom = dom(in(out));
        assertEquals("logging", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGobalContactDefault() throws Exception {
        GeoServerInfo g1 = factory.createGlobal();
        ContactInfo contact = factory.createContact();
        g1.getSettings().setContact(contact);

        ByteArrayOutputStream out = out();
        persister.save(g1, out);

        ByteArrayInputStream in = in(out);
        Document dom = dom(in);

        Element e = (Element) dom.getElementsByTagName("contact").item(0);
        e.removeAttribute("class");
        in = in(dom);

        GeoServerInfo g2 = persister.load(in, GeoServerInfo.class);
        assertEquals(g1, g2);
    }

    static class MyServiceInfo extends ServiceInfoImpl {

        String foo;

        String getFoo() {
            return foo;
        }

        void setFoo(String foo) {
            this.foo = foo;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof MyServiceInfo)) {
                return false;
            }

            MyServiceInfo other = (MyServiceInfo) obj;
            if (foo == null) {
                if (other.foo != null) {
                    return false;
                }
            } else {
                if (!foo.equals(other.foo)) {
                    return false;
                }
            }

            return super.equals(other);
        }
    }

    @Test
    public void testService() throws Exception {
        MyServiceInfo s1 = new MyServiceInfo();
        s1.setAbstract("my service abstract");
        s1.setAccessConstraints("no constraints");
        s1.setCiteCompliant(true);
        s1.setEnabled(true);
        s1.setFees("no fees");
        s1.setFoo("bar");
        s1.setId("id");
        s1.setMaintainer("Bob");
        s1.setMetadataLink(factory.createMetadataLink());
        s1.setName("MS");
        s1.setOnlineResource("http://acme.org?service=myservice");
        s1.setOutputStrategy("FAST");
        s1.setSchemaBaseURL("http://schemas.acme.org/");
        s1.setTitle("My Service");
        s1.setVerbose(true);

        ByteArrayOutputStream out = out();
        persister.save(s1, out);

        MyServiceInfo s2 = persister.load(in(out), MyServiceInfo.class);
        assertEquals(s1.getAbstract(), s2.getAbstract());
        assertEquals(s1.getAccessConstraints(), s2.getAccessConstraints());
        assertEquals(s1.isCiteCompliant(), s2.isCiteCompliant());
        assertEquals(s1.isEnabled(), s2.isEnabled());
        assertEquals(s1.getFees(), s2.getFees());
        assertEquals(s1.getFoo(), s2.getFoo());
        assertEquals(s1.getId(), s2.getId());
        assertEquals(s1.getMaintainer(), s2.getMaintainer());
        assertEquals(s1.getMetadataLink(), s2.getMetadataLink());
        assertEquals(s1.getName(), s2.getName());
        assertEquals(s1.getOnlineResource(), s2.getOnlineResource());
        assertEquals(s1.getOutputStrategy(), s2.getOutputStrategy());
        assertEquals(s1.getSchemaBaseURL(), s2.getSchemaBaseURL());
        assertEquals(s1.getTitle(), s2.getTitle());
        assertEquals(s1.isVerbose(), s2.isVerbose());
    }

    @Test
    public void testServiceOmitGlobal() throws Exception {
        MyServiceInfo s1 = new MyServiceInfo();
        s1.setGeoServer(new GeoServerImpl());

        ByteArrayOutputStream out = out();
        persister.save(s1, out);

        MyServiceInfo s2 = persister.load(in(out), MyServiceInfo.class);

        assertNull(s2.getGeoServer());
    }

    @Test
    public void testServiceCustomAlias() throws Exception {
        XStreamPersister p = persister = new XStreamPersisterFactory().createXMLPersister();
        p.getXStream().alias("ms", MyServiceInfo.class);

        MyServiceInfo s1 = new MyServiceInfo();

        ByteArrayOutputStream out = out();
        p.save(s1, out);

        Document dom = dom(in(out));
        assertEquals("ms", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testDataStore() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        DataStoreInfo ds1 = cFactory.createDataStore();
        ds1.setName("bar");
        ds1.setWorkspace(ws);

        ByteArrayOutputStream out = out();
        persister.save(ds1, out);

        DataStoreInfo ds2 = persister.load(in(out), DataStoreInfo.class);
        assertEquals("bar", ds2.getName());

        assertNotNull(ds2.getWorkspace());
        assertEquals("foo", ds2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("dataStore", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testDataStoreReferencedByName() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        DataStoreInfo ds1 = cFactory.createDataStore();
        ds1.setName("bar");
        ds1.setWorkspace(ws);
        catalog.detach(ds1);
        ((DataStoreInfoImpl) ds1).setId(null);

        ByteArrayOutputStream out = out();
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        persister.setReferenceByName(true);

        persister.save(ds1, out);

        DataStoreInfo ds2 = persister.load(in(out), DataStoreInfo.class);
        assertEquals("bar", ds2.getName());

        assertNotNull(ds2.getWorkspace());
        assertEquals("foo", ds2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("dataStore", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testCoverageStore() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        CoverageStoreInfo cs1 = cFactory.createCoverageStore();
        cs1.setName("bar");
        cs1.setWorkspace(ws);

        ByteArrayOutputStream out = out();
        persister.save(cs1, out);

        CoverageStoreInfo ds2 = persister.load(in(out), CoverageStoreInfo.class);
        assertEquals("bar", ds2.getName());

        assertNotNull(ds2.getWorkspace());
        assertEquals("foo", ds2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("coverageStore", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testCoverageStoreReferencedByName() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        CoverageStoreInfo cs1 = cFactory.createCoverageStore();
        cs1.setName("bar");
        cs1.setWorkspace(ws);
        catalog.detach(cs1);
        ((CoverageStoreInfoImpl) cs1).setId(null);

        ByteArrayOutputStream out = out();
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        persister.setReferenceByName(true);

        persister.save(cs1, out);

        CoverageStoreInfo ds2 = persister.load(in(out), CoverageStoreInfo.class);
        assertEquals("bar", ds2.getName());

        assertNotNull(ds2.getWorkspace());
        assertEquals("foo", ds2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("coverageStore", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testWMSStore() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        WMSStoreInfo wms1 = cFactory.createWebMapServer();
        wms1.setName("bar");
        wms1.setWorkspace(ws);
        wms1.setCapabilitiesURL("http://fake.host/wms?request=GetCapabilities&service=wms");

        ByteArrayOutputStream out = out();
        persister.save(wms1, out);

        WMSStoreInfo wms2 = persister.load(in(out), WMSStoreInfo.class);
        assertEquals("bar", wms2.getName());
        assertEquals(WMSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS, wms2.getMaxConnections());
        assertEquals(WMSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT, wms2.getConnectTimeout());
        assertEquals(WMSStoreInfoImpl.DEFAULT_READ_TIMEOUT, wms2.getReadTimeout());

        assertNotNull(wms2.getWorkspace());
        assertEquals("foo", wms2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("wmsStore", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testWMSStoreReferencedByName() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        WMSStoreInfo wms1 = cFactory.createWebMapServer();
        wms1.setName("bar");
        wms1.setWorkspace(ws);
        wms1.setCapabilitiesURL("http://fake.host/wms?request=GetCapabilities&service=wms");
        catalog.detach(wms1);
        ((WMSStoreInfoImpl) wms1).setId(null);

        ByteArrayOutputStream out = out();
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        persister.setReferenceByName(true);

        persister.save(wms1, out);

        WMSStoreInfo wms2 = persister.load(in(out), WMSStoreInfo.class);
        assertEquals("bar", wms2.getName());
        assertEquals(WMSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS, wms2.getMaxConnections());
        assertEquals(WMSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT, wms2.getConnectTimeout());
        assertEquals(WMSStoreInfoImpl.DEFAULT_READ_TIMEOUT, wms2.getReadTimeout());

        assertNotNull(wms2.getWorkspace());
        assertEquals("foo", wms2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("wmsStore", dom.getDocumentElement().getNodeName());
    }

    /**
     * Check maxConnections, connectTimeout, and readTimeout, stored as metadata properties in a
     * 2.1.3+ configuration are read back as actual properties.
     */
    @Test
    public void testWMSStoreBackwardsCompatibility() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        WMSStoreInfo wms1 = cFactory.createWebMapServer();
        wms1.setName("bar");
        wms1.setWorkspace(ws);
        wms1.setCapabilitiesURL("http://fake.host/wms?request=GetCapabilities&service=wms");
        wms1.getMetadata().put("maxConnections", Integer.valueOf(18));
        wms1.getMetadata().put("connectTimeout", Integer.valueOf(25));
        wms1.getMetadata().put("readTimeout", Integer.valueOf(78));

        ByteArrayOutputStream out = out();
        persister.save(wms1, out);

        WMSStoreInfo wms2 = persister.load(in(out), WMSStoreInfo.class);
        assertEquals("bar", wms2.getName());

        assertEquals(18, wms2.getMaxConnections());
        assertEquals(25, wms2.getConnectTimeout());
        assertEquals(78, wms2.getReadTimeout());

        assertNull(wms2.getMetadata().get("maxConnections"));
        assertNull(wms2.getMetadata().get("connectTimeout"));
        assertNull(wms2.getMetadata().get("readTimeout"));
    }

    @Test
    public void testStyle() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        StyleInfo s1 = cFactory.createStyle();
        s1.setName("foo");
        s1.setFilename("foo.sld");

        ByteArrayOutputStream out = out();
        persister.save(s1, out);

        ByteArrayInputStream in = in(out);

        StyleInfo s2 = persister.load(in, StyleInfo.class);
        assertEquals(s1, s2);

        Document dom = dom(in(out));
        assertEquals("style", dom.getDocumentElement().getNodeName());

        catalog.add(s2);
        assertNull(s2.getWorkspace());
    }

    @Test
    public void testLegacyStyle() throws Exception {
        String xml =
                "<style>\n"
                        + "  <id>StyleInfoImpl--570ae188:124761b8d78:-7fe2</id>\n"
                        + "  <name>raster</name>\n"
                        + "  <filename>raster.sld</filename>\n"
                        + "</style>";

        StyleInfo style =
                persister.load(new ByteArrayInputStream(xml.getBytes("UTF-8")), StyleInfo.class);
        assertEquals(SLDHandler.FORMAT, style.getFormat());
        assertEquals(SLDHandler.VERSION_10, style.getFormatVersion());
    }

    @Test
    public void testWorkspaceStyle() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");

        StyleInfo s1 = cFactory.createStyle();
        s1.setName("bar");
        s1.setFilename("bar.sld");
        s1.setWorkspace(ws);

        ByteArrayOutputStream out = out();
        persister.save(s1, out);

        ByteArrayInputStream in = in(out);

        StyleInfo s2 = persister.load(in, StyleInfo.class);
        assertEquals("bar", s2.getName());

        assertNotNull(s2.getWorkspace());
        assertEquals("foo", s2.getWorkspace().getId());

        Document dom = dom(in(out));
        assertEquals("style", dom.getDocumentElement().getNodeName());

        catalog.add(ws);
        catalog.add(s2);
        // Make sure the catalog resolves the workspace
        assertEquals("foo", s2.getWorkspace().getName());
    }

    @Test
    @Ignore // why do we want to xstream persist the catalog again?
    public void testCatalog() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        CoverageStoreInfo cs = cFactory.createCoverageStore();
        cs.setWorkspace(ws);
        cs.setName("bar");
        catalog.add(cs);

        StyleInfo s = cFactory.createStyle();
        s.setName("style");
        s.setFilename("style.sld");
        catalog.add(s);

        ByteArrayOutputStream out = out();
        persister.save(catalog, out);

        catalog = persister.load(in(out), Catalog.class);
        assertNotNull(catalog);

        assertEquals(1, catalog.getWorkspaces().size());
        assertNotNull(catalog.getDefaultWorkspace());
        ws = catalog.getDefaultWorkspace();
        assertEquals("foo", ws.getName());

        assertEquals(1, catalog.getNamespaces().size());
        assertNotNull(catalog.getDefaultNamespace());
        ns = catalog.getDefaultNamespace();
        assertEquals("acme", ns.getPrefix());
        assertEquals("http://acme.org", ns.getURI());

        assertEquals(1, catalog.getDataStores().size());
        ds = catalog.getDataStores().get(0);
        assertEquals("foo", ds.getName());
        assertNotNull(ds.getWorkspace());
        assertEquals(ws, ds.getWorkspace());

        assertEquals(1, catalog.getCoverageStores().size());
        cs = catalog.getCoverageStores().get(0);
        assertEquals("bar", cs.getName());
        assertEquals(ws, cs.getWorkspace());

        assertEquals(1, catalog.getStyles().size());
        s = catalog.getStyles().get(0);
        assertEquals("style", s.getName());
        assertEquals("style.sld", s.getFilename());
    }

    @Test
    public void testFeatureType() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore(ds);
        ft.setNamespace(ns);
        ft.setName("ft");
        ft.setAbstract("abstract");
        ft.setSRS("EPSG:4326");
        ft.setNativeCRS(CRS.decode("EPSG:4326"));
        ft.setLinearizationTolerance(new Measure(10, SI.METRE));

        ByteArrayOutputStream out = out();
        persister.save(ft, out);

        persister.setCatalog(catalog);
        ft = persister.load(in(out), FeatureTypeInfo.class);
        assertNotNull(ft);

        assertEquals("ft", ft.getName());
        assertEquals(ds, ft.getStore());
        assertEquals(ns, ft.getNamespace());
        assertEquals("EPSG:4326", ft.getSRS());
        assertEquals(new Measure(10, SI.METRE), ft.getLinearizationTolerance());
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), ft.getNativeCRS()));
    }

    @Test
    public void testCoverage() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        CoverageStoreInfo cs = cFactory.createCoverageStore();
        cs.setWorkspace(ws);
        cs.setName("foo");
        catalog.add(cs);

        CoverageInfo cv = cFactory.createCoverage();
        cv.setStore(cs);
        cv.setNamespace(ns);
        cv.setName("cv");
        cv.setAbstract("abstract");
        cv.setSRS("EPSG:4326");
        cv.setNativeCRS(CRS.decode("EPSG:4326"));
        cv.getParameters().put("foo", null);

        ByteArrayOutputStream out = out();
        persister.save(cv, out);

        persister.setCatalog(catalog);
        cv = persister.load(in(out), CoverageInfo.class);
        assertNotNull(cv);

        assertEquals("cv", cv.getName());
        assertEquals(cs, cv.getStore());
        assertEquals(ns, cv.getNamespace());
        assertEquals("EPSG:4326", cv.getSRS());
        assertTrue(cv.getParameters().containsKey("foo"));
        assertNull(cv.getParameters().get("foo"));
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), cv.getNativeCRS()));
    }

    @Test
    public void testWMTSLayer() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        WMTSStoreInfo wmts = cFactory.createWebMapTileServer();
        wmts.setWorkspace(ws);
        wmts.setName("foo");
        wmts.setCapabilitiesURL("http://fake.host/wmts?request=getCapabilities");
        catalog.add(wmts);

        WMTSLayerInfo wl = cFactory.createWMTSLayer();
        wl.setStore(wmts);
        wl.setNamespace(ns);
        wl.setName("wmtsLayer");
        wl.setAbstract("abstract");
        wl.setSRS("EPSG:4326");
        wl.setNativeCRS(CRS.decode("EPSG:4326"));

        ByteArrayOutputStream out = out();
        persister.save(wl, out);
        persister.setCatalog(catalog);
        wl = persister.load(in(out), WMTSLayerInfo.class);
        assertNotNull(wl);

        assertEquals("wmtsLayer", wl.getName());
        assertEquals(wmts, wl.getStore());
        assertEquals(ns, wl.getNamespace());
        assertEquals("EPSG:4326", wl.getSRS());
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), wl.getNativeCRS()));

        Document dom = dom(in(out));
        assertEquals("wmtsLayer", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testWMSLayer() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        WMSStoreInfo wms = cFactory.createWebMapServer();
        wms.setWorkspace(ws);
        wms.setName("foo");
        wms.setCapabilitiesURL("http://fake.host/wms?request=getCapabilities");
        catalog.add(wms);

        WMSLayerInfo wl = cFactory.createWMSLayer();
        wl.setStore(wms);
        wl.setNamespace(ns);
        wl.setName("wmsLayer");
        wl.setAbstract("abstract");
        wl.setSRS("EPSG:4326");
        wl.setNativeCRS(CRS.decode("EPSG:4326"));

        ByteArrayOutputStream out = out();
        persister.save(wl, out);

        // System.out.println( new String(out.toByteArray()) );

        persister.setCatalog(catalog);
        wl = persister.load(in(out), WMSLayerInfo.class);
        assertNotNull(wl);

        assertEquals("wmsLayer", wl.getName());
        assertEquals(wms, wl.getStore());
        assertEquals(ns, wl.getNamespace());
        assertEquals("EPSG:4326", wl.getSRS());
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), wl.getNativeCRS()));

        Document dom = dom(in(out));
        assertEquals("wmsLayer", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testLayer() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore(ds);
        ft.setNamespace(ns);
        ft.setName("ft");
        ft.setAbstract("abstract");
        ft.setSRS("EPSG:4326");
        ft.setNativeCRS(CRS.decode("EPSG:4326"));
        catalog.add(ft);

        StyleInfo s = cFactory.createStyle();
        s.setName("style");
        s.setFilename("style.sld");
        catalog.add(s);

        LayerInfo l = cFactory.createLayer();
        // TODO: reinstate when layer/publish slipt is actually in place
        // l.setName( "layer" );
        l.setResource(ft);
        l.setDefaultStyle(s);
        l.getStyles().add(s);
        catalog.add(l);

        ByteArrayOutputStream out = out();
        persister.save(l, out);

        persister.setCatalog(catalog);
        l = persister.load(in(out), LayerInfo.class);

        assertEquals(l.getResource().getName(), l.getName());
        assertEquals(ft, l.getResource());
        assertEquals(s, l.getDefaultStyle());
        assertNotNull(l.getStyles());
        assertEquals(1, l.getStyles().size());
        assertTrue(l.getStyles().contains(s));
    }

    @Test
    public void testLayerGroupInfo() throws Exception {
        for (LayerGroupInfo.Mode mode : LayerGroupInfo.Mode.values()) {
            testSerializationWithMode(mode);
        }
    }

    private void testSerializationWithMode(Mode mode) throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        LayerGroupInfo group1 = cFactory.createLayerGroup();
        group1.setName("foo");
        group1.setTitle("foo title");
        group1.setAbstract("foo abstract");
        group1.setMode(mode);

        ByteArrayOutputStream out = out();
        persister.save(group1, out);

        // print(in(out));

        ByteArrayInputStream in = in(out);

        LayerGroupInfo group2 = persister.load(in, LayerGroupInfo.class);
        assertEquals(group1.getName(), group2.getName());
        assertEquals(group1.getTitle(), group2.getTitle());
        assertEquals(group1.getAbstract(), group2.getAbstract());
        assertEquals(group1.getMode(), group2.getMode());

        Document dom = dom(in(out));
        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testLegacyLayerGroupWithoutMode() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<layerGroup>\n"
                        + "<name>foo</name>\n"
                        + "<title>foo title</title>\n"
                        + "<abstractTxt>foo abstract</abstractTxt>\n"
                        + "<layers>\n"
                        + "<layer>\n"
                        + "<id>LayerInfoImpl--570ae188:124761b8d78:-7fb0</id>\n"
                        + "</layer>\n"
                        + "</layers>\n"
                        + "<styles>\n"
                        + "<style/>\n"
                        + "</styles>\n"
                        + "<bounds>\n"
                        + "<minx>589425.9342365642</minx>\n"
                        + "<maxx>609518.6719560538</maxx>\n"
                        + "<miny>4913959.224611808</miny>\n"
                        + "<maxy>4928082.949945881</maxy>\n"
                        + "<crs class=\"projected\">EPSG:26713</crs>\n"
                        + "</bounds>\n"
                        + "</layerGroup>\n";

        LayerGroupInfo group =
                persister.load(new ByteArrayInputStream(xml.getBytes()), LayerGroupInfo.class);

        Assert.assertEquals(LayerGroupInfo.Mode.SINGLE, group.getMode());

        Catalog catalog = new CatalogImpl();
        Assert.assertTrue(catalog.validate(group, false).isValid());
    }

    @Test
    public void testVirtualTable() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        VirtualTable vt =
                new VirtualTable(
                        "riverReduced",
                        "select a, b, c * %mulparam% \n from table \n where x > 1 %andparam%");
        vt.addGeometryMetadatata("geom", LineString.class, 4326);
        vt.setPrimaryKeyColumns(Arrays.asList("a", "b"));
        vt.addParameter(new VirtualTableParameter("mulparam", "1", new RegexpValidator("\\d+")));
        vt.addParameter(new VirtualTableParameter("andparam", null));

        FeatureTypeInfo ft = cFactory.createFeatureType();
        ft.setStore(ds);
        ft.setNamespace(ns);
        ft.setName("ft");
        ft.setAbstract("abstract");
        ft.setSRS("EPSG:4326");
        ft.setNativeCRS(CRS.decode("EPSG:4326"));
        ft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        catalog.add(ft);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(ft, out);
        // System.out.println(out.toString());

        persister.setCatalog(catalog);
        ft = persister.load(in(out), FeatureTypeInfo.class);
        VirtualTable vt2 = (VirtualTable) ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);
        assertNotNull(vt2);
        assertEquals(vt, vt2);
    }

    /** Test for GEOS-6052 */
    @Test
    public void testVirtualTableMissingEscapeSql() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        persister.setCatalog(catalog);
        FeatureTypeInfo ft =
                persister.load(
                        getClass()
                                .getResourceAsStream(
                                        "/org/geoserver/config/virtualtable_error.xml"),
                        FeatureTypeInfo.class);
        VirtualTable vt2 = (VirtualTable) ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);
        assertNotNull(vt2);
        assertEquals(1, ft.getMetadata().size());
    }

    /** Another Test for GEOS-6052 */
    @Test
    public void testVirtualTableMissingEscapeSqlDoesntSkipElements() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        persister.setCatalog(catalog);
        FeatureTypeInfo ft =
                persister.load(
                        getClass()
                                .getResourceAsStream(
                                        "/org/geoserver/config/virtualtable_error_2.xml"),
                        FeatureTypeInfo.class);
        VirtualTable vt2 = (VirtualTable) ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);
        assertNotNull(vt2);
        assertEquals(1, ft.getMetadata().size());
        assertEquals(1, vt2.getGeometries().size());
        String geometryName = vt2.getGeometries().iterator().next();
        assertEquals("geometry", geometryName);
        assertNotNull(vt2.getGeometryType(geometryName));
        assertNotNull(vt2.getNativeSrid(geometryName));
    }

    /* Test for GEOS-8929 */
    @Test
    public void testOldJTSBindingConversion() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        DataStoreInfo ds = cFactory.createDataStore();
        ds.setWorkspace(ws);
        ds.setName("foo");
        catalog.add(ds);

        persister.setCatalog(catalog);
        FeatureTypeInfo ft =
                persister.load(
                        getClass().getResourceAsStream("/org/geoserver/config/old_jts_binding.xml"),
                        FeatureTypeInfo.class);
        assertNotNull(ft);
        assertEquals(
                org.locationtech.jts.geom.LineString.class, ft.getAttributes().get(0).getBinding());
    }

    @Test
    public void testCRSConverter() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        CRSConverter c = new CRSConverter();

        assertEquals(crs.toWKT(), c.toString(crs));
        assertEquals(DefaultGeographicCRS.WGS84.toWKT(), c.toString(DefaultGeographicCRS.WGS84));

        CoordinateReferenceSystem crs2 = (CoordinateReferenceSystem) c.fromString(crs.toWKT());
        assertTrue(CRS.equalsIgnoreMetadata(crs, crs2));

        crs2 = (CoordinateReferenceSystem) c.fromString("EPSG:4326");
        assertTrue(CRS.equalsIgnoreMetadata(crs, crs2));
    }

    @Test
    public void testSRSConverter() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4901");
        SRSConverter c = new SRSConverter();

        assertEquals("EPSG:4901", c.toString(crs));
        // definition with odd UOM that won't be matched to the EPSG one
        assertFalse(
                "EPSG:4901"
                        .equals(
                                c.toString(
                                        CRS.parseWKT(
                                                "GEOGCS[\"GCS_ATF_Paris\",DATUM[\"D_ATF\",SPHEROID[\"Plessis_1817\",6376523.0,308.64]],PRIMEM[\"Paris\",2.337229166666667],UNIT[\"Grad\",0.01570796326794897]]"))));
    }

    @Test
    public void testCRSConverterInvalidWKT() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3575");
        try {
            ((Formattable) crs).toWKT(2, true);
            fail("expected exception");
        } catch (UnformattableObjectException e) {
        }

        String wkt = null;
        try {
            wkt = new CRSConverter().toString(crs);
        } catch (UnformattableObjectException e) {
            fail("Should have thrown exception");
        }

        CoordinateReferenceSystem crs2 =
                (CoordinateReferenceSystem) new CRSConverter().fromString(wkt);
        assertTrue(CRS.equalsIgnoreMetadata(crs, crs2));
    }

    @Test
    public void testMultimapConverter() throws Exception {
        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        XStreamPersister xmlPersister = factory.createXMLPersister();
        XStream xs = xmlPersister.getXStream();

        Multimap<String, Object> mmap = ArrayListMultimap.create();
        mmap.put("one", "abc");
        mmap.put("one", Integer.valueOf(2));
        mmap.put("two", new NumberRange<Integer>(Integer.class, 10, 20));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(mmap, out);

        // print(in(out));

        Multimap mmap2 = persister.load(in(out), Multimap.class);
        assertEquals(mmap, mmap2);
    }

    @Test
    public void testPersisterCustomization() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        ws.getMetadata().put("banana", new SweetBanana("Musa acuminata"));

        XStreamPersisterFactory factory = new XStreamPersisterFactory();
        factory.addInitializer(
                new XStreamPersisterInitializer() {

                    @Override
                    public void init(XStreamPersister persister) {
                        persister.getXStream().alias("sweetBanana", SweetBanana.class);
                        persister
                                .getXStream()
                                .aliasAttribute(SweetBanana.class, "scientificName", "name");
                        persister.registerBreifMapComplexType("sweetBanana", SweetBanana.class);
                    }
                });
        XStreamPersister persister = factory.createXMLPersister();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(ws, out);

        WorkspaceInfo ws2 = persister.load(in(out), WorkspaceInfo.class);
        assertEquals(ws, ws2);

        Document dom = dom(in(out));
        // print(in(out));
        XMLAssert.assertXpathEvaluatesTo(
                "Musa acuminata",
                "/workspace/metadata/entry[@key='banana']/sweetBanana/@name",
                dom);
    }

    @Test
    public void testCoverageView() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();
        CoverageInfo coverage = cFactory.createCoverage();
        MetadataMap metadata = coverage.getMetadata();
        coverage.setName("test");
        coverage.setEnabled(true);
        coverage.getAlias().add("alias");
        coverage.getKeywords().add(new Keyword("key"));
        MetadataLinkInfoImpl metadataLink = new MetadataLinkInfoImpl();
        metadataLink.setAbout("about");
        coverage.getMetadataLinks().add(metadataLink);
        CoverageDimensionImpl coverageDimension = new CoverageDimensionImpl("time");
        coverageDimension.setNullValues(Collections.singletonList(Double.valueOf(0)));
        coverage.getDimensions().add(coverageDimension);
        coverage.getInterpolationMethods().add("Bilinear");
        coverage.getParameters().put("ParameterKey", "ParameterValue");
        coverage.getSupportedFormats().add("GEOTIFF");
        coverage.getRequestSRS().add("EPSG:4326");
        coverage.getResponseSRS().add("EPSG:4326");

        final InputCoverageBand band_u =
                new InputCoverageBand("u-component_of_current_surface", "0");
        final CoverageBand outputBand_u =
                new CoverageBand(
                        Collections.singletonList(band_u),
                        "u-component_of_current_surface@0",
                        0,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand band_v =
                new InputCoverageBand("v-component_of_current_surface", "0");
        final CoverageBand outputBand_v =
                new CoverageBand(
                        Collections.singletonList(band_v),
                        "v-component_of_current_surface@0",
                        1,
                        CompositionType.BAND_SELECT);
        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(2);
        coverageBands.add(outputBand_u);
        coverageBands.add(outputBand_v);
        CoverageView coverageView = new CoverageView("regional_currents", coverageBands);
        metadata.put("COVERAGE_VIEW", coverageView);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persister.save(coverage, out);

        CoverageInfo coverage2 = persister.load(in(out), CoverageInfo.class);
        assertEquals(coverage, coverage2);
    }

    @Test
    public void testVirtualTableOrder() throws Exception {
        FeatureTypeInfo ft =
                persister.load(
                        getClass()
                                .getResourceAsStream(
                                        "/org/geoserver/config/virtualtable_order_error.xml"),
                        FeatureTypeInfo.class);
        VirtualTable vtc = (VirtualTable) ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);
        assertEquals(vtc.getSql(), "select * from table\n");
        assertEquals(vtc.getName(), "sqlview");
    }

    @SuppressWarnings("serial")
    @Test
    public void testVirtualTableMultipleGeoms() throws IOException {
        Map<String, String> types =
                new HashMap<String, String>() {
                    {
                        put("southernmost_point", "org.locationtech.jts.geom.Geometry");
                        put("location_polygon", "org.locationtech.jts.geom.Geometry");
                        put("centroid", "org.locationtech.jts.geom.Geometry");
                        put("northernmost_point", "org.locationtech.jts.geom.Geometry");
                        put("easternmost_point", "org.locationtech.jts.geom.Geometry");
                        put("location", "org.locationtech.jts.geom.Geometry");
                        put("location_original", "org.locationtech.jts.geom.Geometry");
                        put("westernmost_point", "org.locationtech.jts.geom.Geometry");
                    }
                };

        Map<String, Integer> srids =
                new HashMap<String, Integer>() {
                    {
                        put("southernmost_point", 4326);
                        put("location_polygon", 3003);
                        put("centroid", 3004);
                        put("northernmost_point", 3857);
                        put("easternmost_point", 4326);
                        put("location", 3003);
                        put("location_original", 3004);
                        put("westernmost_point", 3857);
                    }
                };

        FeatureTypeInfo ft =
                persister.load(
                        getClass()
                                .getResourceAsStream(
                                        "/org/geoserver/config/virtualtable_error_GEOS-7400.xml"),
                        FeatureTypeInfo.class);
        VirtualTable vt3 = (VirtualTable) ft.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

        assertEquals(8, vt3.getGeometries().size());

        for (String g : vt3.getGeometries()) {
            Class<? extends Geometry> geom = vt3.getGeometryType(g);
            assertEquals(srids.get(g).intValue(), vt3.getNativeSrid(g));
            assertEquals(types.get(g), geom.getName());
        }
    }

    /**
     * Test for GEOS-7444. Check GridGeometry is correctly unmarshaled when XML elements are
     * provided on an different order than the marshaling one
     */
    @Test
    public void testGridGeometry2DConverterUnmarshalling() throws Exception {
        Catalog catalog = new CatalogImpl();
        CatalogFactory cFactory = catalog.getFactory();

        WorkspaceInfo ws = cFactory.createWorkspace();
        ws.setName("foo");
        catalog.add(ws);

        NamespaceInfo ns = cFactory.createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);

        CoverageStoreInfo cs = cFactory.createCoverageStore();
        cs.setWorkspace(ws);
        cs.setName("coveragestore");
        catalog.add(cs);

        CoverageInfo cv = cFactory.createCoverage();
        cv.setStore(cs);
        cv.setNamespace(ns);
        cv.setName("coverage");
        cv.setAbstract("abstract");
        cv.setSRS("EPSG:4326");
        cv.setNativeCRS(CRS.decode("EPSG:4326"));
        cv.getParameters().put("foo", null);

        ByteArrayOutputStream out = out();
        persister.save(cv, out);

        ByteArrayInputStream in = in(out);
        Document dom = dom(in);

        Element crs = dom.createElement("crs");
        Text t = dom.createTextNode("EPSG:4326");
        crs.appendChild(t);
        Element high = dom.createElement("high");
        t = dom.createTextNode("4029 4029");
        high.appendChild(t);
        Element low = dom.createElement("low");
        t = dom.createTextNode("0 0");
        low.appendChild(t);
        Element range = dom.createElement("range");
        range.appendChild(high);
        range.appendChild(low);

        Element translateX = dom.createElement("translateX");
        t = dom.createTextNode("0");
        translateX.appendChild(t);
        Element translateY = dom.createElement("translateY");
        t = dom.createTextNode("0");
        translateY.appendChild(t);
        Element scaleX = dom.createElement("scaleX");
        t = dom.createTextNode("1");
        scaleX.appendChild(t);
        Element scaleY = dom.createElement("scaleY");
        t = dom.createTextNode("1");
        scaleY.appendChild(t);
        Element shearX = dom.createElement("shearX");
        t = dom.createTextNode("0");
        shearX.appendChild(t);
        Element shearY = dom.createElement("shearY");
        t = dom.createTextNode("0");
        shearY.appendChild(t);

        Element transform = dom.createElement("transform");
        transform.appendChild(translateX);
        transform.appendChild(translateY);
        transform.appendChild(scaleX);
        transform.appendChild(scaleY);
        transform.appendChild(shearX);
        transform.appendChild(shearY);

        Element grid = dom.createElement("grid");
        grid.setAttribute("dimension", "2");
        grid.appendChild(crs);
        grid.appendChild(range);
        grid.appendChild(transform);

        Element e = (Element) dom.getElementsByTagName("coverage").item(0);
        Element params = (Element) dom.getElementsByTagName("parameters").item(0);
        e.insertBefore(grid, params);

        in = in(dom);

        persister.setCatalog(catalog);
        cv = persister.load(in, CoverageInfo.class);
        assertNotNull(cv);
        assertNotNull(cv.getGrid());
        assertNotNull(cv.getGrid().getGridRange());
        assertNotNull(cv.getCRS());
        assertNotNull(cv.getGrid().getGridToCRS());
        assertEquals(cv.getGrid().getGridRange().getLow(0), 0);
    }

    @Test
    public void readSettingsMetadataInvalidEntry() throws Exception {
        String xml =
                "<global>\n"
                        + "  <settings>\n"
                        + "    <metadata>\n"
                        + "      <map>\n"
                        + "        <entry>\n"
                        + "            <string>key1</string>\n"
                        + "            <string>value1</string>\n"
                        + "        </entry>\n"
                        + "        <entry>\n"
                        + "          <string>NetCDFOutput.Key</string>\n"
                        + "          <netCDFSettings>\n"
                        + "            <compressionLevel>0</compressionLevel>\n"
                        + "            <shuffle>true</shuffle>\n"
                        + "            <copyAttributes>false</copyAttributes>\n"
                        + "            <copyGlobalAttributes>false</copyGlobalAttributes>\n"
                        + "            <dataPacking>NONE</dataPacking>\n"
                        + "          </netCDFSettings>\n"
                        + "        </entry>\n"
                        + "        <entry>\n"
                        + "            <string>key2</string>\n"
                        + "            <string>value2</string>\n"
                        + "        </entry>\n"
                        + "      </map>\n"
                        + "    </metadata>\n"
                        + "    <localWorkspaceIncludesPrefix>true</localWorkspaceIncludesPrefix>\n"
                        + "  </settings>\n"
                        + "</global>\n";
        GeoServerInfo gs =
                persister.load(new ByteArrayInputStream(xml.getBytes()), GeoServerInfo.class);
        SettingsInfo settings = gs.getSettings();
        MetadataMap metadata = settings.getMetadata();
        assertEquals(2, metadata.size());
        assertThat(metadata, hasEntry("key1", "value1"));
        assertThat(metadata, hasEntry("key2", "value2"));
        assertTrue(settings.isLocalWorkspaceIncludesPrefix());

        // check it round trips the same way it came in, minus the bit we could not read
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        persister.save(gs, bos);
        // System.out.println(new String(bos.toByteArray()));
        Document doc = dom(new ByteArrayInputStream(bos.toByteArray()));
        XMLAssert.assertXpathExists("//settings/metadata/map", doc);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//settings/metadata/map/entry)", doc);
        XMLAssert.assertXpathEvaluatesTo("key1", "//settings/metadata/map/entry[1]/string[1]", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "value1", "//settings/metadata/map/entry[1]/string[2]", doc);
        XMLAssert.assertXpathEvaluatesTo("key2", "//settings/metadata/map/entry[2]/string[1]", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "value2", "//settings/metadata/map/entry[2]/string[2]", doc);
    }

    @Test
    public void readCoverageMetadataInvalidEntry() throws Exception {
        String xml =
                "<coverage>\n"
                        + "  <metadata>\n"
                        + "    <entry key=\"key1\">value1</entry>\n"
                        + "    <entry key=\"netcdf\">\n"
                        + "      <netCDFSettings>\n"
                        + "            <compressionLevel>0</compressionLevel>\n"
                        + "            <shuffle>true</shuffle>\n"
                        + "            <copyAttributes>false</copyAttributes>\n"
                        + "            <copyGlobalAttributes>false</copyGlobalAttributes>\n"
                        + "            <dataPacking>NONE</dataPacking>\n"
                        + "      </netCDFSettings>\n"
                        + "    </entry>\n"
                        + "    <entry key=\"key2\">value2</entry>\n"
                        + "  </metadata>\n"
                        + "</coverage>";
        CoverageInfo ci =
                persister.load(new ByteArrayInputStream(xml.getBytes()), CoverageInfo.class);
        MetadataMap metadata = ci.getMetadata();
        assertEquals(3, metadata.size());
        assertThat(metadata, hasEntry("key1", "value1"));
        assertThat(metadata, hasEntry("key2", "value2"));
        assertThat(metadata, hasEntry("netcdf", null));
    }

    @Test
    public void testLegacyWMSLayerInfo() throws Exception {
        // this test asserts that when expecting a legacy wmsLayer xml tag
        // the converter kicks and sets the default values to ensure integrity
        // and avoid mannually re-saving the layer from GUI
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wmsLayer>\n"
                        + "   <id>WMSLayerInfoImpl-622caab0:16ff63f5f7a:-7ffc</id>\n"
                        + "   <name>states</name>\n"
                        + "   <nativeName>topp:states</nativeName>\n"
                        + "   <namespace>\n"
                        + "      <id>NamespaceInfoImpl--570ae188:124761b8d78:-7ffc</id>\n"
                        + "   </namespace>\n"
                        + "   <title>USA Population</title>\n"
                        + "   <description>This is some census data on the states.</description>\n"
                        + "   <abstract>This is some census data on the states.</abstract>\n"
                        + "   <keywords>\n"
                        + "      <string>census</string>\n"
                        + "      <string>united</string>\n"
                        + "      <string>boundaries</string>\n"
                        + "      <string>state</string>\n"
                        + "      <string>states</string>\n"
                        + "   </keywords>\n"
                        + "   <nativeCRS>GEOGCS[\"WGS 84\", &#xD;\n"
                        + "  DATUM[\"World Geodetic System 1984\", &#xD;\n"
                        + "    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], &#xD;\n"
                        + "    AUTHORITY[\"EPSG\",\"6326\"]], &#xD;\n"
                        + "  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], &#xD;\n"
                        + "  UNIT[\"degree\", 0.017453292519943295], &#xD;\n"
                        + "  AXIS[\"Geodetic longitude\", EAST], &#xD;\n"
                        + "  AXIS[\"Geodetic latitude\", NORTH], &#xD;\n"
                        + "  AUTHORITY[\"EPSG\",\"4326\"]]</nativeCRS>\n"
                        + "   <srs>EPSG:4326</srs>\n"
                        + "   <nativeBoundingBox>\n"
                        + "      <minx>-124.73142200000001</minx>\n"
                        + "      <maxx>-66.969849</maxx>\n"
                        + "      <miny>24.955967</miny>\n"
                        + "      <maxy>49.371735</maxy>\n"
                        + "      <crs>EPSG:4326</crs>\n"
                        + "   </nativeBoundingBox>\n"
                        + "   <latLonBoundingBox>\n"
                        + "      <minx>-124.731422</minx>\n"
                        + "      <maxx>-66.969849</maxx>\n"
                        + "      <miny>24.955967</miny>\n"
                        + "      <maxy>49.371735</maxy>\n"
                        + "      <crs>EPSG:4326</crs>\n"
                        + "   </latLonBoundingBox>\n"
                        + "   <projectionPolicy>FORCE_DECLARED</projectionPolicy>\n"
                        + "   <enabled>true</enabled>\n"
                        + "   <store class=\"wmsStore\">\n"
                        + "      <id>WMSStoreInfoImpl-622caab0:16ff63f5f7a:-7fff</id>\n"
                        + "   </store>\n"
                        + "   <serviceConfiguration>false</serviceConfiguration>\n"
                        + "</wmsLayer>";

        WMSLayerInfo wmsLayerInfo =
                persister.load(new ByteArrayInputStream(xml.getBytes()), WMSLayerInfo.class);

        assertTrue(wmsLayerInfo.getPreferredFormat().equalsIgnoreCase("image/png"));
        assertTrue(wmsLayerInfo.getForcedRemoteStyle().isEmpty());
    }

    ByteArrayOutputStream out() {
        return new ByteArrayOutputStream();
    }

    ByteArrayInputStream in(ByteArrayOutputStream in) {
        return new ByteArrayInputStream(in.toByteArray());
    }

    ByteArrayInputStream in(Document dom) throws Exception {
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        tx.setOutputProperty(OutputKeys.INDENT, "yes");

        ByteArrayOutputStream out = out();
        tx.transform(new DOMSource(dom), new StreamResult(out));

        return in(out);
    }

    protected Document dom(InputStream in)
            throws ParserConfigurationException, SAXException, IOException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    }

    protected void print(InputStream in) throws Exception {
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        tx.setOutputProperty(OutputKeys.INDENT, "yes");

        tx.transform(new StreamSource(in), new StreamResult(System.out));
    }

    static class SweetBanana implements Serializable {
        String scientificName;

        public SweetBanana(String scientificName) {
            super();
            this.scientificName = scientificName;
        }
    }
}
