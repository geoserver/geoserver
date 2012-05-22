/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSRequests;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;

public class KMLSuperOverlayTransformer extends KMLTransformerBase {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    /**
     * The map context
     */
    private final WMSMapContext mapContext;

    private final WMS wms;

    public KMLSuperOverlayTransformer(WMS wms, WMSMapContext mapContext) {
        this.wms = wms;
        this.mapContext = mapContext;
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLSuperOverlayerTranslator(handler);
    }

    class KMLSuperOverlayerTranslator extends KMLTranslatorSupport {
        public KMLSuperOverlayerTranslator(ContentHandler contentHandler) {
            super(contentHandler);
        }

        public void encode(Object o) throws IllegalArgumentException {
            MapLayer mapLayer = (MapLayer) o;

            // calculate closest resolution
            ReferencedEnvelope extent = mapContext.getAreaOfInterest();

            // zoom out until the entire bounds requested is covered by a
            // single tile
            Envelope top = KMLUtils.expandToTile(extent);
            int zoomLevel = KMLUtils.findZoomLevel(extent);

            LOGGER.fine("request = " + extent);
            LOGGER.fine("top level = " + top);

            // start document
            if (isStandAlone()) {
                start("kml");
            }

            start("Document");
            if (isStandAlone()) {
                String kmltitle = (String) mapContext.getRequest().getFormatOptions().get("kmltitle");
                element("name", (kmltitle != null && mapContext.layers().size() <= 1 ? kmltitle : mapLayer.getTitle()));
            }

            if ("cached".equals(KMLUtils.getSuperoverlayMode(mapContext.getRequest(), wms))) {
                if (KMLUtils.isRequestGWCCompatible(mapContext, mapLayer, wms)) {
                    encodeGWCLink(mapLayer);
                } else {
                    LOGGER.log(
                            Level.INFO,
                            "Could not use cached mode for this request as the KML "
                                    + "parameters do not match the server defaults. Falling back to 'auto' mode");
                    mapContext.getRequest().getFormatOptions().put("overlayMode", "auto");
                    encodeNetworkLinks(mapLayer, top, zoomLevel);
                }
            } else {
                encodeNetworkLinks(mapLayer, top, zoomLevel);
            }

            // end document
            end("Document");

            if (isStandAlone()) {
                end("kml");
            }
        }

        /**
         * Encode the network links for the specified envelope and zoom level
         * 
         * @param mapLayer
         * @param top
         * @param zoomLevel
         */
        void encodeNetworkLinks(MapLayer mapLayer, Envelope top, int zoomLevel) {
            // encode top level region
            encodeRegion(top, 256, -1);

            // encode the network links
            if (top != KMLUtils.WORLD_BOUNDS_WGS84) {
                // top left
                Envelope e00 = new Envelope(top.getMinX(), top.getMinX() + (top.getWidth() / 2d),
                        top.getMaxY() - (top.getHeight() / 2d), top.getMaxY());

                // top right
                Envelope e01 = new Envelope(e00.getMaxX(), top.getMaxX(), e00.getMinY(),
                        e00.getMaxY());

                // bottom left
                Envelope e10 = new Envelope(e00.getMinX(), e00.getMaxX(), top.getMinY(),
                        e00.getMinY());

                // bottom right
                Envelope e11 = new Envelope(e01.getMinX(), e01.getMaxX(), e10.getMinY(),
                        e10.getMaxY());

                encodeNetworkLink(e00, "00", mapLayer);
                encodeNetworkLink(e01, "01", mapLayer);
                encodeNetworkLink(e10, "10", mapLayer);
                encodeNetworkLink(e11, "11", mapLayer);
            } else {
                // divide up horizontally by two
                Envelope e0 = new Envelope(top.getMinX(), top.getMinX() + (top.getWidth() / 2d),
                        top.getMinY(), top.getMaxY());
                Envelope e1 = new Envelope(e0.getMaxX(), top.getMaxX(), top.getMinY(),
                        top.getMaxY());

                encodeNetworkLink(e0, "0", mapLayer);
                encodeNetworkLink(e1, "1", mapLayer);
            }

            // encode the ground overlay(s)
            if (top == KMLUtils.WORLD_BOUNDS_WGS84) {
                // special case for top since it does not line up as a proper
                // tile -> split it in two
                encodeTileForViewing(mapLayer, zoomLevel, new Envelope(-180, 0, -90, 90));
                encodeTileForViewing(mapLayer, zoomLevel, new Envelope(0, 180, -90, 90));
            } else {
                // encode straight up
                encodeTileForViewing(mapLayer, zoomLevel, top);
            }
        }

