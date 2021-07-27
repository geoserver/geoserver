/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.DATE_FORMAT;
import static org.geoserver.mapml.MapMLConstants.MAPML_MIME_TYPE;
import static org.geoserver.mapml.MapMLController.previewTcrsMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.xml.AxisType;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Datalist;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Input;
import org.geoserver.mapml.xml.InputRelType;
import org.geoserver.mapml.xml.InputType;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.mapml.xml.Option;
import org.geoserver.mapml.xml.PositionType;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.mapml.xml.Select;
import org.geoserver.mapml.xml.UnitType;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.GridSubset;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a method object, meant to simplify {@link org.geoserver.mapml.MapMLController#mapML()
 * mapML}
 *
 * @author prushforth
 */
public class MapMLDocumentBuilder {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.mapml");
    @Autowired GWC GWC;

    private final MapMLController controller;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final String layer;
    private final String proj;
    private final Optional<String> style;
    private final Optional<Boolean> transparent;
    private final Optional<String> format;

    private final GeoServer geoServer;
    private final GWC gwc = GWC.get();

    private ReferencedEnvelope bbox;
    private MetadataMap layerMeta;
    private boolean useTiles;
    private Boolean enableSharding;
    private String[] shardArray;
    private List<Object> extentList;
    private boolean tileLayerExists;
    private boolean isLayerGroup;
    private LayerGroupInfo layerGroupInfo;
    private LayerInfo layerInfo;
    private ResourceInfo resourceInfo = null;
    private Input zoomInput;
    private ProjType projType;
    private boolean queryable = false;
    private String workspace = "";
    private String layerName = "";
    private String styleName;
    private String imageFormat;
    private String baseUrl;
    private String baseUrlPattern;
    private boolean timeEnabled = false;
    private boolean elevationEnabled = false;
    private boolean isTransparent = true;

