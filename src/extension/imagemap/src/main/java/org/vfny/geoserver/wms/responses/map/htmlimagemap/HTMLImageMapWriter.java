/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataSourceException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.wms.responses.map.htmlimagemap.holes.HolesRemover;

/**
 * Encodes a layer in HTMLImageMap format.
 *
 * @author Mauro Bartolomeoli
 */
public class HTMLImageMapWriter extends OutputStreamWriter {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    HTMLImageMapWriter.class.getPackage().getName());

    GeometryFactory gFac = new GeometryFactory();

    /** map of geometry class to writer */
    private Map<Class<?>, HTMLImageMapFeatureWriter> writers;

    WMSMapContent mapContent = null;

    /** rect representing screen coordinates space * */
    Rectangle mapArea = null;

    ReferencedEnvelope mapEnv = null;
    Polygon clippingBox = null;

    /** Transformation from layer (world) coordinates to "screen" coordinates. */
    private AffineTransform worldToScreen = null;

    /**
     * Creates a new HTMLImageMapWriter object.
     *
     * @param out stream to encode the layer to
     * @param mapContent current wms context
     */
    public HTMLImageMapWriter(OutputStream out, WMSMapContent mapContent)
            throws UnsupportedEncodingException, ClassCastException {
        super(out, guessCharset(mapContent));

        this.mapContent = mapContent;
        mapEnv = mapContent.getRenderingArea();
        clippingBox = envToGeometry(mapEnv);
        mapArea = new Rectangle(mapContent.getMapWidth(), mapContent.getMapHeight());
        worldToScreen = RendererUtilities.worldToScreenTransform(mapEnv, mapArea);
        initWriters();
    }

    private static String guessCharset(WMSMapContent mapContent) {
        GetMapRequest request = mapContent.getRequest();
        if (request != null && request.getRequestCharset() != null) {
            String requestCharset = request.getRequestCharset();
            return requestCharset;
        }
        return "UTF-8";
    }

    private Polygon envToGeometry(ReferencedEnvelope env) {

        Coordinate[] coordinates =
                new Coordinate[] {
                    new Coordinate(env.getMinX(), env.getMinY()),
                    new Coordinate(env.getMaxX(), env.getMinY()),
                    new Coordinate(env.getMaxX(), env.getMaxY()),
                    new Coordinate(env.getMinX(), env.getMaxY()),
                    new Coordinate(env.getMinX(), env.getMinY())
                };
        LinearRing bbox = gFac.createLinearRing(coordinates);
        return gFac.createPolygon(bbox, new LinearRing[] {});
    }

    /** Initializes every type of writer (one for every kind of geometry). */
    private void initWriters() {
        writers = new HashMap<Class<?>, HTMLImageMapFeatureWriter>();
        writers.put(Point.class, new PointWriter());
        writers.put(LineString.class, new LineStringWriter());
        writers.put(LinearRing.class, new LineStringWriter());
        writers.put(Polygon.class, new PolygonWriter());
        writers.put(MultiPoint.class, new MultiPointWriter());
        writers.put(MultiLineString.class, new MultiLineStringWriter());
        writers.put(MultiPolygon.class, new MultiPolygonWriter());
        writers.put(GeometryCollection.class, new GeometryCollectionWriter());
    }

    /**
     * Encodes a newline
     *
     * @throws IOException if an error occurs during encoding
     *     <p>public void newline() throws IOException { super.write('\n'); }
     */

    /**
     * Encodes a single layer (FeatureCollection) using the supplied style.
     *
     * @param fColl layer to encode
     * @param ftsList the feature type styles to use for encoding
     * @throws IOException if an error occurs during encoding
     * @throws AbortedException if the operation is aborted
     */
    public void writeFeatures(SimpleFeatureCollection fColl, FeatureTypeStyle[] ftsList)
            throws IOException, AbortedException {
        SimpleFeature ft;
        SimpleFeatureIterator iter = null;
        try {
            SimpleFeatureType featureType = fColl.getSchema();

            // iterates through the single features
            iter = fColl.features();
            while (iter.hasNext()) {
                ft = iter.next();
                Geometry geo = (Geometry) ft.getDefaultGeometry();

                if (!clippingBox.contains(geo)) {
                    try {
                        Geometry clippedGeometry = clippingBox.intersection(geo);
                        ft.setDefaultGeometry(clippedGeometry);
                    } catch (Throwable e) {
                        // ignore and use the original geo
                    }
                }
                // retrieves the right feature writer (based on the geometry type of the feature)
                HTMLImageMapFeatureWriter featureWriter =
                        (HTMLImageMapFeatureWriter) writers.get(ft.getDefaultGeometry().getClass());
                // encodes a single feature, using the supplied style and the current featureWriter
                featureWriter.writeFeature(ft, ftsList);
                ft = null;
            }

            LOGGER.fine("encoded " + featureType.getTypeName());
        } catch (NoSuchElementException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        } finally {
            if (iter != null) {
                // make sure we always close
                iter.close();
            }
        }
    }

    /**
     * Filters the rules of <code>featureTypeStyle</code> returnting only those that apply to <code>
     * feature</code>.
     *
     * <p>This method returns rules for which:
     *
     * <ol>
     *   <li><code>rule.getFilter()</code> matches <code>feature</code>, or:
     *   <li>the rule defines an "ElseFilter", and the feature matches no other rules.
     * </ol>
     *
     * This method returns an empty array in the case of which no rules match.
     *
     * @param featureTypeStyle The feature type style containing the rules.
     * @param feature The feature being filtered against.
     */
    List<Rule> filterRules(FeatureTypeStyle featureTypeStyle, SimpleFeature feature) {
        List<Rule> filtered = new ArrayList<Rule>();

        // process the rules, keep track of the need to apply an else filters
        boolean match = false;
        boolean hasElseFilter = false;

        for (Rule rule : featureTypeStyle.rules()) {
            LOGGER.finer(new StringBuffer("Applying rule: ").append(rule.toString()).toString());

            // does this rule have an else filter
            if (rule.isElseFilter()) {
                hasElseFilter = true;

                continue;
            }
            /*
                     double scaleDenominator;
            try {
            	scaleDenominator = RendererUtilities.calculateScale(mapContent.getAreaOfInterest(), mapContent.getMapWidth(), mapContent.getMapHeight(),100);

                      //is this rule within scale?
                      if ( !EncodeHTMLImageMap.isWithInScale(rule,scaleDenominator)) {
                      	continue;
                      }
                     } catch (TransformException e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            } catch (FactoryException e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            }*/

            // does this rule have a filter which applies to the feature
            Filter filter = rule.getFilter();

            if ((filter == null) || filter.evaluate(feature)) {
                match = true;

                filtered.add(rule);
            }
        }

        // if no rules mached the feautre, re-run through the rules applying
        // any else filters
        if (!match && hasElseFilter) {
            // loop through again and apply all the else rules
            for (Rule rule : featureTypeStyle.rules()) {
                if (rule.isElseFilter()) {
                    filtered.add(rule);
                }
            }
        }

        return filtered;
    }

    /**
     * Base Class for all the feature writers. An implementation is defined for every geometry type.
     *
     * @author Mauro Bartolomeoli
     */
    private abstract class HTMLImageMapFeatureWriter {

        // stores a series of attributes to append to the feature tag definition
        Map<String, String> extraAttributes = new HashMap<String, String>();

        StringBuffer buffer = new StringBuffer();

        /**
         * Encodes a single feature. Default implementation. The encoding is accomplished through
         * many phases: 1) reset writer state 2) process supplied style and apply filters to decide
         * if the feature has to be included in output. If the feature has to be included, proceed
         * with the following phases, else go to the next feature. 3) start feature encoding 4) pre
         * geometry encoding 5) actual geometry encoding 6) post geometry encoding 7) end feature
         * encoding
         *
         * @param ft feature to encode
         * @param fts "cached" ftss matching the FeatureType of the feature
         * @throws IOException if an error occurs during encoding
         */
        protected void writeFeature(SimpleFeature ft, FeatureTypeStyle[] fts) throws IOException {
            // a new feature begins, reset accumulated info, such as extraAttributes
            reset(ft);
            // process the supplied style and store rendering info for the following phases
            // the style processing applies filters to the feature to decide if it has to be
            // included
            // in output
            if (processStyle(ft, fts)) {
                try {
                    Geometry geo = (Geometry) ft.getDefaultGeometry();
                    if (geo != null) {
                        // encodes starting element
                        startElement(ft, "");

                        // pre geometry encoding phase
                        startGeometry(geo);
                        // actual geometry encoding phase
                        writeGeometry(geo, buffer);
                        // post geometry encoding phase
                        endGeometry(geo);
                        // encodes ending element
                        endElement(ft);
                        // if everything has been correctly encoded,
                        // we commit the buffer content to the stream
                        commitBuffer();
                    } else {
                        buffer = new StringBuffer();
                        if (LOGGER.isLoggable(Level.WARNING)) LOGGER.warning("null geometry");
                    }
                } catch (IOException e) {
                    buffer = new StringBuffer();
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.warning("Problems encoding shape: " + e.getMessage());
                } catch (Throwable t) {
                    buffer = new StringBuffer();
                    if (LOGGER.isLoggable(Level.SEVERE))
                        LOGGER.severe("Problems encoding shape: " + t.getMessage());
                }
            }
        }

        protected void commitBuffer() throws IOException {
            write(buffer.toString());
            buffer = new StringBuffer();
        }

        /**
         * Encodes a "MultiFeature", a feature with multiple geometries. Default implementation. The
         * encoding is accomplished through many phases: 1) reset writer state 2) process supplied
         * style and apply filters to decide if the feature has to be included in output. If the
         * feature has to be included, proceed with the following phases, else go to the next
         * feature. 3) loops for all the geometries, with the following phases for every single
         * geometry. a) start feature encoding b) pre geometry encoding c) actual geometry encoding
         * d) post geometry encoding e) end feature encoding
         *
         * @param ft feature to encode
         * @param fts "cached" ftss matching the FeatureType of the feature
         * @throws IOException if an error occurs during encoding
         */
        protected void writeMultiFeature(SimpleFeature ft, FeatureTypeStyle[] fts)
                throws IOException {
            reset(ft);
            if (processStyle(ft, fts)) {
                GeometryCollection geomCollection = (GeometryCollection) ft.getDefaultGeometry();
                for (int i = 0; i < geomCollection.getNumGeometries(); i++) {
                    try {
                        Geometry geo = geomCollection.getGeometryN(i);
                        if (geo != null) {
                            startElement(ft, "." + i);

                            startGeometry(geo);
                            writeGeometry(geo, buffer);
                            endGeometry(geo);
                            endElement(ft);
                            commitBuffer();
                        } else {
                            if (LOGGER.isLoggable(Level.WARNING))
                                LOGGER.warning("Problems encoding shape: null geometry");
                        }

                    } catch (IOException e) {
                        buffer = new StringBuffer();
                        if (LOGGER.isLoggable(Level.WARNING))
                            LOGGER.warning("Problems encoding shape: " + e.getMessage());
                    } catch (Throwable t) {
                        buffer = new StringBuffer();
                        if (LOGGER.isLoggable(Level.SEVERE))
                            LOGGER.severe("Problems encoding shape: " + t.getMessage());
                    }
                }
            }
        }

        /**
         * Encodes the feature starting tag (area).
         *
         * @param feature feature to encode
         * @param suffix (optional) suffix to append to the tag id (useful to have different ids in
         *     the multigeometry scenario.
         * @throws IOException if an error occures during encoding
         */
        protected void startElement(SimpleFeature feature, String suffix) throws IOException {
            // each feature (multi geometry ones are an exception) is represented by an <area> tag
            // each area tag has an id, equal to the feature id, and a shape (rect, poly or circle)
            writeToBuffer(
                    "<area shape=\"" + getShape() + "\" id=\"" + feature.getID() + suffix + "\" ",
                    buffer);
        }

        /**
         * Encodes the geometry definition attribute (coords).
         *
         * @param geom geometry to encode
         * @throws IOException if an error occures during encoding
         */
        protected void startGeometry(Geometry geom) throws IOException {
            writeToBuffer(" coords=\"", buffer);
        }

        /**
         * Each area tag has a shape attribute, defining the coords attribute meaning. The W3C HTML
         * 4.0 standard defines 3 possible shape values: rect, poly and circle.
         *
         * @return the writer associated shape value
         * @throws IOException if an error occures during encoding
         */
        protected abstract String getShape() throws IOException;

        /**
         * Encodes the actual geometry.
         *
         * @param geom geometry to encode
         * @throws IOException if an error occures during encoding
         */
        protected abstract void writeGeometry(Geometry geom, StringBuffer buffer)
                throws IOException;

        /**
         * Encodes the geometry "ending" (closing quotes)
         *
         * @param geom geometry to encode
         * @throws IOException if an error occures during encoding
         */
        protected void endGeometry(Geometry geom) throws IOException {
            writeToBuffer("\"", buffer);
        }

        /**
         * Encodes feature ending. extraAttributes accumulated till now are encoded and the area tag
         * is closed
         *
         * @param feature feature to encode
         * @throws IOException if an error occures during encoding
         */
        protected void endElement(SimpleFeature feature) throws IOException {
            Iterator<String> iter = extraAttributes.keySet().iterator();
            while (iter.hasNext()) {
                String attrName = iter.next();
                writeToBuffer(
                        " " + attrName + "=\"" + extraAttributes.get(attrName) + "\"", buffer);
            }
            writeToBuffer("/>\n", buffer);
        }

        /**
         * Resets writer status. extraAttributes is emptied
         *
         * @param ft current feature to encode
         */
        protected void reset(SimpleFeature ft) {
            extraAttributes = new HashMap<String, String>();
            buffer = new StringBuffer();
        }

        /**
         * Analyze the supplied style and process any matching rule.
         *
         * @param ft feature to which the style is going to be applied
         * @param ftsList cached fts matching the feature
         * @return true if the supplied feature has to be included in the output according to style
         *     filters.
         * @throws IOException if an error occurs during the process
         */
        protected boolean processStyle(SimpleFeature ft, FeatureTypeStyle[] ftsList)
                throws IOException {
            int total = 0;
            for (int i = 0; i < ftsList.length; i++) {
                FeatureTypeStyle fts = ftsList[i];
                List<Rule> rules = filterRules(fts, ft);
                total += rules.size();

                for (Rule rule : rules) {
                    processRule(ft, rule);
                }
            }
            if (total == 0) return false;
            return true;
        }

        /**
         * Process a single style rule and apply meaningful style to the feature
         *
         * @param ft feature to which the rule has to be applied
         * @param rule rule to process
         * @throws IOException if an error occurs during the process
         */
        protected void processRule(SimpleFeature ft, Rule rule) throws IOException {

            List<Symbolizer> symbolizers = rule.symbolizers();
            for (Symbolizer symbolizer : symbolizers) {
                // process any given symbolizer
                processSymbolizer(ft, rule, symbolizer);
            }
        }
        /**
         * Process a single style symbolizer and apply meaningful style to the feature. The default
         * implementation processes TextSymbolizer, using Label definition to apply textual
         * attributes to the area tag.
         *
         * @param ft feature to which the symbolizer has to be applied
         * @param rule current rule to analyze
         * @param symbolizer current symbolizer to analyze
         * @throws IOException if an error occurs during the process
         */
        protected void processSymbolizer(SimpleFeature ft, Rule rule, Symbolizer symbolizer)
                throws IOException {
            if (symbolizer instanceof TextSymbolizer) {
                // TODO: any check for label definition needed here?
                Expression e = SLD.textLabel((TextSymbolizer) symbolizer);
                // eval label actual value
                Object object = e.evaluate(ft);
                String value = null;

                if (object instanceof String) {
                    value = (String) object;
                } else {
                    if (object != null) {
                        value = object.toString();
                    }
                }
                // if any label is defined append a new extra attribute
                // the attribute name is equal to the current rule name (if defined, defaults to
                // "title")
                // the value of the attribute is the current label value
                if ((value != null) && !"".equals(value.trim())) {
                    String attrName = rule.getName();
                    if (attrName == null || attrName.trim().equals("")) attrName = "title";
                    extraAttributes.put(attrName, value);
                }
            }
        }

        /**
         * Gets a point in screen coordinates from a Coordinate in world coordinates
         *
         * @param c projected coordinate
         * @return screen coordinates in textual form (x,y)
         */
        protected String getPoint(Coordinate c) {
            Point2D transformed = worldToScreen.transform(new Point2D.Double(c.x, c.y), null);
            return (int) Math.round(transformed.getX())
                    + ","
                    + (int) Math.round(transformed.getY());
        }

        /**
         * Encodes a list of coordinates in textual form (for the coords attribute) <i>coords</i>
         * Area attribute
         *
         * <p>
         *
         * @param coords coordinates to encode
         * @throws IOException if an error occurs during encoding
         */
        protected void writePathContent(Coordinate[] coords, StringBuffer buf) throws IOException {
            StringBuffer tempBuf = new StringBuffer();
            int nCoords = coords.length;
            for (int i = 0; i < nCoords; i++) {
                Coordinate curr = coords[i];
                String p = getPoint(curr);

                tempBuf.append(" " + p);
            }
            // Close the path if it's not already closed
            if (!coords[nCoords - 1].equals2D(coords[0]))
                tempBuf.append(" " + coords[0].x + "," + coords[0].y);
            if (tempBuf.length() > 0) writeToBuffer(tempBuf.substring(1), buf);
            else throw new IOException("No coordinates");
        }

        protected void writeToBuffer(String substring, StringBuffer buf) {
            buf.append(substring);
        }

        /**
         * Simplifies a geometry to exclude duplicated points. When translating from world to screen
         * coordinates it's possible that many world points collapse to a single screen point. Those
         * colliding points are simplified to a single point.
         */
        Geometry decimate(Geometry geom) {
            DefaultMathTransformFactory f = new DefaultMathTransformFactory();
            MathTransform xform = null;
            try {
                xform = f.createAffineTransform(new GeneralMatrix(worldToScreen.createInverse()));
                Decimator decimator = new Decimator(xform, mapArea);
                geom = decimator.decimate(geom);
            } catch (FactoryException e1) {

            } catch (NoninvertibleTransformException e1) {

            } catch (Exception e1) {

            }
            return geom;
        }
    }

    /** FeatureWriter for point geometry features. Currently supports circle WellKnownName Marks. */
    private class PointWriter extends HTMLImageMapFeatureWriter {
        // size of the symbol
        double size = 2;

        /** Creates a new PointWriter object. */
        public PointWriter() {}

        /** The shape for points is a circle. */
        protected String getShape() throws IOException {
            return "circle";
        }

        /**
         * Uses the supplied style to define point rendering. Currently it gets WellKnownName from a
         * Mark (circle is the only value correctly rendered by now). It also uses the Size
         * parameter to define circle radius.
         */
        protected void processSymbolizer(SimpleFeature ft, Rule rule, Symbolizer symbolizer)
                throws IOException {
            super.processSymbolizer(ft, rule, symbolizer);
            if (symbolizer instanceof PointSymbolizer) {
                Mark mark = SLD.mark((PointSymbolizer) symbolizer);
                Graphic graphic = SLD.graphic((PointSymbolizer) symbolizer);
                if (graphic != null && mark != null) {
                    Object oSize = graphic.getSize().evaluate(null);
                    if (oSize != null) size = Double.parseDouble(oSize.toString());
                }
            }
        }

        /**
         * Actually encodes the point.
         *
         * @param geom point to encode
         * @throws IOException if an error occures during encoding
         */
        protected void writeGeometry(Geometry geom, StringBuffer buf) throws IOException {
            if (geom instanceof Point) {
                Point p = (Point) geom;
                if (p.getCoordinate() != null) {

                    writeToBuffer(getPoint(p.getCoordinate()) + "," + (int) Math.round(size), buf);

                } else throw new IOException("null point coordinate");
            } else throw new IOException("Wrong geometry: it should be a Point");
        }
    }

    /** FeatureWriter for multipoint geometry features. */
    private class MultiPointWriter extends PointWriter {
        /** Creates a new MultiPointWriter object. */
        public MultiPointWriter() {}

        /** Uses writeMultiFeature. */
        protected void writeFeature(SimpleFeature ft, FeatureTypeStyle[] fts) throws IOException {
            writeMultiFeature(ft, fts);
        }
    }

    /**
     * FeatureWriter for LineString geometry features. A buffer is applied to the linear geometry to
     * transform it to a Polygon. The result polygon is then encoded.
     *
     * @author Mauro Bartolomeoli
     */
    private class LineStringWriter extends HTMLImageMapFeatureWriter {
        // default buffer size (in screen coordinates)
        int buffer = 2;
        /** Creates a new LineStringWriter object. */
        public LineStringWriter() {}

        /** The shape for lines is a poly. */
        protected String getShape() throws IOException {
            return "poly";
        }

        /**
         * Uses the supplied style to define line rendering. Currently it gets stroke-width to
         * define the buffer around the linestring.
         */
        protected void processSymbolizer(SimpleFeature ft, Rule rule, Symbolizer symbolizer)
                throws IOException {
            super.processSymbolizer(ft, rule, symbolizer);
            if (symbolizer instanceof LineSymbolizer) {
                buffer = SLD.width((LineSymbolizer) symbolizer);
            }
        }

        /**
         * Actually encodes the linestring.
         *
         * @param geom line to encode
         * @throws IOException if an error occures during encoding
         */
        protected void writeGeometry(Geometry geom, StringBuffer buf) throws IOException {
            if (geom instanceof LineString) {
                LineString l = (LineString) geom;

                try {
                    // transform buffer dimension to world coordinates
                    double bufferMultiplier = worldToScreen.createInverse().getScaleX();
                    // gets buffered linestring
                    Geometry buffered = l.buffer(buffer * bufferMultiplier);
                    if (buffered instanceof Polygon) {
                        Polygon poly = (Polygon) decimate(buffered);
                        if (poly != null) {
                            LineString shell = poly.getExteriorRing();
                            if (shell != null && shell.getCoordinates() != null)
                                writePathContent(shell.getCoordinates(), buf);
                            else throw new IOException("Nothing to encode");
                        } else throw new IOException("Nothing to encode");
                    } else {
                        throw new IOException("Impossible to encode: " + buffered);
                        // TODO: what kind of geometry can a buffer operation
                        // return?
                    }

                } catch (NoninvertibleTransformException e) {
                    throw new IOException(e.getMessage());
                }
            } else throw new IOException("Wrong geometry: it should be a LineString");
        }
    }

    /** FeatureWriter for multiline geometry features. */
    private class MultiLineStringWriter extends LineStringWriter {
        /** Creates a new MultiLineStringWriter object. */
        public MultiLineStringWriter() {}

        /** Uses writeMultiFeature. */
        protected void writeFeature(SimpleFeature ft, FeatureTypeStyle[] fts) throws IOException {
            writeMultiFeature(ft, fts);
        }
    }

    /** FeatureWriter for Polygon geometry features. */
    private class PolygonWriter extends HTMLImageMapFeatureWriter {
        /** Creates a new PolygonWriter object. */
        public PolygonWriter() {}
        /** The shape for polygons is a poly. */
        protected String getShape() throws IOException {
            return "poly";
        }

        /**
         * Actually encodes the polygon.
         *
         * @param geom the polygon to encode
         * @throws IOException if an error occures during encoding
         */
        protected void writeGeometry(Geometry geom, StringBuffer buf) throws IOException {
            Polygon poly = null;
            if (geom instanceof Polygon) {
                poly = (Polygon) geom;
                // if we have any hole
                // we create a new polygon without holes
                // using the HolesRemover
                if (poly.getNumInteriorRing() > 0) {
                    poly = HolesRemover.removeHoles(poly, 1.0 / worldToScreen.getScaleX());
                }
                poly = (Polygon) decimate(poly);
            } else throw new IOException("Impossible to encode: " + geom);
            if (poly != null) {
                LineString shell = poly.getExteriorRing();
                if (shell != null && shell.getCoordinates() != null)
                    writePathContent(shell.getCoordinates(), buf);
                else throw new IOException("Nothing to encode");
            } else throw new IOException("Nothing to encode");
        }
    }

    /** FeatureWriter for multipolygon geometry features. */
    private class MultiPolygonWriter extends PolygonWriter {
        /** Creates a new MultiPolygonWriter object. */
        public MultiPolygonWriter() {}

        /** Uses writeMultiFeature. */
        protected void writeFeature(SimpleFeature ft, FeatureTypeStyle[] fts) throws IOException {
            writeMultiFeature(ft, fts);
        }
    }

    /** FeatureWriter for multipolygon geometry features. */
    private class GeometryCollectionWriter extends HTMLImageMapFeatureWriter {

        HTMLImageMapFeatureWriter delegateWriter = null;

        /** Creates a new MultiPolygonWriter object. */
        public GeometryCollectionWriter() {}

        /**
         * Encodes the GeometryCollection.
         *
         * <p>The encoding is accomplished through many phases: 1) reset writer state 2) loops for
         * all the geometries, with the following phases for every single geometry. a) process
         * supplied style b) start feature encoding c) pre geometry encoding d) actual geometry
         * encoding e) post geometry encoding f) end feature encoding A delegate is used for many of
         * these phases. The delegate is a specific FeatureWriter for the single geometry, during
         * the loop.
         *
         * @param ft feature to encode
         * @param fts "cached" ftss matching the FeatureType of the feature
         * @throws IOException if an error occurs during encoding
         */
        protected void writeFeature(SimpleFeature ft, FeatureTypeStyle[] fts) throws IOException {
            reset(ft);

            GeometryCollection geomCollection = (GeometryCollection) ft.getDefaultGeometry();

            for (int i = 0; i < geomCollection.getNumGeometries(); i++) {
                Geometry geom = geomCollection.getGeometryN(i);
                if (geom != null) {
                    Class<?> gtype = geom.getClass();

                    // retrieves the right feature writer (based on the current geometry type)
                    delegateWriter = (HTMLImageMapFeatureWriter) writers.get(gtype);
                    if (processStyle(ft, fts)) {
                        try {
                            startElement(ft, "." + i);
                            startGeometry(geom);
                            writeGeometry(geom, buffer);
                            endGeometry(geom);
                            endElement(ft);
                            commitBuffer();
                        } catch (IOException e) {
                            buffer = new StringBuffer();
                            if (LOGGER.isLoggable(Level.WARNING))
                                LOGGER.warning("Problems encoding shape: " + e.getMessage());
                        } catch (Throwable t) {
                            buffer = new StringBuffer();
                            if (LOGGER.isLoggable(Level.SEVERE))
                                LOGGER.severe("Problems encoding shape: " + t.getMessage());
                        }
                    }
                } else {
                    buffer = new StringBuffer();
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.warning("Problems encoding shape: null geometry");
                }
            }
        }

        protected String getShape() throws IOException {
            return delegateWriter.getShape();
        }

        /**
         * Analyze the supplied style and process any matching rule.
         *
         * @param ft feature to which the style is going to be applied
         * @param ftsList cached fts matching the feature
         * @return true if the style filters "accept" the feature
         * @throws IOException if an error occurs during the process
         */
        protected boolean processStyle(SimpleFeature ft, FeatureTypeStyle[] ftsList)
                throws IOException {
            if (delegateWriter.processStyle(ft, ftsList)) {
                Iterator<String> iter = delegateWriter.extraAttributes.keySet().iterator();
                while (iter.hasNext()) {
                    String attrName = (String) iter.next();
                    extraAttributes.put(attrName, delegateWriter.extraAttributes.get(attrName));
                }
                return true;
            } else return false;
        }

        /** Actually write the geometry (through the delegate). */
        protected void writeGeometry(Geometry geom, StringBuffer buf) throws IOException {
            if (geom != null) delegateWriter.writeGeometry(geom, buf);
            else throw new IOException("null geometry");
        }
    }
}