        public void encodeGWCLink(MapLayer mapLayer) {
            start("NetworkLink");
            element("name", "GWC-" + mapLayer.getTitle());

            start("Link");

            SimpleFeatureType ft = (SimpleFeatureType) mapLayer.getFeatureSource().getSchema();
            String type = "kml";
            if (FeatureUtilities.isWrappedCoverage(ft)
                    || FeatureUtilities.isWrappedCoverageReader(ft)) {
                type = "png";
            }
            String url = ResponseUtils.buildURL(mapContext.getRequest().getBaseUrl(), 
                    "gwc/service/kml/" + mapLayer.getTitle() + "." + type + ".kml", 
                    null, URLType.SERVICE);
            element("href", url);
            element("viewRefreshMode", "never");

            end("Link");

            end("NetworkLink");
        }

        void encodeTileForViewing(MapLayer mapLayer, int drawOrder, Envelope box) {
            if (shouldDrawVectorLayer(mapLayer, box))
                encodeKMLLink(mapLayer, drawOrder, box);
            if (shouldDrawWMSOverlay(mapLayer, box))
                encodeGroundOverlay(mapLayer, drawOrder, box);
        }

        private boolean shouldDrawVectorLayer(MapLayer layer, Envelope box) {
            // should draw as vector if the layer is a vector layer, and based on mode
            // full: yes, if any regionated vectors are present at this zoom level
            // hybrid: yes, if any regionated vectors are present at this zoom level
            // overview: is the non-regionated feature count for this tile below the cutoff?
            // raster: no
            if (!isVectorLayer(layer))
                return false;

            String overlayMode = KMLUtils.getSuperoverlayMode(mapContext.getRequest(), wms);

            if ("raster".equals(overlayMode))
                return false;

            if ("overview".equals(overlayMode)) {
                // the sixteen here is mostly arbitrary, designed to indicate a couple of regionated
                // levels above the bottom of the hierarchy
                return featuresInTile(layer, box, false) <= getRegionateFeatureLimit(getFeatureTypeInfo(layer));
            }

            return featuresInTile(layer, box, true) > 0;
        }

        private int getRegionateFeatureLimit(FeatureTypeInfo ft) {
            Integer regionateFeatureLimit = ft.getMetadata().get("kml.regionateFeatureLimit",
                    Integer.class);
            return regionateFeatureLimit != null ? regionateFeatureLimit : -1;
        }

        private boolean shouldDrawWMSOverlay(MapLayer layer, Envelope box) {
            // should draw based on the mode:
            // full: no
            // hybrid: yes
            // overview: is the non-regionated feature count for this tile above the cutoff?
            if (!isVectorLayer(layer))
                return true;

            String overlayMode = KMLUtils.getSuperoverlayMode(mapContext.getRequest(), wms);
            if ("hybrid".equals(overlayMode) || "raster".equals(overlayMode))
                return true;
            if ("overview".equals(overlayMode))
                return featuresInTile(layer, box, false) > getRegionateFeatureLimit(getFeatureTypeInfo(layer));

            return false;
        }

        void encodeKMLLink(MapLayer mapLayer, int drawOrder, Envelope box) {
            // copy the format options
            CaseInsensitiveMap fo = new CaseInsensitiveMap(new HashMap());
            fo.putAll(mapContext.getRequest().getFormatOptions());

            // we want to pass through format options except for superoverlay, we need to
            // turn it off so we get actual placemarks back, and not more links
            fo.remove("superoverlay");

            // get the regionate mode
            String overlayMode = (String) fo.get("overlayMode");

            if ("overview".equalsIgnoreCase(overlayMode)) {
                // overview mode, turn off regionation
                fo.remove("regionateBy");
            } else {
                // specify regionateBy=auto if not specified
                if (!fo.containsKey("regionateBy")) {
                    fo.put("regionateBy", "auto");
                }

            }

            String foEncoded = WMSRequests.encodeFormatOptions(fo);

            // encode the link
            start("NetworkLink");
            element("visibility", "1");
            start("Link");

            element("href", KMLUtils.getMapUrl(mapContext, mapLayer, 0, box, new String[] {
                    "width", "256", "height", "256", "format_options", foEncoded, "superoverlay",
                    "false" }, true, wms.getGeoServer()));

            end("Link");
            encodeRegion(box, 128, -1);
            end("NetworkLink");
        }

        boolean isVectorLayer(MapLayer layer) {
            int index = Arrays.asList(mapContext.getLayers()).indexOf(layer);
            MapLayerInfo info = mapContext.getRequest().getLayers().get(index);
            return (info.getType() == MapLayerInfo.TYPE_VECTOR || info.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR);
        }

