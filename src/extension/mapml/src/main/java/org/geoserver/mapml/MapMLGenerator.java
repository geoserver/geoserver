/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import jakarta.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.mapml.xml.ObjectFactory;
import org.geoserver.mapml.xml.PropertyContent;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.gml.producer.CoordinateFormatter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.linearref.LengthIndexedLine;
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
    private static final Logger LOGGER = Logging.getLogger(MapMLGenerator.class);
    public static final double BUFFER_DISTANCE = 0.0001;
    public static final String POLYGON = "Polygon";
    public static final String LINE = "Line";
    public static final String POINT = "Point";
    public static final String CSS_STYLE_DELIMITER = " ";
    static ObjectFactory factory = new ObjectFactory();

    private int DEFAULT_NUM_DECIMALS = 8;
    private CoordinateFormatter formatter = new CoordinateFormatter(DEFAULT_NUM_DECIMALS);

    /**
     * @param sf a feature
     * @param featureCaptionTemplate - template optionally containing ${placeholders}. Can be null.
     * @param mapMLStyles - the applicable MapMLStyles, null if from WFS
     * @return the feature
     * @throws IOException - IOException
     */
    public Optional<Feature> buildFeature(
            SimpleFeature sf, String featureCaptionTemplate, List<MapMLStyle> mapMLStyles)
            throws IOException {
        if (mapMLStyles != null && mapMLStyles.isEmpty()) {
            // no applicable styles, probably because of scale
            return Optional.empty();
        }
        Feature f = new Feature();
        f.setId(sf.getID());
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
                "<table xmlns=\"http://www.w3.org/1999/xhtml\"><thead><tr>"
                        + "<th role=\"columnheader\" scope=\"col\">Property name</th>"
                        + "<th role=\"columnheader\" scope=\"col\">Property value</th>"
                        + "</tr></thead><tbody>");

        Geometry g = null;
        for (AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors()) {
            if (attr.getType() instanceof GeometryType) {
                g = (Geometry) (sf.getAttribute(attr.getName()));
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
        Optional<GeometryContent> geometryContent;
        if (mapMLStyles == null) {
            geometryContent = Optional.of(buildGeometry(g));
        } else {
            // convert geometry to type expected by the first symbolizer
            geometryContent = convertGeometryToSymbolizerType(g, mapMLStyles.get(0));
            String spaceDelimitedCSSClasses =
                    mapMLStyles.stream()
                            .map(MapMLStyle::getCSSClassName)
                            .collect(Collectors.joining(CSS_STYLE_DELIMITER));
            f.setStyle(spaceDelimitedCSSClasses);
        }
        // can't convert geometry to type expected by symbolizer
        if (geometryContent.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(
                        "Could not convert geometry of type"
                                + (g != null ? g.getGeometryType() : "null")
                                + " to symbolizer type: "
                                + mapMLStyles.get(0).getSymbolizerType());
            }
            return Optional.empty();
        }
        f.setGeometry(geometryContent.get());

        return Optional.of(f);
    }

    /**
     * Convert a geometry type to match the symbolizer type
     *
     * @param g - the geometry
     * @param mapMLStyle the applicable MapMLStyle
     * @return the geometry content
     * @throws IOException IOException
     */
    private Optional<GeometryContent> convertGeometryToSymbolizerType(
            Geometry g, MapMLStyle mapMLStyle) throws IOException {
        if (mapMLStyle.getSymbolizerType().startsWith(POINT)) {
            if (g instanceof Point || g instanceof MultiPoint) {
                return Optional.of(buildGeometry(g));
            } else if (g instanceof LineString || g instanceof MultiLineString) {
                LengthIndexedLine indexedLine = new LengthIndexedLine(g);
                double length = indexedLine.getEndIndex();
                double midpointLength = length / 2;
                Coordinate midpoint = indexedLine.extractPoint(midpointLength);
                if (midpoint == null) {
                    return Optional.empty();
                }
                Geometry midpointGeometry = g.getFactory().createPoint(midpoint);
                return Optional.of(buildGeometry(midpointGeometry));
            } else if (g instanceof Polygon || g instanceof MultiPolygon) {
                Geometry centroid = g.getCentroid();
                return Optional.of(buildGeometry(centroid));
            } else {
                return Optional.empty();
            }

        } else if (mapMLStyle.getSymbolizerType().startsWith(LINE)) {
            if (g instanceof LineString || g instanceof MultiLineString) {
                return Optional.of(buildGeometry(g));
            } else if (g instanceof Polygon || g instanceof MultiPolygon) {
                Geometry boundary = g.getBoundary();
                return Optional.of(buildGeometry(boundary));
            } else {
                return Optional.empty();
            }
        } else if (mapMLStyle.getSymbolizerType().startsWith(POLYGON)) {
            if (g instanceof Polygon || g instanceof MultiPolygon) {
                return Optional.of(buildGeometry(g));
            } else if (g instanceof LineString || g instanceof MultiLineString) {
                Geometry buffer = g.buffer(BUFFER_DISTANCE);
                return Optional.of(buildGeometry(buffer));
            } else if (g instanceof Point || g instanceof MultiPoint) {
                Geometry buffer = g.buffer(BUFFER_DISTANCE);
                return Optional.of(buildGeometry(buffer));
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private class AttributeValueResolver {

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
     * Build a MapML geometry from a JTS geometry
     *
     * @param g
     * @return
     * @throws IOException - IOException
     */
    public GeometryContent buildGeometry(Geometry g) throws IOException {
        GeometryContent geom = new GeometryContent();
        if (g instanceof Point) {
            org.geoserver.mapml.xml.Point point = buildPoint((Point) g);
            geom.setGeometryContent(factory.createPoint(point));
        } else if (g instanceof MultiPoint) {
            org.geoserver.mapml.xml.MultiPoint multiPoint = buildMultiPoint((MultiPoint) g);
            geom.setGeometryContent(factory.createMultiPoint(multiPoint));
        } else if (g instanceof LineString) {
            org.geoserver.mapml.xml.LineString lineString = buildLineString((LineString) g);
            geom.setGeometryContent(factory.createLineString(lineString));
        } else if (g instanceof MultiLineString) {
            org.geoserver.mapml.xml.MultiLineString multiLineString =
                    buildMultiLineString((MultiLineString) g);
            geom.setGeometryContent(factory.createMultiLineString(multiLineString));
        } else if (g instanceof Polygon) {
            org.geoserver.mapml.xml.Polygon polygon = buildPolygon((Polygon) g);
            geom.setGeometryContent(factory.createPolygon(polygon));
        } else if (g instanceof MultiPolygon) {
            org.geoserver.mapml.xml.MultiPolygon multiPolygon = buildMultiPolygon((MultiPolygon) g);
            geom.setGeometryContent(factory.createMultiPolygon(multiPolygon));
        } else if (g instanceof GeometryCollection) {
            geom.setGeometryContent(
                    factory.createGeometryCollection(
                            buildGeometryCollection((GeometryCollection) g)));
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
    private Object buildSpecificGeom(Geometry g) throws IOException {
        switch (g.getGeometryType()) {
            case POINT:
                return buildPoint((Point) g);
            case "MultiPoint":
                return buildMultiPoint((MultiPoint) g);
            case "LinearRing":
            case "LineString":
                return buildLineString((LineString) g);
            case "MultiLineString":
                return buildMultiLineString((MultiLineString) g);
            case POLYGON:
                return buildPolygon((Polygon) g);
            case "MultiPolygon":
                return buildMultiPolygon((MultiPolygon) g);
            case "GeometryCollection":
                return buildGeometryCollection((GeometryCollection) g);
            default:
                throw new IOException("Unknown geometry type: " + g.getGeometryType());
        }
    }

    /**
     * @param gc a JTS GeometryCollection
     * @return
     * @throws IOException - IOException
     */
    private org.geoserver.mapml.xml.GeometryCollection buildGeometryCollection(
            GeometryCollection gc) throws IOException {
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
    private org.geoserver.mapml.xml.MultiPolygon buildMultiPolygon(MultiPolygon mpg) {
        org.geoserver.mapml.xml.MultiPolygon multiPoly = new org.geoserver.mapml.xml.MultiPolygon();
        List<org.geoserver.mapml.xml.Polygon> polys = multiPoly.getPolygon();
        for (int i = 0; i < mpg.getNumGeometries(); i++) {
            polys.add(buildPolygon((Polygon) mpg.getGeometryN(i)));
        }
        return multiPoly;
    }

    /**
     * @param ml a JTS MultiLineString
     * @return
     */
    private org.geoserver.mapml.xml.MultiLineString buildMultiLineString(MultiLineString ml) {
        org.geoserver.mapml.xml.MultiLineString multiLine =
                new org.geoserver.mapml.xml.MultiLineString();
        List<JAXBElement<List<String>>> coordLists = multiLine.getTwoOrMoreCoordinatePairs();
        for (int i = 0; i < ml.getNumGeometries(); i++) {
            coordLists.add(
                    factory.createMultiLineStringCoordinates(
                            buildCoordinates(
                                    ((LineString) (ml.getGeometryN(i))).getCoordinateSequence(),
                                    null)));
        }
        return multiLine;
    }

    /**
     * @param l a JTS LineString
     * @return
     */
    private org.geoserver.mapml.xml.LineString buildLineString(LineString l) {
        org.geoserver.mapml.xml.LineString lineString = new org.geoserver.mapml.xml.LineString();
        List<String> lsCoords = lineString.getCoordinates();
        buildCoordinates(l.getCoordinateSequence(), lsCoords);
        return lineString;
    }

    /**
     * @param mp a JTS MultiPoint
     * @return
     */
    private org.geoserver.mapml.xml.MultiPoint buildMultiPoint(MultiPoint mp) {
        org.geoserver.mapml.xml.MultiPoint multiPoint = new org.geoserver.mapml.xml.MultiPoint();
        List<String> mpCoords = multiPoint.getCoordinates();
        buildCoordinates(new CoordinateArraySequence(mp.getCoordinates()), mpCoords);
        return multiPoint;
    }

    /**
     * @param p a JTS Point
     * @return
     */
    private org.geoserver.mapml.xml.Point buildPoint(Point p) {
        org.geoserver.mapml.xml.Point point = new org.geoserver.mapml.xml.Point();
        point.getCoordinates()
                .add(
                        this.formatter.format(p.getX())
                                + CSS_STYLE_DELIMITER
                                + this.formatter.format(p.getY()));
        return point;
    }

    /**
     * @param p a JTS Polygon
     * @return
     */
    private org.geoserver.mapml.xml.Polygon buildPolygon(Polygon p) {
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
    private List<String> buildCoordinates(CoordinateSequence cs, List<String> coordList) {
        if (coordList == null) {
            coordList = new ArrayList<>(cs.size());
        }
        for (int i = 0; i < cs.size(); i++) {
            coordList.add(
                    this.formatter.format(cs.getX(i))
                            + CSS_STYLE_DELIMITER
                            + this.formatter.format(cs.getY(i)));
        }
        return coordList;
    }

    /** @param numDecimals */
    public void setNumDecimals(int numDecimals) {
        // make a copy of relevant object state
        boolean fd = this.formatter.isForcedDecimal();
        boolean pad = this.formatter.isPadWithZeros();
        // create new formatter
        this.formatter = new CoordinateFormatter(numDecimals);
        // apply state to new formatter
        this.formatter.setForcedDecimal(fd);
        this.formatter.setPadWithZeros(pad);
    }

    /** @param forcedDecimal */
    public void setForcedDecimal(boolean forcedDecimal) {
        this.formatter.setForcedDecimal(forcedDecimal);
    }

    /** @param padWithZeros */
    public void setPadWithZeros(boolean padWithZeros) {
        this.formatter.setPadWithZeros(padWithZeros);
    }
}
