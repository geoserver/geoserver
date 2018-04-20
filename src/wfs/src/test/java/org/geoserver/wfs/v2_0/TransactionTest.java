/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import com.vividsolutions.jts.geom.Point;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.Service;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransactionTest extends WFS20TestSupport {

    public static final QName WITH_GML = new QName(SystemTestData.SF_URI,
            "WithGMLProperties", SystemTestData.SF_PREFIX);

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.ROAD_SEGMENTS);
        getTestData().addVectorLayer(WITH_GML, Collections.EMPTY_MAP, org.geoserver.wfs.v1_1.TransactionTest.class, 
                getCatalog());
        getTestData().addVectorLayer(new QName(SystemTestData.SF_URI, "PrimitiveGeoFeatureId", SystemTestData
                .SF_PREFIX), Collections.EMPTY_MAP, TestData.class, getCatalog());
    }
    
    @Test
    public void testInsert1() throws Exception {
        String xml = "<wfs:Transaction service='WFS' version='2.0.0' "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:sf='http://cite.opengeospatial.org/gmlsf'>"
            + "<wfs:Insert handle='insert-1'>"
            + " <sf:PrimitiveGeoFeature gml:id='cite.gmlsf0-f01'>"
            + "  <gml:description>"
            + "Fusce tellus ante, tempus nonummy, ornare sed, accumsan nec, leo."
            + "Vivamus pulvinar molestie nisl."
            + "</gml:description>"
            + "<gml:name>Aliquam condimentum felis sit amet est.</gml:name>"
            //+ "<gml:name codeSpace='http://cite.opengeospatial.org/gmlsf'>cite.gmlsf0-f01</gml:name>"
            + "<sf:curveProperty>"
            + "  <gml:LineString gml:id='cite.gmlsf0-g01' srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "   <gml:posList>47.608284 19.034142 51.286873 16.7836 49.849854 15.764992</gml:posList>"
            + " </gml:LineString>"
            + "</sf:curveProperty>"
            + "<sf:intProperty>1025</sf:intProperty>"
            + "<sf:measurand>7.405E2</sf:measurand>"
            + "<sf:dateTimeProperty>2006-06-23T12:43:12+01:00</sf:dateTimeProperty>"
            + "<sf:decimalProperty>90.62</sf:decimalProperty>"
            + "</sf:PrimitiveGeoFeature>"
            + "</wfs:Insert>"
            + "<wfs:Insert handle='insert-2'>"
            + "<sf:AggregateGeoFeature gml:id='cite.gmlsf0-f02'>"
            + " <gml:description>"
            + "Duis nulla nisi, molestie vel, rhoncus a, ullamcorper eu, justo. Sed bibendum."
            + " Ut sem. Mauris nec nunc a eros aliquet pharetra. Mauris nonummy, pede et"
            + " tincidunt ultrices, mauris lectus fermentum massa, in ullamcorper lectus"
            + "felis vitae metus. Sed imperdiet sollicitudin dolor."
            + " </gml:description>"
            + " <gml:name codeSpace='http://cite.opengeospatial.org/gmlsf'>cite.gmlsf0-f02</gml:name>"
            + " <gml:name>Quisqué viverra</gml:name>"
            + " <gml:boundedBy>"
            + "   <gml:Envelope srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "     <gml:lowerCorner>36.1 8.0</gml:lowerCorner>"
            + "    <gml:upperCorner>52.0 21.1</gml:upperCorner>"
            + "   </gml:Envelope>"
            + "  </gml:boundedBy>"
            + "   <sf:multiPointProperty>"
            + "<gml:MultiPoint srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "<gml:pointMember>"
            + " <gml:Point><gml:pos>49.325176 21.036873</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>36.142586 13.56189</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>51.920937 8.014193</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "</gml:MultiPoint>"
            + "</sf:multiPointProperty>"
            +

            "<sf:doubleProperty>2012.78</sf:doubleProperty>"
            + "  <sf:intRangeProperty>43</sf:intRangeProperty>"
            + " <sf:strProperty>"
            + "Donec ligulä pede, sodales iń, vehicula eu, sodales et, lêo."
            + "</sf:strProperty>"
            + "<sf:featureCode>AK121</sf:featureCode>"
            + "</sf:AggregateGeoFeature>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";


        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getElementsByTagName("fes:ResourceId").getLength() > 0);
    }

    	
    @Test
    public void testInsertWithNoSRS() throws Exception {
        // 1. do a getFeature
        String getFeature = 
            "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cgf:Points\"> "
                + "<wfs:ValueReference>cite:id</wfs:ValueReference> " + "</wfs:Query> "
            + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert = 
            "<wfs:Transaction service='WFS' version='2.0.0' "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "' "
                + "xmlns:gml='" + GML.NAMESPACE + "'> " 
                + "<wfs:Insert > " 
                  + "<cgf:Points>"
                    + "<cgf:pointProperty>" 
                      + "<gml:Point>" 
                      + "<gml:pos>20 40</gml:pos>"
                      + "</gml:Point>" 
                    + "</cgf:pointProperty>"
                  + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Points>" 
              + "</wfs:Insert>" 
           + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);
        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());
        String fid = getFirstElementByTagName(dom, "fes:ResourceId").getAttribute("rid");
        
        // check insertion occurred
        dom = postAsDOM("wfs", getFeature);
        assertEquals(n + 1, dom.getElementsByTagName("cgf:Points").getLength());

        // check coordinate order is preserved
        getFeature = "<wfs:GetFeature service='WFS' version='2.0.0' "
            + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
            + "xmlns:fes='" + FES.NAMESPACE + "' "
            + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
            + "<wfs:Query typeNames=\"cgf:Points\"> "
            + "<fes:Filter>"
              + "<fes:PropertyIsEqualTo>"
                + "<fes:ValueReference>cgf:id</fes:ValueReference>"
                + "<fes:Literal>t0002</fes:Literal>"
              + "</fes:PropertyIsEqualTo>"
            + "</fes:Filter>"
            + "</wfs:Query> "
            + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);
        assertEquals("20 40", getFirstElementByTagName(dom, "gml:pos").getFirstChild()
                .getNodeValue());
    }

    @Test
    public void testInsertWithSRS() throws Exception {

        // 1. do a getFeature
        String getFeature = 
        "<wfs:GetFeature service='WFS' version='2.0.0' xmlns:cgf='http://www.opengis.net/cite/geometry' " +
        "  xmlns:fes='" + FES.NAMESPACE + "' " +
        "  xmlns:wfs='" + WFS.NAMESPACE + "'> " + 
        " <wfs:Query typeNames='cgf:Points'/> " + 
        "</wfs:GetFeature> "; 

        Document dom = postAsDOM("wfs", getFeature);
//        print(dom);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert = 
        "<wfs:Transaction service='WFS' version='2.0.0' xmlns:cgf='http://www.opengis.net/cite/geometry' " + 
        "  xmlns:fes='" + FES.NAMESPACE + "' " +
        "  xmlns:wfs='" + WFS.NAMESPACE + "' " +
        "  xmlns:gml='" + GML.NAMESPACE + "'> " +  
        " <wfs:Insert srsName='EPSG:32615'> " + 
        "  <cgf:Points> " + 
        "   <cgf:pointProperty> " + 
        "    <gml:Point> " + 
        "     <gml:pos>1 1</gml:pos> " + 
        "    </gml:Point> " + 
        "   </cgf:pointProperty> " + 
        "   <cgf:id>t0003</cgf:id> " + 
        "  </cgf:Points> " + 
        " </wfs:Insert> " + 
        "</wfs:Transaction>"; 

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);

        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());

        // do another get feature
        getFeature = "<wfs:GetFeature " + "service=\"WFS\" version=\"2.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" " + 
                "  xmlns:fes='" + FES.NAMESPACE + "' " +
                "  xmlns:wfs='" + WFS.NAMESPACE + "' " +
                "  xmlns:gml='" + GML.NAMESPACE + "'> " +  
                "<wfs:Query typeNames=\"cgf:Points\"> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);

        NodeList pointsList = dom.getElementsByTagName("cgf:Points");
        assertEquals(n + 1, pointsList.getLength());
    }

    @Test
    public void testInsertWithGMLProperties() throws Exception {
    
         String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
             "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
             "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " + 
             "xmlns:gml='" + GML.NAMESPACE + "'>" +  
             "<wfs:Insert>" +    
               "<sf:WithGMLProperties>" + 
                  "<gml:location>" + 
                       "<gml:Point>" + 
                          "<gml:coordinates>2,2</gml:coordinates>" + 
                       "</gml:Point>" + 
                   "</gml:location>" + 
                   "<gml:name>two</gml:name>" + 
                   "<sf:foo>2</sf:foo>" +
                 "</sf:WithGMLProperties>" + 
               "</wfs:Insert>" + 
             "</wfs:Transaction>";
         
         Document dom = postAsDOM("wfs", xml);
         assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
         
         Element inserted = getFirstElementByTagName(dom, "wfs:totalInserted");
         assertEquals( "1", inserted.getFirstChild().getNodeValue());
         
         dom = getAsDOM("wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");
         NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
         assertEquals( 2, features.getLength() );
         
         Element feature = (Element) features.item( 1 );
         assertEquals( "two", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
         assertEquals( "2", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
         
         Element location = getFirstElementByTagName( feature, "gml:location" );
         Element pos = getFirstElementByTagName(location, "gml:pos");
         
        assertEquals("2 2", pos.getFirstChild().getNodeValue());
         
         xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
         "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
         "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " + 
         "xmlns:gml='" + GML.NAMESPACE + "'>" +  
         "<wfs:Insert>" +    
           "<sf:WithGMLProperties>" + 
              "<sf:location>" + 
                   "<gml:Point>" + 
                      "<gml:coordinates>3,3</gml:coordinates>" + 
                   "</gml:Point>" + 
               "</sf:location>" + 
               "<sf:name>three</sf:name>" +
               "<sf:foo>3</sf:foo>" +
             "</sf:WithGMLProperties>" + 
           "</wfs:Insert>" + 
         "</wfs:Transaction>";
         
         dom = postAsDOM("wfs", xml);
         
         assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
         
         dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
         
         features = dom.getElementsByTagName("sf:WithGMLProperties");
         assertEquals( 3, features.getLength() );
         
         feature = (Element) features.item( 2 );
         assertEquals( "three", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
         assertEquals( "3", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
         
         location = getFirstElementByTagName( feature, "gml:location" );
         pos = getFirstElementByTagName(location, "gml:pos");
         
        assertEquals("3 3", pos.getFirstChild().getNodeValue());
    }
    
    @Test
    public void testUpdateWithGMLProperties() throws Exception {
        String xml = 
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
               "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " +
               "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
               "xmlns:fes='" + FES.NAMESPACE + "' " + 
               "xmlns:gml='" + GML.NAMESPACE + "'>" +
               " <wfs:Update typeName=\"sf:WithGMLProperties\">" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>gml:name</wfs:ValueReference>" +
               "     <wfs:Value>two</wfs:Value>" +
               "   </wfs:Property>" + 
               "   <wfs:Property>" +
               "     <wfs:ValueReference>gml:location</wfs:ValueReference>" +
               "     <wfs:Value>" +
               "        <gml:Point>" + 
               "          <gml:coordinates>2,2</gml:coordinates>" + 
               "        </gml:Point>" + 
               "     </wfs:Value>" +
               "   </wfs:Property>" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:foo</wfs:ValueReference>" +
               "     <wfs:Value>2</wfs:Value>" +
               "   </wfs:Property>" +
               "   <fes:Filter>" +
               "     <fes:PropertyIsEqualTo>" +
               "       <fes:ValueReference>foo</fes:ValueReference>" + 
               "       <fes:Literal>1</fes:Literal>" + 
               "     </fes:PropertyIsEqualTo>" + 
               "   </fes:Filter>" +
               " </wfs:Update>" +
              "</wfs:Transaction>"; 

        Document dom = postAsDOM( "wfs", xml );
        assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals( "1", updated.getFirstChild().getNodeValue());
        
        dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
        NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals( 1, features.getLength() );
     
        Element feature = (Element) features.item( 0 );
        assertEquals( "two", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
        assertEquals( "2", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
        
        Element location = getFirstElementByTagName( feature, "gml:location" );
        Element pos = getFirstElementByTagName(location, "gml:pos");
        
        assertEquals("2 2", pos.getFirstChild().getNodeValue());
        
        xml = 
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
                "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " +
                "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
                "xmlns:fes='" + FES.NAMESPACE + "' " + 
                "xmlns:gml='" + GML.NAMESPACE + "'>" +
             " <wfs:Update typeName=\"sf:WithGMLProperties\">" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:name</wfs:ValueReference>" +
               "     <wfs:Value>trhee</wfs:Value>" +
               "   </wfs:Property>" + 
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:location</wfs:ValueReference>" +
               "     <wfs:Value>" +
               "        <gml:Point>" + 
               "          <gml:coordinates>3,3</gml:coordinates>" + 
               "        </gml:Point>" + 
               "     </wfs:Value>" +
               "   </wfs:Property>" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:foo</wfs:ValueReference>" +
               "     <wfs:Value>3</wfs:Value>" +
               "   </wfs:Property>" +
               "   <fes:Filter>" +
               "     <fes:PropertyIsEqualTo>" +
               "       <fes:ValueReference>foo</fes:ValueReference>" + 
               "       <fes:Literal>2</fes:Literal>" + 
               "     </fes:PropertyIsEqualTo>" + 
               "   </fes:Filter>" +
               " </wfs:Update>" +
              "</wfs:Transaction>"; 

        dom = postAsDOM( "wfs", xml );
        assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals( "1", updated.getFirstChild().getNodeValue());
        
        dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
        
        features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals( 1, features.getLength() );
     
        feature = (Element) features.item( 0 );
        assertEquals( "trhee", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
        assertEquals( "3", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
        
        location = getFirstElementByTagName( feature, "gml:location" );
        pos = getFirstElementByTagName(location, "gml:pos");
        
        assertEquals("3 3", pos.getFirstChild().getNodeValue());
    }
    
    @Test
    public void testInsertWithBoundedBy() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' "
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:BasicPolygons>"
            + " <gml:boundedBy>"
            + "  <gml:Envelope>"
            + "<gml:lowerCorner>-1.0 2.0</gml:lowerCorner>"
            + "<gml:upperCorner>2.0 5.0</gml:upperCorner>"
            + "  </gml:Envelope>"
            + " </gml:boundedBy>"
            + "  <cite:the_geom>"
            + "    <gml:MultiPolygon>"
            + "      <gml:polygonMember>" 
            + "         <gml:Polygon>" 
            + "<gml:exterior>" 
            + "<gml:LinearRing>" 
            + "<gml:posList>-1.0 5.0 2.0 5.0 2.0 2.0 -1.0 2.0 -1.0 5.0</gml:posList>" 
            + "</gml:LinearRing>" 
            + "</gml:exterior>" 
            + "         </gml:Polygon>" 
            + "      </gml:polygonMember>"
            + "    </gml:MultiPolygon>"
            + "  </cite:the_geom>"
            + "  <cite:ID>foo</cite:ID>"
            + " </cite:BasicPolygons>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
        assertTrue(dom.getElementsByTagName("fes:ResourceId").getLength() > 0);
    }
    
    @Test
    public void testInsert2() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' "
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:RoadSegments>"
            + "  <cite:the_geom>"
            + "<gml:MultiCurve srsName=\"EPSG:4326\">"
            + " <gml:curveMember>"
            + "   <gml:LineString>"
            + "        <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
            + "   </gml:LineString>"
            + " </gml:curveMember>"
            + "</gml:MultiCurve>"
            + "  </cite:the_geom>"
            + "  <cite:FID>foo</cite:FID>"
            + "  <cite:NAME>bar</cite:NAME>" 
            + " </cite:RoadSegments>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
        
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:RoadSegments&srsName=EPSG:4326&" +
    		"cql_filter=FID%3D'foo'");
        print(dom);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 1, dom.getElementsByTagName("cite:RoadSegments").getLength() );
        
        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments" );
        Element posList = getFirstElementByTagName( roadSegment, "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );
        assertEquals( 4, pos.length );
        assertEquals( 4.2582, Double.parseDouble( pos[0] ), 1E-4 );
        assertEquals( 52.0643, Double.parseDouble( pos[1] ), 1E-4 );
        assertEquals( 4.2584, Double.parseDouble( pos[2] ), 1E-4 );
        assertEquals( 52.0648, Double.parseDouble( pos[3] ), 1E-4 );
    }

    @Test
    public void testInsert3() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
                + " xmlns:wfs='" + WFS.NAMESPACE + "' "
                + " xmlns:gml='" + GML.NAMESPACE + "' "
                + " xmlns:cite=\"http://www.opengis.net/cite\">"
                + "<wfs:Insert>"
                + " <cite:Buildings>"
                + "  <cite:the_geom>" + 
                "<gml:MultiSurface> " + 
                " <gml:surfaceMember> " + 
                "  <gml:Polygon> " + 
                "   <gml:exterior> " + 
                "    <gml:LinearRing> " + 
                "     <gml:posList>-123.9 40.0 -124.0 39.9 -124.1 40.0 -124.0 40.1 -123.9 40.0</gml:posList>" + 
                "    </gml:LinearRing> " + 
                "   </gml:exterior> " + 
                "  </gml:Polygon> " + 
                " </gml:surfaceMember> " + 
                "</gml:MultiSurface> " 
                + "  </cite:the_geom>"
                + "  <cite:FID>115</cite:FID>"
                + "  <cite:ADDRESS>987 Foo St</cite:ADDRESS>" 
                + " </cite:Buildings>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";
        
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings&srsName=EPSG:4326&" +
            "cql_filter=FID%3D'115'");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );

        assertEquals( 1, dom.getElementsByTagName("cite:Buildings").getLength() );
        XMLAssert.assertXpathExists("//gml:Polygon",dom);
        
        Element posList = getFirstElementByTagName( dom.getDocumentElement(), "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );

        assertEquals( 10, pos.length );
        assertEquals( -123.9, Double.parseDouble( pos[0] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[1] ), 1E-1 );
        assertEquals( -124.0, Double.parseDouble( pos[2] ), 1E-1 );
        assertEquals( 39.9, Double.parseDouble( pos[3] ), 1E-1 );
        
        assertEquals( -124.1, Double.parseDouble( pos[4] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[5] ), 1E-1 );
        assertEquals( -124.0, Double.parseDouble( pos[6] ), 1E-1 );
        assertEquals( 40.1, Double.parseDouble( pos[7] ), 1E-1 );
        
        assertEquals( -123.9, Double.parseDouble( pos[8] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[9] ), 1E-1 );
    }

    @Test
    public void testUpdateForcedSRS() throws Exception {
        testUpdate("srsName=\"EPSG:4326\"");
    }
    
    @Test
    public void testUpdateNoSRS() throws Exception {
        testUpdate("");
    }
    
    private void testUpdate(String srs) throws Exception {
        String xml =
        "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
        "xmlns:cite=\"http://www.opengis.net/cite\" " +
        "xmlns:fes='" + FES.NAMESPACE + "' " + 
        "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
        "xmlns:gml='" + GML.NAMESPACE + "'>" + 
        " <wfs:Update typeName=\"cite:RoadSegments\">" +
        "   <wfs:Property>" +
        "     <wfs:ValueReference>cite:the_geom</wfs:ValueReference>" +
        "     <wfs:Value>" +
        "      <gml:MultiCurve " + srs + ">" + 
        "       <gml:curveMember>" + 
        "         <gml:LineString>" +
        "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>" +
        "         </gml:LineString>" +
        "       </gml:curveMember>" +
        "      </gml:MultiCurve>" +
        "     </wfs:Value>" +
        "   </wfs:Property>" + 
        "   <fes:Filter>" +
        "     <fes:PropertyIsEqualTo>" +
        "       <fes:ValueReference>FID</fes:ValueReference>" + 
        "       <fes:Literal>102</fes:Literal>" + 
        "     </fes:PropertyIsEqualTo>" + 
        "   </fes:Filter>" +
        " </wfs:Update>" +
       "</wfs:Transaction>"; 
        
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
        
        String srsBlock = "".equals(srs) ? "" : "&" + srs.replaceAll("\"", "");
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:RoadSegments" + srsBlock + "&" +
            "cql_filter=FID%3D'102'");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 1, dom.getElementsByTagName("cite:RoadSegments").getLength() );
        
        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments" );
        Element posList = getFirstElementByTagName( roadSegment, "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );
        assertEquals( 4, pos.length );
        assertEquals( 4.2582, Double.parseDouble( pos[0] ), 1E-4 );
        assertEquals( 52.0643, Double.parseDouble( pos[1] ), 1E-4 );
        assertEquals( 4.2584, Double.parseDouble( pos[2] ), 1E-4 );
        assertEquals( 52.0648, Double.parseDouble( pos[3] ), 1E-4 );
    }
    
    @Test
    public void testUpdateWithInvalidProperty() throws Exception {
        String xml =
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:cite=\"http://www.opengis.net/cite\"" +
            " xmlns:fes='" + FES.NAMESPACE + "' " + 
            " xmlns:wfs='" + WFS.NAMESPACE + "' " + 
            " xmlns:gml='" + GML.NAMESPACE + "'>" +
            " <wfs:Update typeName=\"cite:RoadSegments\">" +
            "   <wfs:Property>" +
            "     <wfs:ValueReference>INVALID</wfs:ValueReference>" +
            "     <wfs:Value>INVALID</wfs:Value>" +
            "   </wfs:Property>" + 
            "   <fes:Filter>" +
            "     <fes:PropertyIsEqualTo>" +
            "       <fes:ValueReference>FID</fes:ValueReference>" + 
            "       <fes:Literal>102</fes:Literal>" + 
            "     </fes:PropertyIsEqualTo>" + 
            "   </fes:Filter>" +
            " </wfs:Update>" +
           "</wfs:Transaction>"; 
            
            Document dom = postAsDOM( "wfs", xml );
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    @Test
    public void testInsertLayerQualified() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:fes='" + FES.NAMESPACE + "' " 
            + " xmlns:wfs='" + WFS.NAMESPACE + "' " 
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:RoadSegments>"
            + "  <cite:the_geom>"
            + "<gml:MultiCurve xmlns:gml=\"http://www.opengis.net/gml\""
            + "    srsName=\"EPSG:4326\">"
            + " <gml:curveMember>"
            + "                  <gml:LineString>"
            + "                   <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
            + "                 </gml:LineString>"
            + "               </gml:curveMember>"
            + "             </gml:MultiCurve>"
            + "  </cite:the_geom>"
            + "  <cite:FID>foo</cite:FID>"
            + "  <cite:NAME>bar</cite:NAME>" 
            + " </cite:RoadSegments>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM( "cite/Forests/wfs", xml );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        
        dom = postAsDOM( "cite/RoadSegments/wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

    }
    
    @Test
    public void testUpdateLayerQualified() throws Exception {
        String xml =
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" 
            + " xmlns:fes='" + FES.NAMESPACE + "' " 
            + " xmlns:wfs='" + WFS.NAMESPACE + "' " 
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">" + 
            " <wfs:Update typeName=\"RoadSegments\">" +
            "   <wfs:Property>" +
            "     <wfs:ValueReference>cite:the_geom</wfs:ValueReference>" +
            "     <wfs:Value>" +
            "      <gml:MultiCurve>" + 
            "       <gml:curveMember>" + 
            "         <gml:LineString>" +
            "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>" +
            "         </gml:LineString>" +
            "       </gml:curveMember>" +
            "      </gml:MultiCurve>" +
            "     </wfs:Value>" +
            "   </wfs:Property>" + 
            "   <fes:Filter>" +
            "     <fes:PropertyIsEqualTo>" +
            "       <fes:ValueReference>FID</fes:ValueReference>" + 
            "       <fes:Literal>102</fes:Literal>" + 
            "     </fes:PropertyIsEqualTo>" + 
            "   </fes:Filter>" +
            " </wfs:Update>" +
           "</wfs:Transaction>";
            
        Document dom = postAsDOM( "cite/Forests/wfs", xml );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        
        dom = postAsDOM("cite/RoadSegments/wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
    }

    @Test
    public void testUpdateWithDifferentPrefix() throws Exception {
       String xml =
           "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:fes='" + FES.NAMESPACE + "' " +  
            " xmlns:wfs='" + WFS.NAMESPACE + "' " + 
            " xmlns:gml='" + GML.NAMESPACE + "'>" + 
           " <wfs:Update xmlns:foo=\"http://www.opengis.net/cite\" typeName=\"foo:RoadSegments\">" +
           "   <wfs:Property>" +
           "     <wfs:ValueReference>foo:the_geom</wfs:ValueReference>" +
           "     <wfs:Value>" +
           "     </wfs:Value>" +
           "   </wfs:Property>" + 
           "   <fes:Filter>" +
           "     <fes:PropertyIsEqualTo>" +
           "       <fes:ValueReference>FID</fes:ValueReference>" + 
           "       <fes:Literal>102</fes:Literal>" + 
           "     </fes:PropertyIsEqualTo>" + 
           "   </fes:Filter>" +
           " </wfs:Update>" +
          "</wfs:Transaction>";
       
       Document dom = postAsDOM( "wfs", xml );
       assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
       
       Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
       assertEquals( "1", updated.getFirstChild().getNodeValue());
   }

    @Test
    public void testReplace() throws Exception {
       Document dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
           "&cql_filter=FID+EQ+'102'");
       XMLAssert.assertXpathExists("//cite:RoadSegments/cite:FID[text() = '102']", dom);
       
       String xml =
           "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:fes='" + FES.NAMESPACE + "' " +  
            " xmlns:wfs='" + WFS.NAMESPACE + "' " +
            " xmlns:gml='" + GML.NAMESPACE + "' " +
            " xmlns:cite='http://www.opengis.net/cite'>" + 
           " <wfs:Replace>" + 
           "  <cite:RoadSegments gml:id='RoadSegments.1107532045088'> " + 
           "      <cite:the_geom> " + 
           "         <gml:MultiCurve srsDimension='2' srsName='urn:x-ogc:def:crs:EPSG:4326'> " + 
           "          <gml:curveMember> " + 
           "            <gml:LineString> " + 
           "              <gml:posList>1 2 3 5 6 7</gml:posList> " + 
           "            </gml:LineString> " + 
           "          </gml:curveMember> " + 
           "        </gml:MultiCurve> " + 
           "      </cite:the_geom> " + 
           "      <cite:FID>1234</cite:FID> " + 
           "      <cite:NAME>Route 1234</cite:NAME> " + 
           "   </cite:RoadSegments> " + 

           "   <fes:Filter>" + 
           "     <fes:PropertyIsEqualTo>" +
           "       <fes:ValueReference>FID</fes:ValueReference>" + 
           "       <fes:Literal>102</fes:Literal>" + 
           "     </fes:PropertyIsEqualTo>" + 
           "   </fes:Filter>" +
           " </wfs:Replace>" +
          "</wfs:Transaction>";
       
       dom = postAsDOM("wfs", xml);
       assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
       XMLAssert.assertXpathExists("//wfs:totalReplaced[text() = 1]", dom);
       XMLAssert.assertXpathExists("//wfs:ReplaceResults/wfs:Feature/fes:ResourceId", dom);

       dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
               "&cql_filter=FID+EQ+'102'");
       XMLAssert.assertXpathNotExists("//cite:RoadSegments/cite:FID[text() = '102']", dom);
       
       dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
               "&cql_filter=FID+EQ+'1234'");
       XMLAssert.assertXpathExists("//cite:RoadSegments/cite:FID[text() = '1234']", dom);
   }
   
   @Test
   public void testReplaceOnTransactionalLevel() throws Exception {
       GeoServer gs = getGeoServer();
       WFSInfo wfs = gs.getService(WFSInfo.class);
       wfs.setServiceLevel(WFSInfo.ServiceLevel.TRANSACTIONAL);
       gs.save(wfs);
       try {
           testReplace();
       } finally {
           wfs.setServiceLevel(WFSInfo.ServiceLevel.COMPLETE);
           gs.save(wfs);
       }
   }

    @Test
    public void testSOAP() throws Exception {
       String xml = 
          "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
               " <soap:Header/> " + 
               " <soap:Body>"
                 +  "<wfs:Transaction service='WFS' version='2.0.0' "
                       + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                       + "xmlns:fes='" + FES.NAMESPACE + "' "
                       + "xmlns:wfs='" + WFS.NAMESPACE + "' "
                       + "xmlns:gml='" + GML.NAMESPACE + "'> " 
                       + "<wfs:Insert > " 
                         + "<cgf:Points>"
                           + "<cgf:pointProperty>" 
                             + "<gml:Point>" 
                             + "<gml:pos>20 40</gml:pos>"
                             + "</gml:Point>" 
                           + "</cgf:pointProperty>"
                         + "<cgf:id>t0002</cgf:id>"
                       + "</cgf:Points>" 
                     + "</wfs:Insert>" 
                  + "</wfs:Transaction>" + 
               " </soap:Body> " + 
           "</soap:Envelope> "; 
             
       MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
       assertEquals("application/soap+xml", resp.getContentType());
       
       Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
       assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
       assertEquals(1, dom.getElementsByTagName("wfs:TransactionResponse").getLength());
   }

    @Test
    public void elementHandlerOrder() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath());
        cat.add(ds);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("bar");
        tb.add("name", String.class);
        tb.add("geom", Point.class);

        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("bar");

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\""
                + " xmlns:fes='" + FES.NAMESPACE + "' "
                + " xmlns:wfs='" + WFS.NAMESPACE + "' "
                + " xmlns:gml='" + GML.NAMESPACE + "' "
                + " xmlns:cite='http://www.opengis.net/cite'"
                + " xmlns:gs='" + SystemTestData.DEFAULT_URI + "'>"
                + "<wfs:Insert idgen='UseExisting'>"
                + " <gs:bar gml:id='1'>"
                + "    <gs:name>acme</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='2'>"
                + "    <gs:name>wiley</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='3'>"
                + "    <gs:name>bugs</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='4'>"
                + "    <gs:name>roadrunner</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='5'>"
                + "    <gs:name>daffy</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='6'>"
                + "    <gs:name>elmer</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='7'>"
                + "    <gs:name>tweety</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='8'>"
                + "    <gs:name>sylvester</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='9'>"
                + "    <gs:name>marvin</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='10'>"
                + "    <gs:name>yosemite</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='11'>"
                + "    <gs:name>porky</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='12'>"
                + "    <gs:name>speedy</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='13'>"
                + "    <gs:name>taz</gs:name>"
                + " </gs:bar>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        dom = getAsDOM("wfs?request=GetFeature&service=wfs&version=2.0.0&typeNames=gs:bar");

        NodeList elementsByTagName = dom.getElementsByTagName("gs:bar");
        for (int i=1; i<=elementsByTagName.getLength(); i++) {
            String id = elementsByTagName.item(i-1).getAttributes().item(0).getNodeValue();
            assertEquals("bar."+i, id );
        }

        dom = getAsDOM("wfs?request=GetFeature&version=2.0.0&service=wfs&typeNames=gs:bar&featureId=bar.5");
        XMLAssert.assertXpathEvaluatesTo("daffy", "//gml:name/text()", dom);
    }
    
    @Test
    public void testInsertUnknownFeatureType() throws Exception {
        // perform an insert on an invalid feature type on a global service
        testInsertUnkonwnFeatureType("wfs");
    }

    @Test
    public void testInsertUnknownFeatureTypeWorkspaceSpecific() throws Exception {
        // perform an insert on an invalid feature type on a workspace specific service
        testInsertUnkonwnFeatureType("cgf/wfs");
    }

    public void testInsertUnkonwnFeatureType(String path) throws Exception {
        String insert =
                "<wfs:Transaction service='WFS' version='2.0.0' "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:fes='" + FES.NAMESPACE + "' "
                        + "xmlns:wfs='" + WFS.NAMESPACE + "' "
                        + "xmlns:gml='" + GML.NAMESPACE + "'> "
                        + "<wfs:Insert > "
                        + "<cgf:FooBar>"
                        + "<cgf:pointProperty>"
                        + "<gml:Point>"
                        + "<gml:pos>20 40</gml:pos>"
                        + "</gml:Point>"
                        + "</cgf:pointProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:FooBar>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";


        MockHttpServletResponse response = postAsServletResponse(path, insert);
        assertEquals(400, response.getStatus());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        checkOws11Exception(dom, "2.0.0", "InvalidValue", "Transaction");
    }

    @Test
    public void testUpdateBoundedByWithKML() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setCiteCompliant(true);
        gs.save(wfs);

        try {
            // this comes from CITE WFS 2.0 tests... enough said
            String xml = "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs/2.0\"\n" +
                    "                 xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
                    "                 service=\"WFS\"\n" +
                    "                 version=\"2.0.0\">\n" +
                    "  <wfs:Update xmlns:ns17=\"http://cite.opengeospatial.org/gmlsf\"\n" +
                    "               typeName=\"ns17:AggregateGeoFeature\">\n" +
                    "      <wfs:Property>\n" +
                    "         <wfs:ValueReference action=\"replace\">gml:boundedBy</wfs:ValueReference>\n" +
                    "         <wfs:Value>\n" +
                    "            <Point xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "               <coordinates>-123.1,49.25</coordinates>\n" +
                    "            </Point>\n" +
                    "         </wfs:Value>\n" +
                    "      </wfs:Property>\n" +
                    "  </wfs:Update>\n" +
                    "</wfs:Transaction>";

            MockHttpServletResponse response = postAsServletResponse("wfs", xml);
            assertEquals(400, response.getStatus());
            Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
            checkOws11Exception(dom, "2.0.0", WFSException.INVALID_VALUE, "boundedBy");
        } finally {
            wfs.setCiteCompliant(false);
            gs.save(wfs);
        }
    }
    
    @Test
    public void testUpdateOnSimpleXPath() throws Exception {
        // from CITE tests WFS 2.0 again
        String xml = "<wfs:Transaction xmlns:wfs=\"http://www.opengis.net/wfs/2.0\"\n" +
                "                 xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
                "                 xmlns:tns=\"http://cite.opengeospatial.org/gmlsf\"\n" +
                "                 service=\"WFS\"\n" +
                "                 version=\"2.0.0\">\n" +
                "   <wfs:Update handle=\"Update\" typeName=\"tns:GenericEntity\">\n" +
                "      <wfs:Property>\n" +
                "         <wfs:ValueReference>tns:featureRef[1]</wfs:ValueReference>\n" +
                "         <wfs:Value>TEST_VALUE</wfs:Value>\n" +
                "      </wfs:Property>\n" +
                "      <fes:Filter xmlns:fes=\"http://www.opengis.net/fes/2.0\">\n" +
                "         <fes:ResourceId rid=\"GenericEntity.f004\"/>\n" +
                "      </fes:Filter>\n" +
                "   </wfs:Update>\n" +
                "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//wfs:totalUpdated[text() = 1]", dom);

        // query back and verify it was updated
        dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=sf:GenericEntity" +
                "&resourceId=GenericEntity.f004");
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//sf:GenericEntity)", dom);
        XMLAssert.assertXpathEvaluatesTo("TEST_VALUE", "//sf:featureRef", dom);

    }
    
    @Test
    public void testInsertPreserveId() throws Exception {
        WFSInfo wfs = getWFS();
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_20);
        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);
        getGeoServer().reset();

        try {
            String xml = "<wfs:Transaction service='WFS' version='2.0.0' "
                    + " xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:gml='" + GML.NAMESPACE + "' "
                    + " xmlns:sf='http://cite.opengeospatial.org/gmlsf'>"
                    + "<wfs:Insert handle='insert-1'>"
                    + " <sf:PrimitiveGeoFeatureId gml:id='PrimitiveGeoFeatureId.gmlsf0-f01'>"
                    + "  <gml:description>"
                    + "Fusce tellus ante, tempus nonummy, ornare sed, accumsan nec, leo."
                    + "Vivamus pulvinar molestie nisl."
                    + "</gml:description>"
                    + "<gml:name>Aliquam condimentum felis sit amet est.</gml:name>"
                    + "<gml:identifier codeSpace=\"fooBar\">PrimitiveGeoFeatureId.gmlsf0-f01</gml:identifier>"
                    //+ "<gml:name codeSpace='http://cite.opengeospatial.org/gmlsf'>cite.gmlsf0-f01</gml:name>"
                    + "<sf:curveProperty>"
                    + "  <gml:LineString gml:id='cite.gmlsf0-g01' srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
                    + "   <gml:posList>47.608284 19.034142 51.286873 16.7836 49.849854 15.764992</gml:posList>"
                    + " </gml:LineString>"
                    + "</sf:curveProperty>"
                    + "<sf:intProperty>1025</sf:intProperty>"
                    + "<sf:measurand>7.405E2</sf:measurand>"
                    + "<sf:dateTimeProperty>2006-06-23T12:43:12+01:00</sf:dateTimeProperty>"
                    + "<sf:decimalProperty>90.62</sf:decimalProperty>"
                    + "</sf:PrimitiveGeoFeatureId>"
                    + "</wfs:Insert>"
                    + "</wfs:Transaction>";


            Document dom = postAsDOM("wfs", xml, 200);
            assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
            XMLAssert.assertXpathExists("//wfs:totalInserted[text() = 1]", dom);

            // get it back, if it's not found we'll get a 404 not a 200
            dom = getAsDOM("wfs?request=GetFeature&version=2.0.0&storedQueryId=" +
                    StoredQuery.DEFAULT.getName() + "&ID=PrimitiveGeoFeatureId.gmlsf0-f01", 200);
        } finally {
            gml.setOverrideGMLAttributes(true);
            getGeoServer().save(wfs);
        }

    }
}