        private FeatureTypeInfo getFeatureTypeInfo(MapLayer layer) {
            for (MapLayerInfo info : mapContext.getRequest().getLayers())
                if (info.getName().equals(layer.getTitle()))
                    return info.getFeature();
            return null;
        }

        private int featuresInTile(MapLayer mapLayer, Envelope bounds, boolean regionate) {
            if (!isVectorLayer(mapLayer))
                return 1; // for coverages, we want raster tiles everywhere
            Envelope originalBounds = mapContext.getRequest().getBbox();
            mapContext.getRequest().setBbox(bounds);
            mapContext
                    .setAreaOfInterest(new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84));

            String originalRegionateBy = null;
            if (regionate) {
                originalRegionateBy = (String) mapContext.getRequest().getFormatOptions()
                        .get("regionateby");
                if (originalRegionateBy == null)
                    mapContext.getRequest().getFormatOptions().put("regionateby", "auto");
            }

            int numFeatures = 0;

            try {
                final SimpleFeatureCollection fc = KMLUtils.loadFeatureCollection(
                        (SimpleFeatureSource) mapLayer.getFeatureSource(), mapLayer, mapContext, wms, -1);
                numFeatures = fc == null ? 0 : fc.size();
            } catch (ServiceException e) {
                LOGGER.severe("Caught the WmsException!");
                numFeatures = -1;
            } catch (HttpErrorCodeException e) {
                if (e.getErrorCode() == 204) {
                    throw e;
                } else {
                    LOGGER.log(Level.WARNING,
                            "Failure while checking whether a regionated child tile "
                                    + "contained features!", e);
                }
            } catch (Exception e) {
                // Probably just trying to regionate a raster layer...
                LOGGER.log(
                        Level.WARNING,
                        "Failure while checking whether a regionated child tile contained features!",
                        e);
            }

            mapContext.getRequest().setBbox(originalBounds);
            mapContext.setAreaOfInterest(new ReferencedEnvelope(originalBounds,
                    DefaultGeographicCRS.WGS84));
            if (regionate && originalRegionateBy == null) {
                mapContext.getRequest().getFormatOptions().remove("regionateby");
            }

            return numFeatures;
        }

        void encodeGroundOverlay(MapLayer mapLayer, int drawOrder, Envelope box) {
            start("GroundOverlay");
            element("drawOrder", "" + drawOrder);

            start("Icon");

            String href = KMLUtils.getMapUrl(mapContext, mapLayer, 0, box, new String[] { "width",
                    "256", "height", "256", "format", "image/png", "transparent", "true" }, true, wms.getGeoServer());
            element("href", href);
            LOGGER.fine(href);
            end("Icon");

            // make it so that for coverages the lower zoom levels remain active as one zooms in for
            // a longer
            // amount of time
            SimpleFeatureType layerFeatureType = (SimpleFeatureType) mapLayer.getFeatureSource()
                    .getSchema();
            if (FeatureUtilities.isWrappedCoverage(layerFeatureType)
                    || FeatureUtilities.isWrappedCoverageReader(layerFeatureType))
                encodeRegion(box, 128, 2048);
            else
                encodeRegion(box, 128, 512);

            encodeLatLonBox(box);
            end("GroundOverlay");
        }

        void encodeRegion(Envelope box, int minLodPixels, int maxLodPixels) {
            // top level region
            start("Region");

            start("Lod");
            element("minLodPixels", "" + minLodPixels);
            element("maxLodPixels", "" + maxLodPixels);
            end("Lod");

            encodeLatLonAltBox(box);

            end("Region");
        }

        void encodeNetworkLink(Envelope box, String name, MapLayer mapLayer) {
            start("NetworkLink");
            element("name", name);

            encodeRegion(box, 128, -1);

            start("Link");

            String getMap = KMLUtils.getMapUrl(mapContext, mapLayer, 0, box, new String[] {
                    "format", KMLMapOutputFormat.MIME_TYPE, "width", "256", "height", "256",
                    "superoverlay", "true" }, false, wms.getGeoServer());

            element("href", getMap);
            LOGGER.fine("Network link " + name + ":" + getMap);

            element("viewRefreshMode", "onRegion");

            end("Link");

            end("NetworkLink");
        }

        void encodeLatLonAltBox(Envelope box) {
            start("LatLonAltBox");
            encodeBox(box);
            end("LatLonAltBox");
        }

        void encodeLatLonBox(Envelope box) {
            start("LatLonBox");
            encodeBox(box);
            end("LatLonBox");
        }

        void encodeBox(Envelope box) {
            element("north", String.valueOf(box.getMaxY()));
            element("south", String.valueOf(box.getMinY()));
            element("east", String.valueOf(box.getMaxX()));
            element("west", String.valueOf(box.getMinX()));
        }
    }

}
