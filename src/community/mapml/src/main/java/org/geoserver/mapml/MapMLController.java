/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
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
import org.geotools.util.logging.Logging;
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
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.mapml");

    @Autowired GeoServer geoServer;
    @Autowired WMS wms;

    @RequestMapping(
        value = "/{layer}/{proj}",
        method = {RequestMethod.GET, RequestMethod.POST},
        produces = MAPML_MIME_TYPE
    )
    public Mapml mapML(
            HttpServletRequest request,
            @PathVariable("layer") String layer,
            @PathVariable("proj") String proj,
            @RequestParam("style") Optional<String> style,
            @RequestParam("transparent") Optional<Boolean> transparent,
            @RequestParam("format") Optional<String> format)
            throws NoSuchAuthorityCodeException, TransformException, FactoryException, IOException {
        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(layer);
        if (layerInfo == null) {
            // TODO better error handling
            throw new RuntimeException("Invalid layer name: " + layer);
        }

        ProjType projType;
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            // TODO better error handling
            throw new RuntimeException(iae);
        }

        ResourceInfo resourceInfo = layerInfo.getResource();
        MetadataMap layerMeta = resourceInfo.getMetadata();

        // convert the data's bbox into the lcc projection
        ReferencedEnvelope bbox = resourceInfo.getLatLonBoundingBox();
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
        boolean isTransparent = transparent.orElse(!layerInfo.isOpaque());
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
        head.setTitle(layerInfo.getName());
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
        // output the self style link, taking care to handle the default empty string styleName case
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

        // alternate projection links
        for (ProjType pt : ProjType.values()) {
            // skip the current proj
            if (pt.equals(projType)) continue;
            Link styleLink = new Link();
            styleLink.setRel(RelType.ALTERNATE);
            styleLink.setProjection(pt);
            styleLink.setHref(
                    baseUrl
                            + "mapml/"
                            + layer
                            + "/"
                            + pt.value()
                            + "?style="
                            + styleName
                            + (transparent.isPresent() ? "&transparent=" + isTransparent : "")
                            + (format.isPresent() ? "&format=" + imageFormat : ""));
            links.add(styleLink);
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
                        o.setContent(DATE_FORMAT.format(date));
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
                            + layerInfo.getResource().getStore().getWorkspace().getName()
                            + "/wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
                            + "&layers="
                            + layerInfo.getName()
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
                            + layerInfo.getResource().getStore().getWorkspace().getName()
                            + "/wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
                            + "&layers="
                            + layerInfo.getName()
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
        if (layerInfo.isQueryable()) {
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
                            + layerInfo.getResource().getStore().getWorkspace().getName()
                            + "/wms?version=1.3.0&service=WMS&request=GetFeatureInfo&crs=EPSG:"
                            + projType.epsgCode
                            + "&layers="
                            + layerInfo.getName()
                            + "&query_layers="
                            + layerInfo.getName()
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
