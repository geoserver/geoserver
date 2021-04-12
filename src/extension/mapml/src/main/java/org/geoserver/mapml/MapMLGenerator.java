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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.Constants;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * @author Chris Hodgson
 * @author prushforth
 *     <p>methods to convert GeoServer features markup
 */
public class MapMLGenerator {

    static ObjectFactory factory = new ObjectFactory();

    /**
     * @param sf a feature
     * @param featureCaptionTemplate - template optionally containing ${placeholders}. Can be null.
     * @return the feature
     * @throws IOException - IOException
     */
    public static Feature buildFeature(SimpleFeature sf, String featureCaptionTemplate)
            throws IOException {

        Feature f = new Feature();
        f.setId(sf.getID());
        f.setClazz(sf.getFeatureType().getTypeName());
        PropertyContent pc = new PropertyContent();
        f.setProperties(pc);
        if (featureCaptionTemplate != null && !featureCaptionTemplate.isEmpty()) {
            AttributeValueResolver attributeResolver = new AttributeValueResolver(sf);
            String caption =
                    StringEscapeUtils.escapeXml10(
                            attributeResolver.resolveFeatureCaption(featureCaptionTemplate));
            if (caption != null && !caption.trim().isEmpty()) {
                f.setFeatureCaption(caption.trim());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(
                "<table xmlns=\"http://www.w3.org/1999/xhtml/\"><thead><tr>"
                        + "<th role=\"columnheader\" scope=\"col\">Property name</th>"
                        + "<th role=\"columnheader\" scope=\"col\">Property value</th>"
                        + "</tr></thead><tbody>");

        org.locationtech.jts.geom.Geometry g = null;
        for (AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors()) {
            if (attr.getType() instanceof GeometryType) {
                g = (org.locationtech.jts.geom.Geometry) (sf.getAttribute(attr.getName()));
            } else {
                String escapedName = StringEscapeUtils.escapeXml10(attr.getLocalName());
                String value =
                        sf.getAttribute(attr.getName()) != null
                                ? sf.getAttribute(attr.getName()).toString()
                                : "";
                sb.append("<tr><th scope=\"row\">")
                        .append(escapedName)
                        .append("</th>")
                        .append("<td itemprop=\"")
                        .append(escapedName)
                        .append("\">")
                        .append(StringEscapeUtils.escapeXml10(value))
                        .append("</td></tr>");
            }
        }
        sb.append("</tbody></table>");
        pc.setAnyElement(sb.toString());
        f.setGeometry(buildGeometry(g));
        return f;
    }

    private static class AttributeValueResolver {

        private final Constants constants = new Constants(PlaceholderConfigurerSupport.class);
        private final PropertyPlaceholderHelper helper =
                new PropertyPlaceholderHelper(
                        constants.asString("DEFAULT_PLACEHOLDER_PREFIX"),
                        constants.asString("DEFAULT_PLACEHOLDER_SUFFIX"),
                        constants.asString("DEFAULT_VALUE_SEPARATOR"),
                        true);
        private final String nullValue = "null";
        private final SimpleFeature feature;
        private final PropertyPlaceholderHelper.PlaceholderResolver resolver =
                (name) -> resolveAttributeNames(name);
        /**
         * Wrap the feature to caption via this constructor
         *
         * @param feature
         */
        protected AttributeValueResolver(SimpleFeature feature) {
            this.feature = feature;
        }
        /**
         * Take an attribute name, return the attribute. "attribute" may be a band name. Band names
         * can have spaces in them, but these seem to require that the space be replaced by an
         * underscore, so this function performs that transformation.
         *
         * @param attributeName
         * @return null-signifying token (nullValue) or the attribute value
         */
        private String resolveAttributeNames(String attributeName) {
            // have seen band names with spaces in the name, naively replace with
            // underscore seems to work. TBD.  btw regexes are hard. that is all.
            Object attribute = feature.getAttribute(attributeName.trim().replaceAll("\\s", "_"));
            if (attribute == null) return nullValue;
            return feature.getAttribute(attributeName).toString();
        }
        /**
         * Invokes PropertyPlaceholderHelper.replacePlaceholders, which iterates over the
         * userTemplate string to replace placeholders with attribute values of the attribute of
         * that name, if found.
         *
         * @param userTemplate
         * @return A possibly null string with placeholders resolved
         * @throws BeansException if something goes wrong
         */
        protected String resolveFeatureCaption(String userTemplate) throws BeansException {
            String resolved = this.helper.replacePlaceholders(userTemplate, this.resolver);
            return (resolved.equals(nullValue) ? null : resolved);
        }
    }

    /**
     * @param g
     * @return
     * @throws IOException - IOException
     */
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
    /**
     * @param g a JTS Geometry
     * @return
     * @throws IOException - IOException
     */
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
    /**
     * @param gc a JTS GeometryCollection
     * @return
     * @throws IOException - IOException
     */
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
    /**
     * @param mpg a JTS MultiPolygo
     * @return
     */
    private static org.geoserver.mapml.xml.MultiPolygon buildMultiPolygon(
            org.locationtech.jts.geom.MultiPolygon mpg) {
        org.geoserver.mapml.xml.MultiPolygon multiPoly = new org.geoserver.mapml.xml.MultiPolygon();
        List<org.geoserver.mapml.xml.Polygon> polys = multiPoly.getPolygon();
        for (int i = 0; i < mpg.getNumGeometries(); i++) {
            polys.add(buildPolygon((org.locationtech.jts.geom.Polygon) mpg.getGeometryN(i)));
        }
        return multiPoly;
    }
    /**
     * @param ml a JTS MultiLineString
     * @return
     */
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
    /**
     * @param l a JTS LineString
     * @return
     */
    private static org.geoserver.mapml.xml.LineString buildLineString(
            org.locationtech.jts.geom.LineString l) {
        org.geoserver.mapml.xml.LineString lineString = new org.geoserver.mapml.xml.LineString();
        List<String> lsCoords = lineString.getCoordinates();
        buildCoordinates(l.getCoordinateSequence(), lsCoords);
        return lineString;
    }
    /**
     * @param mp a JTS MultiPoint
     * @return
     */
    private static org.geoserver.mapml.xml.MultiPoint buildMultiPoint(
            org.locationtech.jts.geom.MultiPoint mp) {
        org.geoserver.mapml.xml.MultiPoint multiPoint = new org.geoserver.mapml.xml.MultiPoint();
        List<String> mpCoords = multiPoint.getCoordinates();
        buildCoordinates(new CoordinateArraySequence(mp.getCoordinates()), mpCoords);
        return multiPoint;
    }
    /**
     * @param p a JTS Point
     * @return
     */
    private static org.geoserver.mapml.xml.Point buildPoint(org.locationtech.jts.geom.Point p) {
        org.geoserver.mapml.xml.Point point = new org.geoserver.mapml.xml.Point();
        point.getCoordinates().add(p.getX() + " " + p.getY());
        return point;
    }
    /**
     * @param p a JTS Polygon
     * @return
     */
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
    /**
     * @param cs a JTS CoordinateSequence
     * @param coordList a list of coordinate strings to add to
     * @return
     */
    private static List<String> buildCoordinates(CoordinateSequence cs, List<String> coordList) {
        if (coordList == null) {
            coordList = new ArrayList<>(cs.size());
        }
        for (int i = 0; i < cs.size(); i++) {
            coordList.add(cs.getX(i) + " " + cs.getY(i));
        }
        return coordList;
    }
}
