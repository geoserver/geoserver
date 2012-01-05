package org.geoserver.config;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;

public class GeoServerPersisterTest extends GeoServerTestSupport {

    Catalog catalog;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        catalog = getCatalog();
        GeoServerPersister p = 
            new GeoServerPersister( getResourceLoader(), new XStreamPersisterFactory().createXMLPersister() );
        catalog.addListener( p );
    }
    
    public void testAddWorkspace() throws Exception {
        File ws = new File( testData.getDataDirectoryRoot(), "workspaces/acme" );
        assertFalse( ws.exists() );
        
        WorkspaceInfo acme = catalog.getFactory().createWorkspace();
        acme.setName( "acme" );
        catalog.add( acme );
        
        assertTrue( ws.exists() );
    }
    
    public void testRemoveWorkspace() throws Exception {
        testAddWorkspace();
        
        File ws = new File( testData.getDataDirectoryRoot(), "workspaces/acme" );
        assertTrue( ws.exists() );
        
        WorkspaceInfo acme = catalog.getWorkspaceByName( "acme" );
        catalog.remove( acme );
        assertFalse( ws.exists() );
    }
    
    public void testDefaultWorkspace() throws Exception {
        testAddWorkspace();
        WorkspaceInfo ws = catalog.getWorkspaceByName("acme");
        catalog.setDefaultWorkspace(ws);
        
        File dws = new File( testData.getDataDirectoryRoot(), "workspaces/default.xml" );
        assertTrue( dws.exists() );
        
        Document dom = dom(dws);
        assertXpathEvaluatesTo("acme", "/workspace/name", dom );
    }
    
    public void testAddDataStore() throws Exception {
        testAddWorkspace();
        
        File dir = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertFalse( dir.exists() );
        
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setName( "foostore" );
        ds.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( ds );
        
        assertTrue( dir.exists() );
        assertTrue( new File( dir, "datastore.xml").exists() );
    }
    
    public void testModifyDataStore() throws Exception {
        testAddDataStore();
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore" );
        assertTrue( ds.getConnectionParameters().isEmpty() );
        
        ds.getConnectionParameters().put( "foo", "bar" );
        catalog.save( ds );
        
        File f = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/datastore.xml");
        Document dom = dom( f );
        assertXpathExists( "/dataStore/connectionParameters/entry[@key='foo']", dom );
    }
    
    public void testChangeDataStoreWorkspace() throws Exception {
        testAddDataStore();
        File f1 = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/datastore.xml");
        assertTrue( f1.exists() );
        
        WorkspaceInfo nws = catalog.getFactory().createWorkspace();
        nws.setName( "topp");
        catalog.add( nws );
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore" );
        ds.setWorkspace( nws );
        catalog.save( ds );
        
        assertFalse( f1.exists() );
        File f2 = new File( testData.getDataDirectoryRoot(), "workspaces/topp/foostore/datastore.xml");
        assertTrue( f2.exists() );
    }
    
    public void testRemoveDataStore() throws Exception {
        testAddDataStore();
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertTrue( f.exists() );
        
        DataStoreInfo ds = catalog.getDataStoreByName( "acme", "foostore");
        catalog.remove( ds );
        assertFalse( f.exists() );
    }
    
    public void testAddFeatureType() throws Exception {
        testAddDataStore();
        
        File d = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo");
        assertFalse( d.exists() );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "bar" );
        ns.setURI( "http://bar" );
        catalog.add( ns );
        
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName( "foo" );
        ft.setNamespace( ns );
        ft.setStore( catalog.getDataStoreByName( "acme", "foostore"));
        catalog.add( ft );
        
        assertTrue( d.exists() );
    }
    
    public void testChangeFeatureTypeStore() throws Exception {
        testAddFeatureType();
        
        File f1 = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/foo/featuretype.xml");
        assertTrue( f1.exists() );
        
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setName( "barstore" );
        ds.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( ds );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "foo" );
        ft.setStore( ds );
        catalog.save( ft );
        
        assertFalse( f1.exists() );
        File f2 = new File( testData.getDataDirectoryRoot(), "workspaces/acme/barstore/foo/featuretype.xml");
        assertTrue( f2.exists() );
    }
    
    public void testModifyFeatureType() throws Exception {
        testAddFeatureType();
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "bar", "foo" );
        ft.setTitle( "fooTitle" );
        catalog.save( ft );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/featuretype.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "fooTitle", "/featureType/title", dom );
    }
    
    public void testRemoveFeatureType() throws Exception {
        testAddFeatureType();
        
        File d = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo");
        assertTrue( d.exists() );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "bar", "foo" );
        catalog.remove( ft );
        
        assertFalse( d.exists() );
    }
    
    public void testAddCoverageStore() throws Exception {
        testAddWorkspace();
        
        File dir = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertFalse( dir.exists() );
        
        CoverageStoreInfo cs = catalog.getFactory().createCoverageStore();
        cs.setName( "foostore" );
        cs.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( cs );
        
        assertTrue( dir.exists() );
        assertTrue( new File( dir, "coveragestore.xml").exists() );
    }
    
    public void testModifyCoverageStore() throws Exception {
        testAddCoverageStore();
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "acme", "foostore" );
        assertNull( cs.getURL() );
        
        cs.setURL( "file:data/foo.tiff" );
        catalog.save( cs );
        
        File f = 
            new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore/coveragestore.xml");
        Document dom = dom( f );
        assertXpathEvaluatesTo( "file:data/foo.tiff","/coverageStore/url/text()", dom );
    }
    
    public void testRemoveCoverageStore() throws Exception {
        testAddCoverageStore();
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/foostore");
        assertTrue( f.exists() );
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "acme", "foostore");
        catalog.remove( cs );
        assertFalse( f.exists() );
    }
    
    public void testAddCoverage() throws Exception {
        testAddCoverageStore();
        
        File d = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo");
        assertFalse( d.exists() );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "bar" );
        ns.setURI( "http://bar" );
        catalog.add( ns );
        
        CoverageInfo ft = catalog.getFactory().createCoverage();
        ft.setName( "foo" );
        ft.setNamespace( ns );
        ft.setStore( catalog.getCoverageStoreByName( "acme", "foostore"));
        catalog.add( ft );
        
        assertTrue( d.exists() );
    }
    
    public void testModifyCoverage() throws Exception {
        testAddCoverage();
        
        CoverageInfo ft = catalog.getCoverageByName( "bar", "foo" );
        ft.setTitle( "fooTitle" );
        catalog.save( ft );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/coverage.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "fooTitle", "/coverage/title", dom );
    }
    
    public void testRemoveCoverage() throws Exception {
        testAddCoverage();
        
        File d = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo");
        assertTrue( d.exists() );
        
        CoverageInfo ft = catalog.getCoverageByName( "bar", "foo" );
        catalog.remove( ft );
        
        assertFalse( d.exists() );
    }
    
    
    public void testAddWMSStore() throws Exception {
        testAddWorkspace();
        
        File dir = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms");
        assertFalse( dir.exists() );
        
        WMSStoreInfo wms = catalog.getFactory().createWebMapServer();
        wms.setName( "demowms" );
        wms.setWorkspace( catalog.getWorkspaceByName( "acme" ) );
        catalog.add( wms );
        
        assertTrue( dir.exists() );
        assertTrue( new File( dir, "wmsstore.xml").exists() );
    }
    
    public void testModifyWMSStore() throws Exception {
        testAddWMSStore();
        
        WMSStoreInfo wms = catalog.getStoreByName( "acme", "demowms", WMSStoreInfo.class );
        assertNull( wms.getCapabilitiesURL() );
        
        String capsURL = "http://demo.opengeo.org:8080/geoserver/wms?request=GetCapabilites&service=WMS";
        wms.setCapabilitiesURL(capsURL);
        catalog.save( wms );
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms/wmsstore.xml");
        Document dom = dom( f );
        assertXpathEvaluatesTo(capsURL, "/wmsStore/capabilitiesURL/text()", dom);
    }
    
    public void testRemoveWMSStore() throws Exception {
        testAddWMSStore();
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms");
        assertTrue( f.exists() );
        
        WMSStoreInfo wms = catalog.getStoreByName("acme", "demowms", WMSStoreInfo.class);
        catalog.remove( wms );
        assertFalse( f.exists() );
    }
    
    public void testAddWMSLayer() throws Exception {
        testAddWMSStore();
        
        File d = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms/foo");
        assertFalse( d.exists() );
        
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix( "bar" );
        ns.setURI( "http://bar" );
        catalog.add( ns );
        
        WMSLayerInfo wms = catalog.getFactory().createWMSLayer();
        wms.setName( "foo" );
        wms.setNamespace( ns );
        wms.setStore(catalog.getStoreByName("acme", "demowms", WMSStoreInfo.class));
        catalog.add( wms );
        
        assertTrue( d.exists() );
        assertTrue( new File( d, "wmslayer.xml").exists() );
    }
    
    public void testModifyWMSLayer() throws Exception {
        testAddWMSLayer();
        
        WMSLayerInfo wli = catalog.getResourceByName( "bar", "foo", WMSLayerInfo.class );
        wli.setTitle( "fooTitle" );
        catalog.save( wli );
        
        File f = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms/foo/wmslayer.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "fooTitle", "/wmsLayer/title", dom );
    }
    
    public void testRemoveWMSLayer() throws Exception {
        testAddWMSLayer();
        
        File d = new File( testData.getDataDirectoryRoot(), "workspaces/acme/demowms/foo");
        assertTrue( d.exists() );
        
        WMSLayerInfo wli = catalog.getResourceByName( "bar", "foo", WMSLayerInfo.class );
        catalog.remove( wli );
        
        assertFalse( d.exists() );
    }
    
    
    public void testAddLayer() throws Exception {
        testAddFeatureType();
        testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
        "workspaces/acme/foostore/foo/layer.xml");
        assertFalse( f.exists() );
        
        LayerInfo l = catalog.getFactory().createLayer();
        // l.setName("foo");
        l.setResource( catalog.getFeatureTypeByName( "bar", "foo") );
        
        StyleInfo s = catalog.getStyleByName( "foostyle");
        l.setDefaultStyle(s);
        catalog.add( l );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyLayer() throws Exception {
        testAddLayer();
        
        LayerInfo l = catalog.getLayerByName( "foo" );
        l.setPath( "/foo/bar" );
        catalog.save( l );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/layer.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "/foo/bar", "/layer/path", dom );
    }
    
    public void testRemoveLayer() throws Exception {
        testAddLayer();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "workspaces/acme/foostore/foo/layer.xml");
        assertTrue( f.exists() );
        
        LayerInfo l = catalog.getLayerByName( "foo" );
        catalog.remove( l );
        
        assertFalse( f.exists() );
    }
    
    public void testAddStyle() throws Exception {
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        assertFalse( f.exists() );
        
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename( "foostyle.sld");
        catalog.add( s );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyStyle() throws Exception {
        testAddStyle();
        
        StyleInfo s = catalog.getStyleByName( "foostyle" );
        s.setFilename( "foostyle2.sld");
        catalog.save( s );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        Document dom = dom( f );
        
        assertXpathEvaluatesTo( "foostyle2.sld", "/style/filename", dom );
    }
    
    public void testRemoveStyle() throws Exception {
        testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "styles/foostyle.xml");
        assertTrue( f.exists() );
        
        StyleInfo s = catalog.getStyleByName( "foostyle" );
        catalog.remove( s );
        
        assertFalse( f.exists() );
    }

    public void testAddLayerGroup() throws Exception {
        testAddLayer();
        //testAddStyle();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        assertFalse( f.exists() );
        
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lg");
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( catalog.getStyleByName( "foostyle") );
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( /* default style */ null);
        lg.getLayers().add( catalog.getLayerByName( "foo") );
        lg.getStyles().add( catalog.getStyleByName( "foostyle"));

        catalog.add( lg );
        
        assertTrue( f.exists() );
    }
    
    public void testModifyLayerGroup() throws Exception {
        testAddLayerGroup();
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "lg" );
        
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName( "foostyle2" );
        s.setFilename( "foostyle2.sld");
        catalog.add( s );
        
        lg.getStyles().set( 0, s );
        catalog.save( lg );
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        Document dom = dom( f );
        assertXpathEvaluatesTo( s.getId(), "/layerGroup/styles/style/id", dom );
    }
    
    public void testRemoveLayerGroup() throws Exception {
        testAddLayerGroup();
        
        File f = new File( testData.getDataDirectoryRoot(), 
            "layergroups/lg.xml");
        assertTrue( f.exists() );
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "lg" );
        catalog.remove( lg );
        
        assertFalse( f.exists() );
    }
    
    public void testModifyGlobal() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setAdminUsername("roadRunner");
        global.setTitle( "ACME");
        getGeoServer().save( global );
        
        File f = new File( testData.getDataDirectoryRoot(), "global.xml" ); 
        Document dom = dom( f );
        assertXpathEvaluatesTo( "roadRunner", "/global/adminUsername", dom );
        assertXpathEvaluatesTo( "ACME", "/global/settings/title", dom );
    }

    public void testAddSettings() throws Exception {
        testAddWorkspace();
        WorkspaceInfo ws = catalog.getWorkspaceByName("acme");
       
        SettingsInfo settings = getGeoServer().getFactory().createSettings();
        settings.setTitle("ACME");
        settings.setWorkspace(ws);
        
        File f = catalog.getResourceLoader().find("workspaces", ws.getName(), "settings.xml");
        assertNull(f);

        getGeoServer().add(settings);
        f = catalog.getResourceLoader().find("workspaces", ws.getName(), "settings.xml");
        assertNotNull(f);
        Document dom = dom( f );
        assertXpathEvaluatesTo( "ACME", "/settings/title", dom );
    }

    public void testModifySettings() throws Exception {
        testAddSettings();
        WorkspaceInfo ws = catalog.getWorkspaceByName("acme");

        SettingsInfo settings = getGeoServer().getSettings(ws);
        settings.setTitle("FOO");
        getGeoServer().save(settings);
        
        File f = catalog.getResourceLoader().find("workspaces", ws.getName(), "settings.xml");
        assertNotNull(f);

        Document dom = dom( f );
        assertXpathEvaluatesTo( "FOO", "/settings/title", dom );
    }

    public void testModifySettingsChangeWorkspace() throws Exception {
        testAddSettings();

        WorkspaceInfo ws1 = catalog.getWorkspaceByName("acme");
        WorkspaceInfo ws2 = catalog.getFactory().createWorkspace();
        ws2.setName("foo");
        catalog.add(ws2);

        SettingsInfo settings = getGeoServer().getSettings(ws1);
        settings.setWorkspace(ws2);
        getGeoServer().save(settings);

        File f = catalog.getResourceLoader().find("workspaces", ws1.getName(), "settings.xml");
        assertNull(f);

        f = catalog.getResourceLoader().find("workspaces", ws2.getName(), "settings.xml");
        assertNotNull(f);

        Document dom = dom( f );
        assertXpathEvaluatesTo( ws2.getId(), "/settings/workspace/id", dom );   
    }

    public void testRemoveSettings() throws Exception {
        testAddSettings();
        WorkspaceInfo ws = catalog.getWorkspaceByName("acme");

        File f = catalog.getResourceLoader().find("workspaces", ws.getName(), "settings.xml");
        assertNotNull(f);

        SettingsInfo settings = getGeoServer().getSettings(ws);
        getGeoServer().remove(settings);
        
        f = catalog.getResourceLoader().find("workspaces", ws.getName(), "settings.xml");
        assertNull(f);
    }

    Document dom( File f ) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( f );
    }
}
