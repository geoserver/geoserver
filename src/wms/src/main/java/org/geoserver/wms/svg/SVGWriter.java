/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import org.geotools.data.DataSourceException;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Gabriel Roldan
 * @version $Id$
 */
class SVGWriter extends OutputStreamWriter {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(SVGWriter.class.getPackage().getName());

    /**
     * a number formatter setted up to write SVG legible numbers ('.' as decimal separator, no group
     * separator
     */
    private static DecimalFormat formatter;

    /** map of geometry class to writer */
    private HashMap<Class<? extends Geometry>, SVGFeatureWriter> writers;

    static {
        Locale locale = new Locale("en", "US");
        DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(locale);
        decimalSymbols.setDecimalSeparator('.');
        formatter = new DecimalFormat();
        formatter.setDecimalFormatSymbols(decimalSymbols);

        // do not group
        formatter.setGroupingSize(0);

        // do not show decimal separator if it is not needed
        formatter.setDecimalSeparatorAlwaysShown(false);
        formatter.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        // set default number of fraction digits
        formatter.setMaximumFractionDigits(5);

        // minimun fraction digits to 0 so they get not rendered if not needed
        formatter.setMinimumFractionDigits(0);
    }

    private double minY;

    private double maxY;

    private int coordsSkipCount;

    private int coordsWriteCount;

    private SVGFeatureWriterHandler writerHandler = new SVGFeatureWriterHandler();

    private SVGFeatureWriter featureWriter = null;

    private double minCoordDistance;

    private boolean pointsAsCircles;

    /** Creates a new SVGWriter object. */
    public SVGWriter(OutputStream out, Envelope mapAreaOfInterest) {
        super(out);

        this.minY = mapAreaOfInterest.getMinY();
        this.maxY = mapAreaOfInterest.getMaxY();

        initWriters();
    }

    private void initWriters() {
        writers = new HashMap<Class<? extends Geometry>, SVGFeatureWriter>();
        writers.put(Point.class, new PointWriter());
        writers.put(LineString.class, new LineStringWriter());
        writers.put(LinearRing.class, new LineStringWriter());
        writers.put(Polygon.class, new PolygonWriter());
        writers.put(MultiPoint.class, new MultiPointWriter());
        writers.put(MultiLineString.class, new MultiLineStringWriter());
        writers.put(MultiPolygon.class, new MultiPolygonWriter());
    }

    public void setPointsAsCircles(boolean asCircles) {
        this.pointsAsCircles = asCircles;
    }

    public void setGeometryType(Class gtype) {
        featureWriter = (SVGFeatureWriter) writers.get(gtype);

        if (featureWriter == null) {
            // check for abstract Geometry type
            if (gtype == Geometry.class) {
                featureWriter = new GeometryWriter();
            } else {
                throw new IllegalArgumentException("No SVG Feature writer defined for " + gtype);
            }
        }

        // if (gtype == Point.class) {
        // featureWriter = new PointWriter();
        // } else if (gtype == MultiPoint.class) {
        // featureWriter = new MultiPointWriter();
        // } else if (gtype == LineString.class) {
        // featureWriter = new LineStringWriter();
        // } else if (gtype == MultiLineString.class) {
        // featureWriter = new MultiLineStringWriter();
        // } else if (gtype == Polygon.class) {
        // featureWriter = new PolygonWriter();
        // } else if (gtype == MultiPolygon.class) {
        // featureWriter = new MultiPolygonWriter();
        // } else {
        // throw new IllegalArgumentException(
        // "No SVG Feature writer defined for " + gtype);
        // }

        /*
         * if (config.isCollectGeometries()) { this.writerHandler = new
         * CollectSVGHandler(featureWriter); } else { this.writerHandler = new
         * SVGFeatureWriterHandler(); this.writerHandler = new FIDSVGHandler(this.writerHandler);
         * this.writerHandler = new BoundsSVGHandler(this.writerHandler); this.writerHandler = new
         * AttributesSVGHandler(this.writerHandler); }
         */
    }

    public void setWriterHandler(SVGFeatureWriterHandler handler) {
        this.writerHandler = handler;
    }

    public void setMinCoordDistance(double minCoordDistance) {
        this.minCoordDistance = minCoordDistance;
    }

    /**
     * if a reference space has been set, returns a translated Y coordinate wich is inverted based
     * on the height of such a reference space, otherwise just returns <code>y</code>
     */
    public double getY(double y) {
        return (maxY - y) + minY;
    }

    public double getX(double x) {
        return x;
    }

    public void setMaximunFractionDigits(int numDigits) {
        formatter.setMaximumFractionDigits(numDigits);
    }

    public int getMaximunFractionDigits() {
        return formatter.getMaximumFractionDigits();
    }

    public void setMinimunFractionDigits(int numDigits) {
        formatter.setMinimumFractionDigits(numDigits);
    }

