/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.CiteTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * This test must be run with the server configured with the wfs 1.0 cite
 * configuration, with data initialized.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class TransactionTest extends WFSTestSupport {

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.POINTS);
        revertLayer(CiteTestData.FIFTEEN);
        revertLayer(CiteTestData.LINES);
        revertLayer(CiteTestData.POLYGONS);
    }

    @Test
    public void testDelete() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"cgf:Points\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember")
                .getLength());

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cgf:Points\"> " + "<ogc:Filter> "
                + "<ogc:PropertyIsEqualTo> "
                + "<ogc:PropertyName>cgf:id</ogc:PropertyName> "
                + "<ogc:Literal>t0000</ogc:Literal> "
                + "</ogc:PropertyIsEqualTo> " + "</ogc:Filter> "
                + "</wfs:Delete> " + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement()
                .getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);

        assertEquals(0, dom.getElementsByTagName("gml:featureMember")
                .getLength());

    }
    
	@Test
    public void testDoubleDelete() throws Exception {
    	// see 

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"cdf:Fifteen\"/> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("15", "count(//gml:featureMember)", dom);

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> " 
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> " 
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.2\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement()
                .getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        XMLAssert.assertXpathEvaluatesTo("13", "count(//gml:featureMember)", dom);
    }

	@Test
    public void testInsert() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember")
                .getLength());

        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>" + "</gml:LineString>"
                + "</cgf:lineStringProperty>" + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>" + "</wfs:Insert>" + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
        assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember")
                .getLength());
    }
	
	@Test
    public void testInsertWithGetFeatureInThePath() throws Exception {
        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>" + "</gml:LineString>"
                + "</cgf:lineStringProperty>" + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>" + "</wfs:Insert>" + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cgf:Lines", insert);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
        assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);
    }

	@Test
    public void testUpdate() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"cgf:Polygons\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember")
                .getLength());
        assertEquals("t0002", dom.getElementsByTagName("cgf:id").item(0)
                .getFirstChild().getNodeValue());

        // perform an update
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:Polygons\" > " + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>" + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>" + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
                + "</wfs:Update>" + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals("t0003", dom.getElementsByTagName("cgf:id").item(0)
                .getFirstChild().getNodeValue());
    }
    
	@Test
    public void testUpdateLayerQualified() throws Exception {
        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"Polygons\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        Document dom = postAsDOM("cgf/Polygons/wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember")
                .getLength());
        assertEquals("t0002", dom.getElementsByTagName("cgf:id").item(0)
                .getFirstChild().getNodeValue());

        // perform an update
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"Polygons\" > " + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>" + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>" + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
                + "</wfs:Update>" + "</wfs:Transaction>";

        dom = postAsDOM("cgf/Lines/wfs", insert);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);
        
        dom = postAsDOM("cgf/Polygons/wfs", insert);

        // do another get feature
        dom = postAsDOM("cgf/Polygons/wfs", getFeature);
        assertEquals("t0003", dom.getElementsByTagName("cgf:id").item(0)
                .getFirstChild().getNodeValue());
    }

	@Test
    public void testInsertWithBoundedBy() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
            + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
            + " xmlns:gml=\"http://www.opengis.net/gml\" "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:BasicPolygons>"
            + "<gml:boundedBy>"
            + "<gml:Box>"
            + "<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">-2,-1 2,6</gml:coordinates>"
            + "</gml:Box>"
            + "</gml:boundedBy>"
            + "  <cite:the_geom>"
            + "<gml:MultiPolygon>"
            + "<gml:polygonMember>"
            + "<gml:Polygon>"
            + "<gml:outerBoundaryIs>"
            + "<gml:LinearRing>"
            + "<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">-1,0 0,1 1,0 0,-1 -1,0</gml:coordinates>"
            + "</gml:LinearRing>"
            + "</gml:outerBoundaryIs>"
            + "</gml:Polygon>"
            + "</gml:polygonMember>"
            + "</gml:MultiPolygon>"
            + "  </cite:the_geom>"
            + "  <cite:ID>foo</cite:ID>"
            + " </cite:BasicPolygons>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM("wfs", xml);
        
        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getElementsByTagName("ogc:FeatureId").getLength() > 0);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() > 0);
    }
    
	@Test
    public void testInsertWorkspaceQualified() throws Exception {
     // 1. do a getFeature
        String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"Lines\"> "
                + "<ogc:PropertyName>id</ogc:PropertyName> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        Document dom = postAsDOM("cgf/wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember")
                .getLength());

        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>" + "</gml:LineString>"
                + "</cgf:lineStringProperty>" + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>" + "</wfs:Insert>" + "</wfs:Transaction>";

        dom = postAsDOM("cgf/wfs", insert);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
        assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);

        dom = postAsDOM("sf/wfs", insert);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);
        
        // do another get feature
        dom = postAsDOM("cgf/wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember")
                .getLength());
    }
    
	@Test
    public void testInsertLayerQualified() throws Exception {
        // 1. do a getFeature
       String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
               + "version=\"1.0.0\" "
               + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
               + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
               + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
               + "<wfs:Query typeName=\"Lines\"> "
               + "<ogc:PropertyName>id</ogc:PropertyName> "
               + "</wfs:Query> " + "</wfs:GetFeature>";

       Document dom = postAsDOM("cgf/Lines/wfs", getFeature);
       assertEquals(1, dom.getElementsByTagName("gml:featureMember")
               .getLength());

       // perform an insert
       String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
               + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
               + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
               + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
               + "xmlns:gml=\"http://www.opengis.net/gml\"> "
               + "<wfs:Insert > "
               + "<cgf:Lines>"
               + "<cgf:lineStringProperty>"
               + "<gml:LineString>"
               + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
               + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
               + "</gml:coordinates>" + "</gml:LineString>"
               + "</cgf:lineStringProperty>" + "<cgf:id>t0002</cgf:id>"
               + "</cgf:Lines>" + "</wfs:Insert>" + "</wfs:Transaction>";

       dom = postAsDOM("cgf/Lines/wfs", insert);
       assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
       assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);

       dom = postAsDOM("cgf/Polygons/wfs", insert);
       XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);
       
       // do another get feature
       dom = postAsDOM("cgf/Lines/wfs", getFeature);
       assertEquals(2, dom.getElementsByTagName("gml:featureMember")
               .getLength());
   }

	@Test
   public void testUpdateWithDifferentPrefix() throws Exception {

      // 1. do a getFeature
      String getFeature = "<wfs:GetFeature " + "service=\"WFS\" "
              + "version=\"1.0.0\" "
              + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
              + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
              + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
              + "<wfs:Query typeName=\"cgf:Polygons\"> "
              + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
              + "</wfs:Query> " + "</wfs:GetFeature>";
 
      Document dom = postAsDOM("wfs", getFeature);
      assertEquals(1, dom.getElementsByTagName("gml:featureMember")
              .getLength());
      assertEquals("t0002", dom.getElementsByTagName("cgf:id").item(0)
              .getFirstChild().getNodeValue());
 
      // perform an update
      String update = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
              + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
              + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
              + "xmlns:gml=\"http://www.opengis.net/gml\"> "
              + "<wfs:Update xmlns:foo=\"http://www.opengis.net/cite/geometry\" typeName=\"foo:Polygons\" > " + "<wfs:Property>"
              + "<wfs:Name>id</wfs:Name>" + "<wfs:Value>t0003</wfs:Value>"
              + "</wfs:Property>" + "<ogc:Filter>"
              + "<ogc:PropertyIsEqualTo>"
              + "<ogc:PropertyName>id</ogc:PropertyName>"
              + "<ogc:Literal>t0002</ogc:Literal>"
              + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
              + "</wfs:Update>" + "</wfs:Transaction>";
 
      dom = postAsDOM("wfs", update);
 
      // do another get feature
      dom = postAsDOM("wfs", getFeature);
      assertEquals("t0003", dom.getElementsByTagName("cgf:id").item(0)
              .getFirstChild().getNodeValue());
   }

}
