package org.geoserver.monitor.ows;

import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.*;

import java.util.Arrays;

import javax.xml.namespace.QName;

import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wcs10.DescribeCoverageType;
import net.opengis.wcs10.GetCoverageType;
import net.opengis.wcs10.Wcs10Factory;
import net.opengis.wcs11.Wcs11Factory;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.RequestData;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.feature.NameImpl;
import org.geotools.util.Version;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;

public class MonitorCallbackTest {

    static Monitor monitor;
    MonitorCallback callback;
    RequestData data;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        MonitorDAO dao = new MemoryMonitorDAO();
        new MonitorTestData(dao).setup();
        monitor = new Monitor(dao);
    }
    
    @Before
    public void setUp() throws Exception {
        callback = new MonitorCallback(monitor);
        data = monitor.start();
    }
    
    public void tearDown() throws Exception {
        monitor.complete();
    }
    
    @Test
    public void testBasic() throws Exception {
        callback.operationDispatched(new Request(), op("foo", "bar", "1.2.3", null));
        
        assertEquals("BAR", data.getOwsService());
        assertEquals("foo", data.getOwsOperation());
        assertEquals("1.2.3", data.getOwsVersion());
    }
    
    @Test
    public void testWFSDescribeFeatureType() throws Exception {
        DescribeFeatureTypeType dft = WfsFactory.eINSTANCE.createDescribeFeatureTypeType();
        dft.getTypeName().add(new QName("http://acme.org", "foo", "acme"));
        dft.getTypeName().add(new QName("http://acme.org", "bar", "acme"));
        
        Operation op = op("DescribeFeatureType", "WFS", "1.0.0", dft);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWFSGetFeature() throws Exception {
        GetFeatureType gf = WfsFactory.eINSTANCE.createGetFeatureType();
        
        QueryType q = WfsFactory.eINSTANCE.createQueryType();
        q.setTypeName(Arrays.asList(new QName("http://acme.org", "foo", "acme")));
        gf.getQuery().add(q);
        
        q = WfsFactory.eINSTANCE.createQueryType();
        q.setTypeName(Arrays.asList(new QName("http://acme.org", "bar", "acme")));
        gf.getQuery().add(q);
        
        Operation op = op("GetFeature", "WFS", "1.0.0", gf);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWFSLockFeature() throws Exception {
        LockFeatureType lf = WfsFactory.eINSTANCE.createLockFeatureType();
        
        LockType l = WfsFactory.eINSTANCE.createLockType();
        l.setTypeName(new QName("http://acme.org", "foo", "acme"));
        lf.getLock().add(l);
        
        Operation op = op("LockFeature", "WFS", "1.0.0", lf);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getLayers().get(0));
    }
    
    @Test
    public void testWFSTransaction() throws Exception {
        TransactionType t = WfsFactory.eINSTANCE.createTransactionType();
        
        UpdateElementType ue = WfsFactory.eINSTANCE.createUpdateElementType();
        ue.setTypeName(new QName("http://acme.org", "foo", "acme"));
        t.getUpdate().add(ue);
        
        DeleteElementType de = WfsFactory.eINSTANCE.createDeleteElementType();
        de.setTypeName(new QName("http://acme.org", "bar", "acme"));
        t.getDelete().add(de);
        
        Operation op = op("Transaction", "WFS", "1.0.0", t);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWMSGetMap() throws Exception {
        GetMapRequest gm = new GetMapRequest();
        
        gm.setLayers(Arrays.asList(createMapLayer("foo", "acme")));
        callback.operationDispatched(new Request(), op("GetMap", "WMS", "1.1.1", gm));
        
        assertEquals("acme:foo", data.getLayers().get(0));
    }
    
    @Test
    public void testWMSGetFeatureInfo() throws Exception {
        GetFeatureInfoRequest gfi = new GetFeatureInfoRequest();
        
        gfi.setQueryLayers(Arrays.asList(createMapLayer("foo", "acme"), createMapLayer("bar", "acme")));
        callback.operationDispatched(new Request(), op("GetFeatureInfo", "WMS", "1.1.1", gfi));
        
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWMSGetLegendGraphic() throws Exception {
        WMS wms = new WMS(createMock(GeoServer.class));
        GetLegendGraphicRequest glg = new GetLegendGraphicRequest();
        
        FeatureType type = createMock(FeatureType.class);
        expect(type.getName()).andReturn(new NameImpl("http://acme.org", "foo")).anyTimes();
        replay(type);
        
        glg.setLayer(type);
        callback.operationDispatched(new Request(), op("GetFeatureInfo", "WMS", "1.1.1", glg));
    
        assertEquals("http://acme.org:foo", data.getLayers().get(0));
    }
    
    @Test
    public void testWCS10DescribeCoverage() throws Exception {
        DescribeCoverageType dc = Wcs10Factory.eINSTANCE.createDescribeCoverageType();
        dc.getCoverage().add("acme:foo");
        dc.getCoverage().add("acme:bar");
        
        callback.operationDispatched(new Request(), op("DescribeCoverage", "WCS", "1.0.0", dc));
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWCS10GetCoverage() throws Exception {
        GetCoverageType gc = Wcs10Factory.eINSTANCE.createGetCoverageType();
        gc.setSourceCoverage("acme:foo");
        callback.operationDispatched(new Request(), op("GetCoverage", "WCS", "1.0.0", gc));
        
        assertEquals("acme:foo", data.getLayers().get(0));
    }
    
    @Test
    public void testWCS11DescribeCoverage() throws Exception {
        net.opengis.wcs11.DescribeCoverageType dc = Wcs11Factory.eINSTANCE.createDescribeCoverageType();
        dc.getIdentifier().add("acme:foo");
        dc.getIdentifier().add("acme:bar");
        
        callback.operationDispatched(new Request(), op("DescribeCoverage", "WCS", "1.1.0", dc));
        assertEquals("acme:foo", data.getLayers().get(0));
        assertEquals("acme:bar", data.getLayers().get(1));
    }
    
    @Test
    public void testWCS11GetCoverage() throws Exception {
        net.opengis.wcs11.GetCoverageType gc = Wcs11Factory.eINSTANCE.createGetCoverageType();
        
        CodeType c = Ows11Factory.eINSTANCE.createCodeType();
        c.setValue("acme:bar");
        gc.setIdentifier(c);
        
        callback.operationDispatched(new Request(), op("GetCoverage", "WCS", "1.1.0", gc));
        assertEquals("acme:bar", data.getLayers().get(0));
    }
    
    MapLayerInfo createMapLayer(String name, String ns) {
        ResourceInfo r = createMock(ResourceInfo.class);
        expect(r.getName()).andReturn(name);
        expect(r.getPrefixedName()).andReturn(ns + ":" + name);
        expect(r.getTitle()).andReturn(name);
        expect(r.getAbstract()).andReturn(name);
        replay(r);
        
        LayerInfo l = createMock(LayerInfo.class);
        expect(l.getResource()).andReturn(r);
        expect(l.getType()).andReturn(LayerInfo.Type.VECTOR);
        replay(l);
        
        return new MapLayerInfo(l);
    }
    
    Operation op(String name, String service, String version, Object request) {
        return new Operation(name, new Service(service, null, new Version(version), null), 
            null, new Object[]{request});
    }
    
}
