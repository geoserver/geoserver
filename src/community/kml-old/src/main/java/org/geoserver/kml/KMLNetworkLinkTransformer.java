/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Encodes a KML document contianing a network link.
 * <p>
 * This transformer transforms a {@link GetMapRequest} object.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * 
 */
public class KMLNetworkLinkTransformer extends TransformerBase {

    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    /**
     * flag controlling whether the network link should be a super overlay.
     */
    boolean encodeAsRegion = false;

    /**
     * flag controlling whether the network link should be a direct GWC one when possible
     */
    boolean cachedMode = false;

    private boolean standalone;

    /**
     * @see #setInline
     */
    private boolean inline;

    /**
     * The map context
     */
    private final WMSMapContent mapContent;
    
    private WMS wms;

    public KMLNetworkLinkTransformer(WMS wms, WMSMapContent mapContent) {
        this.wms = wms;
        this.mapContent = mapContent;
        standalone = true;
    }

    public void setStandalone(boolean standalone){
        this.standalone = standalone;
    }
    
    public boolean isStandalone(){
        return standalone;
    }

    /**
     * @return {@code true} if the document is to be generated inline (i.e. without an enclosing
     *         Folder element). Defaults to {@code false}
     */
    public boolean isInline() {
        return inline;
    }

    /**
     * @param inline if {@code true} network links won't be enclosed inside a Folder element
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public void setCachedMode(boolean cachedMode) {
        this.cachedMode = cachedMode;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLNetworkLinkTranslator(handler);
    }

    public void setEncodeAsRegion(boolean encodeAsRegion) {
        this.encodeAsRegion = encodeAsRegion;
    }

    class KMLNetworkLinkTranslator extends TranslatorSupport {

        public KMLNetworkLinkTranslator(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {
            final WMSMapContent context = (WMSMapContent) o;
            final GetMapRequest request = context.getRequest();
            // restore target mime type for the network links
            if (NetworkLinkMapOutputFormat.KML_MIME_TYPE.equals(request.getFormat())) {
                request.setFormat(KMLMapOutputFormat.MIME_TYPE);
            } else {
                request.setFormat(KMZMapOutputFormat.MIME_TYPE);
            }

            if(standalone){
                start("kml");
            }
            if (!inline) {
                start("Folder");
                if (standalone) {
                    String kmltitle = (String) mapContent.getRequest().getFormatOptions().get("kmltitle");
                    element("name", (kmltitle != null ? kmltitle : ""));
                }
            }
            final List<MapLayerInfo> layers = request.getLayers();

            final KMLLookAt lookAt = parseLookAtOptions(request);

            ReferencedEnvelope aggregatedBounds;
            List<ReferencedEnvelope> layerBounds;
            layerBounds = new ArrayList<ReferencedEnvelope>(layers.size());

            aggregatedBounds = computePerLayerQueryBounds(context, layerBounds, lookAt);

            if (encodeAsRegion) {
                encodeAsSuperOverlay(request, lookAt, layerBounds);
            } else {
                encodeAsOverlay(request, lookAt, layerBounds);
            }

            // look at
            encodeLookAt(aggregatedBounds, lookAt);

            if (!inline) {
                end("Folder");
            }
            if (standalone) {
                end("kml");
            }
        }

        /**
         * @return the aggregated bounds for all the requested layers, taking into account whether
         *         the whole layer or filtered bounds is used for each layer
         */
        private ReferencedEnvelope computePerLayerQueryBounds(final WMSMapContent context,
                final List<ReferencedEnvelope> target, final KMLLookAt lookAt) {

            // no need to compute queried bounds if request explicitly specified the view area
            final boolean computeQueryBounds = lookAt.getLookAt() == null;

            ReferencedEnvelope aggregatedBounds;
            try {
                boolean longitudeFirst = true;
                aggregatedBounds = new ReferencedEnvelope(CRS.decode("EPSG:4326", longitudeFirst));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            aggregatedBounds.setToNull();

            final List<Layer> mapLayers = context.layers();
            final List<MapLayerInfo> layerInfos = context.getRequest().getLayers();
            for (int i = 0; i < mapLayers.size(); i++) {
                final Layer Layer = mapLayers.get(i);
                final MapLayerInfo layerInfo = layerInfos.get(i);

                ReferencedEnvelope layerLatLongBbox;
                layerLatLongBbox = computeLayerBounds(Layer, layerInfo, computeQueryBounds);
                try {
                    layerLatLongBbox = layerLatLongBbox.transform(aggregatedBounds.getCoordinateReferenceSystem(), true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } 
                target.add(layerLatLongBbox);
                aggregatedBounds.expandToInclude(layerLatLongBbox);
            }
            return aggregatedBounds;
        }

        @SuppressWarnings("rawtypes")
        private ReferencedEnvelope computeLayerBounds(Layer layer, MapLayerInfo layerInfo,
                boolean computeQueryBounds) {

            final Query layerQuery = layer.getQuery();
            // make sure if layer is gonna be filtered, the resulting bounds are obtained instead of
            // the whole bounds
            final Filter filter = layerQuery.getFilter();
            if (layerQuery.getFilter() == null || Filter.INCLUDE.equals(filter)) {
                computeQueryBounds = false;
            }
            if (!computeQueryBounds && !layerQuery.isMaxFeaturesUnlimited()) {
                computeQueryBounds = true;
            }

            ReferencedEnvelope layerLatLongBbox = null;
            if (computeQueryBounds) {
                FeatureSource featureSource = layer.getFeatureSource();
                try {
                    CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
                    FeatureCollection features = featureSource.getFeatures(layerQuery);
                    layerLatLongBbox = features.getBounds();
                    layerLatLongBbox = layerLatLongBbox.transform(targetCRS, true);
                } catch (Exception e) {
                    LOGGER.info("Error computing bounds for " + featureSource.getName() + " with "
                            + layerQuery);
                }

            }
            if (layerLatLongBbox == null) {
                try {
                    layerLatLongBbox = layerInfo.getLatLongBoundingBox();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return layerLatLongBbox;
        }

        @SuppressWarnings("unchecked")
        private KMLLookAt parseLookAtOptions(final GetMapRequest request) {
            final KMLLookAt lookAt;
            if (request.getFormatOptions() == null) {
                // use a default LookAt properties
                lookAt = new KMLLookAt();
            } else {
                // use the requested LookAt properties
                Map<String, Object> formatOptions;
                formatOptions = new HashMap<String, Object>(request.getFormatOptions());
                lookAt = new KMLLookAt(formatOptions);
                /*
                 * remove LOOKATBBOX and LOOKATGEOM from format options so KMLUtils.getMapRequest
                 * does not include them in the network links, but do include the other options such
                 * as tilt, range, etc.
                 */
                request.getFormatOptions().remove("LOOKATBBOX");
                request.getFormatOptions().remove("LOOKATGEOM");
            }
            return lookAt;
        }

        protected void encodeAsSuperOverlay(GetMapRequest request, KMLLookAt lookAt,
                List<ReferencedEnvelope> layerBounds) {

            List<MapLayerInfo> layers = request.getLayers();
            List<Style> styles = request.getStyles();
            for (int i = 0; i < layers.size(); i++) {
                MapLayerInfo layer = layers.get(i);
                if ("cached".equals(KMLUtils.getSuperoverlayMode(request, wms))
                        && KMLUtils.isRequestGWCCompatible(request, i, wms)) {
                    encodeGWCLink(request, layer);
                } else {
                    String styleName = i < styles.size() ? styles.get(i).getName() : null;
                    ReferencedEnvelope bounds = layerBounds.get(i);
                    encodeLayerSuperOverlay(request, layer, styleName, i, bounds, lookAt);
                }
            }
        }

        public void encodeGWCLink(GetMapRequest request, MapLayerInfo layer) {
            start("NetworkLink");
            String prefixedName = layer.getResource().getPrefixedName();
            element("name", "GWC-" + prefixedName);

            start("Link");

            String type = layer.getType() == MapLayerInfo.TYPE_RASTER ? "png" : "kml";
            String url = ResponseUtils.buildURL(request.getBaseUrl(), "gwc/service/kml/" + 
                    prefixedName + "." + type + ".kml", null, URLType.SERVICE);
            element("href", url);
            element("viewRefreshMode", "never");

            end("Link");

            end("NetworkLink");
        }

        private void encodeLayerSuperOverlay(GetMapRequest request, MapLayerInfo layer,
                String styleName, int layerIndex, ReferencedEnvelope bounds, KMLLookAt lookAt) {
            start("NetworkLink");
            element("name", layer.getName());
            element("open", "1");
            element("visibility", "1");

            // look at for the network link for this single layer
            if (bounds != null) {
                encodeLookAt(bounds, lookAt);
            }

            // region
            start("Region");

            Envelope bbox = request.getBbox();
            start("LatLonAltBox");
            element("north", "" + bbox.getMaxY());
            element("south", "" + bbox.getMinY());
            element("east", "" + bbox.getMaxX());
            element("west", "" + bbox.getMinX());
            end("LatLonAltBox");

            start("Lod");
            element("minLodPixels", "128");
            element("maxLodPixels", "-1");
            end("Lod");

            end("Region");

            // link
            start("Link");

            String href = WMSRequests.getGetMapUrl(request, layer.getName(), layerIndex, styleName,
                    null, null);
            try {
                // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                // 6 doesn't like URL encoded parameters. See GEOS-4483
                href = URLDecoder.decode(href, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            start("href");
            cdata(href);
            end("href");

            // element( "viewRefreshMode", "onRegion" );
            end("Link");

            end("NetworkLink");
        }

        protected void encodeAsOverlay(GetMapRequest request, KMLLookAt lookAt,
                List<ReferencedEnvelope> layerBounds) {

            final List<MapLayerInfo> layers = request.getLayers();
            final List<Style> styles = request.getStyles();
            for (int i = 0; i < layers.size(); i++) {
                MapLayerInfo layerInfo = layers.get(i);
                start("NetworkLink");
                element("name", layerInfo.getName());
                element("visibility", "1");
                element("open", "1");

                // look at for the network link for this single layer
                ReferencedEnvelope latLongBoundingBox = layerBounds.get(i);
                if (latLongBoundingBox != null) {
                    encodeLookAt(latLongBoundingBox, lookAt);
                }

                start("Url");

                // set bbox to null so its not included in the request, google
                // earth will append it for us
                request.setBbox(null);

                String style = i < styles.size() ? styles.get(i).getName() : null;
                String href = WMSRequests.getGetMapUrl(request, layers.get(i).getName(), i, style,
                        null, null);
                try {
                    // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                    // 6 doesn't like URL encoded parameters. See GEOS-4483
                    href = URLDecoder.decode(href, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                start("href");
                cdata(href);
                end("href");

                element("viewRefreshMode", "onStop");
                element("viewRefreshTime", "1");
                end("Url");

                end("NetworkLink");
            }
        }

        private void encodeLookAt(Envelope bounds, KMLLookAt lookAt) {

            Envelope lookAtEnvelope = null;
            if (lookAt.getLookAt() == null) {
                lookAtEnvelope = bounds;
            }

            KMLLookAtTransformer tr;
            tr = new KMLLookAtTransformer(lookAtEnvelope, getIndentation(), getEncoding());
            Translator translator = tr.createTranslator(contentHandler);
            translator.encode(lookAt);
        }

    }
}
