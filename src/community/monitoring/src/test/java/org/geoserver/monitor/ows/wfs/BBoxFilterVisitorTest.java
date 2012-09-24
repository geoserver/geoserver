package org.geoserver.monitor.ows.wfs;

import static org.junit.Assert.*;

import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.geoserver.monitor.BBoxAsserts;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;


public class BBoxFilterVisitorTest {
    
	BoundingBox getResult(BBoxFilterVisitor visitor, Filter filter, Object data) {
		filter.accept(visitor, data);
		return visitor.getBbox();
	}
	
	public void standardTest(String cql, BoundingBox expected){
        Filter filter = parseFilter(cql);
        
        BBoxFilterVisitor visitor = new BBoxFilterVisitor();
        
        BoundingBox result = getResult(visitor, filter, null);
        
        BBoxAsserts.assertEqualsBbox(expected, result, 0.01);
	}
	
    Filter parseFilter(String cql) {
        try {
            return CQL.toFilter(cql);
        } catch (CQLException ex){
             fail("Error in CQL in test case.");
             return null; // To make Java compiler happy even though it's never reached.
        }
    }

    @Test
    public void testSimpleNonSpatial() {
        Filter filter = parseFilter("a < 5000");
        
        BBoxFilterVisitor visitor = new BBoxFilterVisitor();
        
        BoundingBox result = getResult(visitor, filter, null);
        
        assertNull(result);
        
    }
    
    @Test
    public void testSimpleBbox() {
    	standardTest(
    			"BBOX(the_geom, -90, 40, -60, 45)", 
    			new ReferencedEnvelope(-90, -60, 40, 45, null)
    			);
    }
    
    @Test
    public void testTwoBboxOr() {
        standardTest(
        		"BBOX(the_geom, -90, 40, -60, 45) or BBOX(the_geom, -30, 50, -10, 60)", 
        		new ReferencedEnvelope(-90, -10, 40, 60, null)
        		);
     }
    
    @Test
    public void testTwoBboxAndDistinctFields() {
    	standardTest(
    			"BBOX(geom1, -90, 40, -60, 45) or BBOX(geom2, -30, 50, -10, 60)",
    			new ReferencedEnvelope(-90, -10, 40, 60, null)
    			);

        }
    
    @Test
    public void testIgnoreWithinNot() {
    	standardTest(
    			"BBOX(geom1, -90, 40, -60, 45) and not BBOX(geom1, -30, 50, -10, 60)",
    			new ReferencedEnvelope(-90, -60, 40, 45, null)
    			);
        }
    
    @Test
    public void testWithinPoly() {
    	standardTest(
    			"WITHIN(the_geom,POLYGON((30 10, 15 20, 20 40, 40 45, 30 10)))",
    			new ReferencedEnvelope(15, 40, 10, 45, null)
    			);
    }
    
    
}
