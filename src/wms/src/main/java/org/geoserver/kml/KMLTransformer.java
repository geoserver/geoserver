/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.MapLayer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Transformer to create a KML document from a {@link WMSMapContext}.
 * 
 * @author Justin Deoliveira
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

    public KMLTransformer(WMS wms) {
        this.wms = wms;
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
            //start("kml");
            start("kml", KMLUtils.attributes(
                    new String[] {
                            "xmlns", "http://www.opengis.net/kml/2.2",
                            "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
                            "xsi:schemaLocation", "http://www.opengis.net/kml/2.2 http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd"
                    }));
            
            WMSMapContext mapContext = (WMSMapContext) o;
            GetMapRequest request = mapContext.getRequest();
            MapLayer[] layers = mapContext.getLayers();
            
            //calculate scale denominator
            scaleDenominator = 1; 
            try {
               scaleDenominator = 
                       RendererUtilities.calculateScale(mapContext.getAreaOfInterest(), mapContext.getMapWidth(), mapContext.getMapHeight(), null);
            } 
            catch( Exception e ) {
               LOGGER.log(Level.WARNING, "Error calculating scale denominator", e);
            }
            LOGGER.log(Level.FINE, "scale denominator = " + scaleDenominator);

            //if we have more than one layer ( or a legend was requested ),
            //use the name "GeoServer" to group them
            boolean group = (layers.length > 1) || request.getLegend();

            if (group) {
                StringBuffer sb = new StringBuffer();
                for ( int i = 0; i < layers.length; i++ ) {
                    sb.append( layers[i].getTitle() + "," );
                }
                sb.setLength(sb.length()-1);
               
                start("Document");
                //element("name", sb.toString() );
                String kmltitle = (String) mapContext.getRequest().getFormatOptions().get("kmltitle");
                element("name", (kmltitle != null ? kmltitle : sb.toString()));
            }

            //for every layer specified in the request
            for (int i = 0; i < layers.length; i++) {
                //layer and info
                MapLayer layer = layers[i];
                MapLayerInfo layerInfo = mapContext.getRequest().getLayers().get(i);

                //was a super overlay requested?
                Boolean superoverlay = (Boolean)mapContext.getRequest().getFormatOptions().get("superoverlay");
                superoverlay = (superoverlay == null ? Boolean.FALSE : superoverlay);
                if (superoverlay) {
                    //encode as super overlay
                    encodeSuperOverlayLayer(mapContext, layer, group);
                } else {
                    //figure out which type of layer this is, raster or vector
                    if (layerInfo.getType() != MapLayerInfo.TYPE_RASTER) {
                        //vector 
                        encodeVectorLayer(mapContext, layer, group);
                    } else {
                        //encode as normal ground overlay
                        encodeRasterLayer(mapContext, layer, group);
                    }
                }
            }

            //legend suppoer
            if (request.getLegend()) {
                //for every layer specified in the request
                for (int i = 0; i < layers.length; i++) {
                    //layer and info
                    MapLayer layer = layers[i];
                    encodeLegend(mapContext, layer, group);
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
        @SuppressWarnings("unchecked")
        protected void encodeVectorLayer(WMSMapContext mapContext, MapLayer layer, boolean group) {
            //get the data
            SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
            SimpleFeatureCollection features = null;

            try {
                features = KMLUtils.loadFeatureCollection(featureSource, layer, mapContext, wms, scaleDenominator);
                if(features == null) {
                    // it means no features need to be depicted with this style/scale denominator
                    return;
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //was kmz requested?
            if (kmz) {
                //calculate kmscore to determine if we shoud write as vectors
                // or pre-render
                int kmscore = wms.getKmScore();
                Object kmScoreObj = mapContext.getRequest().getFormatOptions().get("kmscore");
                if(kmScoreObj != null) {
                    kmscore = (Integer) kmScoreObj;
                }
                boolean useVector = useVectorOutput(kmscore, features.size());

                if (useVector) {
                    //encode
                    KMLVectorTransformer tx = createVectorTransformer(mapContext, layer);
                    initTransformer(tx, group);
                    tx.setScaleDenominator(scaleDenominator);
                    tx.createTranslator(contentHandler).encode(features);
                } else {
                    KMLRasterTransformer tx = createRasterTransfomer(mapContext);
                    initTransformer(tx, group);
                    
                    //set inline to true to have the transformer reference images
                    // inline in the zip file
                    tx.setInline(true);
                    tx.createTranslator(contentHandler).encode(layer);
                }
            } else {
                //kmz not selected, just do straight vector
                KMLVectorTransformer tx = createVectorTransformer(mapContext, layer);
                initTransformer(tx, group);
                tx.setScaleDenominator(scaleDenominator);
                tx.createTranslator(contentHandler).encode(features);
            }
        }

        /**
         * Factory method, allows subclasses to inject their own version of the raster transfomer
         * @param mapContext
         * @return
         */
        protected KMLRasterTransformer createRasterTransfomer(WMSMapContext mapContext) {
            return new KMLRasterTransformer(wms, mapContext);
        }

        /**
         * Factory method, allows subclasses to inject their own version of the vector transfomer
         * @param mapContext
         * @return
         */
        protected KMLVectorTransformer createVectorTransformer(WMSMapContext mapContext,
                MapLayer layer) {
            return new KMLVectorTransformer(wms, mapContext, layer);
        }

        /**
         * Encodes a raster layer as kml.
         */
        protected void encodeRasterLayer(WMSMapContext mapContext, MapLayer layer, boolean group) {
            KMLRasterTransformer tx = createRasterTransfomer(mapContext);
            initTransformer(tx, group);
            
            tx.setInline(kmz);
            tx.createTranslator(contentHandler).encode(layer);
        }

        /**
         * Encodes a layer as a super overlay.
         */
        protected void encodeSuperOverlayLayer(WMSMapContext mapContext, MapLayer layer, boolean group) {
            KMLSuperOverlayTransformer tx = new KMLSuperOverlayTransformer(wms, mapContext);
            initTransformer(tx, group);
            tx.createTranslator(contentHandler).encode(layer);
        }

        /**
         * Encodes the legend for a maper layer as a scree overlay.
         */
        protected void encodeLegend(WMSMapContext mapContext, MapLayer layer, boolean group) {
            KMLLegendTransformer tx = new KMLLegendTransformer(mapContext);
            initTransformer(tx, group);
            tx.createTranslator(contentHandler).encode(layer);
        }
        
        protected void initTransformer(KMLTransformerBase delegate, boolean group) {
            delegate.setIndentation( getIndentation() );
            delegate.setEncoding(getEncoding());
            delegate.setStandAlone(!group);
        }

        double computeScaleDenominator(MapLayer layer, WMSMapContext mapContext) {
            Rectangle paintArea = new Rectangle(mapContext.getMapWidth(), mapContext.getMapHeight());
            AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(mapContext
                    .getAreaOfInterest(), paintArea);

            try {
                //90 = OGC standard DPI (see SLD spec page 37)
                return RendererUtilities.calculateScale(mapContext.getAreaOfInterest(),
                    mapContext.getCoordinateReferenceSystem(), paintArea.width, paintArea.height, 90);
            } catch (Exception e) {
                //probably either (1) no CRS (2) error xforming, revert to
                // old method - the best we can do (DJB)
                return 1 / worldToScreen.getScaleX();
            }
        }

        /**
         * Determines whether to return a vector (KML) result of the data or to
         * return an image instead.
         * If the kmscore is 100, then the output should always be vector. If
         * the kmscore is 0, it should always be raster. In between, the number of
         * features is weighed against the kmscore value.
         * kmscore determines whether to return the features as vectors, or as one
         * raster image. It is the point, determined by the user, where X number of
         * features is "too many" and the result should be returned as an image instead.
         *
         * kmscore is logarithmic. The higher the value, the more features it takes
         * to make the algorithm return an image. The lower the kmscore, the fewer
         * features it takes to force an image to be returned.
         * (in use, the formula is exponential: as you increase the KMScore value,
         * the number of features required increases exponentially).
         *
         * @param kmscore the score, between 0 and 100, use to determine what output to use
         * @param numFeatures how many features are being rendered
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
            // The most useful kmscore values are between 20 and 70 (21 and 46000 features respectively)
            // A good default kmscore value is around 40 (464 features)
            double magic = Math.pow(10, kmscore / 15);

            if (numFeatures > magic) {
                return false; // return raster
            } else {
                return true; // return vector
            }
        }
    }
}
