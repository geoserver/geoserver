/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.feature;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Returns a clone of the provided filter where all geometries and bboxes that
 * do not have a CRS gets the specified default one.
 * 
 * @author Andrea Aime - The Open Planning Project
 * 
 */
public class DefaultCRSFilterVisitor extends DuplicatingFilterVisitor {
    private CoordinateReferenceSystem defaultCrs;

    public DefaultCRSFilterVisitor(FilterFactory2 factory, CoordinateReferenceSystem defaultCrs) {
        super(factory);
        this.defaultCrs = defaultCrs;
    }
    
    public Object visit(BBOX filter, Object extraData) {
        // if no srs is specified we can't transform anyways
        String srs = filter.getSRS();
        if (srs != null && !"".equals(srs.trim()))
            return super.visit(filter, extraData);

        try {
            // grab the original envelope data
            double minx = filter.getMinX();
            double miny = filter.getMinY();
            double maxx = filter.getMaxX();
            double maxy = filter.getMaxY();
            String propertyName = filter.getPropertyName();
            String defaultSrs = GML2EncodingUtils.crs(defaultCrs);

            return getFactory(extraData).bbox(propertyName, minx, miny, maxx, maxy, defaultSrs);
        } catch (Exception e) {
            throw new RuntimeException("Could not decode srs '" + srs + "'", e);
        }

    }
    
    public Object visit(Literal expression, Object extraData) {
        if (!(expression.getValue() instanceof Geometry))
            return super.visit(expression, extraData);

        // check if reprojection is needed
        Geometry geom = (Geometry) expression.getValue();
        if(geom.getUserData() != null && geom.getUserData() instanceof CoordinateReferenceSystem)
            return super.visit(expression, extraData);
        
        // clone the geometry and assign the new crs
        Geometry clone = geom.getFactory().createGeometry(geom);
        clone.setUserData(defaultCrs);

        // clone
        return ff.literal(clone);
    }
}
