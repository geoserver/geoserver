/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.xml.transform.Translator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes an RSS feed tagged with geo information.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class RSSGeoRSSTransformer extends GeoRSSTransformerBase {

    private WMS wms;

    public RSSGeoRSSTransformer(WMS wms) {
        this.wms = wms;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new RSSGeoRSSTranslator(wms, handler);
    }

    class RSSGeoRSSTranslator extends GeoRSSTranslatorSupport {
        private WMS wms;

        public RSSGeoRSSTranslator(WMS wms, ContentHandler contentHandler) {
            super(contentHandler, null, null);
            this.wms = wms;
            nsSupport.declarePrefix("georss", "http://www.georss.org/georss");
            nsSupport.declarePrefix("atom", "http://www.w3.org/2005/Atom");
        }

        public void encode(Object o) throws IllegalArgumentException {
            WMSMapContent map = (WMSMapContent) o;

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "version", "version", null, "2.0");

            start("rss", atts);
            start("channel");

            element("title", AtomUtils.getFeedTitle(map));
            element("description", AtomUtils.getFeedDescription(map));

            start("link");
            cdata(AtomUtils.getFeedURL(map));
            end("link");

            atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, AtomUtils.getFeedURL(map));
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("atom:link", null, atts);

            // items
            try {
                encodeItems(map);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            end("channel");
            end("rss");
        }

        void encodeItems(WMSMapContent map) throws IOException {
            List featureCollections = loadFeatureCollections(map);
            for (Iterator f = featureCollections.iterator(); f.hasNext(); ) {
                SimpleFeatureCollection features = (SimpleFeatureCollection) f.next();
                FeatureIterator<SimpleFeature> iterator = null;

                try {
                    iterator = features.features();

                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        try {
                            encodeItem(feature, map);
                        } catch (Exception e) {
                            LOGGER.warning("Encoding failed for feature: " + feature.getID());
                            LOGGER.log(Level.FINE, "", e);
                        }
                    }
                } finally {
                    if (iterator != null) {
                        iterator.close();
                    }
                }
            }
        }

        void encodeItem(SimpleFeature feature, WMSMapContent map) throws IOException {
            start("item");

            String title = feature.getID();
            String link = null;

            try {
                title = AtomUtils.getFeatureTitle(feature);
                link = AtomUtils.getEntryURL(wms, feature, map);
            } catch (Exception e) {
                String msg = "Error occured executing title template for: " + feature.getID();
                LOGGER.log(Level.WARNING, msg, e);
            }

            element("title", title);

            // create the link as getFeature request with fid filter
            start("link");
            cdata(link);
            end("link");

            start("guid");
            cdata(link);
            end("guid");

            start("description");
            cdata(AtomUtils.getFeatureDescription(feature));
            end("description");

            GeometryCollection col =
                    feature.getDefaultGeometry() instanceof GeometryCollection
                            ? (GeometryCollection) feature.getDefaultGeometry()
                            : null;

            if (geometryEncoding == GeometryEncoding.LATLONG
                    || (col == null && feature.getDefaultGeometry() != null)) {
                geometryEncoding.encode((Geometry) feature.getDefaultGeometry(), this);
                end("item");
            } else if (col == null) {
                end("item");
            } else {
                geometryEncoding.encode(col.getGeometryN(0), this);
                end("item");

                for (int i = 1; i < col.getNumGeometries(); i++) {
                    encodeRelatedGeometryItem(col.getGeometryN(i), title, link, i);
                }
            }
        }

        void encodeRelatedGeometryItem(Geometry g, String title, String link, int count) {
            start("item");
            element("title", "Continuation of " + title);
            element("link", link);
            element("guid", link + "#" + count);
            element("description", "Continuation of " + title);
            geometryEncoding.encode(g, this);
            end("item");
        }
    }
}
