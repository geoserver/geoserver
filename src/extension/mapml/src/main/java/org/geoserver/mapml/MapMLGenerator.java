/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Coordinates;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.ObjectFactory;
import org.geoserver.mapml.xml.PropertyContent;
import org.geoserver.mapml.xml.Span;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.gml.producer.CoordinateFormatter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
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
    public static final String SPACE = " ";

    static ObjectFactory factory = new ObjectFactory();

    private int DEFAULT_NUM_DECIMALS = 8;
    private CoordinateFormatter formatter = new CoordinateFormatter(DEFAULT_NUM_DECIMALS);
    private Envelope clipBounds;
    private boolean skipAttributes;

    private MapMLSimplifier simplifier;

    /**
     * Builds a MapML feature from a SimpleFeature
     *
     * @param sf a feature
     * @param featureCaptionTemplate - template optionally containing ${placeholders}. Can be null.
     * @param mapMLStyles - list of applicable MapMLStyles
     * @param templateOptional - optional template to use for geometry
     * @param applyGeomTransform - whether to apply geometry transformation
     * @return the feature (eventually null if the feature is outside the clip bounds or has no matching style when
     *     applyGeomTransform is true)
     * @throws IOException - IOException
     */
    public Optional<Feature> buildFeature(
            SimpleFeature sf,
            String featureCaptionTemplate,
            List<MapMLStyle> mapMLStyles,
            Optional<Mapml> templateOptional,
            boolean applyGeomTransform)
            throws IOException {
        if (mapMLStyles != null && mapMLStyles.isEmpty()) {
            // no applicable styles, probably because of scale
            return Optional.empty();
        }
        Feature f = new Feature();
        f.setId(sf.getID());
        Optional<Map<String, String>> replacmentAttsOptional = getTemplateAttributes(templateOptional);
        if (featureCaptionTemplate != null && !featureCaptionTemplate.isEmpty()) {
            AttributeValueResolver attributeResolver = new AttributeValueResolver(sf);
            String caption =
                    StringEscapeUtils.escapeXml10(attributeResolver.resolveFeatureCaption(featureCaptionTemplate));
            if (caption != null && !caption.trim().isEmpty()) {
                f.setFeatureCaption(caption.trim());
            }
        }

        if (!skipAttributes) {
            PropertyContent pc = new PropertyContent();
            f.setProperties(pc);
            pc.setAnyElement(collectAttributes(sf, replacmentAttsOptional));
        }
        if (applyGeomTransform) {
            // if clipping is enabled, clip the geometry and return null if the clip removed it
            // entirely
            Geometry g = (Geometry) sf.getDefaultGeometry();
            if (g == null) return Optional.empty();

            // intersection check
            if (clipBounds != null && !clipBounds.intersects(g.getEnvelopeInternal())) return Optional.empty();
            // eventual simplification (for WMS usage)
            if (simplifier != null) {
                try {
                    g = simplifier.simplify(g);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to simplify geometry, using it as is.", e);
                }
            }

            // eventual geometry conversion due to style type
            if (mapMLStyles != null) {
                // convert geometry to type expected by the first symbolizer
                g = convertGeometryToSymbolizerType(g, mapMLStyles.get(0));
                // can't convert geometry to type expected by symbolizer?
                if (g == null) {
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Could not convert geometry of type"
                                + (g != null ? g.getGeometryType() : "null")
                                + " to symbolizer type: "
                                + mapMLStyles.get(0).getSymbolizerType());
                    }
                    return Optional.empty();
                }
                String spaceDelimitedCSSClasses =
                        mapMLStyles.stream().map(MapMLStyle::getCSSClassName).collect(Collectors.joining(SPACE));
                f.setStyle(spaceDelimitedCSSClasses);
            }

            // and clipping (again for WMS usage)
            if (g != null && !g.isEmpty() && clipBounds != null) {
                MapMLGeometryClipper clipper = new MapMLGeometryClipper(g, clipBounds);
                g = clipper.clipAndTag();
            }
            if (g == null || g.isEmpty()) return Optional.empty();

            // if there is an template geometry and the original geometry is not tagged, use it
            // instead
            // of the original geometry
            GeometryContent geometryContent = null;
            if (templateOptional.isPresent() && g.getUserData() == null) {
                geometryContent =
                        templateOptional.get().getBody().getFeatures().get(0).getGeometry();
                // format the geometry coming from the template using the formatter
                Object geometry = geometryContent.getGeometryContent().getValue();
                formatGeometry(geometry);
            } else {
                geometryContent = buildGeometry(g);
            }

            f.setGeometry(geometryContent);
        }
        return Optional.of(f);
    }

    /**
     * Builds a MapML feature from a simple feature, by default applying geometry transformations as needed
     *
     * @param sf a feature
     * @param featureCaptionTemplate - template optionally containing ${placeholders}. Can be null.
     * @param mapMLStyles - list of applicable MapMLStyles
     * @param templateOptional - optional template to use for geometry
     * @return the feature (eventually null if the feature is outside the clip bounds or has no matching style
     * @throws IOException - IOException
     */
    public Optional<Feature> buildFeature(
            SimpleFeature sf,
            String featureCaptionTemplate,
            List<MapMLStyle> mapMLStyles,
            Optional<Mapml> templateOptional)
            throws IOException {
        return buildFeature(sf, featureCaptionTemplate, mapMLStyles, templateOptional, true);
    }

    /**
     * Formats the geometry using the formatter including the number of decimals
     *
     * @param geometry the geometry
     */
    private void formatGeometry(Object geometry) {
        if (geometry instanceof org.geoserver.mapml.xml.Point point) {
            formatCoordinates(point.getCoordinates());
        } else if (geometry instanceof org.geoserver.mapml.xml.MultiPoint multiPoint) {
            formatCoordinates(multiPoint.getCoordinates());
        } else if (geometry instanceof org.geoserver.mapml.xml.LineString lineString) {
            formatCoordinates(lineString.getCoordinates());
        } else if (geometry instanceof org.geoserver.mapml.xml.MultiLineString multiLineString) {
            formatCoordinates(multiLineString.getTwoOrMoreCoordinatePairs());
        } else if (geometry instanceof org.geoserver.mapml.xml.Polygon polygon) {
            formatCoordinates(polygon.getThreeOrMoreCoordinatePairs());
        } else if (geometry instanceof org.geoserver.mapml.xml.MultiPolygon multiPolygon) {
            for (org.geoserver.mapml.xml.Polygon polygon : multiPolygon.getPolygon()) {
                formatCoordinates(polygon.getThreeOrMoreCoordinatePairs());
            }

        } else if (geometry instanceof org.geoserver.mapml.xml.GeometryCollection geometryCollection) {
            for (Object geom : geometryCollection.getPointOrLineStringOrPolygon()) {
                formatGeometry(geom);
            }
        } else if (geometry instanceof org.geoserver.mapml.xml.A a) {
            formatGeometry(a.getGeometryContent().getValue());
        }
    }

    /**
     * Formats the coordinates using the formatter including the number of decimals
     *
     * @param coordinates the coordinates
     */
    private void formatCoordinates(List<Coordinates> coordinates) {
        for (Coordinates coords : coordinates) {
            List<Object> coordList = coords.getCoordinates();
            for (Object coord : coordList) {
                if (coord instanceof Span span) {
                    List<String> spanCoords = span.getCoordinates();
                    for (int i = 0; i < spanCoords.size(); i++) {
                        String[] xyArray = formatCoordStrings(spanCoords.get(i));
                        spanCoords.set(i, String.join(" ", xyArray));
                    }
                } else {
                    String xy = coord.toString();
                    String[] xyArray = formatCoordStrings(xy);
                    coord = String.join(" ", xyArray);
                }
            }
        }
    }

    /**
     * Formats the coordinates using the formatter including the number of decimals
     *
     * @param xy the coordinates
     * @return the formatted coordinates
     */
    private String[] formatCoordStrings(String xy) {
        String[] xyArray = Arrays.asList(xy.split("\\s+")).stream()
                .filter(s -> !s.trim().isEmpty())
                .toArray(String[]::new);
        for (int i = 0; i < xyArray.length; i++) {
            xyArray[i] = formatter.format(Double.parseDouble(xyArray[i]));
        }
        return xyArray;
    }

    private static Optional<Map<String, String>> getTemplateAttributes(Optional<Mapml> templateOptional) {

        return templateOptional
                .map(Mapml::getBody)
                .map(BodyContent::getFeatures)
                .filter(features -> features != null && !features.isEmpty())
                .map(features -> features.get(0))
                .map(Feature::getProperties)
                .map(PropertyContent::getOtherAttributes)
                .filter(attributes -> attributes != null
                        && !attributes.isEmpty()
                        && attributes.values().size() % 2 == 0)
                .map(attributes -> {
                    List<String> values = new ArrayList<>(attributes.values());
                    return IntStream.range(0, values.size() / 2)
                            .boxed()
                            .collect(Collectors.toMap(i -> values.get(i * 2), i -> values.get(i * 2 + 1)));
                });
    }

    /** Collects the attributes representation as a HTML table */
    private String collectAttributes(SimpleFeature sf, Optional<Map<String, String>> replacmentAttsOptional) {
        StringBuilder sb = new StringBuilder("<table xmlns=\"http://www.w3.org/1999/xhtml\"><thead><tr>"
                + "<th role=\"columnheader\" scope=\"col\">Property name</th>"
                + "<th role=\"columnheader\" scope=\"col\">Property value</th>"
                + "</tr></thead><tbody>");

        if (replacmentAttsOptional.isEmpty()) {
            for (AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors()) {
                if (!(attr.getType() instanceof GeometryType)) {
                    String escapedName = StringEscapeUtils.escapeXml10(attr.getLocalName());
                    String value = sf.getAttribute(attr.getName()) != null
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
        } else {
            for (Map.Entry<String, String> entry : replacmentAttsOptional.get().entrySet()) {
                String escapedName = StringEscapeUtils.escapeXml10(entry.getKey());
                String value = StringEscapeUtils.escapeXml10(entry.getValue());
                sb.append("<tr><th scope=\"row\">")
                        .append(escapedName)
                        .append("</th>")
                        .append("<td itemprop=\"")
                        .append(escapedName)
                        .append("\">")
                        .append(value)
                        .append("</td></tr>");
            }
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    /**
     * Convert a geometry type to match the symbolizer type
     *
     * @param g - the geometry
     * @param mapMLStyle the applicable MapMLStyle
     * @return the geometry content
     * @throws IOException IOException
     */
    private Geometry convertGeometryToSymbolizerType(Geometry g, MapMLStyle mapMLStyle) throws IOException {
        if (mapMLStyle.getSymbolizerType().startsWith(POINT)) {
            if (g instanceof Point || g instanceof MultiPoint) {
                return g;
            } else if (g instanceof LineString || g instanceof MultiLineString) {
                LengthIndexedLine indexedLine = new LengthIndexedLine(g);
                double length = indexedLine.getEndIndex();
                double midpointLength = length / 2;
                Coordinate midpoint = indexedLine.extractPoint(midpointLength);
                if (midpoint == null) return null;
                Geometry midpointGeometry = g.getFactory().createPoint(midpoint);
                return midpointGeometry;
            } else if (g instanceof Polygon || g instanceof MultiPolygon) {
                Geometry centroid = g.getCentroid();
                return centroid;
            }
        } else if (mapMLStyle.getSymbolizerType().startsWith(LINE)) {
            // there is no need to covert from polygon to line, as the boundary can already
            // be painted client side (the stroke properties apply to the polygon boundary directly)
            if (g instanceof LineString
                    || g instanceof MultiLineString
                    || g instanceof Polygon
                    || g instanceof MultiPolygon) {
                return g;
            }
        } else if (mapMLStyle.getSymbolizerType().startsWith(POLYGON)) {
            if (g instanceof Polygon || g instanceof MultiPolygon) {
                return g;
            } else if (g instanceof LineString
                    || g instanceof MultiLineString
                    || g instanceof Point
                    || g instanceof MultiPoint) {
                Geometry buffer = g.buffer(BUFFER_DISTANCE);
                return buffer;
            }
        }
        return null;
    }

    /** Sets the clip bounds, for tiled MapML output */
    public void setClipBounds(Envelope clipBounds) {
        this.clipBounds = clipBounds;
    }

    /** Enables disables alphanumeric attribute skipping */
    public void setSkipAttributes(boolean skipAttributes) {
        this.skipAttributes = skipAttributes;
    }

    public void setSimplifier(MapMLSimplifier simplifier) {
        this.simplifier = simplifier;
    }

    private static class AttributeValueResolver {

        private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
                PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX,
                PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX,
                PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR,
                null,
                true);
        private final String nullValue = "null";
        private final SimpleFeature feature;
        private final PropertyPlaceholderHelper.PlaceholderResolver resolver = this::resolveAttributeNames;

        /**
         * Wrap the feature to caption via this constructor
         *
         * @param feature - feature to resolve attributes from
         */
        protected AttributeValueResolver(SimpleFeature feature) {
            this.feature = feature;
        }

        /**
         * Take an attribute name, return the attribute. "attribute" may be a band name. Band names can have spaces in
         * them, but these seem to require that the space be replaced by an underscore, so this function performs that
         * transformation.
         *
         * @param attributeName - name of attribute to resolve
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
         * Invokes PropertyPlaceholderHelper.replacePlaceholders, which iterates over the userTemplate string to replace
         * placeholders with attribute values of the attribute of that name, if found.
         *
         * @param userTemplate - template string possibly containing ${placeholders}
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
     * @param g a JTS Geometry
     * @return the geometry content
     * @throws IOException - IOException
     */
    public GeometryContent buildGeometry(Geometry g) throws IOException {
        GeometryContent geom = new GeometryContent();
        if (g instanceof Point point1) {
            org.geoserver.mapml.xml.Point point = buildPoint(point1);
            geom.setGeometryContent(factory.createPoint(point));
        } else if (g instanceof MultiPoint point) {
            org.geoserver.mapml.xml.MultiPoint multiPoint = buildMultiPoint(point);
            geom.setGeometryContent(factory.createMultiPoint(multiPoint));
        } else if (g instanceof LineString string1) {
            org.geoserver.mapml.xml.LineString lineString = buildLineString(string1);
            geom.setGeometryContent(factory.createLineString(lineString));
        } else if (g instanceof MultiLineString string) {
            org.geoserver.mapml.xml.MultiLineString multiLineString = buildMultiLineString(string);
            geom.setGeometryContent(factory.createMultiLineString(multiLineString));
        } else if (g instanceof Polygon polygon1) {
            org.geoserver.mapml.xml.Polygon polygon = buildPolygon(polygon1);
            geom.setGeometryContent(factory.createPolygon(polygon));
        } else if (g instanceof MultiPolygon polygon) {
            org.geoserver.mapml.xml.MultiPolygon multiPolygon = buildMultiPolygon(polygon);
            geom.setGeometryContent(factory.createMultiPolygon(multiPolygon));
        } else if (g instanceof GeometryCollection collection) {
            geom.setGeometryContent(factory.createGeometryCollection(buildGeometryCollection(collection)));
        } else if (g != null) {
            throw new IOException("Unknown geometry type: " + g.getGeometryType());
        }

        return geom;
    }

    /**
     * @param g a JTS Geometry
     * @return the specific geometry object
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
    private org.geoserver.mapml.xml.GeometryCollection buildGeometryCollection(GeometryCollection gc)
            throws IOException {
        org.geoserver.mapml.xml.GeometryCollection geomColl = new org.geoserver.mapml.xml.GeometryCollection();
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
        org.geoserver.mapml.xml.MultiLineString multiLine = new org.geoserver.mapml.xml.MultiLineString();
        List<Coordinates> coordLists = multiLine.getTwoOrMoreCoordinatePairs();
        for (int i = 0; i < ml.getNumGeometries(); i++) {
            LineString ls = (LineString) ml.getGeometryN(i);
            String coordList = buildCoordinates(ls.getCoordinateSequence());
            coordLists.add(new Coordinates(coordList));
        }
        return multiLine;
    }

    /**
     * @param l a JTS LineString
     * @return
     */
    private org.geoserver.mapml.xml.LineString buildLineString(LineString l) {
        org.geoserver.mapml.xml.LineString lineString = new org.geoserver.mapml.xml.LineString();
        List<Coordinates> lsCoords = lineString.getCoordinates();
        buildCoordinates(l.getCoordinateSequence(), lsCoords);
        return lineString;
    }

    /**
     * @param mp a JTS MultiPoint
     * @return
     */
    private org.geoserver.mapml.xml.MultiPoint buildMultiPoint(MultiPoint mp) {
        org.geoserver.mapml.xml.MultiPoint multiPoint = new org.geoserver.mapml.xml.MultiPoint();
        List<Coordinates> mpCoords = multiPoint.getCoordinates();
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
                .add(new Coordinates(this.formatter.format(p.getX()) + SPACE + this.formatter.format(p.getY())));
        return point;
    }

    /**
     * @param p a JTS Polygon
     * @return
     */
    private org.geoserver.mapml.xml.Polygon buildPolygon(Polygon p) {
        if (p.getUserData() instanceof TaggedPolygon) return buildTaggedPolygon((TaggedPolygon) p.getUserData());

        org.geoserver.mapml.xml.Polygon poly = new org.geoserver.mapml.xml.Polygon();
        List<Coordinates> ringList = poly.getThreeOrMoreCoordinatePairs();
        String coordList = buildCoordinates(p.getExteriorRing().getCoordinateSequence());
        ringList.add(new Coordinates(coordList));
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            coordList = buildCoordinates(p.getInteriorRingN(i).getCoordinateSequence());
            ringList.add(new Coordinates(coordList));
        }
        return poly;
    }

    private org.geoserver.mapml.xml.Polygon buildTaggedPolygon(TaggedPolygon taggedPolygon) {
        org.geoserver.mapml.xml.Polygon poly = new org.geoserver.mapml.xml.Polygon();
        List<Coordinates> ringList = poly.getThreeOrMoreCoordinatePairs();
        ringList.add(new Coordinates(buildTaggedLinestring(taggedPolygon.getBoundary())));
        for (TaggedPolygon.TaggedLineString hole : taggedPolygon.getHoles()) {
            ringList.add(new Coordinates(buildTaggedLinestring(hole)));
        }
        return poly;
    }

    private List<Object> buildTaggedLinestring(TaggedPolygon.TaggedLineString ls) {
        List<Object> result = new ArrayList<>();
        List<TaggedPolygon.TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        int last = coordinates.size();
        for (int i = 0; i < last; i++) {
            TaggedPolygon.TaggedCoordinateSequence cs = coordinates.get(i);
            Object value = buildTaggedCoordinateSequence(cs);
            // client oddity: needs spaces before and after the map-span elements to work
            if (value instanceof String) {
                if (i > 0) {
                    value = " " + value;
                }
                if (i < last - 1) {
                    value = value + " ";
                }
            }
            result.add(value);
        }

        return result;
    }

    private Object buildTaggedCoordinateSequence(TaggedPolygon.TaggedCoordinateSequence cs) {
        if (cs.isVisible()) {
            return buildCoordinates(cs.getCoordinates());
        } else {
            return new Span(
                    "bbox",
                    buildSpanCoordinates(
                            new CoordinateArraySequence(cs.getCoordinates().toArray(n -> new Coordinate[n])), null));
        }
    }

    /**
     * @param cs a JTS CoordinateSequence
     * @param coordList a list of coordinate strings to add to
     * @return
     */
    private List<String> buildSpanCoordinates(CoordinateSequence cs, List<String> coordList) {
        if (coordList == null) {
            coordList = new ArrayList<>(cs.size());
        }
        for (int i = 0; i < cs.size(); i++) {
            coordList.add(this.formatter.format(cs.getX(i)) + SPACE + this.formatter.format(cs.getY(i)));
        }
        return coordList;
    }

    /**
     * @param cs a JTS CoordinateSequence
     * @param coordList a list of coordinate strings to add to
     * @return
     */
    private List<Coordinates> buildCoordinates(CoordinateSequence cs, List<Coordinates> coordList) {
        if (coordList == null) {
            coordList = new ArrayList<>(cs.size());
        }
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 0; i < cs.size(); i++) {
            joiner.add(this.formatter.format(cs.getX(i)) + SPACE + this.formatter.format(cs.getY(i)));
        }
        Coordinates coords = new Coordinates(joiner.toString());
        coordList.add(coords);
        return coordList;
    }

    /**
     * Builds a space separated representation of the coordinate sequence
     *
     * @param cs a JTS CoordinateSequence
     * @return
     */
    private String buildCoordinates(CoordinateSequence cs) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 0; i < cs.size(); i++) {
            joiner.add(this.formatter.format(cs.getX(i)) + " " + this.formatter.format(cs.getY(i)));
        }
        return joiner.toString();
    }

    /**
     * Builds a space separated representation of the list of coordinates
     *
     * @param cs a Coordinate list
     * @return
     */
    private String buildCoordinates(List<Coordinate> cs) {
        StringJoiner joiner = new StringJoiner(" ");
        for (Coordinate c : cs) {
            joiner.add(this.formatter.format(c.getX()) + " " + this.formatter.format(c.getY()));
        }
        return joiner.toString();
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
