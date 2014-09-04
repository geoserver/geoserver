/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.RasterLayer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Transformer to create a KML document from a {@link WMSMapContent}.
 * 
 * @author Justin Deoliveira
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 * @version $Id$
 * @see KMLVectorTransformer
 * @see KMLRasterTransformer
 */
public class KMLTransformer extends TransformerBase {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    /**
     * Flag controlling wether kmz was requested.
     */
    boolean kmz = false;

    private WMS wms;

    private final Map<String, Style> embeddedIcons;

    public KMLTransformer(WMS wms) {
        this.wms = wms;
        this.embeddedIcons = new HashMap<String, Style>();
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLTranslator(handler);
    }

    public void setKmz(boolean kmz) {
        this.kmz = kmz;
    }

    protected class KMLTranslator extends TranslatorSupport {
        /**
         * Tolerance used to compare doubles for equality
         */
        static final double TOLERANCE = 1e-6;

        static final int RULES = 0;

        static final int ELSE_RULES = 1;

        private double scaleDenominator;

        public KMLTranslator(ContentHandler handler) {
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
                            "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
                            "xsi:schemaLocation",
                            "http://www.opengis.net/kml/2.2 http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd" }));

            // calculate scale denominator
            scaleDenominator = 1;
            try {
                scaleDenominator = RendererUtilities.calculateOGCScale(mapContent.getRenderingArea(),
                        mapContent.getMapWidth(), null);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error calculating scale denominator", e);
            }
            LOGGER.log(Level.FINE, "scale denominator = " + scaleDenominator);

            // if we have more than one layer ( or a legend was requested ),
            // use the name "GeoServer" to group them
            boolean group;
            Boolean legend = (Boolean) request.getFormatOptions().get("legend");
            if (legend != null) {
                group = (layers.size() > 1) || legend.booleanValue();
            } else {
                group = (layers.size() > 1);
            }
            
            if (group) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < layers.size(); i++) {
                    sb.append(layers.get(i).getTitle() + ",");
                }
                sb.setLength(sb.length() - 1);

                start("Document");
                String kmltitle = (String) mapContent.getRequest().getFormatOptions().get("kmltitle");
                element("name", (kmltitle != null ? kmltitle : sb.toString()));
            }

            // for every layer specified in the request
            for (int i = 0; i < layers.size(); i++) {
                // layer and info
                Layer layer = layers.get(i);

                // was a super overlay requested?
                Boolean superoverlay = (Boolean) mapContent.getRequest().getFormatOptions()
                        .get("superoverlay");
                superoverlay = (superoverlay == null ? Boolean.FALSE : superoverlay);
                if (superoverlay) {
                    // encode as super overlay
                    encodeSuperOverlayLayer(mapContent, layer);
                } else {
                    // figure out which type of layer this is, raster or vector
                    if (layer instanceof FeatureLayer) {
                        // vector
                        encodeVectorLayer(mapContent, layer, lookAtOpts);
                    } else if(layer instanceof RasterLayer){
                        // encode as normal ground overlay
                        encodeRasterLayer(mapContent, layer, lookAtOpts);
                    }
                }
            }

            // legend suppoer
            if (legend != null && legend.booleanValue()) {
                // for every layer specified in the request
                for (int i = 0; i < layers.size(); i++) {
                    // layer and info
                    Layer layer = layers.get(i);
                    encodeLegend(mapContent, layer);
                }
            }

            if (group) {
                end("Document");
            }

            end("kml");
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

            // was kmz requested?
            if (kmz) {
                // calculate kmscore to determine if we shoud write as vectors
                // or pre-render
                int kmscore = wms.getKmScore();
                Object kmScoreObj = mapContent.getRequest().getFormatOptions().get("kmscore");
                if (kmScoreObj != null) {
                    kmscore = (Integer) kmScoreObj;
                }
                boolean useVector = useVectorOutput(kmscore, features.size());

                if (useVector) {
                    // encode
                    KMLVectorTransformer tx = createVectorTransformer(mapContent, layer, lookAtOpts, embeddedIcons);
                    initTransformer(tx);
                    tx.setScaleDenominator(scaleDenominator);
                    tx.createTranslator(contentHandler).encode(features);
                } else {
                    KMLRasterTransformer tx = createRasterTransfomer(mapContent, lookAtOpts);
                    initTransformer(tx);

                    // set inline to true to have the transformer reference images
                    // inline in the zip file
                    tx.setInline(true);
                    tx.createTranslator(contentHandler).encode(layer);
                }
            } else {
                // kmz not selected, just do straight vector
                KMLVectorTransformer tx = createVectorTransformer(mapContent, layer, lookAtOpts);
                initTransformer(tx);
                tx.setScaleDenominator(scaleDenominator);
                tx.createTranslator(contentHandler).encode(features);
            }
        }

        /**
         * Factory method, allows subclasses to inject their own version of the raster transfomer
         * 
         * @param mapContent
         * @param lookAtOpts
         * @return
         */
        protected KMLRasterTransformer createRasterTransfomer(WMSMapContent mapContent, KMLLookAt lookAtOpts) {
            return new KMLRasterTransformer(wms, mapContent, lookAtOpts);
        }

        /**
         * Factory method, allows subclasses to inject their own version of the vector transfomer
         * 
         * @param mapContent
         * @param lookAtOpts
         * @return
         */
        protected KMLVectorTransformer createVectorTransformer(WMSMapContent mapContent, Layer layer, KMLLookAt lookAtOpts) {
            return new KMLVectorTransformer(wms, mapContent, layer, lookAtOpts);
        }

        protected KMLVectorTransformer createVectorTransformer(WMSMapContent mapContent, Layer layer, KMLLookAt lookAtOpts, Map<String, Style> iconStyles) {
            return new KMLVectorTransformer(wms, mapContent, layer, lookAtOpts, iconStyles);
        }

        /**
         * Encodes a raster layer as kml.
         */
        protected void encodeRasterLayer(WMSMapContent mapContent, Layer layer,
                KMLLookAt lookAtOpts) {
            KMLRasterTransformer tx = createRasterTransfomer(mapContent, lookAtOpts);
            initTransformer(tx);

            tx.setInline(kmz);
            tx.createTranslator(contentHandler).encode(layer);
        }

        /**
         * Encodes a layer as a super overlay.
         * @param group 
         */
        protected void encodeSuperOverlayLayer(WMSMapContent mapContent, Layer layer) {
            KMLSuperOverlayTransformer tx = new KMLSuperOverlayTransformer(wms, mapContent);
            initTransformer(tx);
            tx.createTranslator(contentHandler).encode(layer);
        }

        /**
         * Encodes the legend for a maper layer as a scree overlay.
         */
        protected void encodeLegend(WMSMapContent mapContent, Layer layer) {
            KMLLegendTransformer tx = new KMLLegendTransformer(mapContent);
            initTransformer(tx);
            tx.createTranslator(contentHandler).encode(layer);
        }

        protected void initTransformer(KMLTransformerBase delegate) {
            delegate.setIndentation(getIndentation());
            delegate.setEncoding(getEncoding());
            delegate.setStandAlone(false);
        }

        double computeScaleDenominator(Layer layer, WMSMapContent mapContent) {
            Rectangle paintArea = new Rectangle(mapContent.getMapWidth(), mapContent.getMapHeight());
            AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(
                    mapContent.getRenderingArea(), paintArea);

            try {
                // 90 = OGC standard DPI (see SLD spec page 37)
                return RendererUtilities.calculateOGCScale(mapContent.getRenderingArea(), paintArea.width, null);
            } catch (Exception e) {
                // probably either (1) no CRS (2) error xforming, revert to
                // old method - the best we can do (DJB)
                return 1 / worldToScreen.getScaleX();
            }
        }

        /**
         * Determines whether to return a vector (KML) result of the data or to return an image
         * instead. If the kmscore is 100, then the output should always be vector. If the kmscore
         * is 0, it should always be raster. In between, the number of features is weighed against
         * the kmscore value. kmscore determines whether to return the features as vectors, or as
         * one raster image. It is the point, determined by the user, where X number of features is
         * "too many" and the result should be returned as an image instead.
         * 
         * kmscore is logarithmic. The higher the value, the more features it takes to make the
         * algorithm return an image. The lower the kmscore, the fewer features it takes to force an
         * image to be returned. (in use, the formula is exponential: as you increase the KMScore
         * value, the number of features required increases exponentially).
         * 
         * @param kmscore
         *            the score, between 0 and 100, use to determine what output to use
         * @param numFeatures
         *            how many features are being rendered
         * @return true: use just kml vectors, false: use raster result
         */
        boolean useVectorOutput(int kmscore, int numFeatures) {
            if (kmscore == 100) {
                return true; // vector KML
            }

            if (kmscore == 0) {
                return false; // raster KMZ
            }

            // For numbers in between, determine exponentionally based on kmscore value:
            // 10^(kmscore/15)
            // This results in exponential growth.
            // The lowest bound is 1 feature and the highest bound is 3.98 million features
            // The most useful kmscore values are between 20 and 70 (21 and 46000 features
            // respectively)
            // A good default kmscore value is around 40 (464 features)
            double magic = Math.pow(10, kmscore / 15);

            if (numFeatures > magic) {
                return false; // return raster
            } else {
                return true; // return vector
            }
        }
    }

    public Map<String, Style> getEmbeddedIcons() {
        return Collections.unmodifiableMap(embeddedIcons);
    }
}
