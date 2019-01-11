/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.feature.ReprojectingFeatureCollection;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Base class for RSS/Atom xml transformers
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Andrea Aime - GeoSolutions
 */
public abstract class GeoRSSTransformerBase extends TransformerBase {
    /** logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.georss");

    static final Configuration GML_CONFIGURATION = new GMLConfiguration();

    /**
     * Enumeration for geometry encoding.
     *
     * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
     */
    public static class GeometryEncoding {
        private GeometryEncoding() {}

        public String getPrefix() {
            return null;
        }

        public String getNamespaceURI() {
            return null;
        }

        public void encode(Geometry g, GeoRSSTranslatorSupport translator) {}

        /**
         * "simple" encoding:
         *
         * <p>ex: <georss:point>45.256 -71.92</georss:point>,<georss:line>...</georss:line>,...
         */
        public static GeometryEncoding SIMPLE =
                new GeometryEncoding() {
                    public String getPrefix() {
                        return "georss";
                    }

                    public String getNamespaceURI() {
                        return "http://www.georss.org/georss";
                    }

                    public void encode(Geometry g, GeoRSSTranslatorSupport t) {
                        if (g instanceof Point) {
                            Point p = (Point) g;
                            t.element("georss:point", p.getY() + " " + p.getX());
                        }

                        if (g instanceof LineString) {
                            LineString l = (LineString) g;

                            StringBuffer sb = new StringBuffer();

                            for (int i = 0; i < l.getNumPoints(); i++) {
                                Coordinate c = l.getCoordinateN(i);
                                sb.append(c.y).append(" ").append(c.x).append(" ");
                            }

                            sb.setLength(sb.length() - 1);

                            t.element("georss:line", sb.toString());
                        }

                        if (g instanceof Polygon) {
                            Polygon p = (Polygon) g;
                            LineString line = p.getExteriorRing();

                            StringBuffer sb = new StringBuffer();

                            for (int i = 0; i < line.getNumPoints(); i++) {
                                Coordinate c = line.getCoordinateN(i);
                                sb.append(c.y).append(" ").append(c.x).append(" ");
                            }

                            sb.setLength(sb.length() - 1);

                            t.element("georss:polygon", sb.toString());
                        }
                    }
                };

        /**
         * gml encoding:
         *
         * <p>ex: <gml:Point> <gml:pos>45.256 -71.92</gml:pos> </gml:Point>
         */
        public static GeometryEncoding GML =
                new GeometryEncoding() {
                    public String getPrefix() {
                        return "gml";
                    }

                    public String getNamespaceURI() {
                        return "http://www.opengis.net/gml";
                    }

                    public void encode(Geometry g, final GeoRSSTranslatorSupport translator) {
                        try {
                            // get the proper element name
                            QName elementName = null;
                            if (g instanceof Point) {
                                elementName = org.geotools.gml2.GML.Point;
                            } else if (g instanceof LineString) {
                                elementName = org.geotools.gml2.GML.LineString;
                            } else if (g instanceof Polygon) {
                                elementName = org.geotools.gml2.GML.Polygon;
                            } else if (g instanceof MultiPoint) {
                                elementName = org.geotools.gml2.GML.MultiPoint;
                            } else if (g instanceof MultiLineString) {
                                elementName = org.geotools.gml2.GML.MultiLineString;
                            } else if (g instanceof MultiPolygon) {
                                elementName = org.geotools.gml2.GML.MultiPolygon;
                            } else {
                                elementName = org.geotools.gml2.GML._Geometry;
                            }

                            // encode in GML3
                            Encoder encoder = new Encoder(GML_CONFIGURATION);
                            encoder.encode(g, elementName, translator);
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Cannot transform the specified geometry in GML", e);
                        }
                    };
                };

        /**
         * lat/long encoding:
         *
         * <p>ex: <geo:lat>45.256</geo:lat> <geo:long>-71.92</geo:long>
         */
        public static GeometryEncoding LATLONG =
                new GeometryEncoding() {
                    public String getPrefix() {
                        return "geo";
                    }

                    public String getNamespaceURI() {
                        return "http://www.w3.org/2003/01/geo/wgs84_pos#";
                    }

                    public void encode(Geometry g, GeoRSSTranslatorSupport t) {
                        // encode the centroid
                        Point p = g.getCentroid();
                        t.element("geo:lat", "" + p.getY());
                        t.element("geo:long", "" + p.getX());
                    }
                };
    };

    /** Geometry encoding to use. */
    protected GeometryEncoding geometryEncoding = GeometryEncoding.LATLONG;

