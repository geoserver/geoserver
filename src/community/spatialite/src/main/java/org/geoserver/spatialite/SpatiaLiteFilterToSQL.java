/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.spatialite;

import java.io.IOException;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.filter.FilterCapabilities;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;

public class SpatiaLiteFilterToSQL extends FilterToSQL {

    
    @Override
    protected FilterCapabilities createFilterCapabilities() {
        FilterCapabilities caps = super.createFilterCapabilities();
        caps.addType(BBOX.class);
        caps.addType(Contains.class);
        caps.addType(Crosses.class);
        caps.addType(Disjoint.class);
        caps.addType(Equals.class);
        caps.addType(Intersects.class);
        caps.addType(Overlaps.class);
        caps.addType(Touches.class);
        caps.addType(Within.class);
        caps.addType(DWithin.class);
        caps.addType(Beyond.class);
        
        return caps;
    }
    
    @Override
    protected void visitLiteralGeometry(Literal expression) throws IOException {
        Geometry g = (Geometry) evaluateLiteral(expression, Geometry.class);
        if (g instanceof LinearRing) {
            //WKT does not support linear rings
            g = g.getFactory().createLineString(((LinearRing) g).getCoordinateSequence());
        }
        out.write( "GeomFromText('"+g.toText()+"', "+currentSRID+")");
    }
    
    @Override
    protected Object visitBinarySpatialOperator(BinarySpatialOperator filter,
        PropertyName property, Literal geometry, boolean swapped, Object extraData) {
        
        try {
            if (filter instanceof DistanceBufferOperator) {
                out.write("Distance(");
                property.accept(this, extraData);
                out.write(", ");
                geometry.accept(this, extraData);
                out.write(")");
                
                if (filter instanceof DWithin) {
                    out.write("<");
                }
                else if (filter instanceof Beyond) {
                    out.write(">");
                }
                else {
                    throw new RuntimeException("Unknown distance operator");
                }
                out.write(Double.toString(((DistanceBufferOperator)filter).getDistance()));
            }
            else if (filter instanceof BBOX) {
                out.write("MbrIntersects(");
                property.accept(this, extraData);
                out.write(",");
                geometry.accept(this, extraData);
                out.write(") = 1");
            }
            else {
             
                if (filter instanceof Contains) {
                    out.write("Contains(");
                }
                else if (filter instanceof Crosses) {
                    out.write("Crosses(");
                }
                else if (filter instanceof Disjoint) {
                    out.write("Disjoint(");
                }
                else if (filter instanceof Equals) {
                    out.write("Equals(");
                }
                else if (filter instanceof Intersects) {
                    out.write("Intersects(");
                }
                else if (filter instanceof Overlaps) {
                    out.write("Overlaps(");
                }
                else if (filter instanceof Touches) {
                    out.write("Touches(");
                }
                else if (filter instanceof Within) {
                    out.write("Within(");
                }
                else {
                    throw new RuntimeException("Unknown operator: " + filter);
                }
                
                if (swapped) {
                    geometry.accept(this, extraData);
                    out.write(", ");
                    property.accept(this, extraData);
                }
                else {
                    property.accept(this, extraData);
                    out.write(", ");
                    geometry.accept(this, extraData);
                }
                
                out.write(")");
            }
            
            if (!(filter instanceof Disjoint)) {
                String spatialIndex = (String) 
                    currentGeometry.getUserData().get(SpatiaLiteDialect.SPATIALITE_SPATIAL_INDEX);
                if (spatialIndex != null) {
                    Envelope e = geometry.evaluate(null, Envelope.class);
                    out.write(" AND ROWID IN (");
                    out.write("SELECT pkid FROM \""+spatialIndex+"\" WHERE ");
                    out.write("xmin <= " + e.getMaxX() + " AND ");
                    out.write("xmax >= " + e.getMinX() + " AND ");
                    out.write("ymin <= " + e.getMaxY() + " AND ");
                    out.write("ymax >= " + e.getMinY());
                    out.write(")");
                }
            }
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return extraData;
    }
}