    public int getMinimunFractionDigits() {
        return formatter.getMinimumFractionDigits();
    }

    public void write(double d) throws IOException {
        write(formatter.format(d));
    }

    public void write(char c) throws IOException {
        super.write(c);
    }

    public void newline() throws IOException {
        super.write('\n');
    }

    public void writeFeatures(
            SimpleFeatureType featureType, SimpleFeatureIterator reader, String style)
            throws IOException {
        SimpleFeature ft;

        try {
            Class gtype = featureType.getGeometryDescriptor().getType().getBinding();

            boolean doCollect = false;
            /*
             * boolean doCollect = config.isCollectGeometries() && (gtype != Point.class) && (gtype
             * != MultiPoint.class);
             */
            setGeometryType(gtype);

            setPointsAsCircles("#circle".equals(style));

            setUpWriterHandler(featureType, doCollect);

            if (doCollect) {
                write("<path ");
                write("d=\"");
            }

            while (reader.hasNext()) {
                ft = reader.next();
                writeFeature(ft);
                ft = null;
            }

            if (doCollect) {
                write("\"/>\n");
            }

            LOGGER.fine("encoded " + featureType.getTypeName());
        } catch (NoSuchElementException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        }
    }

    private void setUpWriterHandler(SimpleFeatureType featureType, boolean doCollect)
            throws IOException {
        if (doCollect) {
            this.writerHandler = new CollectSVGHandler(featureWriter);
            LOGGER.finer("Established a collecting features writer handler");
        } else {
            this.writerHandler = new SVGFeatureWriterHandler();

            /*
             * REVISIT: get rid of all this attribute stuff, since if attributes are needed it fits
             * better to have SVG with gml attributes as another output format for WFS's getFeature.
             */
            List atts = new ArrayList(0); // config.getAttributes(typeName);

            if (atts.contains("#FID")) {
                this.writerHandler = new FIDSVGHandler(this.writerHandler);
                atts.remove("#FID");
                LOGGER.finer("Added FID handler decorator");
            }

            if (atts.contains("#BOUNDS")) {
                this.writerHandler = new BoundsSVGHandler(this.writerHandler);
                atts.remove("#BOUNDS");
                LOGGER.finer("Added BOUNDS handler decorator");
            }

            if (atts.size() > 0) {
                this.writerHandler = new AttributesSVGHandler(this.writerHandler);
                LOGGER.finer("Added ATTRIBUTES handler decorator");
            }
        }
    }

    public void writeFeature(SimpleFeature ft) throws IOException {
        writerHandler.startFeature(featureWriter, ft);
        writerHandler.startGeometry(featureWriter, ft);
        writerHandler.writeGeometry(featureWriter, ft);
        writerHandler.endGeometry(featureWriter, ft);
        writerHandler.endFeature(featureWriter, ft);
    }

    public class SVGFeatureWriterHandler {
        public void startFeature(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            featureWriter.startElement(ft);
        }

        public void endFeature(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            featureWriter.endElement(ft);
        }

        public void startGeometry(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            featureWriter.startGeometry((Geometry) ft.getDefaultGeometry());
        }

        public void writeGeometry(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            featureWriter.writeGeometry((Geometry) ft.getDefaultGeometry());
        }

        public void endGeometry(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            featureWriter.endGeometry((Geometry) ft.getDefaultGeometry());
        }
    }

    public class CollectSVGHandler extends SVGFeatureWriterHandler {

        private SVGFeatureWriter featureWriter;

        /** Creates a new CollectSVGHandler object. */
        public CollectSVGHandler(SVGFeatureWriter featureWriter) {
            this.featureWriter = featureWriter;
        }

        public void writeFeature(SimpleFeature ft) throws IOException {
            featureWriter.writeGeometry((Geometry) ft.getDefaultGeometry());
            write('\n');
        }
    }

    /** decorator handler that adds the feature id as the "id" attribute */
    public class FIDSVGHandler extends SVGFeatureWriterHandler {

        private SVGFeatureWriterHandler handler;

        /** Creates a new NormalSVGHandler object. */
        public FIDSVGHandler(SVGFeatureWriterHandler handler) {
            this.handler = handler;
        }

        public void startFeature(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            handler.startFeature(featureWriter, ft);
            write(" id=\"");

            try {
                write(ft.getID());
            } catch (IOException ex) {
                LOGGER.severe("error getting fid from " + ft);
                throw ex;
            }

            write("\"");
        }
    }

    /** decorator handler that adds the feature id as the "id" attribute */
    public class BoundsSVGHandler extends SVGFeatureWriterHandler {

        private SVGFeatureWriterHandler handler;

        public BoundsSVGHandler(SVGFeatureWriterHandler handler) {
            this.handler = handler;
        }

