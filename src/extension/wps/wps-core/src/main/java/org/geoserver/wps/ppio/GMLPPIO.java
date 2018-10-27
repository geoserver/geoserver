/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import javax.xml.namespace.QName;
import org.geotools.gml2.GML;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xml.sax.ContentHandler;

public class GMLPPIO extends XMLPPIO {

    Configuration xml;

    protected GMLPPIO(Class type, String mimeType, QName element) {
        super(type, type, mimeType, element);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = getParser(xml);
        return p.parse(input);
    }

    @Override
    public void encode(Object obj, ContentHandler handler) throws Exception {
        Encoder e = new Encoder(xml);
        e.encode(obj, element, handler);
    }

    public static class GML2 extends GMLPPIO {

        public GML2(Class type, QName element) {
            super(type, "text/xml; subtype=gml/2.1.2", element);
            xml = new GMLConfiguration();
        }

        /** Place holder for process params which declare Geometry.class as the type. */
        public static class Geometry extends GML2 {
            public Geometry() {
                super(org.locationtech.jts.geom.Geometry.class, GML._Geometry);
            }

            @Override
            public void encode(Object object, ContentHandler output) throws Exception {
                if (object instanceof GeometryCollection) {
                    if (object instanceof MultiPoint) {
                        new GML2(MultiPoint.class, GML.MultiPoint).encode(object, output);
                    } else if (object instanceof MultiLineString) {
                        new GML2(MultiLineString.class, GML.MultiLineString).encode(object, output);
                    } else if (object instanceof MultiPolygon) {
                        new GML2(MultiPolygon.class, GML.MultiPolygon).encode(object, output);
                    } else {
                        new GML2(GeometryCollection.class, GML._Geometry).encode(object, output);
                    }
                } else {
                    if (object instanceof Point) {
                        new GML2(Point.class, GML.Point).encode(object, output);
                    } else if (object instanceof LineString) {
                        new GML2(LineString.class, GML.LineString).encode(object, output);
                    } else if (object instanceof Polygon) {
                        new GML2(Polygon.class, GML.Polygon).encode(object, output);
                    }
                }
            }
        }

        /** PPIO with alternate mime type suitable for usage in Execute KVP */
        public static class GeometryAlternate extends Geometry {

            public GeometryAlternate() {
                super();
                mimeType = "application/gml-2.1.2";
            }
        }
    }

    public static class GML3 extends GMLPPIO {

        public GML3(Class type, QName element) {
            super(type, "text/xml; subtype=gml/3.1.1", element);
            xml = new org.geotools.gml3.GMLConfiguration();
        }

        /** Place holder for process params which declare Geometry.class as the type. */
        public static class Geometry extends GML3 {
            public Geometry() {
                super(org.locationtech.jts.geom.Geometry.class, org.geotools.gml3.GML._Geometry);
                xml = new org.geotools.gml3.GMLConfiguration();
            }

            @Override
            public void encode(Object object, ContentHandler output) throws Exception {
                if (object instanceof GeometryCollection) {
                    if (object instanceof MultiPoint) {
                        new GML3(MultiPoint.class, GML.MultiPoint).encode(object, output);
                    } else if (object instanceof MultiLineString) {
                        new GML3(MultiLineString.class, GML.MultiLineString).encode(object, output);
                    } else if (object instanceof MultiPolygon) {
                        new GML3(MultiPolygon.class, GML.MultiPolygon).encode(object, output);
                    } else {
                        new GML3(GeometryCollection.class, GML._Geometry).encode(object, output);
                    }
                } else {
                    if (object instanceof Point) {
                        new GML3(Point.class, GML.Point).encode(object, output);
                    } else if (object instanceof LineString) {
                        new GML3(LineString.class, GML.LineString).encode(object, output);
                    } else if (object instanceof Polygon) {
                        new GML3(Polygon.class, GML.Polygon).encode(object, output);
                    }
                }
            }
        }

        /** PPIO with alternate mime type suitable for usage in Execute KVP */
        public static class GeometryAlternate extends Geometry {

            public GeometryAlternate() {
                super();
                mimeType = "application/gml-3.1.1";
            }
        }
    }
}
