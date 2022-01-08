/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import net.sf.json.JSONObject;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.notification.common.Bounds;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.geonode.kombu.KombuCoverageInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerGroupInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerInfo;
import org.geoserver.notification.geonode.kombu.KombuLayerSimpleInfo;
import org.geoserver.notification.geonode.kombu.KombuMessage;
import org.geoserver.notification.geonode.kombu.KombuStoreInfo;
import org.geoserver.notification.geonode.kombu.KombuWMSLayerInfo;
import org.geoserver.notification.support.BrokerManager;
import org.geoserver.notification.support.Receiver;
import org.geoserver.notification.support.ReceiverService;
import org.geoserver.notification.support.Utils;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

public class IntegrationTest extends CatalogRESTTestSupport {

    private static BrokerManager brokerStarter;

    private static Receiver rc;

    @BeforeClass
    public static void startup() throws Exception {
        brokerStarter = new BrokerManager();
        brokerStarter.startBroker(false);
        rc = new Receiver("guest", "guest");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        brokerStarter.stopBroker();
    }

    @After
    public void before() throws Exception {
        if (rc != null) {
            rc.close();
        }
    }

    public void addStatesWmsLayer() throws Exception {
        WMSLayerInfo wml = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMSLayer();
            wml.setName("states");
            wml.setNativeName("topp:states");
            wml.setStore(catalog.getStoreByName("demo", WMSStoreInfo.class));
            wml.setCatalog(catalog);
            wml.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wml.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wml.setNativeCRS(wgs84);
            wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

            catalog.add(wml);
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        new File(testData.getDataDirectoryRoot(), "notifier").mkdir();
        testData.copyTo(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(NotifierInitializer.PROPERTYFILENAME),
                "notifier/" + NotifierInitializer.PROPERTYFILENAME);
    }

    /*
     * @Override protected void onSetUp(SystemTestData testData) throws Exception { super.onSetUp(testData);
     *
     * }
     */

    @Test
    public void catalogAddNamespaces() throws Exception {
        ReceiverService service = new ReceiverService(2);
        rc.receive(service);
        String json = "{'namespace':{ 'prefix':'foo', 'uri':'http://foo.com' }}";
        postAsServletResponse("/rest/namespaces", json, "text/json");
        List<byte[]> ret = service.getMessages();

        assertEquals(2, ret.size());
        KombuMessage nsMsg = Utils.toKombu(ret.get(0));
        assertEquals(Notification.Action.Add.name(), nsMsg.getAction());
        assertEquals("Catalog", nsMsg.getType());
        assertEquals("NamespaceInfo", nsMsg.getSource().getType());
        KombuMessage wsMsg = Utils.toKombu(ret.get(1));
        assertEquals("Catalog", wsMsg.getType());
        assertEquals("WorkspaceInfo", wsMsg.getSource().getType());
    }

    @Test
    public void catalogChangeLayerStyle() throws Exception {
        ReceiverService service = new ReceiverService(1);
        rc.receive(service);
        LayerInfo l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Buildings", l.getDefaultStyle().getName());
        JSONObject json = (JSONObject) getAsJSON("/rest/layers/cite:Buildings.json");
        JSONObject layer = (JSONObject) json.get("layer");
        JSONObject style = (JSONObject) layer.get("defaultStyle");
        style.put("name", "polygon");
        style.put("href", "http://localhost:8080/geoserver/rest/styles/polygon.json");
        String updatedJson = json.toString();
        putAsServletResponse("/rest/layers/cite:Buildings", updatedJson, "application/json");
        List<byte[]> ret = service.getMessages();
        assertEquals(1, ret.size());
        KombuMessage nsMsg = Utils.toKombu(ret.get(0));
        assertEquals(Notification.Action.Update.name(), nsMsg.getAction());
        assertEquals("Catalog", nsMsg.getType());
        KombuLayerInfo source = (KombuLayerInfo) nsMsg.getSource();
        assertEquals("LayerInfo", source.getType());
        assertEquals("polygon", source.getDefaultStyle());
    }

