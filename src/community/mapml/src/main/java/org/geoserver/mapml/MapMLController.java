/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.geoserver.mapml.tcrs.Point;
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
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.grid.GridSubset;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/mapml")
@CrossOrigin
public class MapMLController {

    @Autowired GeoServer geoServer;
    @Autowired WMS wms;

    public static final HashMap<String, TiledCRS> previewTcrsMap = new HashMap<>();
    private static final GWC gwc = GWC.get();

    private static final Bounds DISPLAY_BOUNDS_PHONE_PORTRAIT =
            new Bounds(new Point(0, 0), new Point(300, 812));
    private static final Bounds DISPLAY_BOUNDS_PHONE_LANDSCAPE =
            new Bounds(new Point(0, 0), new Point(812, 300));
    private static final Bounds DISPLAY_BOUNDS_TABLET_PORTRAIT =
            new Bounds(new Point(0, 0), new Point(760, 1024));
    private static final Bounds DISPLAY_BOUNDS_TABLET_LANDSCAPE =
            new Bounds(new Point(0, 0), new Point(1024, 760));
    private static final Bounds DISPLAY_BOUNDS_DESKTOP_PORTRAIT =
            new Bounds(new Point(0, 0), new Point(1024, 768));
    private static final Bounds DISPLAY_BOUNDS_DESKTOP_LANDSCAPE =
            new Bounds(new Point(0, 0), new Point(768, 1024));
    private static final HashMap<String, List<Bounds>> DISPLAYS = new HashMap<>();

    static {
        previewTcrsMap.put("OSMTILE", new TiledCRS("OSMTILE"));
        previewTcrsMap.put("CBMTILE", new TiledCRS("CBMTILE"));
        previewTcrsMap.put("APSTILE", new TiledCRS("APSTILE"));
        previewTcrsMap.put("WGS84", new TiledCRS("WGS84"));

        ArrayList<Bounds> phones = new ArrayList<>();
        phones.add(DISPLAY_BOUNDS_PHONE_PORTRAIT);
        phones.add(DISPLAY_BOUNDS_PHONE_LANDSCAPE);
        DISPLAYS.put("PHONE", phones);

        ArrayList<Bounds> tablets = new ArrayList<>();
        tablets.add(DISPLAY_BOUNDS_TABLET_PORTRAIT);
        tablets.add(DISPLAY_BOUNDS_TABLET_LANDSCAPE);
        DISPLAYS.put("TABLET", tablets);

        ArrayList<Bounds> desktops = new ArrayList<>();
        desktops.add(DISPLAY_BOUNDS_DESKTOP_PORTRAIT);
        desktops.add(DISPLAY_BOUNDS_DESKTOP_LANDSCAPE);
        DISPLAYS.put("DESKTOP", desktops);
    }

