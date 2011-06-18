package org.geoserver.wfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.MockData;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReprojectionWriteTest extends WFSTestSupport {
    private static final String TARGET_CRS_CODE = "EPSG:900913";
    public static QName NULL_GEOMETRIES = new QName(MockData.CITE_URI, "NullGeometries", MockData.CITE_PREFIX);
    public static QName GOOGLE = new QName(MockData.CITE_URI, "GoogleFeatures", MockData.CITE_PREFIX);
    MathTransform tx;
    
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        CoordinateReferenceSystem epsg4326 = CRS.decode(TARGET_CRS_CODE);
        CoordinateReferenceSystem epsg32615 = CRS.decode("EPSG:32615");
        
        tx = CRS.findMathTransform(epsg32615, epsg4326);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addPropertiesType(NULL_GEOMETRIES, 
                ReprojectionTest.class.getResource("NullGeometries.properties"), Collections.EMPTY_MAP);
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put(MockData.KEY_SRS_HANDLINGS, ProjectionPolicy.REPROJECT_TO_DECLARED);
        extra.put(MockData.KEY_SRS_NUMBER, 900913);
        dataDirectory.addPropertiesType(GOOGLE, 
                ReprojectionTest.class.getResource("GoogleFeatures.properties"), extra);
    }
    
    public void testInsertSrsName() throws Exception {
        String q = "wfs?request=getfeature&service=wfs&version=1.0.0&typeName=" + 
            MockData.POLYGONS.getLocalPart();
        Document dom = getAsDOM( q );
        
        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName( polygonProperty, "gml:coordinates");
        
        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length/2);
        
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
        + " xmlns:gml=\"http://www.opengis.net/gml\" "
        + " xmlns:cgf=\"" + MockData.CGF_URI + "\">"
        + "<wfs:Insert handle=\"insert-1\" srsName=\"" + TARGET_CRS_CODE + "\">"
        + " <cgf:Polygons>"
        +    "<cgf:polygonProperty>"
        +      "<gml:Polygon >" 
        +       "<gml:outerBoundaryIs>"
        +          "<gml:LinearRing>" 
        +             "<gml:coordinates>";
        for ( int i = 0; i < cr.length; ) {
            xml += cr[i++] + "," + cr[i++];
            if ( i < cr.length - 1 ) {
                xml += " ";
            }
        }
        xml +=        "</gml:coordinates>"
        +        "</gml:LinearRing>"
        +      "</gml:outerBoundaryIs>"
        +    "</gml:Polygon>"
        +   "</cgf:polygonProperty>"
        + " </cgf:Polygons>"
        + "</wfs:Insert>"
        + "</wfs:Transaction>";
        postAsDOM( "wfs", xml );
        
        dom = getAsDOM( q );
        
        assertEquals( 2, dom.getElementsByTagName( MockData.POLYGONS.getPrefix() + ":" + MockData.POLYGONS.getLocalPart()).getLength() );
        
    }
    
    public void testInsertGeomSrsName() throws Exception {
        String q = "wfs?request=getfeature&service=wfs&version=1.0&typeName=" + 
            MockData.POLYGONS.getLocalPart();
        Document dom = getAsDOM( q );
        
        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName( polygonProperty, "gml:coordinates");
        
        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length/2);
        
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
        + " xmlns:gml=\"http://www.opengis.net/gml\" "
        + " xmlns:cgf=\"" + MockData.CGF_URI + "\">"
        + "<wfs:Insert handle=\"insert-1\">"
        + " <cgf:Polygons>"
        +    "<cgf:polygonProperty>"
        +      "<gml:Polygon srsName=\"" + TARGET_CRS_CODE + "\">" 
        +       "<gml:outerBoundaryIs>"
        +          "<gml:LinearRing>" 
        +             "<gml:coordinates>";
        for ( int i = 0; i < cr.length; ) {
            xml += cr[i++] + "," + cr[i++];
            if ( i < cr.length - 1 ) {
                xml += " ";
            }
        }
        xml +=        "</gml:coordinates>"
        +        "</gml:LinearRing>"
        +      "</gml:outerBoundaryIs>"
        +    "</gml:Polygon>"
        +   "</cgf:polygonProperty>"
        + " </cgf:Polygons>"
        + "</wfs:Insert>"
        + "</wfs:Transaction>";
        postAsDOM( "wfs", xml );
        
        dom = getAsDOM( q );
        
        assertEquals( 2, dom.getElementsByTagName( MockData.POLYGONS.getPrefix() + ":" + MockData.POLYGONS.getLocalPart()).getLength() );
        
    }
    
    public void testUpdate() throws Exception {
        String q = "wfs?request=getfeature&service=wfs&version=1.0&typeName=" + 
        MockData.POLYGONS.getLocalPart();
        
        Document dom = getAsDOM( q );
        
        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName( polygonProperty, "gml:coordinates");
        
        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length/2);
        
        // perform an update
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:Polygons\" > " + "<wfs:Property>"
                + "<wfs:Name>polygonProperty</wfs:Name>" 
                + "<wfs:Value>" 
                +      "<gml:Polygon srsName=\"" + TARGET_CRS_CODE + "\">" 
                +       "<gml:outerBoundaryIs>"
                +          "<gml:LinearRing>" 
                +             "<gml:coordinates>";
                for ( int i = 0; i < cr.length; ) {
                    xml += cr[i++] + "," + cr[i++];
                    if ( i < cr.length - 1 ) {
                        xml += " ";
                    }
                }
                xml +=        "</gml:coordinates>"
                +        "</gml:LinearRing>"
                +      "</gml:outerBoundaryIs>"
                +    "</gml:Polygon>"
                + "</wfs:Value>" 
                + "</wfs:Property>" 
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>" + "</ogc:Filter>"
                + "</wfs:Update>" + "</wfs:Transaction>";
                
        dom = postAsDOM( "wfs", xml );
        
        assertEquals( "wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName() );
        Element success = getFirstElementByTagName(dom, "wfs:SUCCESS" );
        assertNotNull(success);
        
        dom = getAsDOM(q);
        
        polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        posList = getFirstElementByTagName( polygonProperty, "gml:coordinates");
        double[] c1 = coordinates(posList.getFirstChild().getNodeValue());

        assertEquals( c.length, c1.length );
        for ( int i = 0; i < c.length; i++ ) {
            int x = (int)(c[i] + 0.5);
            int y = (int)(c1[i] + 0.5);
            
            assertEquals(x,y);
        }
        
    }
    
    private double[] coordinates(String string) {
        StringTokenizer st = new StringTokenizer(string, " ");
        double[] coordinates = new double[st.countTokens()*2];
        int i = 0;
        while(st.hasMoreTokens()) {
            String tuple = st.nextToken();
            coordinates[i++] = Double.parseDouble(tuple.split(",")[0]);
            coordinates[i++] = Double.parseDouble(tuple.split(",")[1]);
        }
        
        return coordinates;
    }
}