    @Test
    public void catalogChangeLayerStyles() throws Exception {
        ReceiverService service = new ReceiverService(1);
        rc.receive(service);
        String xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";
        postAsServletResponse("/rest/workspaces/cite/styles", xml);
        xml =
                "<layer>"
                        + "<styles>"
                        + "<style>"
                        + "<name>foo</name>"
                        + "<workspace>cite</workspace>"
                        + "</style>"
                        + "</styles>"
                        + "<enabled>true</enabled>"
                        + "</layer>";
        putAsServletResponse("/rest/layers/cite:Buildings", xml, "application/xml");
        List<byte[]> ret = service.getMessages();
        assertEquals(1, ret.size());
        KombuMessage updateMsg = Utils.toKombu(ret.get(0));
        assertEquals("Catalog", updateMsg.getType());
        assertEquals(Notification.Action.Update.name(), updateMsg.getAction());
        KombuLayerInfo source = (KombuLayerInfo) updateMsg.getSource();
        assertEquals("LayerInfo", source.getType());
        assertEquals("foo", source.getStyles());
    }

    @Test
    public void catalogAddAndDeleteWMSLayer() throws Exception {
        ReceiverService service = new ReceiverService(3);
        rc.receive(service);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        catalog.add(wms);
        addStatesWmsLayer();
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        deleteAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers/states");
        List<byte[]> ret = service.getMessages();
        assertEquals(3, ret.size());
        KombuMessage addStrMsg = Utils.toKombu(ret.get(0));
        assertEquals(Notification.Action.Add.name(), addStrMsg.getAction());
        KombuStoreInfo source1 = (KombuStoreInfo) addStrMsg.getSource();
        assertEquals("StoreInfo", source1.getType());
        KombuMessage addLayerMsg = Utils.toKombu(ret.get(1));
        assertEquals(Notification.Action.Add.name(), addLayerMsg.getAction());
        KombuWMSLayerInfo source2 = (KombuWMSLayerInfo) addLayerMsg.getSource();
        assertEquals("WMSLayerInfo", source2.getType());
        KombuMessage deleteMsg = Utils.toKombu(ret.get(2));
        assertEquals("Catalog", deleteMsg.getType());
        assertEquals(Notification.Action.Remove.name(), deleteMsg.getAction());
        KombuWMSLayerInfo source3 = (KombuWMSLayerInfo) deleteMsg.getSource();
        assertEquals("WMSLayerInfo", source3.getType());
        assertEquals("states", source3.getName());
        catalog.remove(wms);
    }

