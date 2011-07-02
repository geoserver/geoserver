/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.kml.KMLLookAt;
import org.geoserver.kml.KMLNetworkLinkTransformer;
import org.geoserver.kml.KMLRasterTransformer;
import org.geoserver.kml.KMLTransformerBase;
import org.geoserver.kml.KMLUtils;
import org.geoserver.kml.KMLVectorTransformer;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.Layer;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Transformer to create a KML document from a {@link WMSMapContent}.
 * <p>
 * The document is meant as a "metadata" document, and contains title and abstract taken from the
 * {@link WMSMapContent#getTitle()} and {@link WMSMapContent#getAbstract()} respectively, and a
 * networklink to the live kml contents.
 */
class KMLMetadataDocumentTransformer extends TransformerBase {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    private WMS wms;

    public KMLMetadataDocumentTransformer(WMS wms) {
        this.wms = wms;
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLMetadataDocumentTranslator(handler);
    }

    /**
     * 
     *
     */
    protected class KMLMetadataDocumentTranslator extends TranslatorSupport {
        /**
         * Tolerance used to compare doubles for equality
         */
        static final double TOLERANCE = 1e-6;

        static final int RULES = 0;

        static final int ELSE_RULES = 1;

        private double scaleDenominator;

        public KMLMetadataDocumentTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {

            final WMSMapContent mapContent = (WMSMapContent) o;
            final GetMapRequest request = mapContent.getRequest();
            final List<Layer> layers = mapContent.layers();

            final KMLLookAt lookAtOpts = new KMLLookAt(request.getFormatOptions());
            // start("kml");
            start("kml",
                    KMLUtils.attributes(new String[] { "xmlns", "http://www.opengis.net/kml/2.2",
                            "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", "xmlns:atom",
                            "http://www.w3.org/2005/Atom", "xsi:schemaLocation",
                            "http://www.opengis.net/kml/2.2 http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd" }));

            // calculate scale denominator
            scaleDenominator = 1;
            try {
                scaleDenominator = RendererUtilities.calculateScale(mapContent.getRenderingArea(),
                        mapContent.getMapWidth(), mapContent.getMapHeight(), null);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error calculating scale denominator", e);
            }
            LOGGER.log(Level.FINE, "scale denominator = " + scaleDenominator);

            start("Document");
            String title = mapContent.getTitle();
            element("name", title);
            element("visibility", "1");
            element("open", "1");
            GeoServerInfo geoServerInfo = wms.getGeoServer().getGlobal();
            element("atom:author", geoServerInfo.getContact().getContactPerson());

            AttributesImpl href = new AttributesImpl();
            href.addAttribute("", "href", "href", "", wms.getGeoServer().getGlobal()
                    .getOnlineResource());
            element("atom:link", null, href);

            String abstract1 = buildDescription(mapContent);
            element("description", abstract1);
            // encodeBbox(mapContent.getAreaOfInterest());

            KMLNetworkLinkTransformer networkLinkTransformer = new KMLNetworkLinkTransformer(wms);
            networkLinkTransformer.setStandalone(false);
            networkLinkTransformer.setInline(true);
            networkLinkTransformer.setIndentation(getIndentation());
            networkLinkTransformer.setOmitXMLDeclaration(true);
            networkLinkTransformer.setEncodeAsRegion(false);
            // start("Folder");
            // element("name", "Full Online Content");
            networkLinkTransformer.createTranslator(contentHandler).encode(mapContent);
            // end("Folder");

            boolean includeSampleData = false;
            for (int i = 0; i < layers.size(); i++) {
                // layer and info
                MapLayerInfo layerInfo = mapContent.getRequest().getLayers().get(i);
                final int type = layerInfo.getType();
                if (MapLayerInfo.TYPE_VECTOR == type || MapLayerInfo.TYPE_REMOTE_VECTOR == type) {
                    includeSampleData = true;
                }
            }
            if (includeSampleData) {
                // start("Folder");
                // element("name", "Sample data");
                // for every layer specified in the request
                for (int i = 0; i < layers.size(); i++) {
                    // layer and info
                    Layer layer = layers.get(i);
                    MapLayerInfo layerInfo = mapContent.getRequest().getLayers().get(i);

                    // encodeSuperOverlayLayer(mapContent, layer);
                    if (layerInfo.getType() != MapLayerInfo.TYPE_RASTER) {
                        // vector
                        encodeVectorLayer(mapContent, layer, lookAtOpts);
                    } else {
                        // encode as normal ground overlay
                        encodeRasterLayer(mapContent, layer, lookAtOpts);
                    }
                }
                // end("Folder");
            }

            end("Document");
            end("kml");
        }

        // private void encodeBbox(final ReferencedEnvelope latlonbbox) {
        // start("LatLonBox");
        // element("north", String.valueOf(latlonbbox.getMaxY()));
        // element("south", String.valueOf(latlonbbox.getMinY()));
        // element("east", String.valueOf(latlonbbox.getMaxX()));
        // element("west", String.valueOf(latlonbbox.getMinX()));
        // end("LatLonBox");
        // }

        private String buildDescription(WMSMapContent mapContent) {
            StringBuilder sb = new StringBuilder();
            if (null != mapContent.getAbstract()) {
                sb.append(mapContent.getAbstract());
            }
            if (null != mapContent.getKeywords()) {
                sb.append("\n");
                for (String kw : mapContent.getKeywords()) {
                    if (null != kw) {
                        sb.append(kw).append(" ");
                    }
                }
            }
            return sb.toString();
        }

        /**
         * Encodes a vector layer as kml.
         */
        protected void encodeVectorLayer(WMSMapContent mapContent, Layer layer,
                KMLLookAt lookAtOpts) {
            // get the data
            SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
            SimpleFeatureCollection features = null;

            try {
                features = KMLUtils.loadFeatureCollection(featureSource, layer, mapContent, wms,
                        scaleDenominator);
                if (features == null) {
                    // it means no features need to be depicted with this style/scale denominator
                    return;
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // kmz not selected, just do straight vector
            KMLVectorTransformer tx = createVectorTransformer(mapContent, layer, lookAtOpts);
            initTransformer(tx);
            tx.setScaleDenominator(scaleDenominator);
            Translator kmlTranslator = tx.createTranslator(contentHandler);
            kmlTranslator.encode(features);
        }

        /**
         * Factory method, allows subclasses to inject their own version of the raster transfomer
         * 
         * @param mapContent
         * @param lookAtOpts
         * @return
         */
        protected KMLRasterTransformer createRasterTransfomer(WMSMapContent mapContent,
                KMLLookAt lookAtOpts) {
            return new KMLRasterTransformer(wms, mapContent, lookAtOpts);
        }

        /**
         * Factory method, allows subclasses to inject their own version of the vector transfomer
         * 
         * @param mapContent
         * @param lookAtOpts
         * @return
         */
        protected KMLVectorTransformer createVectorTransformer(WMSMapContent mapContent,
                Layer layer, KMLLookAt lookAtOpts) {
            return new KMLVectorTransformer(wms, mapContent, layer, lookAtOpts);
        }

        /**
         * Encodes a raster layer as kml.
         */
        protected void encodeRasterLayer(WMSMapContent mapContent, Layer layer,
                KMLLookAt lookAtOpts) {

            GetMapRequest request = mapContent.getRequest();
            request.getFormatOptions().put("superoverlay", Boolean.TRUE);
            KMLRasterTransformer tx = createRasterTransfomer(mapContent, lookAtOpts);
            initTransformer(tx);

            tx.createTranslator(contentHandler).encode(layer);
        }

        protected void initTransformer(KMLTransformerBase delegate) {
            delegate.setIndentation(getIndentation());
            delegate.setEncoding(getEncoding());
            delegate.setStandAlone(false);
        }

    }
}
