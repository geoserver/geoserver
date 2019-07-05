/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.mapml.xml.ObjectFactory;
import org.geoserver.mapml.xml.PropertyContent;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;

public class MapMLGenerator {

    static ObjectFactory factory = new ObjectFactory();

    public static Feature buildFeature(SimpleFeature sf) throws IOException {

        Feature f = new Feature();
        f.setId(sf.getID());
        f.setClazz(sf.getFeatureType().getTypeName());
        PropertyContent pc = new PropertyContent();
        f.setProperties(pc);

        StringBuilder sb = new StringBuilder();
        sb.append(
                "<table><thead><tr>"
                        + "<th role=\"columnheader\" scope=\"col\">Property name</th>"
                        + "<th role=\"columnheader\" scope=\"col\">Property value</th>"
                        + "</tr></thead><tbody>");

        org.locationtech.jts.geom.Geometry g = null;
        for (AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors()) {
            if (attr.getType() instanceof GeometryType) {
                g = (org.locationtech.jts.geom.Geometry) (sf.getAttribute(attr.getName()));
            } else {
                String escapedName = StringEscapeUtils.escapeXml10(attr.getLocalName());
                sb.append(
                        "<tr><th scope=\"row\">"
                                + escapedName
                                + "</th>"
                                + "<td itemprop=\""
                                + escapedName
                                + "\">"
                                + StringEscapeUtils.escapeXml10(
                                        sf.getAttribute(attr.getName()).toString())
                                + "</td></tr>");
            }
        }

        sb.append("</tbody></table>");
        pc.setAnyElement(sb.toString());
        f.setGeometry(buildGeometry(g));
        return f;
    }

    public static GeometryContent buildGeometry(org.locationtech.jts.geom.Geometry g)
            throws IOException {
        GeometryContent geom = new GeometryContent();
        if (g instanceof org.locationtech.jts.geom.Point) {
            geom.setGeometryContent(
                    factory.createPoint(buildPoint((org.locationtech.jts.geom.Point) g)));
        } else if (g instanceof org.locationtech.jts.geom.MultiPoint) {
            geom.setGeometryContent(
                    factory.createMultiPoint(
                            buildMultiPoint((org.locationtech.jts.geom.MultiPoint) g)));
        } else if (g instanceof org.locationtech.jts.geom.LinearRing
                || g instanceof org.locationtech.jts.geom.LineString) {
            geom.setGeometryContent(
                    factory.createLineString(
                            buildLineString((org.locationtech.jts.geom.LineString) g)));
        } else if (g instanceof org.locationtech.jts.geom.MultiLineString) {
            geom.setGeometryContent(
                    factory.createMultiLineString(
                            buildMultiLineString((org.locationtech.jts.geom.MultiLineString) g)));
        } else if (g instanceof org.locationtech.jts.geom.Polygon) {
            geom.setGeometryContent(
                    factory.createPolygon(buildPolygon((org.locationtech.jts.geom.Polygon) g)));
        } else if (g instanceof org.locationtech.jts.geom.MultiPolygon) {
            geom.setGeometryContent(
                    factory.createMultiPolygon(
                            buildMultiPolygon((org.locationtech.jts.geom.MultiPolygon) g)));
        } else if (g instanceof org.locationtech.jts.geom.GeometryCollection) {
            geom.setGeometryContent(
                    factory.createGeometryCollection(
                            buildGeometryCollection(
                                    (org.locationtech.jts.geom.GeometryCollection) g)));
        } else if (g != null) {
            throw new IOException("Unknown geometry type: " + g.getGeometryType());
        }

        return geom;
    }

    private static Object buildSpecificGeom(org.locationtech.jts.geom.Geometry g)
            throws IOException {
        switch (g.getGeometryType()) {
            case "Point":
                return buildPoint((org.locationtech.jts.geom.Point) g);
            case "MultiPoint":
                return buildMultiPoint((org.locationtech.jts.geom.MultiPoint) g);
            case "LinearRing":
            case "LineString":
                return buildLineString((org.locationtech.jts.geom.LineString) g);
            case "MultiLineString":
                return buildMultiLineString((org.locationtech.jts.geom.MultiLineString) g);
            case "Polygon":
                return buildPolygon((org.locationtech.jts.geom.Polygon) g);
            case "MultiPolygon":
                return buildMultiPolygon((org.locationtech.jts.geom.MultiPolygon) g);
            case "GeometryCollection":
                return buildGeometryCollection((org.locationtech.jts.geom.GeometryCollection) g);
            default:
                throw new IOException("Unknown geometry type: " + g.getGeometryType());
        }
    }

