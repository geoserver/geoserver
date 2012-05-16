package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetCapabilitiesTest extends WFSTestSupport {
//    /**
//     * This is a READ ONLY TEST so we can use one time setup
//     */
//    public static Test suite() {
//        return new OneTimeTestSetup(new GetCapabilitiesTest());
//    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.disableDataStore(MockData.CITE_PREFIX);
    }
    

    public void testGet() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities");
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength() > 0);
    }
    
    public void testSkipMisconfiguredLayers() throws Exception {
        // configure geoserver to skip misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        // introduce misconfiguration
        FeatureTypeInfo ftype = getCatalog().getFeatureTypeByName(MockData.UPDATES.getLocalPart());
        ftype.setLatLonBoundingBox(null);
        getCatalog().save(ftype);
        
        // fetch capabilities document
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities");
        print(doc);
        int count = 0; 
        for (FeatureTypeInfo ft : getCatalog().getFeatureTypes()) {
            if (ft.enabled()) count++;
        }
        assertXpathEvaluatesTo(String.valueOf(count - 1), "count(//wfs:FeatureType)", doc);
    }
    
    public void testNamespaceFilter() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=getCapabilities&namespace=sf");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());
        
        // try again with a missing one
        doc = getAsDOM("wfs?service=WFS&request=getCapabilities&namespace=NotThere");
        e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength());
    }

    public void testPost() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" version=\"1.0.0\""
                + " xmlns=\"http://www.opengis.net/wfs\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs "
                + " http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\"/>";
        Document doc = postAsDOM("wfs", xml);
        
        assertEquals("WFS_Capabilities", doc.getDocumentElement().getNodeName());

    }
    
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=1.0.0");
        
        Element outputFormats = getFirstElementByTagName(doc, "ResultFormat");
        NodeList formats = outputFormats.getChildNodes();
        
        TreeSet s1 = new TreeSet();
        for ( int i = 0; i < formats.getLength(); i++ ) {
            String format = formats.item(i).getNodeName();
            s1.add( format );
        }
        
        List extensions = GeoServerExtensions.extensions( WFSGetFeatureOutputFormat.class );
        
        TreeSet s2 = new TreeSet();
        for ( Iterator e = extensions.iterator(); e.hasNext(); ) {
            WFSGetFeatureOutputFormat extension = (WFSGetFeatureOutputFormat) e.next();
            s2.add( extension.getCapabilitiesElementName() );
        }
        
        assertEquals( s1, s2 );
    }

    public void testTypeNameCount() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        XpathEngine xpath = XMLUnit.newXpathEngine();

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (!ft.isEnabled()) {
                it.remove();
            }
        }
        final int enabledCount = enabledTypes.size();

        assertEquals(enabledCount, xpath.getMatchingNodes(
                "/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType", doc).getLength());
    }

    public void testTypeNames() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (ft.isEnabled()) {
                String prefixedName = ft.getPrefixedName();

                String xpathExpr = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/"
                        + "wfs:FeatureType/wfs:Name[text()=\"" + prefixedName + "\"]";

                XMLAssert.assertXpathExists(xpathExpr, doc);
            }
        }
    }
    
    public void testWorkspaceQualified() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("sf/wfs?service=WFS&version=1.0.0&request=getCapabilities");
        
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());

        assertEquals(6, xpath.getMatchingNodes("//wfs:Get[contains(@onlineResource,'sf/wfs')]", doc).getLength());
        assertEquals(6, xpath.getMatchingNodes("//wfs:Post[contains(@onlineResource,'sf/wfs')]", doc).getLength());
        
        //TODO: test with a non existing workspace
    }
    
    public void testLayerQualified() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wfs?service=WFS&version=1.0.0&request=getCapabilities");
        
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());

        assertEquals(6, xpath.getMatchingNodes("//wfs:Get[contains(@onlineResource,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        assertEquals(6, xpath.getMatchingNodes("//wfs:Post[contains(@onlineResource,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        
        //TODO: test with a non existing workspace
    }
    
    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.MLINES);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            Document dom = getAsDOM("wfs?request=getCapabilities&version=1.0.0");
            assertXpathExists("//wfs:FeatureType[wfs:Name='" + layerId + "']", dom);
            
            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            dom = getAsDOM("wfs?request=getCapabilities&version=1.0.0");
            assertXpathNotExists("//wfs:FeatureType[wfs:Name = '" + layerId + "']", dom);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }
}
