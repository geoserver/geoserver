package org.geoserver.monitor.ows.wfs;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Within;
import org.opengis.geometry.BoundingBox;

/**
 * Traverses a filter tree and determines a bounding box for the area covered by it.  
 * 
 * In the case of complex or unusual queries it only makes a reasonable effort to figure out the general area of interest.
 * 
 * @author Kevin Smith, OpenGeo
 */
public class BBoxFilterVisitor extends DefaultFilterVisitor implements
    FilterVisitor {
    
    BoundingBox bbox;
    
    public BoundingBox getBbox() {
        return bbox;
    }
    
    @Override
    public Object visit(BBOX filter, Object data) {
        if(bbox==null){
            bbox=filter.getBounds();
        } else {
            bbox.include(filter.getBounds());
        }
        return super.visit(filter, data);
    }
    
    @Override
    public Object visit(Not filter, Object data) {
        // Ignore anything within a NOT filter by not calling super.
        return data;
    }

    @Override
    public Object visit(Within filter, Object data) {
        Expression e = filter.getExpression1();
        
        return super.visit(filter, data);
    }
    
    
}