    @Test
    public void cpuLoadTest() throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        threadMXBean.setThreadCpuTimeEnabled(true);
        ReceiverService service = new ReceiverService(3);
        rc.receive(service);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        try {
            catalog.add(wms);
        } catch (Exception e) {
        }
        addStatesWmsLayer();
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        deleteAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers/states");
        List<byte[]> ret = service.getMessages();
        assertEquals(3, ret.size());
        Thread.sleep(1000);
        ThreadInfo[] threadInfo = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds());
        for (ThreadInfo threadInfo2 : threadInfo) {
            if (threadInfo2.getThreadName().equals(NotifierInitializer.THREAD_NAME)) {
                long blockedTime = threadInfo2.getBlockedTime();
                long waitedTime = threadInfo2.getWaitedTime();
                long cpuTime = threadMXBean.getThreadCpuTime(threadInfo2.getThreadId());
                long userTime = threadMXBean.getThreadUserTime(threadInfo2.getThreadId());
                String msg =
                        String.format(
                                "%s: %d ms cpu time, %d ms user time, blocked for %d ms, waited %d ms",
                                threadInfo2.getThreadName(),
                                cpuTime / 1000000,
                                userTime / 1000000,
                                blockedTime,
                                waitedTime);
                System.out.println(msg);
                assertTrue(waitedTime > 0);
                break;
            }
        }
        catalog.remove(wms);
    }

    @Test
    public void catalogAddCoverage() throws Exception {
        ReceiverService service = new ReceiverService(4);
        rc.receive(service);

        addCoverageStore();

        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix("bar");
        ns.setURI("http://bar");
        catalog.add(ns);

        CoverageInfo ft = catalog.getFactory().createCoverage();
        ft.setName("foo");
        ft.setNamespace(ns);
        ft.setStore(catalog.getCoverageStoreByName("acme", "foostore"));
        catalog.add(ft);

        List<byte[]> ret = service.getMessages();
        assertEquals(4, ret.size());

        KombuMessage coverageMsg = Utils.toKombu(ret.get(3));
        assertEquals("Catalog", coverageMsg.getType());
        assertEquals(Notification.Action.Add.name(), coverageMsg.getAction());
        KombuCoverageInfo source = (KombuCoverageInfo) coverageMsg.getSource();
        assertEquals("CoverageInfo", source.getType());
        assertEquals(ft.getName(), source.getName());
    }

    @Test
    public void catalogAddLayerGroup() throws Exception {
        ReceiverService service = new ReceiverService(1);
        rc.receive(service);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("sfLayerGroup");
        LayerInfo l = catalog.getLayerByName("cite:Buildings");
        lg.getLayers().add(l);
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POLYGON));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        List<byte[]> ret = service.getMessages();
        assertEquals(1, ret.size());
        KombuMessage groupMsg = Utils.toKombu(ret.get(0));
        assertEquals("Catalog", groupMsg.getType());
        assertEquals(Notification.Action.Add.name(), groupMsg.getAction());
        KombuLayerGroupInfo source = (KombuLayerGroupInfo) groupMsg.getSource();
        assertEquals("LayerGroupInfo", source.getType());
        assertEquals(1, source.getLayers().size());
        KombuLayerSimpleInfo kl = source.getLayers().get(0);
        assertEquals(l.getName(), kl.getName());
        assertEquals(l.getDefaultStyle().getName(), kl.getStyle());
    }

    @Test
    public void transactionDoubleAdd() throws Exception {
        ReceiverService service = new ReceiverService(1);
        rc.receive(service);

        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Lines\"> "
                        + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // do a double insert
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert handle='insert-1'> "
                        + "<cgf:Lines>"
                        + "<cgf:lineStringProperty>"
                        + "<gml:LineString>"
                        + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                        + "5,5 6,6"
                        + "</gml:coordinates>"
                        + "</gml:LineString>"
                        + "</cgf:lineStringProperty>"
                        + "<cgf:id>t0001</cgf:id>"
                        + "</cgf:Lines>"
                        + "</wfs:Insert>"
                        + "<wfs:Insert handle='insert-2'> "
                        + "<cgf:Lines>"
                        + "<cgf:lineStringProperty>"
                        + "<gml:LineString>"
                        + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                        + "7,7 8,8"
                        + "</gml:coordinates>"
                        + "</gml:LineString>"
                        + "</cgf:lineStringProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:Lines>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        postAsDOM("wfs", xml);

        List<byte[]> ret = service.getMessages();
        assertEquals(1, ret.size());

        KombuMessage tMsg = Utils.toKombu(ret.get(0));
        assertEquals("Data", tMsg.getType());
        assertEquals(2, tMsg.getProperties().get(NotificationTransactionListener.INSERTED));
        assertNotNull(tMsg.getProperties().get(NotificationTransactionListener.BOUNDS));
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
                com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        Bounds b =
                mapper.convertValue(
                        tMsg.getProperties().get(NotificationTransactionListener.BOUNDS),
                        Bounds.class);
        assertEquals(5d, b.getMinx().doubleValue(), 0);
        assertEquals(5d, b.getMiny().doubleValue(), 0);
        assertEquals(8d, b.getMaxx().doubleValue(), 0);
        assertEquals(8d, b.getMaxy().doubleValue(), 0);

        // 2. do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(3, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    public void addWorkspace() throws Exception {
        WorkspaceInfo acme = catalog.getFactory().createWorkspace();
        acme.setName("acme");
        catalog.add(acme);
    }

    public void addCoverageStore() throws Exception {
        addWorkspace();
        CoverageStoreInfo cs = catalog.getFactory().createCoverageStore();
        cs.setName("foostore");
        cs.setWorkspace(catalog.getWorkspaceByName("acme"));
        catalog.add(cs);
    }
}