    private Mapml mapml;
    /**
     * @param controller
     * @param request
     * @param response
     * @param layer
     * @param proj
     * @param style
     * @param transparent
     * @param format
     */
    public MapMLDocumentBuilder(
            MapMLController controller,
            HttpServletRequest request,
            HttpServletResponse response,
            String layer,
            String proj,
            Optional<String> style,
            Optional<Boolean> transparent,
            Optional<String> format) {
        this.controller = controller;
        this.request = request;
        this.response = response;
        this.layer = layer;
        this.proj = proj;
        this.style = style;
        this.transparent = transparent;
        this.format = format;
        this.geoServer = controller.geoServer;
    }
    /**
     * Initialize fields, generate and return MapML document
     *
     * @return
     */
    public Mapml getMapMLDocument() {
        try {
            initialize();
        } catch (RuntimeException re) {
            LOGGER.log(Level.INFO, re.getMessage());
            return null;
        }
        prepareDocument();
        return this.mapml;
    }
    /** Init all the private fields used while generating MapML document */
    private void initialize() {
        layerInfo = geoServer.getCatalog().getLayerByName(this.layer);
        bbox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        this.isLayerGroup = (this.layerInfo == null);
        if (isLayerGroup) {
            layerGroupInfo = geoServer.getCatalog().getLayerGroupByName(layer);
            if (layerGroupInfo == null) {
                try {
                    response.sendError(
                            HttpServletResponse.SC_NOT_FOUND,
                            "Invalid layer or layer group name: " + layer);
                    throw new RuntimeException("Invalid layer or layer group name: " + layer);
                } catch (IOException ioe) {
                }
                return;
            }
            for (LayerInfo li : layerGroupInfo.layers()) {
                bbox.expandToInclude(li.getResource().getLatLonBoundingBox());
            }
            layerMeta = layerGroupInfo.getMetadata();
            workspace =
                    (layerGroupInfo.getWorkspace() != null
                            ? layerGroupInfo.getWorkspace().getName()
                            : "");
            queryable = !layerGroupInfo.isQueryDisabled();
            layerName = layerGroupInfo.getName();
        } else {
            resourceInfo = layerInfo.getResource();
            bbox = layerInfo.getResource().getLatLonBoundingBox();
            layerMeta = resourceInfo.getMetadata();
            workspace =
                    (resourceInfo.getStore().getWorkspace() != null
                            ? resourceInfo.getStore().getWorkspace().getName()
                            : "");
            queryable = layerInfo.isQueryable();
            isTransparent = transparent.orElse(!layerInfo.isOpaque());
            layerName = layerInfo.getName();
        }
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            try {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST, "Invalid TCRS name: " + proj);
                throw new RuntimeException("Invalid TCRS name: " + proj);
            } catch (IOException ioe) {
            }
            return;
        }
        styleName = style.orElse("");
        imageFormat = format.orElse("image/png");
        baseUrl =
                ResponseUtils.buildURL(
                        ResponseUtils.baseURL(request), null, null, URLMangler.URLType.EXTERNAL);
        baseUrlPattern = baseUrl;
        // handle shard config
        enableSharding = layerMeta.get("mapml.enableSharding", Boolean.class);
        String shardListString = layerMeta.get("mapml.shardList", String.class);
        shardArray = new String[0];
        if (shardListString != null) {
            shardArray = shardListString.split("[,\\s]+");
        }
        String shardServerPattern = layerMeta.get("mapml.shardServerPattern", String.class);
        if (shardArray.length < 1 || shardServerPattern == null || shardServerPattern.isEmpty()) {
            enableSharding = Boolean.FALSE;
        }
        // if we have a valid shard config
        if (Boolean.TRUE.equals(enableSharding)) {
            baseUrlPattern = shardBaseURL(request, shardServerPattern);
        }
        useTiles = Boolean.TRUE.equals(layerMeta.get("mapml.useTiles", Boolean.class));
        tileLayerExists =
                gwc.hasTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                        && gwc.getTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                                        .getGridSubset(projType.value())
                                != null;
    }
    /** Create Mapml JAXB object */
    private void prepareDocument() {
        // build the mapML doc
        mapml = new Mapml();
        mapml.setHead(prepareHead());
        mapml.setBody(prepareBody());
    }

    /**
     * Create and return Mapml HeadContent JAXB object
     *
     * @return
     */
    private HeadContent prepareHead() {
        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerName);
        Base base = new Base();
        base.setHref(ResponseUtils.buildURL(baseUrl, "mapml/", null, URLMangler.URLType.EXTERNAL));
        head.setBase(base);
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("utf-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MAPML_MIME_TYPE + ";projection=" + projType.value());
        metas.add(meta);
        meta = new Meta();
        meta.setName("projection");
        meta.setContent(projType.value());
        List<Link> links = head.getLinks();

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link titleLink = new Link();
            titleLink.setRel(RelType.LICENSE);
            if (licenseTitle != null) {
                titleLink.setTitle(licenseTitle);
            }
            if (licenseLink != null) {
                titleLink.setHref(licenseLink);
            }
            links.add(titleLink);
        }
        if (!isLayerGroup) {

            // styles
            Set<StyleInfo> styles = layerInfo.getStyles();
            String effectiveStyleName = styleName;
            if (effectiveStyleName.isEmpty()) {
                effectiveStyleName = layerInfo.getDefaultStyle().getName();
            }

            // style links
            for (StyleInfo si : styles) {
                // skip the self style case (if it is even listed)
                if (si.getName().equals(effectiveStyleName)) continue;
                Link styleLink = new Link();
                styleLink.setRel(RelType.STYLE);
                styleLink.setTitle(si.getName());
                HashMap<String, String> params = new HashMap<>();
                params.put("style", si.getName());
                if (transparent.isPresent()) {
                    params.put("transparent", Boolean.toString(isTransparent));
                }
                if (format.isPresent()) {
                    params.put("format", imageFormat);
                }
                String path = "mapml/" + layer + "/" + proj;
                String url =
                        ResponseUtils.buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
                styleLink.setHref(url);
                links.add(styleLink);
            }
            // output the self style link, taking care to handle the default empty string styleName
            // case
            Link selfStyleLink = new Link();
            selfStyleLink.setRel(RelType.SELF_STYLE);
            selfStyleLink.setTitle(effectiveStyleName);
            HashMap<String, String> params = new HashMap<>();
            params.put("style", styleName);
            if (transparent.isPresent()) {
                params.put("transparent", Boolean.toString(isTransparent));
            }
            if (format.isPresent()) {
                params.put("format", imageFormat);
            }
            String path = "mapml/" + layer + "/" + proj;
            String url = ResponseUtils.buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            selfStyleLink.setHref(url);
            links.add(selfStyleLink);
        }

        // alternate projection links
        for (ProjType pt : ProjType.values()) {
            // skip the current proj
            if (pt.equals(projType)) continue;
            Link projectionLink = new Link();
            projectionLink.setRel(RelType.ALTERNATE);
            projectionLink.setProjection(pt);
            HashMap<String, String> params = new HashMap<>();
            params.put("style", styleName);
            if (transparent.isPresent()) {
                params.put("transparent", Boolean.toString(isTransparent));
            }
            if (format.isPresent()) {
                params.put("format", imageFormat);
            }
            String path = "mapml/" + layer + "/" + pt.value();
            String url = ResponseUtils.buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            projectionLink.setHref(url);
            links.add(projectionLink);
        }
        return head;
    }
    /**
     * Create and return MapML BodyContent JAXB object
     *
     * @return
     */
    private BodyContent prepareBody() {
        BodyContent body = new BodyContent();
        try {
            body.setExtent(prepareExtent());
        } catch (IOException ioe) {

        }
        return body;
    }
    /**
     * Create and return a Mapml Extent JAXB object
     *
     * @return
     * @throws IOException In the event of an I/O error.
     */
    private Extent prepareExtent() throws IOException {
        Extent extent = new Extent();
        extent.setUnits(projType);
        extentList = extent.getInputOrDatalistOrLink();

        // zoom
        zoomInput = new Input();
        zoomInput.setName("z");
        zoomInput.setType(InputType.ZOOM);
        zoomInput.setMin("0");
        int mxz = previewTcrsMap.get(projType.value()).getScales().length - 1;
        zoomInput.setMax(Integer.toString(mxz));
        zoomInput.setValue(Integer.toString(mxz));
        extentList.add(zoomInput);

        Input input;
        // shard list
        if (Boolean.TRUE.equals(enableSharding)) {
            input = new Input();
            input.setName("s");
            input.setType(InputType.HIDDEN);
            input.setShard("true");
            input.setList("servers");
            extentList.add(input);
            Datalist datalist = new Datalist();
            datalist.setId("servers");
            List<Option> options = datalist.getOptions();
            for (String sa : shardArray) {
                Option o = new Option();
                o.setValue(sa);
                options.add(o);
            }
            extentList.add(datalist);
        }

        String dimension = layerMeta.get("mapml.dimension", String.class);
        if ("Time".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo timeInfo =
                        typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo.isEnabled()) {
                    timeEnabled = true;
                    Set<Date> dates = controller.wms.getFeatureTypeTimes(typeInfo);
                    Select select = new Select();
                    select.setId("time");
                    select.setName("time");
                    extentList.add(select);
                    List<Option> options = select.getOptions();
                    for (Date date : dates) {
                        Option o = new Option();
                        o.setContent(new SimpleDateFormat(DATE_FORMAT).format(date));
                        options.add(o);
                    }
                }
            }
        } else if ("Elevation".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo elevInfo =
                        typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
                if (elevInfo.isEnabled()) {
                    elevationEnabled = true;
                    Set<Double> elevs = controller.wms.getFeatureTypeElevations(typeInfo);
                    Select select = new Select();
                    select.setId("elevation");
                    select.setName("elevation");
                    extentList.add(select);
                    List<Option> options = select.getOptions();
                    for (Double elev : elevs) {
                        Option o = new Option();
                        o.setContent(elev.toString());
                        options.add(o);
                    }
                }
            }
        }
        generateTemplatedLinks();
        return extent;
    }
    /** Generate the JAXB Extent object contents: inputs and templated client links */
    private void generateTemplatedLinks() {
        if (useTiles) {
            if (tileLayerExists) {
                generateWMTSClientLinks();
            } else {
                generateTiledWMSClientLinks();
            }
        } else {
            // will use full GetMap requests, no tiles involved
            generateWMSClientLinks();
        }

        // query inputs
        if (queryable) {
            if (useTiles && tileLayerExists) {
                generateWMTSQueryClientLinks();
            } else {
                generateWMSQueryClientLinks();
            }
        }
    }
    /** Generate inputs and templated links that the client will use to make WMTS tile requests */
    private void generateWMTSClientLinks() {
        // emit MapML extent that uses TileMatrix coordinates, allowing
        // client requests for WMTS tiles (GetTile)

        GeoServerTileLayer gstl = gwc.getTileLayer(isLayerGroup ? layerGroupInfo : resourceInfo);
        GridSubset gss = gstl.getGridSubset(projType.value());
        // zoom start/stop are the min/max published zoom levels
        zoomInput.setValue(Integer.toString(gss.getZoomStop()));
        zoomInput.setMin(Integer.toString(gss.getZoomStart()));
        zoomInput.setMax(Integer.toString(gss.getZoomStop()));

        // tilematrix inputs
        Input input = new Input();
        input.setName("x");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setAxis(AxisType.COLUMN);
        long[][] minMax = gss.getWMTSCoverages();
        input.setMin(Long.toString(minMax[minMax.length - 1][0]));
        input.setMax(Long.toString(minMax[minMax.length - 1][2]));
        // there's no way to specify min/max here because
        // the zoom is set by the client
        // need to specify min/max in pcrs or gcrs units
        // OR set the zoom value to the maximum and then
        // specify the min/max at that zoom level

        extentList.add(input);

        input = new Input();
        input.setName("y");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setAxis(AxisType.ROW);
        input.setMin(Long.toString(minMax[minMax.length - 1][1]));
        input.setMax(Long.toString(minMax[minMax.length - 1][3]));
        extentList.add(input);
        // tile link
        Link tileLink = new Link();
        tileLink.setRel(RelType.TILE);
        String path = "gwc/service/wmts";
        HashMap<String, String> params = new HashMap<>();
        params.put("layer", (workspace.isEmpty() ? "" : workspace + ":") + layerName);
        params.put("style", styleName);
        params.put("tilematrixset", projType.value());
        params.put("service", "WMTS");
        params.put("request", "GetTile");
        params.put("version", "1.0.0");
        params.put("tilematrix", "{z}");
        params.put("TileCol", "{x}");
        params.put("TileRow", "{y}");
        params.put("format", imageFormat);
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }
    /**
     * Gnerate inputs and templated links that the client will use to make WMS requests for
     * individual tiles i.e. a GetMap for each 256x256 tile image
     */
    private void generateTiledWMSClientLinks() {
        // generateTiledWMSClientLinks
        // emit MapML extent that uses WMS GetMap requests to request tiles

        // TODO the axis name should be gettable from the TCRS.
        // need an api like this, perhaps:
        // previewTcrsMap.get(projType.value()).getCRS(UnitType.PCRS).getAxis(AxisDirection.DISPLAY_RIGHT);
        // TODO what is the pcrs of WGS84 ? What are its units?
        // I believe the answer to the above question is that the PCRS
        // of WGS84 is a cartesian cs per the table on this page:
        // https://docs.geotools.org/stable/javadocs/org/opengis/referencing/cs/package-summary.html#AxisNames
        // input.setAxis(previewTcrsMap.get(projType.value()).getCRS(UnitType.PCRS).getAxisByDirection(AxisDirection.DISPLAY_RIGHT));
        ReferencedEnvelope bbbox =
                new ReferencedEnvelope(previewTcrsMap.get(projType.value()).getCRS());
        try {
            bbbox =
                    isLayerGroup
                            ? layerGroupInfo.getBounds()
                            : layerInfo.getResource().boundingBox();
            bbbox = bbbox.transform(previewTcrsMap.get(projType.value()).getCRS(), true);
        } catch (Exception e) {
            // sometimes, when the bbox is right to 90N or 90S, in epsg:3857,
            // the transform method will throw. In that case, use the
            // bounds of the TCRS to define the bbox for the layer
            TiledCRS t = previewTcrsMap.get(projType.value());
            double x1 = t.getBounds().getMax().x;
            double y1 = t.getBounds().getMax().y;
            double x2 = t.getBounds().getMin().x;
            double y2 = t.getBounds().getMin().y;
            bbbox = new ReferencedEnvelope(x1, x2, y1, y2, t.getCRS());
        }

        // tile inputs
        // txmin
        Input input = new Input();
        input.setName("txmin");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // tymin
        input = new Input();
        input.setName("tymin");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.BOTTOM_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // txmax
        input = new Input();
        input.setName("txmax");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_RIGHT);
        input.setRel(InputRelType.TILE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // tymax
        input = new Input();
        input.setName("tymax");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // tile link
        Link tileLink = new Link();
        tileLink.setRel(RelType.TILE);
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetMap");
        params.put("crs", previewTcrsMap.get(projType.value()).getCode());
        params.put("layers", layerName);
        params.put("styles", styleName);
        if (timeEnabled) {
            params.put("time", "{time}");
        }
        if (elevationEnabled) {
            params.put("elevation", "{elevation}");
        }
        params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
        params.put("format", imageFormat);
        params.put("transparent", Boolean.toString(isTransparent));
        params.put("width", "256");
        params.put("height", "256");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }
    /**
     * Generate inputs and links that the client will use to create WMS GetMap requests for full map
     * images
     */
    public void generateWMSClientLinks() {
        // generateWMSClientLinks
        // emit MapML extent that uses WMS requests to request complete images

        ReferencedEnvelope bbbox;
        try {
            // initialization is necessary so as to set the PCRS to which
            // the resource's bbox will be transformed, below.
            bbbox = new ReferencedEnvelope(previewTcrsMap.get(projType.value()).getCRS());
            bbbox =
                    isLayerGroup
                            ? layerGroupInfo.getBounds()
                            : layerInfo.getResource().boundingBox();
            // transform can cause an exception if the bbox coordinates fall
            // too near the pole (at least in OSMTILE, where the poles are
            // undefined/out of scope).
            // If it throws, we need to reset the bbbox value so that its
            // crs is that of the underlying pcrs from the TCRS, because
            // the bbbox.transform will leave the CRS set to that of whatever
            // was returned by layerInfo.getResource().boundingBox() or
            // layerGroupInfo.getBounds(), above.
            bbbox = bbbox.transform(previewTcrsMap.get(projType.value()).getCRS(), true);
        } catch (Exception e) {
            // get the default max/min of the pcrs from the TCRS
            Bounds defaultBounds = previewTcrsMap.get(projType.value()).getBounds();
            double x1, x2, y1, y2;
            x1 = defaultBounds.getMin().x;
            x2 = defaultBounds.getMax().x;
            y1 = defaultBounds.getMin().y;
            y2 = defaultBounds.getMax().y;
            // use the bounds of the TCRS as the default bounds for this layer
            bbbox =
                    new ReferencedEnvelope(
                            x1, x2, y1, y2, previewTcrsMap.get(projType.value()).getCRS());
        }
        // image inputs
        // xmin
        Input input = new Input();
        input.setName("xmin");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymin
        input = new Input();
        input.setName("ymin");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.BOTTOM_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // xmax
        input = new Input();
        input.setName("xmax");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_RIGHT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymax
        input = new Input();
        input.setName("ymax");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // width
        input = new Input();
        input.setName("w");
        input.setType(InputType.WIDTH);
        input.setMin("1");
        input.setMax("10000");
        extentList.add(input);

        // height
        input = new Input();
        input.setName("h");
        input.setType(InputType.HEIGHT);
        input.setMin("1");
        input.setMax("10000");
        extentList.add(input);

        // image link
        Link imageLink = new Link();
        imageLink.setRel(RelType.IMAGE);
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetMap");
        params.put("crs", previewTcrsMap.get(projType.value()).getCode());
        params.put("layers", layerName);
        params.put("styles", styleName);
        if (timeEnabled) {
            params.put("time", "{time}");
        }
        if (elevationEnabled) {
            params.put("elevation", "{elevation}");
        }
        params.put("bbox", "{xmin},{ymin},{xmax},{ymax}");
        params.put("format", imageFormat);
        params.put("transparent", Boolean.toString(isTransparent));
        params.put("width", "{w}");
        params.put("height", "{h}");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        imageLink.setTref(urlTemplate);
        extentList.add(imageLink);
    }
    /**
     * Generate inputs and links that the client will use to generate WMTS GetFeatureInfo requests
     */
    private void generateWMTSQueryClientLinks() {
        // query i value (x)
        Input input = new Input();
        input.setName("i");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILE);
        input.setAxis(AxisType.I);
        extentList.add(input);

        // query j value (y)
        input = new Input();
        input.setName("j");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILE);
        input.setAxis(AxisType.J);
        extentList.add(input);

        // query link
        Link queryLink = new Link();
        queryLink.setRel(RelType.QUERY);
        String path = "gwc/service/wmts";
        HashMap<String, String> params = new HashMap<>();
        params.put("layer", (workspace.isEmpty() ? "" : workspace + ":") + layerName);
        params.put("tilematrix", "{z}");
        params.put("TileCol", "{x}");
        params.put("TileRow", "{y}");
        params.put("tilematrixset", projType.value());
        params.put("service", "WMTS");
        params.put("version", "1.0.0");
        params.put("request", "GetFeatureInfo");
        params.put("feature_count", "50");
        params.put("format", imageFormat);
        params.put("style", styleName);
        params.put("infoformat", "text/mapml");
        params.put("i", "{i}");
        params.put("j", "{j}");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        queryLink.setTref(urlTemplate);
        extentList.add(queryLink);
    }
    /** Generate inputs and links the client will use to create WMS GetFeatureInfo requests */
    private void generateWMSQueryClientLinks() {
        UnitType units = UnitType.MAP;
        if (useTiles) {
            units = UnitType.TILE;
        }
        // query i value (x)
        Input input = new Input();
        input.setName("i");
        input.setType(InputType.LOCATION);
        input.setUnits(units);
        input.setAxis(AxisType.I);
        extentList.add(input);

        // query j value (y)
        input = new Input();
        input.setName("j");
        input.setType(InputType.LOCATION);
        input.setUnits(units);
        input.setAxis(AxisType.J);
        extentList.add(input);

        // query link
        Link queryLink = new Link();
        queryLink.setRel(RelType.QUERY);
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetFeatureInfo");
        params.put("feature_count", "50");
        params.put("crs", previewTcrsMap.get(projType.value()).getCode());
        params.put("layers", layerName);
        params.put("query_layers", layerName);
        params.put("styles", styleName);
        if (timeEnabled) {
            params.put("time", "{time}");
        }
        if (elevationEnabled) {
            params.put("elevation", "{elevation}");
        }
        if (useTiles) {
            params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
            params.put("width", "256");
            params.put("height", "256");
        } else {
            params.put("bbox", "{xmin},{ymin},{xmax},{ymax}");
            params.put("width", "{w}");
            params.put("height", "{h}");
        }
        params.put("info_format", "text/mapml");
        params.put("transparent", Boolean.toString(isTransparent));
        params.put("x", "{i}");
        params.put("y", "{j}");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        queryLink.setTref(urlTemplate);
        extentList.add(queryLink);
    }
    /**
     * @param req
     * @param shardServerPattern
     * @return
     */
    private String shardBaseURL(HttpServletRequest req, String shardServerPattern) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://")
                .append(shardServerPattern)
                .append(":")
                .append(req.getServerPort())
                .append(req.getContextPath())
                .append("/");
        return sb.toString();
    }
}
