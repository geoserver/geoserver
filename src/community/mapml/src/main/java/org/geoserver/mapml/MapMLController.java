/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.geoserver.mapml.tcrs.LatLngBounds;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.xml.AxisType;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Datalist;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Input;
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
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

    static {
        previewTcrsMap.put("OSMTILE", new TiledCRS("OSMTILE"));
        previewTcrsMap.put("CBMTILE", new TiledCRS("CBMTILE"));
        previewTcrsMap.put("APSTILE", new TiledCRS("APSTILE"));
        // TODO add WGS84 support
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
        LayerGroupInfo layerGroupInfo;
        boolean isLayerGroup = (layerInfo == null);
        String layerName = "";
        if (isLayerGroup) {
            layerGroupInfo = geoServer.getCatalog().getLayerGroupByName(layer);
            if (layerGroupInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "Invalid layer or layer group name: " + layer;
            }
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
        }
        ProjType projType;
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "Invalid TCRS name: " + proj;
        }

        Double longitude = bbox.centre().getX();
        Double latitude = bbox.centre().getY();

        // convert bbox to projected bounds

        // allowing for the data to be displayed at 1024x768 pixels, figure out
        // the zoom level at which the projected bounds fits into 1024x768
        // create a function that accepts a display size e.g. 1024,768 (w,h)
        // and a lat-long bounds, and a tcrs name and returns a zoom
        // recommended for display of the bounds on that screen size

        int zoom =
                previewTcrsMap
                        .get(projType.value())
                        .fitLatLngBoundsToDisplay(new LatLngBounds(bbox), 1024, 768);

        String viewerPath = request.getContextPath() + request.getServletPath() + "/viewer";
        String title = "GeoServer MapML preview " + layerName;

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
                .append("<script src=\"")
                .append(viewerPath)
                .append("/bower_components/webcomponentsjs/webcomponents-lite.min.js\"></script>\n")
                .append("<link rel=\"import\" href=\"")
                .append(viewerPath)
                .append("/bower_components/web-map/web-map.html\">\n")
                .append("<style>\n")
                .append("* {margin: 0;padding: 0;}\n")
                .append("map { display: flexbox; height: 100vh;}\n")
                .append("</style>")
                .append("</head>\n")
                .append("<body>\n")
                .append("<map is=\"web-map\" projection=\"")
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
                .append("/\" checked></layer->\n")
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
        LayerGroupInfo layerGroupInfo;
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

        CoordinateReferenceSystem projSrs = CRS.decode("EPSG:" + projType.epsgCode);
        Collection<? extends GeographicExtent> geoExtents =
                projSrs.getDomainOfValidity().getGeographicElements();
        // we assume there is only one geoExtent and that it is a GeographicBoundingBox; otherwise
        // we can't really do anything
        if (geoExtents.size() == 1) {
            for (GeographicExtent ge : geoExtents) {
                if (ge instanceof GeographicBoundingBox) {
                    GeographicBoundingBox gbb = (GeographicBoundingBox) ge;
                    Envelope e =
                            new Envelope(
                                    gbb.getEastBoundLongitude(),
                                    gbb.getWestBoundLongitude(),
                                    gbb.getSouthBoundLatitude(),
                                    gbb.getNorthBoundLatitude());
                    // reduce the data's bbox to fit in the domain of the projection
                    bbox = bbox.intersection(e);
                }
            }
        }
        ReferencedEnvelope cbmBbox = bbox.transform(projSrs, true);

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
        Input input = new Input();
        input.setName("z");
        input.setType(InputType.ZOOM);
        input.setValue("0");
        input.setMin(0);
        input.setMax(0);
        extentList.add(input);

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

        Boolean useTiles = layerMeta.get("mapml.useTiles", Boolean.class);
        if (Boolean.TRUE.equals(useTiles)) {
            // tile inputs
            // txmin
            input = new Input();
            input.setName("txmin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILEMATRIX);
            input.setPosition(PositionType.TOP_LEFT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // tymin
            input = new Input();
            input.setName("tymin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILEMATRIX);
            input.setPosition(PositionType.BOTTOM_LEFT);
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // txmax
            input = new Input();
            input.setName("txmax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILEMATRIX);
            input.setPosition(PositionType.TOP_RIGHT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // tymax
            input = new Input();
            input.setName("tymax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILEMATRIX);
            input.setPosition(PositionType.TOP_LEFT);
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // tile link
            Link tileLink = new Link();
            tileLink.setRel(RelType.TILE);
            tileLink.setTref(
                    baseUrlPattern
                            + (workspace.isEmpty() ? "" : workspace + "/")
                            + "wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
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
        } else {
            // image inputs
            // xmin
            input = new Input();
            input.setName("xmin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS);
            input.setPosition(PositionType.TOP_LEFT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // ymin
            input = new Input();
            input.setName("ymin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS);
            input.setPosition(PositionType.BOTTOM_LEFT);
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // xmax
            input = new Input();
            input.setName("xmax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS);
            input.setPosition(PositionType.TOP_RIGHT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // ymax
            input = new Input();
            input.setName("ymax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS);
            input.setPosition(PositionType.TOP_LEFT);
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // width
            input = new Input();
            input.setName("w");
            input.setType(InputType.WIDTH);
            input.setMin(1);
            input.setMax(10000);
            extentList.add(input);

            // height
            input = new Input();
            input.setName("h");
            input.setType(InputType.HEIGHT);
            input.setMin(1);
            input.setMax(10000);
            extentList.add(input);

            // image link
            Link imageLink = new Link();
            imageLink.setRel(RelType.IMAGE);
            imageLink.setTref(
                    baseUrlPattern
                            + (workspace.isEmpty() ? "" : workspace + "/")
                            + "wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
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
                            + "wms?version=1.3.0&service=WMS&request=GetFeatureInfo&FEATURE_COUNT=50&crs=EPSG:"
                            + projType.epsgCode
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
