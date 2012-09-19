/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows;

import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.*;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wcs10.DescribeCoverageType;
import net.opengis.wcs10.GetCoverageType;
import net.opengis.wcs10.Wcs10Factory;
import net.opengis.wcs10.Wcs10Package;
import net.opengis.wcs11.Wcs11Factory;
import net.opengis.wcs11.impl.DomainSubsetTypeImpl;
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
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;



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
        
        assertEquals("BAR", data.getService());
        assertEquals("foo", data.getOperation());
        assertEquals("1.2.3", data.getOwsVersion());
    }
    
    @Test
    public void testWFSDescribeFeatureType() throws Exception {
        DescribeFeatureTypeType dft = WfsFactory.eINSTANCE.createDescribeFeatureTypeType();
        dft.getTypeName().add(new QName("http://acme.org", "foo", "acme"));
        dft.getTypeName().add(new QName("http://acme.org", "bar", "acme"));
        
        Operation op = op("DescribeFeatureType", "WFS", "1.0.0", dft);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
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
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
    }
    
    @Test
    public void testWFSLockFeature() throws Exception {
        LockFeatureType lf = WfsFactory.eINSTANCE.createLockFeatureType();
        
        LockType l = WfsFactory.eINSTANCE.createLockType();
        l.setTypeName(new QName("http://acme.org", "foo", "acme"));
        lf.getLock().add(l);
        
        Operation op = op("LockFeature", "WFS", "1.0.0", lf);
        callback.operationDispatched(new Request(), op);
        
        assertEquals("acme:foo", data.getResources().get(0));
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
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
    }
    
    @Test
    public void testWMSGetMap() throws Exception {
        GetMapRequest gm = new GetMapRequest();
        
        gm.setLayers(Arrays.asList(createMapLayer("foo", "acme")));
        
        Envelope env = new Envelope(100,110,70,80);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        gm.setBbox(env);
        gm.setCrs(crs);
        callback.operationDispatched(new Request(), op("GetMap", "WMS", "1.1.1", gm));
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals(new ReferencedEnvelope(env,crs),data.getBbox());
    }
    
    @Test
    public void testWMSGetFeatureInfo() throws Exception {
        GetFeatureInfoRequest gfi = new GetFeatureInfoRequest();
        
        GetMapRequest gm = new GetMapRequest();
        gm.setHeight(20);
        gm.setWidth(10);
        Envelope env = new Envelope(100,110,30,50);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        gm.setBbox(env);
        gm.setCrs(crs);
        gfi.setGetMapRequest(gm);
        gfi.setXPixel(6);
        gfi.setYPixel(17);
        
        gfi.setQueryLayers(Arrays.asList(createMapLayer("foo", "acme"), createMapLayer("bar", "acme")));
        callback.operationDispatched(new Request(), op("GetFeatureInfo", "WMS", "1.1.1", gfi));
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
        assertEquals(new ReferencedEnvelope(106,106,33,33,crs),data.getBbox());
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
    
        assertEquals("http://acme.org:foo", data.getResources().get(0));
    }
    
    @Test
    public void testWCS10DescribeCoverage() throws Exception {
        DescribeCoverageType dc = Wcs10Factory.eINSTANCE.createDescribeCoverageType();
        dc.getCoverage().add("acme:foo");
        dc.getCoverage().add("acme:bar");
        
        callback.operationDispatched(new Request(), op("DescribeCoverage", "WCS", "1.0.0", dc));
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
    }
    
    @Test
    public void testWCS10GetCoverage() throws Exception {
        GetCoverageType gc = Wcs10Factory.eINSTANCE.createGetCoverageType();
        net.opengis.wcs10.SpatialSubsetType spatialSubset = Wcs10Factory.eINSTANCE.createSpatialSubsetType();
        
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        GeneralEnvelope env = new GeneralEnvelope(new double[]{-123.4, 48.2}, new double[]{-120.9, 50.1});
        env.setCoordinateReferenceSystem(crs);
        BoundingBox bbox = new ReferencedEnvelope(env);

        spatialSubset.getEnvelope().clear();
        spatialSubset.getEnvelope().add(env);
        net.opengis.wcs10.DomainSubsetType domainSubset = Wcs10Factory.eINSTANCE.createDomainSubsetType();
        domainSubset.setSpatialSubset(spatialSubset);
        
        gc.setSourceCoverage("acme:foo");
        gc.setDomainSubset(domainSubset);
        
        callback.operationDispatched(new Request(), op("GetCoverage", "WCS", "1.0.0", gc));
        
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals(bbox, data.getBbox());
    }
    
    @Test
    public void testWCS11DescribeCoverage() throws Exception {
        net.opengis.wcs11.DescribeCoverageType dc = Wcs11Factory.eINSTANCE.createDescribeCoverageType();
        dc.getIdentifier().add("acme:foo");
        dc.getIdentifier().add("acme:bar");
        
        callback.operationDispatched(new Request(), op("DescribeCoverage", "WCS", "1.1.0", dc));
        assertEquals("acme:foo", data.getResources().get(0));
        assertEquals("acme:bar", data.getResources().get(1));
    }
    
    @Test
    public void testWCS11GetCoverage() throws Exception {
        net.opengis.wcs11.GetCoverageType gc = Wcs11Factory.eINSTANCE.createGetCoverageType();
        
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        GeneralEnvelope env = new GeneralEnvelope(new double[]{-123.4, 48.2}, new double[]{-120.9, 50.1});
        env.setCoordinateReferenceSystem(crs);
        BoundingBox bbox = new ReferencedEnvelope(env);
        net.opengis.ows11.BoundingBoxType wcsBbox = net.opengis.ows11.Ows11Factory.eINSTANCE.createBoundingBoxType();
        wcsBbox.setLowerCorner(Arrays.asList(-123.4d, 48.2d));
        wcsBbox.setUpperCorner(Arrays.asList(-120.9d, 50.1d));
        //wcsBbox.setCrs("urn:ogc:def:crs:OGC:1.3:CRS84");
        wcsBbox.setCrs("urn:ogc:def:crs:EPSG:4326");
        net.opengis.wcs11.DomainSubsetType domainSubset = Wcs11Factory.eINSTANCE.createDomainSubsetType();
        domainSubset.setBoundingBox(wcsBbox);
        
        gc.setDomainSubset(domainSubset);
        
        CodeType c = Ows11Factory.eINSTANCE.createCodeType();
        c.setValue("acme:bar");
        gc.setIdentifier(c);
        
        callback.operationDispatched(new Request(), op("GetCoverage", "WCS", "1.1.0", gc));
        assertEquals("acme:bar", data.getResources().get(0));
        assertEquals(bbox, data.getBbox());
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