    public void setGeometryEncoding(GeometryEncoding geometryEncoding) {
        this.geometryEncoding = geometryEncoding;
    }

    abstract class GeoRSSTranslatorSupport extends TranslatorSupport implements ContentHandler {
        public GeoRSSTranslatorSupport(ContentHandler contentHandler, String prefix, String nsURI) {
            super(contentHandler, prefix, nsURI);

            nsSupport.declarePrefix(
                    geometryEncoding.getPrefix(), geometryEncoding.getNamespaceURI());
        }

        /** Encodes the geometry of a feature. */
        protected void encodeGeometry(SimpleFeature feature) {
            if (feature.getDefaultGeometry() != null) {
                Geometry g = (Geometry) feature.getDefaultGeometry();

                // handle case of multi geometry with a single geometry in it
                if (g instanceof GeometryCollection) {
                    GeometryCollection mg = (GeometryCollection) g;

                    if (mg.getNumGeometries() == 1) {
                        g = mg.getGeometryN(0);
                    }
                }

                geometryEncoding.encode(g, this);
            }
        }

        // overrides to increase visiblity
        public void start(String element) {
            super.start(element);
        }

        public void element(String element, String content) {
            super.element(element, content);
        }

        @SuppressWarnings("unchecked")
        protected List loadFeatureCollections(WMSMapContent map) throws IOException {
            ReferencedEnvelope mapArea = map.getRenderingArea();
            CoordinateReferenceSystem wgs84 = null;
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            try {
                // this should never throw an exception, but we have to deal with it anyways
                wgs84 = CRS.decode("EPSG:4326");
            } catch (Exception e) {
                throw (IOException) (new IOException("Unable to decode WGS84...").initCause(e));
            }

            List featureCollections = new ArrayList();
            for (Layer layer : map.layers()) {
                Query query = layer.getQuery();

                SimpleFeatureCollection features = null;
                try {
                    SimpleFeatureSource source;
                    source = (SimpleFeatureSource) layer.getFeatureSource();

                    GeometryDescriptor gd = source.getSchema().getGeometryDescriptor();
                    if (gd == null) {
                        // geometryless layers...
                        features = source.getFeatures(query);
                    } else {
                        // make sure we are querying the source with the bbox in the right CRS, if
                        // not, reproject the bbox
                        ReferencedEnvelope env = new ReferencedEnvelope(mapArea);
                        CoordinateReferenceSystem sourceCRS = gd.getCoordinateReferenceSystem();
                        if (sourceCRS != null
                                && !CRS.equalsIgnoreMetadata(
                                        mapArea.getCoordinateReferenceSystem(), sourceCRS)) {
                            env = env.transform(sourceCRS, true);
                        }

                        // build the mixed query
                        Query mixed = new Query(query);
                        Filter original = query.getFilter();
                        Filter bbox =
                                ff.bbox(
                                        gd.getLocalName(),
                                        env.getMinX(),
                                        env.getMinY(),
                                        env.getMaxX(),
                                        env.getMaxY(),
                                        null);
                        mixed.setFilter(ff.and(original, bbox));

                        // query and eventually reproject
                        features = source.getFeatures(mixed);
                        if (sourceCRS != null && !CRS.equalsIgnoreMetadata(wgs84, sourceCRS)) {
                            ReprojectingFeatureCollection coll =
                                    new ReprojectingFeatureCollection(features, wgs84);
                            coll.setDefaultSource(sourceCRS);
                            features = coll;
                        }

                        if (features == null) throw new NullPointerException();

                        featureCollections.add(features);
                    }
                } catch (Exception e) {
                    String msg = "Unable to encode map layer: " + layer;
                    LOGGER.log(Level.SEVERE, msg, e);
                }
            }

            return featureCollections;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            String string = new String(ch, start, length);
            chars(string);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            // working around a bug in the GML encoder, it won't properly setup the qName
            end("gml:" + localName);
        }

        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            start(qName, atts);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (getIndentation() > 0) {
                characters(ch, start, length);
            }
        }

        public void endDocument() throws SAXException {
            // nothing to do
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            // nothing to do
        }

        public void processingInstruction(String target, String data) throws SAXException {
            // nothing do do
        }

        public void setDocumentLocator(Locator locator) {
            // nothing do do
        }

        public void skippedEntity(String name) throws SAXException {
            // nothing to do

        }

        public void startDocument() throws SAXException {
            // nothing to do

        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // nothing to do
        }
    }
}
