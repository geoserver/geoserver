package org.geoserver.monitor.ows.wfs;

import static org.junit.Assert.*;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.geoserver.monitor.BBoxAsserts;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.BeforeClass;
import org.junit.Test;


public class BBoxFilterVisitorTest {
    
    static CoordinateReferenceSystem logCrs;
    static CoordinateReferenceSystem differentCrs;
    
    BoundingBox getResult(BBoxFilterVisitor visitor, Filter filter, Object data) {
        filter.accept(visitor, data);
        return visitor.getBbox();
    }
    
    @BeforeClass
    public static void setUpData() throws Exception {
        logCrs=CRS.decode("EPSG:4326");
        differentCrs=CRS.decode("EPSG:3348");
    }

    public void standardTest(String cql, BoundingBox expected){
        Filter filter = parseFilter(cql);
        BBoxFilterVisitor visitor;
        try {
            visitor = new BBoxFilterVisitor(logCrs, CRS.decode("EPSG:4326"));
        } catch (NoSuchAuthorityCodeException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
        BoundingBox result = getResult(visitor, filter, null);
        
        try {
            BBoxAsserts.assertEqualsBbox(expected.toBounds(logCrs), result, 0.01);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }
    
    static public Filter parseFilter(String cql) {
        try {
            return CQL.toFilter(cql);
        } catch (CQLException ex){
            throw new IllegalArgumentException(ex);
        }
    }

    @Test
    public void testSimpleNonSpatial() {
        standardTest(
                "a < 5000", 
                new ReferencedEnvelope() // Null Envelope
                );
    }
    
    @Test
    public void testSimpleBbox() {
        standardTest(
                "BBOX(the_geom, 40, -90, 45, -60)", 
                new ReferencedEnvelope(40, 45, -90, -60, logCrs)
                );
    }
    
    @Test
    public void testTwoBboxOr() {
        standardTest(
                "BBOX(the_geom, 40, -90, 45, -60) or BBOX(the_geom, 50, -30, 60, -10)", 
                new ReferencedEnvelope(40, 60, -90, -10, logCrs)
                );
     }
    
    @Test
    public void testTwoBboxAndDistinctFields() {
        standardTest(
                "BBOX(geom1, 40, -90, 45, -60) and BBOX(geom2, 50, -30, 60, -10)",
                new ReferencedEnvelope(40, 60, -90, -10, logCrs)
                );
        
        }
    
/*    @Test
    public void testIgnoreWithinNot() {
        standardTest(
                "BBOX(geom1, 40, -90, 45, 60) and not BBOX(geom1, 50, -30, 60, -10)",
                new ReferencedEnvelope(40, 45, -90, -60, null)
                );
        }
*/    
    @Test
    public void testWithinPoly() {
        standardTest(
                "WITHIN(the_geom,POLYGON((30 10, 15 20, 20 40, 40 45, 30 10)))",
                new ReferencedEnvelope(15, 40, 10, 45, logCrs)
                );
    }
    
    @Test
    public void testTwoBboxOrSpecificCrs() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
        Filter bboxF1 = ff.bbox("the_geom", 40, -90, 45, -60, null);
        Filter bboxF2 = ff.bbox("the_geom", 5988504.35,851278.90, 7585113.55,1950872.01, "EPSG:3348");
        Filter filter = ff.or(bboxF1, bboxF2);
        BoundingBox expected = new ReferencedEnvelope(40, 53.73, -95.1193,-60 ,logCrs);
        BBoxFilterVisitor visitor = new BBoxFilterVisitor(logCrs, logCrs);
        BoundingBox result = getResult(visitor, filter, null);
        BBoxAsserts.assertEqualsBbox(expected, result, 0.01);
     }
    
    @Test
    public void testBboxDifferentDefaultCrs() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
        Filter filter = ff.bbox("the_geom", 5988504.35,851278.90, 7585113.55,1950872.01, null);
        // xMin,yMin -95.1193,42.2802 : xMax,yMax -71.295,53.73
        BoundingBox expected = new ReferencedEnvelope(42.2802, 53.73, -95.1193,-71.295 ,logCrs);
        BBoxFilterVisitor visitor = new BBoxFilterVisitor(logCrs, differentCrs);
        BoundingBox result = getResult(visitor, filter, null);
        BBoxAsserts.assertEqualsBbox(expected, result, 0.01);
     }
    
    
}