    @RequestMapping(
        value = "/{layer}/{proj}",
        method = {RequestMethod.GET, RequestMethod.POST},
        produces = {"text/html", "text/html;charset=UTF-8", "!" + MAPML_MIME_TYPE}
    )
    public String Html(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("layer") String layer,
            @PathVariable("proj") String proj,
            @RequestParam("style") Optional<String> style,
            @RequestParam("transparent") Optional<Boolean> transparent,
            @RequestParam("format") Optional<String> format) {
        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(layer);
        ReferencedEnvelope bbox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        ResourceInfo resourceInfo = null;
        LayerGroupInfo layerGroupInfo = null;
        MetadataMap layerMeta;
        boolean isLayerGroup = (layerInfo == null);
        boolean isTransparent = transparent.orElse(true);
        String layerName = "";
        final String workspace;
        String styleName = style.orElse("");
        String imageFormat = format.orElse("image/png");
        String baseUrl = ResponseUtils.baseURL(request);
        String baseUrlPattern = baseUrl;
        if (isLayerGroup) {
            layerGroupInfo = geoServer.getCatalog().getLayerGroupByName(layer);
            if (layerGroupInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "Invalid layer or layer group name: " + layer;
            }
            workspace =
                    (layerGroupInfo.getWorkspace() != null
                            ? layerGroupInfo.getWorkspace().getName()
                            : "");
            layerMeta = layerGroupInfo.getMetadata();
            for (LayerInfo li : layerGroupInfo.layers()) {
                bbox.expandToInclude(li.getResource().getLatLonBoundingBox());
            }
            // if the layerGroupInfo.getName() is empty, the layer group isn't
            // available to a getMap request, so we should probably throw in
            // that case, or perhaps let the mapML method deal with iterating
            // the child layers.
            layerName =
                    layerGroupInfo.getTitle() == null || layerGroupInfo.getTitle().isEmpty()
                            ? layerGroupInfo.getName().isEmpty() ? layer : layerGroupInfo.getName()
                            : layerGroupInfo.getTitle();
        } else {
            resourceInfo = layerInfo.getResource();
            bbox = layerInfo.getResource().getLatLonBoundingBox();
            layerName =
                    resourceInfo.getTitle().isEmpty()
                            ? layerInfo.getName().isEmpty() ? layer : layerInfo.getName()
                            : resourceInfo.getTitle();
            layerMeta = resourceInfo.getMetadata();
            workspace =
                    (resourceInfo.getStore().getWorkspace() != null
                            ? resourceInfo.getStore().getWorkspace().getName()
                            : "");
            isTransparent = transparent.orElse(!layerInfo.isOpaque());
        }
        ProjType projType;
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Invalid TCRS name: " + proj;
        }
        TiledCRS TCRS = previewTcrsMap.get(projType.value());

        Double longitude = bbox.centre().getX();
        Double latitude = bbox.centre().getY();