        public void startFeature(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            handler.startFeature(featureWriter, ft);

            Geometry geom = (Geometry) ft.getDefaultGeometry();
            Envelope env = geom.getEnvelopeInternal();
            write(" bounds=\"");
            write(env.getMinX());
            write(' ');
            write(env.getMinY());
            write(' ');
            write(env.getMaxX());
            write(' ');
            write(env.getMaxY());
            write('\"');
        }
    }

    /** decorator handler that adds the feature id as the "id" attribute */
    public class AttributesSVGHandler extends SVGFeatureWriterHandler {

        private SVGFeatureWriterHandler handler;

        public AttributesSVGHandler(SVGFeatureWriterHandler handler) {
            this.handler = handler;
        }

        public void startFeature(SVGFeatureWriter featureWriter, SimpleFeature ft)
                throws IOException {
            handler.startFeature(featureWriter, ft);

            SimpleFeatureType type = ft.getFeatureType();
            int numAtts = type.getAttributeCount();
            Object value;

            for (int i = 0; i < numAtts; i++) {
                value = ft.getAttribute(i);

                if ((value != null) && !(value instanceof Geometry)) {
                    write(' ');
                    write(type.getDescriptor(i).getName().getLocalPart());
                    write("=\"");
                    encodeAttribute(String.valueOf(value));
                    write('\"');
                }
            }
        }

        /**
         * Parses the passed string, and encodes the special characters (used in xml for special
         * purposes) with the appropriate codes. e.g. '&lt;' is changed to '&amp;lt;'
         *
         * @param inData The string to encode into xml.
         * @task REVISIT: Once we write directly to out, as we should, this method should be
         *     simpler, as we can just write strings with escapes directly to out, replacing as we
         *     iterate of chars to write them.
         */
        private void encodeAttribute(String inData) throws IOException {
            // return null, if null is passed as argument
            if (inData == null) {
                return;
            }

            // get the length of input String
            int length = inData.length();

            char charToCompare;

            // iterate over the input String
            for (int i = 0; i < length; i++) {
                charToCompare = inData.charAt(i);

                // if the ith character is special character, replace by code
                if (charToCompare == '"') {
                    write("&quot;");
                } else if (charToCompare > 127) {
                    writeUnicodeEscapeSequence(charToCompare);
                } else {
                    write(charToCompare);
                }
            }
        }

        /**
         * returns the xml unicode escape sequence for the character <code>c</code>, such as <code>
         * "&#x00d1;"</code> for the character <code>'?'</code>
         */
        private void writeUnicodeEscapeSequence(char c) throws IOException {
            write("&#x");

            String hex = Integer.toHexString(c);
            int pendingZeros = 4 - hex.length();

            for (int i = 0; i < pendingZeros; i++) write('0');

            write(hex);
            write(';');
        }
    }

    private abstract class SVGFeatureWriter {

        protected abstract void startElement(SimpleFeature feature) throws IOException;

        protected abstract void startGeometry(Geometry geom) throws IOException;

        protected abstract void writeGeometry(Geometry geom) throws IOException;

        protected void endGeometry(Geometry geom) throws IOException {
            write("\"");
        }

        protected void endElement(SimpleFeature feature) throws IOException {
            write("/>\n");
        }

        /**
         * Writes the content of the <b>d</b> attribute in a <i>path</i> SVG element
         *
         * <p>While iterating over the coordinate array passed as parameter, this method performs a
         * kind of very basic path generalization, verifying that the distance between the current
         * coordinate and the last encoded one is greater than the minimun distance expressed by the
         * field <code>minCoordDistance</code> and established by the method {@link
         * #setReferenceSpace(Envelope, float) setReferenceSpace(Envelope, blurFactor)}
         */
        protected void writePathContent(Coordinate[] coords) throws IOException {
            write('M');

            Coordinate prev = coords[0];
            Coordinate curr = null;
            write(getX(prev.x));
            write(' ');
            write(getY(prev.y));

            int nCoords = coords.length;
            write('l');

            for (int i = 1; i < nCoords; i++) {
                curr = coords[i];

                // let at least 3 points in case it is a polygon
                if ((i > 3) && (prev.distance(curr) <= minCoordDistance)) {
                    ++coordsSkipCount;

                    continue;
                }

                ++coordsWriteCount;
                write((getX(curr.x) - getX(prev.x)));
                write(' ');
                write(getY(curr.y) - getY(prev.y));
                write(' ');
                prev = curr;
            }
        }

        protected void writeClosedPathContent(Coordinate[] coords) throws IOException {
            writePathContent(coords);
            write('Z');
        }
    }

    /** */
    private class PointWriter extends SVGFeatureWriter {
        /** Creates a new PointWriter object. */
        public PointWriter() {}

        protected void startElement(SimpleFeature feature) throws IOException {
            write(pointsAsCircles ? "<circle r='0.25%' fill='blue'" : "<use");
        }

