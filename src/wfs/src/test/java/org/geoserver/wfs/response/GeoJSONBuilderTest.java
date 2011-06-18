/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.StringWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

import junit.framework.TestCase;

public class GeoJSONBuilderTest extends TestCase {

    StringWriter writer;
    GeoJSONBuilder builder;
    
    @Override
    protected void setUp() throws Exception {
        writer = new StringWriter();
        builder = new GeoJSONBuilder(writer);
    }
    
    public void testWriteNormal() throws Exception {
        Geometry g = new WKTReader().read("MULTILINESTRING((0 0, 1 1))");
        builder.writeGeom(g);
        
        assertEquals("{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,1]]]}",
            writer.toString());
    }
    
    public void testWriteGeometrySubclass() throws Exception {
        builder.writeGeom(new MyPoint(1,2));
        
        assertEquals("{\"type\":\"Point\",\"coordinates\":[1,2]}", writer.toString());
    }
    
    class MyPoint extends Point {
        
        public MyPoint(double x, double y) {
            super(new Coordinate(x, y), new PrecisionModel(), -1);
        }
    }
}