        String dimension = layerMeta.get("mapml.dimension", String.class);
        boolean timeEnabled = false;
        boolean elevationEnabled = false;
        if ("Time".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo timeInfo =
                        typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo.isEnabled()) {
                    timeEnabled = true;
                }
            }
        } else if ("Elevation".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo elevInfo =
                        typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
                if (elevInfo.isEnabled()) {
                    elevationEnabled = true;
                }
            }
        }

        final boolean te = timeEnabled;
        final boolean ee = elevationEnabled;
        final ReferencedEnvelope bbbox;
        int zoom = 0;
        final boolean t = isTransparent;
        Set<String> sources = new HashSet<>();
        try {
            ReferencedEnvelope lb =
                    isLayerGroup
                            ? layerGroupInfo.getBounds()
                            : layerInfo.getResource().boundingBox();
            bbbox = lb.transform(previewTcrsMap.get(projType.value()).getCRS(), true);
            final Bounds pb =
                    new Bounds(
                            new Point(bbbox.getMinX(), bbbox.getMinY()),
                            new Point(bbbox.getMaxX(), bbbox.getMaxY()));
            // allowing for the data to be displayed at 1024x768 pixels, figure out
            // the zoom level at which the projected bounds fits into 1024x768
            // in both dimensions
            zoom = TCRS.fitProjectedBoundsToDisplay(pb, DISPLAY_BOUNDS_DESKTOP_LANDSCAPE);

            for (String deviceType : DISPLAYS.keySet()) {
                String source =
                        "<source media=\"(min-width: "
                                + new Double(DISPLAYS.get(deviceType).get(0).getWidth()).intValue()
                                + "px)\" sizes=\"100vw\" srcset=\""
                                + DISPLAYS.get(deviceType)
                                        .stream()
                                        .map(
                                                b -> {
                                                    int z = TCRS.fitProjectedBoundsToDisplay(pb, b);
                                                    Bounds db =
                                                            TCRS.getProjectedBoundsForDisplayBounds(
                                                                    z, pb.getCentre(), b);
                                                    return new StringBuilder()
                                                            .append(baseUrlPattern)
                                                            .append(
                                                                    workspace.isEmpty()
                                                                            ? ""
                                                                            : workspace)
                                                            .append("/")
                                                            .append(
                                                                    "wms?version=1.3.0&service=WMS&request=GetMap&crs=")
                                                            .append(TCRS.getCode())
                                                            .append("&layers=")
                                                            .append(layer)
                                                            .append("&styles=")
                                                            .append(styleName)
                                                            .append(te ? "&time={time}" : "")
                                                            .append(
                                                                    ee
                                                                            ? "&elevation={elevation}"
                                                                            : "")
                                                            .append("&bbox=")
                                                            .append(db.getMin().x)
                                                            .append(",")
                                                            .append(db.getMin().y)
                                                            .append(",")
                                                            .append(db.getMax().x)
                                                            .append(",")
                                                            .append(db.getMax().y)
                                                            .append("&format=")
                                                            .append(imageFormat)
                                                            .append("&transparent=")
                                                            .append(t)
                                                            .append("&width=")
                                                            .append(
                                                                    new Double(b.getWidth())
                                                                            .intValue())
                                                            .append("&height=")
                                                            .append(
                                                                    new Double(b.getHeight())
                                                                            .intValue())
                                                            .append(" ")
                                                            .append(
                                                                    new Double(b.getWidth())
                                                                            .intValue())
                                                            .append("w")
                                                            .toString();
                                                })
                                        .collect(Collectors.joining(","))
                                + "\">";
                sources.add(source);
            }
        } catch (Exception e) {
            // what to do?
            // log.info("Exception occured retrieving bbox for "+ layer
            // );
        }

        String viewerPath = request.getContextPath() + request.getServletPath() + "/viewer";
        String title = "GeoServer MapML preview " + layerName;
        String removeImageScript =
                "function removeAllChildren(element){\n"
                        + "  while(element.firstChild){\n"
                        + "    element.removeChild(element.firstChild);\n"
                        + "  }\n"
                        + "};\n"
                        + "document.addEventListener('DOMContentLoaded',function() {  \n"
                        + "  removeAllChildren(document.getElementById('data-web-map-responsive')); \n"
                        + "});";
        // figure out JavaScript to make the map responsively fill the
        // browser window. This would be a media query I think... and the
        // map could receive a zoom parameter based on the media query

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>")
                .append(title)
                .append("</title>\n")
                .append("<meta charset='utf-8'>\n")
                .append("<script>")
                .append(removeImageScript)
                .append("</script>")
                .append("<script src=\"")
                .append(viewerPath)
                .append("/bower_components/webcomponentsjs/webcomponents-lite.min.js\"></script>\n")
                .append("<link rel=\"import\" href=\"")
                .append(viewerPath)
                .append("/bower_components/web-map/web-map.html\">\n")
                .append("<style>\n")
                .append("* {margin: 0;padding: 0;}\n")
                .append("map { height: 100vh;}\n") // important map must have a height
                .append("</style>")
                .append("</head>\n")
                .append("<body>\n")
                .append("<map name=\"mappy\" is=\"web-map\" projection=\"")
                .append(projType.value())
                .append("\" ")
                .append("zoom=\"")
                .append(zoom)
                .append("\" lat=\"")
                .append(latitude)
                .append("\" ")
                .append("lon=\"")
                .append(longitude)
                .append("\" controls>\n")
                .append("<layer- label=\"")
                .append(layerName)
                .append("\" ")
                .append("src=\"")
                .append(request.getContextPath())
                .append(request.getServletPath())
                .append("/")
                .append(layer)
                .append("/")
                .append(proj)
                .append("/")
                .append(!styleName.isEmpty() ? "?style=" + styleName : "")
                .append("\" checked></layer->\n")
                .append("</map>\n")
                .append("<div id=data-web-map-responsive>")
                .append("<picture>\n")
                .append(sources.stream().collect(Collectors.joining()))
                .append("<img usemap=\"#mappy\" ")
                .append("src=\"\"")
                .append(" style=\"object-fit: contain; ")
                .append("width: 100vw; height: 100vh\" ")
                .append(" alt=\"Map of ")
                .append(layerName)
                .append("\">\n")
                .append("</picture>\n")
                .append("</div>")
                .append("</body>\n")
                .append("</html>");
        return sb.toString();
    }

    @RequestMapping(
        value = "/{layer}/{proj}",
        method = {RequestMethod.GET, RequestMethod.POST},
        produces = {MAPML_MIME_TYPE, "!text/html;charset=UTF-8"}
    )
    public Mapml mapML(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable("layer") String layer,
            @PathVariable("proj") String proj,
            @RequestParam("style") Optional<String> style,
            @RequestParam("transparent") Optional<Boolean> transparent,
            @RequestParam("format") Optional<String> format)
            throws NoSuchAuthorityCodeException, TransformException, FactoryException, IOException {
        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(layer);
        ReferencedEnvelope bbox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        ResourceInfo resourceInfo = null;
        MetadataMap layerMeta;
        LayerGroupInfo layerGroupInfo = null;
        String workspace = "";
        boolean isTransparent = true;
        boolean queryable = false;
        boolean isLayerGroup = (layerInfo == null);
        String layerName = "";
        if (isLayerGroup) {
            layerGroupInfo = geoServer.getCatalog().getLayerGroupByName(layer);
            if (layerGroupInfo == null) {
                try {
                    response.sendError(
                            HttpServletResponse.SC_NOT_FOUND,
                            "Invalid layer or layer group name: " + layer);
                } catch (IOException ioe) {
                }
                return null;
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

        ProjType projType;
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            try {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST, "Invalid TCRS name: " + proj);
            } catch (IOException ioe) {
            }
            return null;
        }

        //        CoordinateReferenceSystem projSrs =
        //                CRS.decode(previewTcrsMap.get(projType.value()).getCode());
        //        Collection<? extends GeographicExtent> geoExtents =
        //                projSrs.getDomainOfValidity().getGeographicElements();
        //        // we assume there is only one geoExtent and that it is a GeographicBoundingBox;
        // otherwise
        //        // we can't really do anything
        //        if (geoExtents.size() == 1) {
        //            for (GeographicExtent ge : geoExtents) {
        //                if (ge instanceof GeographicBoundingBox) {
        //                    GeographicBoundingBox gbb = (GeographicBoundingBox) ge;
        //                    Envelope e =
        //                            new Envelope(
        //                                    gbb.getEastBoundLongitude(),
        //                                    gbb.getWestBoundLongitude(),
        //                                    gbb.getSouthBoundLatitude(),
        //                                    gbb.getNorthBoundLatitude());
        //                    // reduce the data's bbox to fit in the domain of the projection
        //                    bbox = bbox.intersection(e);
        //                }
        //            }
        //        }
        //        ReferencedEnvelope cbmBbox = bbox.transform(projSrs, true);

        String styleName = style.orElse("");
        String imageFormat = format.orElse("image/png");

        String baseUrl = ResponseUtils.baseURL(request);
        String baseUrlPattern = baseUrl;

        // handle shard config
        Boolean enableSharding = layerMeta.get("mapml.enableSharding", Boolean.class);
        String shardListString = layerMeta.get("mapml.shardList", String.class);
        String[] shardArray = new String[0];
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

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerName);
        Base base = new Base();
        base.setHref(baseUrl + "mapml");
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
                styleLink.setHref(
                        baseUrl
                                + "mapml/"
                                + layer
                                + "/"
                                + proj
                                + "?style="
                                + si.getName()
                                + (transparent.isPresent() ? "&transparent=" + isTransparent : "")
                                + (format.isPresent() ? "&format=" + imageFormat : ""));
                links.add(styleLink);
            }
            // output the self style link, taking care to handle the default empty string styleName
            // case
            Link selfStyleLink = new Link();
            selfStyleLink.setRel(RelType.SELF_STYLE);
            selfStyleLink.setTitle(effectiveStyleName);
            selfStyleLink.setHref(
                    baseUrl
                            + "mapml/"
                            + layer
                            + "/"
                            + proj
                            + "?style="
                            + styleName
                            + (transparent.isPresent() ? "&transparent=" + isTransparent : "")
                            + (format.isPresent() ? "&format=" + imageFormat : ""));

            links.add(selfStyleLink);
        }

        // alternate projection links
        for (ProjType pt : ProjType.values()) {
            // skip the current proj
            if (pt.equals(projType)) continue;
            Link projectionLink = new Link();
            projectionLink.setRel(RelType.ALTERNATE);
            projectionLink.setProjection(pt);
            projectionLink.setHref(
                    baseUrl
                            + "mapml/"
                            + layer
                            + "/"
                            + pt.value()
                            + "?style="
                            + styleName
                            + (transparent.isPresent() ? "&transparent=" + isTransparent : "")
                            + (format.isPresent() ? "&format=" + imageFormat : ""));
            links.add(projectionLink);
        }

        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        Extent extent = new Extent();
        extent.setUnits(projType);
        List<Object> extentList = extent.getInputOrDatalistOrLink();

        // zoom
        Input zoomInput = new Input();
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
            for (int s = 0; s < shardArray.length; s++) {
                Option o = new Option();
                o.setValue(shardArray[s]);
                options.add(o);
            }
            extentList.add(datalist);
        }

        String dimension = layerMeta.get("mapml.dimension", String.class);
        boolean timeEnabled = false;
        boolean elevationEnabled = false;
        if ("Time".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo timeInfo =
                        typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo.isEnabled()) {
                    timeEnabled = true;
                    Set<Date> dates = wms.getFeatureTypeTimes(typeInfo);
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
                    Set<Double> elevs = wms.getFeatureTypeElevations(typeInfo);
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
        final boolean tileLayerExists =
                gwc.hasTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                        && gwc.getTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                                        .getGridSubset(projType.value())
                                != null;

        Boolean useTiles = layerMeta.get("mapml.useTiles", Boolean.class);
        if (Boolean.TRUE.equals(useTiles)) {
            if (tileLayerExists) {
                // emit MapML extent that uses TileMatrix coordinates

                GeoServerTileLayer gstl =
                        gwc.getTileLayer(isLayerGroup ? layerGroupInfo : resourceInfo);
                GridSubset gss = gstl.getGridSubset(projType.value());
                // zoom start/stop are the min/max published zoom levels
                zoomInput.setValue(Integer.toString(gss.getZoomStop()));
                zoomInput.setMin(Integer.toString(gss.getZoomStart()));
                zoomInput.setMax(Integer.toString(gss.getZoomStop()));

                // tilematrix inputs
                input = new Input();
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
                tileLink.setTref(
                        baseUrlPattern
                                + (baseUrlPattern.endsWith("/") ? "" : "/")
                                + "gwc/service/wmts?layer="
                                + (workspace.isEmpty() ? "" : workspace + ":")
                                + layerName
                                + "&style="
                                + styleName
                                + "&tilematrixset="
                                + projType.value()
                                + "&service=WMTS&request=GetTile"
                                + "&version=1.0.0"
                                + "&tilematrix={z}"
                                + "&TileCol={x}"
                                + "&TileRow={y}"
                                + "&format="
                                + imageFormat);
                extentList.add(tileLink);
            } else {
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
                } catch (Exception e) {
                    //                    log.info("Exception occured retrieving bbox for "+ layer
                    // );
                }
                bbbox = bbbox.transform(previewTcrsMap.get(projType.value()).getCRS(), true);

                // tile inputs
                // txmin
                input = new Input();
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
                tileLink.setTref(
                        baseUrlPattern
                                + (workspace.isEmpty() ? "" : workspace + "/")
                                + "wms?version=1.3.0&service=WMS&request=GetMap&crs="
                                + previewTcrsMap.get(projType.value()).getCode()
                                + "&layers="
                                + layerName
                                + "&styles="
                                + styleName
                                + (timeEnabled ? "&time={time}" : "")
                                + (elevationEnabled ? "&elevation={elevation}" : "")
                                + "&bbox={txmin},{tymin},{txmax},{tymax}"
                                + "&format="
                                + imageFormat
                                + "&transparent="
                                + isTransparent
                                + "&width=256&height=256");
                extentList.add(tileLink);
            }
        } else {
            // emit MapML extent that uses WMS requests to request complete images

            ReferencedEnvelope bbbox =
                    new ReferencedEnvelope(previewTcrsMap.get(projType.value()).getCRS());
            try {
                bbbox =
                        isLayerGroup
                                ? layerGroupInfo.getBounds()
                                : layerInfo.getResource().boundingBox();
            } catch (Exception e) {
                //                    log.info("Exception occured retrieving bbox for "+ layer
                // );
            }
            bbbox = bbbox.transform(previewTcrsMap.get(projType.value()).getCRS(), true);

            // image inputs
            // xmin
            input = new Input();
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
            imageLink.setTref(
                    baseUrlPattern
                            + (workspace.isEmpty() ? "" : workspace + "/")
                            + "wms?version=1.3.0&service=WMS&request=GetMap&crs="
                            + previewTcrsMap.get(projType.value()).getCode()
                            + "&layers="
                            + layerName
                            + "&styles="
                            + styleName
                            + (timeEnabled ? "&time={time}" : "")
                            + (elevationEnabled ? "&elevation={elevation}" : "")
                            + "&bbox={xmin},{ymin},{xmax},{ymax}"
                            + "&format="
                            + imageFormat
                            + "&transparent="
                            + isTransparent
                            + "&width={w}&height={h}");
            extentList.add(imageLink);
        }

        // query inputs
        if (queryable) {
            if (Boolean.TRUE.equals(useTiles) && tileLayerExists) {
                // query i value (x)
                input = new Input();
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
                queryLink.setTref(
                        baseUrlPattern
                                + (baseUrlPattern.endsWith("/") ? "" : "/")
                                + "gwc/service/wmts/"
                                + "?LAYER="
                                + (workspace.isEmpty() ? "" : workspace + ":")
                                + layerName
                                + "&TILEMATRIX={z}"
                                + "&TileCol={x}&TileRow={y}"
                                + "&TILEMATRIXSET="
                                + projType.value()
                                + "&SERVICE=WMTS"
                                + "&VERSION=1.0.0"
                                + "&REQUEST=GetFeatureInfo&FEATURE_COUNT=50"
                                + "&FORMAT="
                                + imageFormat
                                + "&STYLE="
                                + styleName
                                + "&INFOFORMAT=text/mapml"
                                + "&I={i}&J={j}");
                extentList.add(queryLink);
            } else {

                UnitType units = UnitType.MAP;
                if (Boolean.TRUE.equals(useTiles)) {
                    units = UnitType.TILE;
                }
                // query i value (x)
                input = new Input();
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
                queryLink.setTref(
                        baseUrlPattern
                                + (workspace.isEmpty() ? "" : workspace + "/")
                                + "wms?version=1.3.0&service=WMS&request=GetFeatureInfo&FEATURE_COUNT=50&crs="
                                + previewTcrsMap.get(projType.value()).getCode()
                                + "&layers="
                                + layerName
                                + "&query_layers="
                                + layerName
                                + "&styles="
                                + styleName
                                + (timeEnabled ? "&time={time}" : "")
                                + (elevationEnabled ? "&elevation={elevation}" : "")
                                + (Boolean.TRUE.equals(useTiles)
                                        ? "&bbox={txmin},{tymin},{txmax},{tymax}&width=256&height=256"
                                        : "&bbox={xmin},{ymin},{xmax},{ymax}&width={w}&height={h}")
                                + "&info_format=text/mapml"
                                + "&transparent="
                                + isTransparent
                                + "&x={i}&y={j}");
                extentList.add(queryLink);
            }
        }

        body.setExtent(extent);
        mapml.setBody(body);
        return mapml;
    }

    private static String shardBaseURL(HttpServletRequest req, String shardServerPattern) {
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