    private static org.geoserver.mapml.xml.GeometryCollection buildGeometryCollection(
            org.locationtech.jts.geom.GeometryCollection gc) throws IOException {
        org.geoserver.mapml.xml.GeometryCollection geomColl =
                new org.geoserver.mapml.xml.GeometryCollection();
        List<Object> geoms = geomColl.getPointOrLineStringOrPolygon();
        for (int i = 0; i < gc.getNumGeometries(); i++) {
            geoms.add(buildSpecificGeom(gc.getGeometryN(i)));
        }
        return geomColl;
    }

    private static org.geoserver.mapml.xml.MultiPolygon buildMultiPolygon(
            org.locationtech.jts.geom.MultiPolygon mpg) {
        org.geoserver.mapml.xml.MultiPolygon multiPoly = new org.geoserver.mapml.xml.MultiPolygon();
        List<org.geoserver.mapml.xml.Polygon> polys = multiPoly.getPolygon();
        for (int i = 0; i < mpg.getNumGeometries(); i++) {
            polys.add(buildPolygon((org.locationtech.jts.geom.Polygon) mpg.getGeometryN(i)));
        }
        return multiPoly;
    }

    private static org.geoserver.mapml.xml.MultiLineString buildMultiLineString(
            org.locationtech.jts.geom.MultiLineString ml) {
        org.geoserver.mapml.xml.MultiLineString multiLine =
                new org.geoserver.mapml.xml.MultiLineString();
        List<JAXBElement<List<String>>> coordLists = multiLine.getTwoOrMoreCoordinatePairs();
        for (int i = 0; i < ml.getNumGeometries(); i++) {
            coordLists.add(
                    factory.createMultiLineStringCoordinates(
                            buildCoordinates(
                                    ((org.locationtech.jts.geom.LineString) (ml.getGeometryN(i)))
                                            .getCoordinateSequence(),
                                    null)));
        }
        return multiLine;
    }

    private static org.geoserver.mapml.xml.LineString buildLineString(
            org.locationtech.jts.geom.LineString l) {
        org.geoserver.mapml.xml.LineString lineString = new org.geoserver.mapml.xml.LineString();
        List<String> lsCoords = lineString.getCoordinates();
        buildCoordinates(l.getCoordinateSequence(), lsCoords);
        return lineString;
    }

    private static org.geoserver.mapml.xml.MultiPoint buildMultiPoint(
            org.locationtech.jts.geom.MultiPoint mp) {
        org.geoserver.mapml.xml.MultiPoint multiPoint = new org.geoserver.mapml.xml.MultiPoint();
        List<String> mpCoords = multiPoint.getCoordinates();
        buildCoordinates(new CoordinateArraySequence(mp.getCoordinates()), mpCoords);
        return multiPoint;
    }

    private static org.geoserver.mapml.xml.Point buildPoint(org.locationtech.jts.geom.Point p) {
        org.geoserver.mapml.xml.Point point = new org.geoserver.mapml.xml.Point();
        point.getCoordinates().add(p.getX() + " " + p.getY());
        return point;
    }

    private static org.geoserver.mapml.xml.Polygon buildPolygon(
            org.locationtech.jts.geom.Polygon p) {
        org.geoserver.mapml.xml.Polygon poly = new org.geoserver.mapml.xml.Polygon();
        List<JAXBElement<List<String>>> ringList = poly.getThreeOrMoreCoordinatePairs();
        List<String> coordList =
                buildCoordinates(p.getExteriorRing().getCoordinateSequence(), null);
        ringList.add(factory.createPolygonCoordinates(coordList));
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            coordList = buildCoordinates(p.getInteriorRingN(i).getCoordinateSequence(), null);
            ringList.add(factory.createPolygonCoordinates(coordList));
        }
        return poly;
    }

    private static List<String> buildCoordinates(CoordinateSequence cs, List<String> coordList) {
        if (coordList == null) {
            coordList = new ArrayList<String>(cs.size());
        }
        for (int i = 0; i < cs.size(); i++) {
            coordList.add(cs.getX(i) + " " + cs.getY(i));
        }
        return coordList;
    }
}
