/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.builder;

import de.micromata.opengis.kml.v_2_2_0.AbstractLatLonBox;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.LatLonAltBox;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.Lod;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Region;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.kml.KMLMapOutputFormat;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.NetworkLinkMapOutputFormat;
import org.geoserver.kml.decorator.LookAtDecoratorFactory;
import org.geoserver.kml.regionate.Tile;
import org.geoserver.kml.utils.KMLFeatureAccessor;
import org.geoserver.kml.utils.LookAtOptions;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Builds a KML document with a superoverlay hierarchy for each layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SuperOverlayNetworkLinkBuilder extends AbstractNetworkLinkBuilder {

    private GetMapRequest request;

    private WMSMapContent mapContent;

    private WMS wms;

    static final String VISIBLE_KEY = "kmlvisible";

    public SuperOverlayNetworkLinkBuilder(KmlEncodingContext context) {
        super(context);
        this.request = context.getRequest();
        this.mapContent = context.getMapContent();
        this.wms = context.getWms();
    }

    @Override
    void encodeDocumentContents(Document container) {
        boolean cachedMode = "cached".equals(context.getSuperOverlayMode());

        // normalize the requested bounds to match a WGS84 hierarchy tile
        Tile tile = new Tile(new ReferencedEnvelope(request.getBbox(), Tile.WGS84));
        while (tile.getZ() > 0 && !tile.getEnvelope().contains(request.getBbox())) {
            tile = tile.getParent();
        }
        Envelope normalizedEnvelope = null;
        if (tile.getZ() >= 0 && tile.getEnvelope().contains(request.getBbox())) {
            normalizedEnvelope = tile.getEnvelope();
        } else {
            normalizedEnvelope = KmlEncodingContext.WORLD_BOUNDS_WGS84;
        }
        int zoomLevel = (int) tile.getZ();
        // encode top level region, which is always visible
        addRegion(container, normalizedEnvelope, Integer.MAX_VALUE, -1);

        List<MapLayerInfo> layers = request.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayerInfo layer = layers.get(i);
            if (cachedMode && isRequestGWCCompatible(request, i, wms)) {
                encodeGWCLink(container, request, layer);
            } else {
                encodeLayerSuperOverlay(container, layer, i, normalizedEnvelope, zoomLevel);
            }
        }
    }

    public void encodeGWCLink(Document container, GetMapRequest request, MapLayerInfo layer) {
        NetworkLink nl = container.createAndAddNetworkLink();
        String prefixedName = layer.getResource().prefixedName();
        nl.setName("GWC-" + prefixedName);
        Link link = nl.createAndSetLink();
        String type = layer.getType() == MapLayerInfo.TYPE_RASTER ? "png" : "kml";
        String url =
                ResponseUtils.buildURL(
                        request.getBaseUrl(),
                        "gwc/service/kml/" + prefixedName + "." + type + ".kml",
                        null,
                        URLType.SERVICE);
        link.setHref(url);
        link.setViewRefreshMode(ViewRefreshMode.NEVER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void encodeLayerSuperOverlay(
            Document container,
            MapLayerInfo layerInfo,
            int layerIndex,
            Envelope bounds,
            int zoomLevel) {
        Map formatOptions = request.getFormatOptions();

        Layer layer = mapContent.layers().get(layerIndex);
        Folder folder = container.createAndAddFolder();
        folder.setName(layerInfo.getLabel());
        if (layerInfo.getDescription() != null && layerInfo.getDescription().length() > 0) {
            folder.setDescription(layerInfo.getDescription());
        }

        // Allow for all layers to be disabled by default.  This can be advantageous with
        // multiple large data-sets.
        if (formatOptions.get(VISIBLE_KEY) != null) {
            boolean visible = Boolean.parseBoolean(formatOptions.get(VISIBLE_KEY).toString());
            folder.setVisibility(visible);
        } else {
            folder.setVisibility(true);
        }

        LookAtOptions lookAtOptions = new LookAtOptions(formatOptions);
        if (bounds != null) {
            LookAtDecoratorFactory lookAtFactory = new LookAtDecoratorFactory();
            ReferencedEnvelope layerBounds = layer.getBounds();
            CoordinateReferenceSystem layerCRS = layerBounds.getCoordinateReferenceSystem();
            if (layerCRS != null
                    && !CRS.equalsIgnoreMetadata(layerCRS, DefaultGeographicCRS.WGS84)) {
                try {
                    layerBounds = layerBounds.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw new ServiceException(
                            "Failed to transform the layer bounds for "
                                    + layer.getTitle()
                                    + " to WGS84",
                            e);
                }
            }
            LookAt la = lookAtFactory.buildLookAt(layerBounds, lookAtOptions, false);
            folder.setAbstractView(la);

            encodeNetworkLinks(folder, layer, bounds, zoomLevel);
        }
    }

    /** Encode the network links for the specified envelope and zoom level */
    void encodeNetworkLinks(Folder folder, Layer layer, Envelope top, int zoomLevel) {
        // encode the network links
        if (top != KmlEncodingContext.WORLD_BOUNDS_WGS84) {
            // top left
            Envelope e00 =
                    new Envelope(
                            top.getMinX(),
                            top.getMinX() + (top.getWidth() / 2d),
                            top.getMaxY() - (top.getHeight() / 2d),
                            top.getMaxY());

            // top right
            Envelope e01 = new Envelope(e00.getMaxX(), top.getMaxX(), e00.getMinY(), e00.getMaxY());

            // bottom left
            Envelope e10 = new Envelope(e00.getMinX(), e00.getMaxX(), top.getMinY(), e00.getMinY());

            // bottom right
            Envelope e11 = new Envelope(e01.getMinX(), e01.getMaxX(), e10.getMinY(), e10.getMaxY());

            addNetworkLink(folder, e00, "00", layer);
            addNetworkLink(folder, e01, "01", layer);
            addNetworkLink(folder, e10, "10", layer);
            addNetworkLink(folder, e11, "11", layer);
        } else {
            // divide up horizontally by two
            Envelope e0 =
                    new Envelope(
                            top.getMinX(),
                            top.getMinX() + (top.getWidth() / 2d),
                            top.getMinY(),
                            top.getMaxY());
            Envelope e1 = new Envelope(e0.getMaxX(), top.getMaxX(), top.getMinY(), top.getMaxY());

            addNetworkLink(folder, e0, "0", layer);
            addNetworkLink(folder, e1, "1", layer);
        }

        // encode the ground overlay(s)
        if (top == KmlEncodingContext.WORLD_BOUNDS_WGS84) {
            // special case for top since it does not line up as a proper
            // tile -> split it in two
            encodeTileContents(
                    folder, layer, "contents-0", zoomLevel, new Envelope(-180, 0, -90, 90));
            encodeTileContents(
                    folder, layer, "contents-1", zoomLevel, new Envelope(0, 180, -90, 90));
        } else {
            // encode straight up
            encodeTileContents(folder, layer, "contents", zoomLevel, top);
        }
    }

    void addRegion(Feature container, Envelope box, int minLodPixels, int maxLodPixels) {
        Region region = container.createAndSetRegion();
        Lod lod = region.createAndSetLod();
        lod.setMinLodPixels(minLodPixels);
        lod.setMaxLodPixels(maxLodPixels);
        LatLonAltBox llaBox = region.createAndSetLatLonAltBox();
        setEnvelope(box, llaBox);
    }

    private void setEnvelope(Envelope box, AbstractLatLonBox llBox) {
        llBox.setNorth(box.getMaxY());
        llBox.setSouth(box.getMinY());
        llBox.setEast(box.getMaxX());
        llBox.setWest(box.getMinX());
    }

    void addNetworkLink(Folder container, Envelope box, String name, Layer layer) {
        // check if we are going to get any feature from this layer
        String overlayMode = context.getSuperOverlayMode();
        if (!"raster".equals(overlayMode)
                && layer instanceof FeatureLayer
                && !shouldDrawVectorLayer(layer, box)) {
            return;
        }

        NetworkLink nl = container.createAndAddNetworkLink();
        nl.setName(name);
        addRegion(nl, box, 128, -1);
        Link link = nl.createAndSetLink();
        String getMap =
                WMSRequests.getGetMapUrl(
                        request,
                        layer,
                        0,
                        box,
                        new String[] {
                            "format",
                            KMLMapOutputFormat.MIME_TYPE,
                            "width",
                            "256",
                            "height",
                            "256",
                            "format",
                            NetworkLinkMapOutputFormat.KML_MIME_TYPE
                        });
        link.setHref(getMap);
        LOGGER.fine("Network link " + name + ":" + getMap);
        link.setViewRefreshMode(ViewRefreshMode.ON_REGION);
    }

    void encodeTileContents(
            Folder container, Layer layer, String name, int drawOrder, Envelope box) {
        try {
            if (shouldDrawVectorLayer(layer, box))
                encodeKMLLink(container, layer, name, drawOrder, box);
            if (shouldDrawWMSOverlay(layer, box))
                encodeGroundOverlay(container, layer, drawOrder, box);
        } catch (HttpErrorCodeException e) {
            // no contents, ok
        }
    }

    private boolean shouldDrawVectorLayer(Layer layer, Envelope box) {
        try {
            // should draw as vector if the layer is a vector layer, and based on mode
            // full: yes, if any regionated vectors are present at this zoom level
            // hybrid: yes, if any regionated vectors are present at this zoom level
            // overview: is the non-regionated feature count for this tile below the cutoff?
            // raster: no
            if (!isVectorLayer(layer)) return false;

            String overlayMode = context.getSuperOverlayMode();
            if ("raster".equals(overlayMode)) return false;

            if ("overview".equals(overlayMode)) {
                int featureCount = featuresInTile(layer, box, false);
                return featureCount <= getRegionateFeatureLimit(getFeatureTypeInfo(layer));
            }

            int featureCount = featuresInTile(layer, box, true);
            return featureCount > 0;
        } catch (HttpErrorCodeException e) {
            // fine, it means there was no data.... sigh...
            return false;
        }
    }

    private int getRegionateFeatureLimit(FeatureTypeInfo ft) {
        Integer regionateFeatureLimit =
                ft.getMetadata().get("kml.regionateFeatureLimit", Integer.class);
        return regionateFeatureLimit != null ? regionateFeatureLimit : -1;
    }

    private boolean shouldDrawWMSOverlay(Layer layer, Envelope box) {
        // should draw based on the mode:
        // full: no
        // hybrid: yes
        // overview: is the non-regionated feature count for this tile above the cutoff?
        if (!isVectorLayer(layer)) return true;

        String overlayMode = context.getSuperOverlayMode();
        if ("hybrid".equals(overlayMode) || "raster".equals(overlayMode)) return true;
        if ("overview".equals(overlayMode))
            return featuresInTile(layer, box, false)
                    > getRegionateFeatureLimit(getFeatureTypeInfo(layer));

        return false;
    }

    @SuppressWarnings("rawtypes")
    void encodeKMLLink(Folder container, Layer layer, String name, int drawOrder, Envelope box) {
        // copy the format options
        CaseInsensitiveMap fo = new CaseInsensitiveMap(new HashMap());
        fo.putAll(mapContent.getRequest().getFormatOptions());

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
        NetworkLink nl = container.createAndAddNetworkLink();
        nl.setName(name);
        addRegion(nl, box, 128, -1);
        nl.setVisibility(true);
        Link link = nl.createAndSetLink();
        String url =
                WMSRequests.getGetMapUrl(
                        request,
                        layer,
                        0,
                        box,
                        new String[] {
                            "width",
                            "256",
                            "height",
                            "256",
                            "format_options",
                            foEncoded,
                            "superoverlay",
                            "true"
                        });
        link.setHref(url);
    }

    boolean isVectorLayer(Layer layer) {
        int index = mapContent.layers().indexOf(layer);
        MapLayerInfo info = mapContent.getRequest().getLayers().get(index);
        return (info.getType() == MapLayerInfo.TYPE_VECTOR
                || info.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR);
    }

    private FeatureTypeInfo getFeatureTypeInfo(Layer layer) {
        for (MapLayerInfo info : mapContent.getRequest().getLayers())
            if (info.getName().equals(layer.getTitle())) return info.getFeature();
        return null;
    }

    @SuppressWarnings("unchecked")
    private int featuresInTile(Layer layer, Envelope bounds, boolean regionate) {
        if (!isVectorLayer(layer)) return 1; // for coverages, we want raster tiles everywhere
        Envelope originalBounds = mapContent.getRequest().getBbox();
        mapContent.getRequest().setBbox(bounds);
        mapContent
                .getViewport()
                .setBounds(new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84));

        String originalRegionateBy = null;
        if (regionate) {
            originalRegionateBy =
                    (String) mapContent.getRequest().getFormatOptions().get("regionateby");
            if (originalRegionateBy == null)
                mapContent.getRequest().getFormatOptions().put("regionateby", "auto");
        }

        int numFeatures = 0;

        try {
            numFeatures = new KMLFeatureAccessor().getFeatureCount(layer, mapContent, wms, -1);
        } catch (ServiceException e) {
            LOGGER.severe("Caught the WmsException!");
            numFeatures = -1;
        } catch (HttpErrorCodeException e) {
            if (e.getErrorCode() == 204) {
                throw e;
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "Failure while checking whether a regionated child tile "
                                + "contained features!",
                        e);
            }
        } catch (Exception e) {
            // Probably just trying to regionate a raster layer...
            LOGGER.log(
                    Level.WARNING,
                    "Failure while checking whether a regionated child tile contained features!",
                    e);
        }

        mapContent.getRequest().setBbox(originalBounds);
        mapContent
                .getViewport()
                .setBounds(new ReferencedEnvelope(originalBounds, DefaultGeographicCRS.WGS84));
        if (regionate && originalRegionateBy == null) {
            mapContent.getRequest().getFormatOptions().remove("regionateby");
        }

        return numFeatures;
    }

    void encodeGroundOverlay(Folder container, Layer layer, int drawOrder, Envelope box) {
        GroundOverlay go = container.createAndAddGroundOverlay();
        go.setDrawOrder(drawOrder);
        Icon icon = go.createAndSetIcon();
        String href =
                WMSRequests.getGetMapUrl(
                        request,
                        layer,
                        0,
                        box,
                        new String[] {
                            "width",
                            "256",
                            "height",
                            "256",
                            "format",
                            "image/png",
                            "transparent",
                            "true"
                        });
        icon.setHref(href);
        LOGGER.fine(href);

        // make sure the ground overlay disappears as the lower tiles activate
        addRegion(go, box, 128, 512);

        LatLonBox llBox = go.createAndSetLatLonBox();
        setEnvelope(box, llBox);
    }

    /** Returns true if the request is GWC compatible */
    @SuppressWarnings("unchecked")
    private boolean isRequestGWCCompatible(GetMapRequest request, int layerIndex, WMS wms) {
        // check the kml params are the same as the defaults (GWC uses always the defaults)
        boolean requestKmAttr = context.isDescriptionEnabled();
        if (requestKmAttr != wms.getKmlKmAttr()) {
            return false;
        }

        boolean requestKmplacemark = context.isPlacemarkForced();
        if (requestKmplacemark != wms.getKmlPlacemark()) {
            return false;
        }

        int requestKmscore = context.getKmScore();
        if (requestKmscore != wms.getKmScore()) {
            return false;
        }

        // check the layer is local
        if (request.getLayers().get(layerIndex).getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
            return false;
        }

        // check the layer is using the default style
        Style requestedStyle = request.getStyles().get(layerIndex);
        Style defaultStyle = request.getLayers().get(layerIndex).getDefaultStyle();
        if (!defaultStyle.equals(requestedStyle)) {
            return false;
        }

        // check there is no extra filtering applied to the layer
        List<Filter> filters = request.getFilter();
        if (filters != null && filters.size() > 0 && filters.get(layerIndex) != Filter.INCLUDE) {
            return false;
        }

        // no extra sorts
        List<List<SortBy>> sortBy = request.getSortBy();
        if (sortBy != null && sortBy.size() > 0) {
            return false;
        }

        // no fiddling with antialiasing settings
        String antialias = (String) request.getFormatOptions().get("antialias");
        if (antialias != null && !"FULL".equalsIgnoreCase(antialias)) {
            return false;
        }

        // no custom palette
        if (request.getPalette() != null) {
            return false;
        }

        // no custom start index
        if (request.getStartIndex() != null && request.getStartIndex() != 0) {
            return false;
        }

        // no custom max features
        if (request.getMaxFeatures() != null) {
            return false;
        }

        // no sql view params
        if (request.getViewParams() != null && request.getViewParams().size() > 0) {
            return false;
        }

        // ok, it seems everything is the same as GWC cached it
        return true;
    }
}
