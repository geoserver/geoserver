/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;


import org.geoserver.wms.WMSMapContent;
import org.geotools.gml.producer.GeometryTransformer;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Geometry transformer for KML geometries.
 * <p>
 * While KML geometry encoding is quite similar to GML, there are a couple of tweaks:
 * <ul><li>the GML namespace is not used </li>
 *     <li>there are a couple of extra tags that can be inserted to tell the client 
 *         how to treat 3d data (eg, <code>&lt;extrude&gt;</code> and 
 *         <code>&lt;altitudeMode&gt;</code>) </li>
 * </ul>
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class KMLGeometryTransformer extends GeometryTransformer {
    static final String[] validAltitudeModes= new String[]{
        "relativeToGround",
        "absolute",
        "clampToGround" 
    };

    public Translator createTranslator(ContentHandler handler, WMSMapContent context) {
        return new KMLGeometryTranslator(handler, numDecimals, useDummyZ, context);
    }

    /**
     * Subclass which sets prefix and nsuri to null.
     */
    public static class KMLGeometryTranslator extends GeometryTranslator {
        private boolean extrude = true;
        private String altitudeMode = "relativeToGround";
        private boolean extrudeEnabled = true;

        private static final String[] GEOMETRYNAMES = new String[] {
            "Point",
            "LineString",
            "Polygon",
            "MultiPoint",
            "MultiLineString",
            "MultiPolygon",
            "GeometryCollection"
        };


        public KMLGeometryTranslator(
                ContentHandler handler, 
                int numDecimals,
                boolean useDummyZ,
                WMSMapContent context
                ) {
            //super(handler, "kml", "http://earth.google.com/kml/2.0" );
            super(handler, null, null, numDecimals, useDummyZ, 3);
            coordWriter = new KMLCoordinateWriter(numDecimals, useDummyZ);

            String extrudeValue = 
                (String)context.getRequest().getFormatOptions().get("extrude");
            if (extrudeValue != null){
                extrude = Boolean.valueOf(extrudeValue).booleanValue();
            }

            String requestedAltitudeMode = 
                (String)context.getRequest().getFormatOptions().get("altitudeMode");
            if (requestedAltitudeMode != null){
                for (String mode : validAltitudeModes){
                    if (mode.equalsIgnoreCase(requestedAltitudeMode.trim())){
                        altitudeMode = mode;
                    }
                }
            }
        }

        public void encode(Object o, String srsname){
            if (o instanceof Geometry){
                extrudeEnabled = inspectGeometry((Geometry)o);
            }
            super.encode(o, srsname);
        }

        public void encode(Object o){
            if (o instanceof Geometry){
                extrudeEnabled = inspectGeometry((Geometry)o);
            }
            super.encode(o);
        }
        
        public void encode(Geometry g, String srsname){
            extrudeEnabled = inspectGeometry(g);
            super.encode(g, srsname);
        }

        public void encode(Geometry g){
            extrudeEnabled = inspectGeometry(g);
            super.encode(g);
        }

        protected void start(String element, Attributes atts){
            super.start(element,atts);
            if (isGeometryElement(element)){
                insertExtrudeTags();
            }
        }

        private boolean inspectGeometry(Geometry g){
            double d = g.getCoordinate().z;
            return !(Double.isNaN(d) || d == 0);
        }

        public void insertExtrudeTags(){
            if (extrudeEnabled){
                element("extrude", extrude ? "1" : "0");
                element("altitudeMode", altitudeMode);
            }
        }

        public void insertExtrudeTags(Geometry g){
            if (inspectGeometry(g)){
                element("extrude", extrude ? "1" : "0");
                element("altitudeMode", altitudeMode);
            }
        }

        private boolean isGeometryElement(String elementName){
            for (String name : GEOMETRYNAMES){
                if (name.equals(elementName)) return true;
            }

            return false;
        }
    }
}
