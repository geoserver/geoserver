/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global.dto;

import junit.framework.TestCase;

import org.vfny.geoserver.global.xml.GMLSchemaTranslator;
import org.vfny.geoserver.global.xml.NameSpaceTranslatorFactory;
import org.vfny.geoserver.global.xml.XMLSchemaTranslator;

/**
 * @author Jody Garnett
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DataTransferObjectFactoryTest extends TestCase {
    XMLSchemaTranslator xs = (XMLSchemaTranslator) NameSpaceTranslatorFactory.getInstance().getNameSpaceTranslator("xs");        
    GMLSchemaTranslator gml = (GMLSchemaTranslator) NameSpaceTranslatorFactory.getInstance().getNameSpaceTranslator("gml");
    
    public DataTransferObjectFactoryTest(String name) {
        super(name);
    }

    protected void assertString(String expected, Object value) {
        assertEquals(expected, String.valueOf(value));
    }

//    public void testPointPropertyTypeMappings() {
//        Set expected = new HashSet();
//        NameSpaceElement nse = null;
//        nse = gml.getElement("PointPropertyType");
//System.out.println("Expected PointPropertyType : "+nse.getTypeDefName());
//        expected.add( nse );
//        nse = gml.getElement("PointType");
//System.out.println("Expected PointType : "+nse.getTypeDefName());
//        expected.add( nse );
//        nse = gml.getElement("GeometryPropertyType");
//System.out.println("Expected GeometryPropertyType : "+nse.getTypeDefName());
//		expected.add( nse );
//		nse = gml.getElement("AbstractGeometryType");
//		System.out.println("Expected AbstractGeometryType : "+nse.getTypeDefName());
//        expected.add( nse );
//        nse = xs.getElement("string");
//System.out.println("Expected string : "+nse.getTypeDefName());
//        expected.add( nse );
//
//        List mappings = DataTransferObjectFactory.getElements("pointProperty", Point.class);
//Iterator i = mappings.iterator();
//while(i.hasNext()){
//	System.out.println("Result : "+((NameSpaceElement)i.next()).getTypeDefName());
//}
//System.out.println("Result End");
//        assertEquals(expected, new HashSet(mappings));
//    }

    public void testSimpleTypes() {
/*        assertString("xs:string", GMLUtils.schema("x", String.class));
        assertString("xs:integer", GMLUtils.schema("x", BigInteger.class));
        assertString("xs:decimal", GMLUtils.schema("x", BigDecimal.class));
        assertString("xs:int", GMLUtils.schema("x", Integer.class));
        assertString("xs:long", GMLUtils.schema("x", Long.class));
        assertString("xs:short", GMLUtils.schema("x", Short.class));
        assertString("xs:byte", GMLUtils.schema("x", Byte.class));
        assertString("xs:double", GMLUtils.schema("x", Double.class));
        assertString("xs:float", GMLUtils.schema("x", Float.class));
        assertString("xs:boolean", GMLUtils.schema("x", Boolean.class));
    }

    public void testPrimativeTypes() {
        assertString("xs:int", GMLUtils.schema("x", Integer.TYPE));
        assertString("xs:long", GMLUtils.schema("x", Long.TYPE));
        assertString("xs:short", GMLUtils.schema("x", Short.TYPE));
        assertString("xs:byte", GMLUtils.schema("x", Byte.TYPE));
        assertString("xs:double", GMLUtils.schema("x", Double.TYPE));
        assertString("xs:float", GMLUtils.schema("x", Float.TYPE));
        assertString("xs:boolean", GMLUtils.schema("x", Boolean.TYPE));
    }

    public void testJTSGeometryTypes() {
        assertString("gml:AbstractGeometryType",
            GMLUtils.schema("x", Geometry.class));
        assertString("gml:PointType", GMLUtils.schema("x", Point.class));
        assertString("gml:LineStringType",
            GMLUtils.schema("x", LineString.class));
        assertString("gml:LinearRingType",
            GMLUtils.schema("x", LinearRing.class));
        assertString("gml:PolygonType", GMLUtils.schema("x", Polygon.class));
        assertString("gml:GeometryCollectionType",
            GMLUtils.schema("x", GeometryCollection.class));
        assertString("gml:MultiPointType",
            GMLUtils.schema("x", MultiPoint.class));
        assertString("gml:MultiPolygonType",
            GMLUtils.schema("x", MultiPolygon.class));
        assertString("gml:MultiLineStringType",
            GMLUtils.schema("x", MultiLineString.class));
        assertString("gml:MultiLineStringType",
            GMLUtils.schema("x", MultiLineString.class));
    }

    public void testJTSTypes() {
        assertString("gml:BoxType", GMLUtils.schema("x", Envelope.class));
        assertString("gml:CoordType", GMLUtils.schema("x", Coordinate.class));
        assertString("gml:CoordinatesType",
            GMLUtils.schema("x", CoordinateList.class));
    }

    public void testGMLPropertyTypes() {
        //assertString( "gml:AbstractGeometryPropertyType", GMLUtils.schema( "AbstractGeometry", Geometry.class ) );        
        assertString("gml:PointPropertyType",
            GMLUtils.schema("PointPropertyType", Point.class));
        assertString("gml:LineStringPropertyType",
            GMLUtils.schema("LineStringPropertyType", LineString.class));
        assertString("gml:PolygonPropertyType",
            GMLUtils.schema("PolygonPropertyType", Polygon.class));
        assertString("gml:MultiGeometryPropertyType",
            GMLUtils.schema("MultiGeometryPropertyType",
                GeometryCollection.class));
        assertString("gml:MultiPointPropertyType",
            GMLUtils.schema("MultiPointPropertyType", MultiPoint.class));
        assertString("gml:MultiPolygonPropertyType",
            GMLUtils.schema("MultiPolygonPropertyType", MultiPolygon.class));
        assertString("gml:MultiLineStringPropertyType",
            GMLUtils.schema("MultiLineStringPropertyType", MultiLineString.class));*/
    }
}
