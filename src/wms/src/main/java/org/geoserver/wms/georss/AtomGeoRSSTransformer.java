/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import java.io.IOException;
import java.util.Date;
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

public class AtomGeoRSSTransformer extends GeoRSSTransformerBase {

    private WMS wms;

    public AtomGeoRSSTransformer(WMS wms) {
        this.wms = wms;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new AtomGeoRSSTranslator(wms, handler);
    }

    public class AtomGeoRSSTranslator extends GeoRSSTranslatorSupport {

        private WMS wms;

        public AtomGeoRSSTranslator(WMS wms, ContentHandler contentHandler) {
            super(contentHandler, null, "http://www.w3.org/2005/Atom");
            this.wms = wms;
            nsSupport.declarePrefix("georss", "http://www.georss.org/georss");
        }

        public void encode(Object o) throws IllegalArgumentException {
            WMSMapContent map = (WMSMapContent) o;

            start("feed");

            // title
            element("title", AtomUtils.getFeedTitle(map));

            // TODO: Revist URN scheme
            element("id", AtomUtils.getFeedURI(map));

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, AtomUtils.getFeedURL(map));
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("link", null, atts);

            // updated
            element("updated", AtomUtils.dateToRFC3339(new Date()));

            // entries
            try {
                encodeEntries(map);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            end("feed");
        }

        void encodeEntries(WMSMapContent map) throws IOException {
            List featureCollections = loadFeatureCollections(map);
            for (Iterator f = featureCollections.iterator(); f.hasNext(); ) {
                SimpleFeatureCollection features = (SimpleFeatureCollection) f.next();
                FeatureIterator<SimpleFeature> iterator = null;

                try {
                    iterator = features.features();

                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        try {
                            encodeEntry(feature, map);
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

        void encodeEntry(SimpleFeature feature, WMSMapContent map) {
            start("entry");

            // title
            element("title", feature.getID());

            start("author");
            element("name", wms.getGeoServer().getSettings().getContact().getContactPerson());
            end("author");

            // id
            element("id", AtomUtils.getEntryURI(wms, feature, map));

            String link = AtomUtils.getEntryURL(wms, feature, map);
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, link);
            atts.addAttribute(null, "rel", "rel", null, "self");
            element("link", null, atts);

            // updated
            element("updated", AtomUtils.dateToRFC3339(new Date()));

            // content
            atts = new AttributesImpl();
            atts.addAttribute(null, "type", "type", null, "html");
            element("content", AtomUtils.getFeatureDescription(feature), atts);

            // where
            if (geometryEncoding == GeometryEncoding.LATLONG
                    || !(feature.getDefaultGeometry() instanceof GeometryCollection)) {
                start("georss:where");
                geometryEncoding.encode((Geometry) feature.getDefaultGeometry(), this);
                end("georss:where");
                end("entry");
            } else {
                GeometryCollection col = (GeometryCollection) feature.getDefaultGeometry();
                start("georss:where");
                geometryEncoding.encode(col.getGeometryN(0), this);
                end("georss:where");
                end("entry");

                for (int i = 1; i < col.getNumGeometries(); i++) {
                    encodeRelatedGeometryEntry(
                            col.getGeometryN(i), feature.getID(), link, link + "#" + i);
                }
            }
        }

        void encodeRelatedGeometryEntry(Geometry g, String title, String link, String id) {
            start("entry");
            element("id", id);
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, "href", "href", null, link);
            atts.addAttribute(null, "rel", "rel", null, "related");
            element("link", null, atts);
            element("title", title);
            start("georss:where");
            geometryEncoding.encode(g, this);
            end("georss:where");
            end("entry");
        }
    }
}
