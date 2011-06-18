/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.util.List;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Returns a clone of the provided filter where all geometries and bboxes have
 * been reprojected to the CRS of the associated attributes. The working
 * assumption is that the filters specified are strictly compliant with the OGC
 * spec, so the first item is always a {@link PropertyName}, and the second
 * always a {@link Literal}
 * 
 * @author Andrea Aime - The Open Planning Project
 * 
 */
public class ReprojectingFilterVisitor extends DuplicatingFilterVisitor {
    FeatureType featureType;

    public ReprojectingFilterVisitor(FilterFactory2 factory, FeatureType featureType) {
        super(factory);
        this.featureType = featureType;
    }

    /**
     * Returns the CRS associated to a property in the feature type. May be null
     * if the property is not geometric, or if the CRS is not set
     * 
     * @param propertyName
     * @return
     */
    private CoordinateReferenceSystem findPropertyCRS(PropertyName propertyName) {
        AttributeDescriptor at = (AttributeDescriptor) propertyName.evaluate(featureType);
        if (at instanceof GeometryDescriptor) {
            GeometryDescriptor gat = (GeometryDescriptor) at;
            return gat.getCoordinateReferenceSystem();
        } else {
            return null;
        }
    }

    public Object visit(BBOX filter, Object extraData) {
        // if no srs is specified we can't transform anyways
        String srs = filter.getSRS();
        if (srs == null || "".equals(srs.trim()))
            return super.visit(filter, extraData);

        try {
            // grab the original envelope data
            double minx = filter.getMinX();
            double miny = filter.getMinY();
            double maxx = filter.getMaxX();
            double maxy = filter.getMaxY();
            // parse the srs, it might be a code or a WKT definition
            CoordinateReferenceSystem crs;
            try {
                crs = CRS.decode(srs);
            } catch (NoSuchAuthorityCodeException e) {
                crs = CRS.parseWKT(srs);
            }

            // grab the property data
            String propertyName = filter.getPropertyName();
            CoordinateReferenceSystem targetCrs = findPropertyCRS(ff.property(propertyName));

            // if there is a mismatch, reproject and replace
            if (crs != null && targetCrs != null && !CRS.equalsIgnoreMetadata(crs, targetCrs)) {
                ReferencedEnvelope envelope = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
                envelope = envelope.transform(targetCrs, true);
                minx = envelope.getMinX();
                miny = envelope.getMinY();
                maxx = envelope.getMaxX();
                maxy = envelope.getMaxY();
                
                // set the srs. If we have a code we use it, otherwise we use a WKT definition
                if(targetCrs.getIdentifiers().isEmpty()) {
                    // fall back to WKT
                    srs = targetCrs.toString();
                } else {
                    srs = targetCrs.getIdentifiers().iterator().next().toString();
                }
            }

            return getFactory(extraData).bbox(propertyName, minx, miny, maxx, maxy, srs);
        } catch (Exception e) {
            throw new RuntimeException("Could not decode srs '" + srs + "'", e);
        }

    }
    
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        return new BinaryComparisonTransformer() {

            Object cloneFilter(BinaryComparisonOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((PropertyIsEqualTo) filter, extraData);
            }
            
            Object cloneFilter(BinaryComparisonOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.equal(ex1, ex2, bso.isMatchingCase());
            }
        }.transform(filter, extraData);
    }
    
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return new BinaryComparisonTransformer() {

            Object cloneFilter(BinaryComparisonOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((PropertyIsNotEqualTo) filter, extraData);
            }
            
            Object cloneFilter(BinaryComparisonOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.notEqual(ex1, ex2, bso.isMatchingCase());
            }
        }.transform(filter, extraData);
    }

    public Object visit(Beyond filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Beyond) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                Beyond filter = (Beyond) bso;
                return ff.beyond(ex1, ex2, filter.getDistance(), filter.getDistanceUnits());
            }
        }.transform(filter, extraData);
    }

    public Object visit(Contains filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Contains) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.contains(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(Crosses filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Crosses) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.crosses(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(Disjoint filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Disjoint) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.disjoint(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(DWithin filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((DWithin) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                DWithin filter = (DWithin) bso;
                return ff.dwithin(ex1, ex2, filter.getDistance(), filter.getDistanceUnits());
            }
        }.transform(filter, extraData);
    }

    public Object visit(Intersects filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Intersects) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.intersects(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(Overlaps filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Overlaps) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.overlaps(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(Touches filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Touches) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.touches(ex1, ex2);
            }
        }.transform(filter, extraData);
    }

    public Object visit(Within filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Within) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.within(ex1, ex2);
            }
        }.transform(filter, extraData);
    }
    
    public Object visit(Equals filter, Object extraData) {
        return new GeometryFilterTransformer() {

            Object cloneFilter(BinarySpatialOperator filter, Object extraData) {
                return ReprojectingFilterVisitor.super.visit((Equals) filter, extraData);
            }

            Object cloneFilter(BinarySpatialOperator bso, Object extraData, Expression ex1,
                    Expression ex2) {
                return ff.equal(ex1, ex2);
            }
        }.transform(filter, extraData);
    }
    
    /**
     * Helper method to reproject a geometry.
     */
    protected Geometry reproject( Object value, CoordinateReferenceSystem propertyCrs) {
        if ( value == null ) {
            return null;
        }
        
        if (!(value instanceof Geometry))
            throw new IllegalArgumentException("Binary geometry filter, but second expression "
                    + "is not a geometry literal? (it's a " + value.getClass() + ")");
        Geometry geom = (Geometry) value;
        
        // does it make sense to proceed?
        if (geom.getUserData() == null
                || !(geom.getUserData() instanceof CoordinateReferenceSystem))
            return geom;

        try {
            // reproject
            CoordinateReferenceSystem geomCRS = (CoordinateReferenceSystem) geom.getUserData();
            Geometry transformed = JTS.transform(geom, CRS.findMathTransform(geomCRS, propertyCrs, true));
            transformed.setUserData(propertyCrs);
            
            return transformed;
        } catch(Exception e) {
            throw new RuntimeException("Could not reproject geometry " + value, e);
        }
    }
    
    Expression reproject(final Expression expression,
            final CoordinateReferenceSystem propertyCrs, boolean forceReprojection) {
        // check for case of section filter being a function
        if (expression instanceof Function) {
            //wrap the function in one that will transform the result
            final Function delegate = (Function) expression;
            return  new FunctionReprojector(propertyCrs, delegate);
        } else if (expression instanceof Literal) {
            // second expression is a geometry literal
            Object value = ((Literal) expression).getValue();
            return ff.literal(reproject(value,propertyCrs));
        } else if(forceReprojection) {
            throw new IllegalArgumentException("Binary geometry filter, but second expression "
                    + "is not a literal or function? (it's a " + expression.getClass() + ")");
        } else {
            // we were not forced to reproject, then return the original expression
            return null;
        }
    }

    /**
     * Factors out most of the logic needed to reproject a geometry filter, leaving subclasses
     * only the need to call the appropriate methods to create the new binary spatial filter
     * @author Andrea Aime - The Open Plannig Project
     *
     */
    private abstract class GeometryFilterTransformer {
        Object transform(final BinarySpatialOperator filter, Object extraData) {
            // check working assumptions, first expression is a property
            if (!(filter.getExpression1() instanceof PropertyName))
                throw new IllegalArgumentException("Binary geometry filter, but first expression "
                        + "is not a property name? (it's a " + filter.getExpression1().getClass()
                        + ")");
            final CoordinateReferenceSystem propertyCrs = findPropertyCRS((PropertyName) filter.getExpression1());

            if (propertyCrs == null)
                return cloneFilter(filter, extraData);
            
            // "transformed" expressions
            Expression ex1 =  (Expression) filter.getExpression1().accept(
                    ReprojectingFilterVisitor.this, extraData);
            Expression ex2 = reproject(filter.getExpression2(), propertyCrs, true);
            
            return cloneFilter(filter, extraData, ex1, ex2 );
        }

        /**
         * Straight cloning using cascaded visit
         * 
         * @param filter
         * @param extraData
         * @return
         */
        abstract Object cloneFilter(BinarySpatialOperator filter, Object extraData);

        /**
         * Clone with the provided parameters as first and second expressions
         * 
         * @param filter
         * @param extraData
         * @param ex1
         * @param ex2
         * @return
         */
        abstract Object cloneFilter(BinarySpatialOperator filter, Object extraData, Expression ex1,
                Expression ex2);
    }
    
    /**
     * Factors out most of the logic needed to reproject a binary comparison filter, leaving subclasses
     * only the need to call the appropriate methods to create the new binary spatial filter
     * @author Andrea Aime - The Open Plannig Project
     *
     */
    private abstract class BinaryComparisonTransformer {
        Object transform(BinaryComparisonOperator filter, Object extraData) {
            // binary filters may use two random expressions, check if we have
            // enough information for a reprojection
            PropertyName name;
            Expression other;
            if ((filter.getExpression1() instanceof PropertyName)) {
                name = (PropertyName) filter.getExpression1();
                other = filter.getExpression2();
            } else if(filter.getExpression2() instanceof PropertyName) {
                name = (PropertyName) filter.getExpression2();
                other = filter.getExpression1();
            } else {
                return cloneFilter(filter, extraData);
            }
                
            CoordinateReferenceSystem propertyCrs = findPropertyCRS(name);

            // we have to reproject only if the property is geometric 
            if (propertyCrs == null)
                return cloneFilter(filter, extraData);
            
            // "transformed" expressions
            Expression ex1 =  (Expression) name.accept(ReprojectingFilterVisitor.this, extraData);
            Expression ex2 = reproject(other, propertyCrs, false);
            if(ex2 == null)
                ex2 = (Expression) other.accept(ReprojectingFilterVisitor.this, extraData);
                        
            return cloneFilter(filter, extraData, ex1, ex2 );
        }

        /**
         * Straight cloning using cascaded visit
         * 
         * @param filter
         * @param extraData
         * @return
         */
        abstract Object cloneFilter(BinaryComparisonOperator filter, Object extraData);

        /**
         * Clone with the provided parameters as first and second expressions
         * 
         * @param filter
         * @param extraData
         * @param ex1
         * @param ex2
         * @return
         */
        abstract Object cloneFilter(BinaryComparisonOperator filter, Object extraData, Expression ex1,
                Expression ex2);
        
    }
    
    /**
     * Makes sure that the result of a function gets reprojected to the specified CRS, should
     * it be a Geometry
     * @author Justin DeOliveira - TOPP
     *
     */
    protected class FunctionReprojector implements Function {
        private final CoordinateReferenceSystem propertyCrs;

        private final Function delegate;

        protected FunctionReprojector(CoordinateReferenceSystem propertyCrs, Function delegate) {
            this.propertyCrs = propertyCrs;
            this.delegate = delegate;
        }

        public String getName() {
            return delegate.getName();
        }

        public List<Expression> getParameters() {
            return delegate.getParameters();
        }

        public Object accept(ExpressionVisitor visitor, Object extraData) {
            return delegate.accept( visitor, extraData );
        }

        public Object evaluate(Object object) {
            Object value = delegate.evaluate( object );
            return reproject(value, propertyCrs);
        }

        public <T> T evaluate(Object object, Class<T> context) {
            T value = delegate.evaluate( object, context );
            return (T) reproject(value, propertyCrs);
        }

        public Literal getFallbackValue() {
            return null;
        }
    }

}