        protected void startGeometry(Geometry geom) throws IOException {}

        /**
         * overrides writeBounds for points to do nothing. You can get the position of the point
         * with the x and y attributes of the "use" SVG element written to represent each point
         */
        protected void writeBounds(Envelope env) throws IOException {}

        protected void writeGeometry(Geometry geom) throws IOException {
            Point p = (Point) geom;

            if (pointsAsCircles) {
                write(" cx=\"");
                write(getX(p.getX()));
                write("\" cy=\"");
                write(getY(p.getY()));
            } else {
                write(" x=\"");
                write(getX(p.getX()));
                write("\" y=\"");
                write(getY(p.getY()));
                // Issue GEOS-193, from John Steining.
                write("\" xlink:href=\"#point");

                // putting this in to fix the issue, but I am not sure about
                // the broader implications - I don't think we need it for
                // pointsAsCircles. And it looks like the quote gets closed
                // somewhere else, but I'm not sure where.
            }
        }
    }

    /** */
    private class MultiPointWriter extends PointWriter {

        public MultiPointWriter() {}

        protected void startElement(SimpleFeature feature) throws IOException {
            write("<g ");
        }

        protected void startGeometry(Geometry geom) throws IOException {
            write("/>\n");
        }

        protected void writeGeometry(Geometry geom) throws IOException {
            MultiPoint mp = (MultiPoint) geom;

            for (int i = 0; i < mp.getNumGeometries(); i++) {
                super.startElement(null);
                super.writeGeometry(mp.getGeometryN(i));
                super.endGeometry(mp.getGeometryN(i));
                super.endElement(null);
            }
        }

        protected void endElement(SimpleFeature feature) throws IOException {
            write("</g>\n");
        }
    }

    /**
     * Writer to handle feature types which contain a Geometry attribute that is actually of the
     * class Geometry. This can occur in heterogeneous data sets.
     *
     * @author Justin Deoliveira, jdeolive@openplans.org
     */
    private class GeometryWriter extends SVGFeatureWriter {
        SVGFeatureWriter delegate;

        protected void startElement(SimpleFeature feature) throws IOException {
            Geometry g = (Geometry) feature.getDefaultGeometry();
            delegate = null;

            if (g != null) {
                delegate = (SVGFeatureWriter) writers.get(g.getClass());
            }

            if (delegate == null) {
                throw new IllegalArgumentException("No SVG Feature writer defined for " + g);
            }

            delegate.startElement(feature);
        }

        protected void startGeometry(Geometry geom) throws IOException {
            delegate.startGeometry(geom);
        }

        protected void writeGeometry(Geometry geom) throws IOException {
            delegate.writeGeometry(geom);
        }
    }

    /** */
    private class LineStringWriter extends SVGFeatureWriter {
        /** Creates a new LineStringWriter object. */
        public LineStringWriter() {}

        protected void startElement(SimpleFeature feature) throws IOException {
            write("<path");
        }

        protected void startGeometry(Geometry geom) throws IOException {
            write(" d=\"");
        }

        protected void writeGeometry(Geometry geom) throws IOException {
            writePathContent(((LineString) geom).getCoordinates());
        }
    }

    /** */
    private class MultiLineStringWriter extends LineStringWriter {
        /** Creates a new MultiLineStringWriter object. */
        public MultiLineStringWriter() {}

        protected void writeGeometry(Geometry geom) throws IOException {
            MultiLineString ml = (MultiLineString) geom;

            for (int i = 0; i < ml.getNumGeometries(); i++) {
                super.writeGeometry(ml.getGeometryN(i));
            }
        }
    }

    /** */
    private class PolygonWriter extends SVGFeatureWriter {
        /** Creates a new PolygonWriter object. */
        public PolygonWriter() {}

        protected void startElement(SimpleFeature feature) throws IOException {
            write("<path");
        }

        protected void startGeometry(Geometry geom) throws IOException {
            write(" d=\"");
        }

        protected void writeGeometry(Geometry geom) throws IOException {
            Polygon poly = (Polygon) geom;
            LineString shell = poly.getExteriorRing();
            int nHoles = poly.getNumInteriorRing();
            writeClosedPathContent(shell.getCoordinates());

            for (int i = 0; i < nHoles; i++)
                writeClosedPathContent(poly.getInteriorRingN(i).getCoordinates());
        }
    }

    /** */
    private class MultiPolygonWriter extends PolygonWriter {
        /** Creates a new MultiPolygonWriter object. */
        public MultiPolygonWriter() {}

        protected void writeGeometry(Geometry geom) throws IOException {
            MultiPolygon mpoly = (MultiPolygon) geom;

            for (int i = 0; i < mpoly.getNumGeometries(); i++) {
                super.writeGeometry(mpoly.getGeometryN(i));
            }
        }
    }
}
