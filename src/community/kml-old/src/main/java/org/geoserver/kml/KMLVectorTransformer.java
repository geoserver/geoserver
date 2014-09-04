/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Transforms a feature collection to a kml document consisting of nested "Style" and "Placemark"
 * elements for each feature in the collection. A new transfomer must be instantianted for each
 * feature collection, the feature collection provided to the translator is supposed to be the one
 * coming out of the MapLayer
 * <p>
 * Usage:
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * 
 */
public class KMLVectorTransformer extends KMLMapTransformer {

    private KMLLookAt lookAtOpts;
    private final Map<String, Style> iconStyles;

    public KMLVectorTransformer(WMS wms, WMSMapContent mapContent, Layer mapLayer) {
        this(wms, mapContent, mapLayer, null);
    }

    public KMLVectorTransformer(WMS wms, WMSMapContent mapContent, Layer mapLayer, KMLLookAt lookAtOpts) {
        this(wms, mapContent, mapLayer, lookAtOpts, null);

        setNamespaceDeclarationEnabled(false);
        this.lookAtOpts = lookAtOpts;
    }
    
    public KMLVectorTransformer(WMS wms, WMSMapContent mapContent, Layer mapLayer, KMLLookAt lookAtOpts, Map<String, Style> iconStyles) {
        super(wms, mapContent, mapLayer, iconStyles);
        
        setNamespaceDeclarationEnabled(false);
        this.lookAtOpts = lookAtOpts;
        this.iconStyles = iconStyles;
    }

    /**
     * Sets the scale denominator.
     */
    public void setScaleDenominator(double scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLTranslator(handler);
    }

    protected class KMLTranslator extends KMLMapTranslatorSupport {
        /**
         * Store the regionating strategy being applied
         */
        private RegionatingStrategy myStrategy;

        public KMLTranslator(ContentHandler contentHandler) {
            super(contentHandler);

            KMLGeometryTransformer geometryTransformer = new KMLGeometryTransformer();
            // geometryTransformer.setUseDummyZ( true );
            geometryTransformer.setOmitXMLDeclaration(true);
            geometryTransformer.setNamespaceDeclarationEnabled(true);

            GeoServer config = wms.getGeoServer();
            geometryTransformer.setNumDecimals(config.getSettings().getNumDecimals());

            geometryTranslator = (KMLGeometryTransformer.KMLGeometryTranslator) geometryTransformer
                    .createTranslator(contentHandler, mapContent);
        }

        public void setRegionatingStrategy(RegionatingStrategy rs) {
            myStrategy = rs;
        }

        public void encode(Object o) throws IllegalArgumentException {
            SimpleFeatureCollection features = (SimpleFeatureCollection) o;
            SimpleFeatureType featureType = features.getSchema();

            if (isStandAlone()) {
                start("kml");
            }

            // start the root document, name it the name of the layer
            start("Document",
                    KMLUtils.attributes(new String[] { "xmlns:atom", "http://purl.org/atom/ns#" }));
            String kmltitle = (String) mapContent.getRequest().getFormatOptions().get("kmltitle");
            element("name", (kmltitle != null && mapContent.layers().size() <= 1 ? kmltitle : mapLayer.getTitle()));

            if(lookAtOpts != null){
                ReferencedEnvelope bounds = features.getBounds();
                if(bounds != null){
                    KMLLookAtTransformer tx;
                    tx = new KMLLookAtTransformer(bounds, getIndentation(), getEncoding());
                    Translator translator = tx.createTranslator(contentHandler);
                    translator.encode(lookAtOpts);
                }
            }
            
            String relLinks = (String) mapContent.getRequest().getFormatOptions().get("relLinks");
            // Add prev/next links if requested
            if (mapContent.getRequest().getMaxFeatures() != null && relLinks != null
                    && relLinks.equalsIgnoreCase("true")) {

                String linkbase = "";
                try {
                    linkbase = getFeatureTypeURL();
                    linkbase += ".kml";
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }

                int maxFeatures = mapContent.getRequest().getMaxFeatures();
                int startIndex = (mapContent.getRequest().getStartIndex() == null) ? 0 : mapContent
                        .getRequest().getStartIndex().intValue();
                int prevStart = startIndex - maxFeatures;
                int nextStart = startIndex + maxFeatures;

                // Previous page, if any
                if (prevStart >= 0) {
                    String prevLink = linkbase + "?startindex=" + prevStart + "&maxfeatures="
                            + maxFeatures;
                    element("atom:link", null,
                            KMLUtils.attributes(new String[] { "rel", "prev", "href", prevLink }));
                    encodeSequentialNetworkLink(linkbase, prevStart, maxFeatures, "prev",
                            "Previous page");
                }

                // Next page, if any
                if (features.size() >= maxFeatures) {
                    String nextLink = linkbase + "?startindex=" + nextStart + "&maxfeatures="
                            + maxFeatures;
                    element("atom:link", null,
                            KMLUtils.attributes(new String[] { "rel", "next", "href", nextLink }));
                    encodeSequentialNetworkLink(linkbase, nextStart, maxFeatures, "next",
                            "Next page");
                }
            }

            // get the styles for the layer
            FeatureTypeStyle[] featureTypeStyles = KMLUtils.filterFeatureTypeStyles(
                    mapLayer.getStyle(), featureType);

            // encode the schemas (kml 2.2)
            encodeSchemas(features);

            // encode the layers
            encode(features, mapLayer.getStyle(), featureTypeStyles);

            // encode the legend
            // encodeLegendScreenOverlay();
            end("Document");

            if (isStandAlone()) {
                end("kml");
            }
        }

        /**
         * 
         * Encodes a networklink for previous or next document in a sequence
         * 
         * Note that in KML 2.2 atom:link is supported and may be better.
         * 
         * @param linkbase
         *            the base fore creating URLs
         * @param prevStart
         *            previous start value
         * @param maxFeatures
         *            maximum number of features to return
         * @param id
         *            attribute to use for this NetworkLink
         * @param readableName
         *            goes into linkName
         */
        private void encodeSequentialNetworkLink(String linkbase, int prevStart, int maxFeatures,
                String id, String readableName) {
            String link = linkbase + "?startindex=" + prevStart + "&maxfeatures=" + maxFeatures;
            start("NetworkLink", KMLUtils.attributes(new String[] { "id", id }));
            element("description", readableName);
            start("Link");
            element("href", link);
            end("Link");
            end("NetworkLink");
        }

        /**
         * Encodes the <Schema> element in kml 2.2
         * 
         * @param featureTypeStyles
         */
        protected void encodeSchemas(SimpleFeatureCollection featureTypeStyles) {
            // the code is at the moment in KML3VectorTransformer
        }

        protected void encode(SimpleFeatureCollection features, Style style, FeatureTypeStyle[] styles) {
            // grab a reader and process
            SimpleFeatureIterator reader = null;

            try {
                // grab a reader and process
                reader = features.features();

                // Write Styles
                while (reader.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) reader.next();
                    try {
                        List<Symbolizer> symbolizers = filterSymbolizers(feature, styles);
                        if (symbolizers.size() > 0) {
                            encodePlacemark(feature, style, symbolizers, lookAtOpts);
                        }
                    } catch (RuntimeException t) {
                        // if the stream has been closed by the client don't keep on going forward,
                        // this is not a feature local issue
                        //
                        if (t.getCause() instanceof SAXException)
                            throw t;
                        else
                            LOGGER.log(Level.WARNING, "Failure tranforming feature to KML:"
                                    + feature.getID(), t);
                    }
                }
            } finally {
                // make sure we always close
                reader.close();
            }
        }
    }

}
