package org.geoserver.monitor.ows.wfs;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Traverses a filter tree looking for geometry literals and BBOX filters and creates an overall 
 * BBox around them.  If none are found it generates a null bounding box.
 * 
 * @author Kevin Smith, OpenGeo
 */
public class BBoxFilterVisitor extends DefaultFilterVisitor implements
    FilterVisitor {
    
    BoundingBox bbox;
    CoordinateReferenceSystem defaultCrs;
    
    public BBoxFilterVisitor(CoordinateReferenceSystem crs, CoordinateReferenceSystem defaultCrs) {
        this.bbox = new ReferencedEnvelope(crs);
        this.defaultCrs = defaultCrs;
    }
    
    public BBoxFilterVisitor(BoundingBox bbox, CoordinateReferenceSystem defaultCrs) {
        this.bbox = new ReferencedEnvelope(bbox);
        this.defaultCrs = defaultCrs;
    }
    
    public BoundingBox getBbox() {
        return bbox;
    }
    
    void addBbox(BoundingBox bbox) {
        try {
            if(this.bbox!=null){
                if (bbox.getCoordinateReferenceSystem() == null) {
                    bbox = ReferencedEnvelope.reference(bbox, defaultCrs);
                }
                this.bbox.include(bbox.toBounds(this.bbox.getCoordinateReferenceSystem()));
            }
        } catch (TransformException e) {
            this.bbox=null;
        }
    }
    
    @Override
    public Object visit(BBOX filter, Object data)  {
        addBbox(filter.getBounds());
        return data;
    }
    
 /*   @Override
    public Object visit(Not filter, Object data) {
        // Ignore anything within a NOT filter by not calling super.
        return data;
    }
*/
    static Geometry getGeometryFromExpression(Expression e){
         if(e instanceof Literal){
            Object value = ((Literal)e).getValue();
            if(value instanceof Geometry){
                return (Geometry)value;
            }
        }
        return null;
    }

    static BoundingBox bboxFromBSO(BinarySpatialOperator op) {
        Geometry geom1 = getGeometryFromExpression(op.getExpression1());
        Geometry geom2 = getGeometryFromExpression(op.getExpression2());
        
        ReferencedEnvelope bbox = new ReferencedEnvelope();
        
        if(geom1!=null) bbox.expandToInclude(geom1.getEnvelopeInternal());
        if(geom2!=null) bbox.expandToInclude(geom2.getEnvelopeInternal());
        
        return bbox;
    }
    
    @Override
    public Object visit(Within filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Beyond filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Contains filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Crosses filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Disjoint filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(DWithin filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Intersects filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Overlaps filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
    }
    
    @Override
    public Object visit(Touches filter, Object data) {
        addBbox(bboxFromBSO(filter));
        return data;
        }
    
    
}
