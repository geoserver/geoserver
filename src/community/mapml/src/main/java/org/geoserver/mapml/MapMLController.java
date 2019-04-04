package org.geoserver.mapml;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
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
import org.geoserver.mapml.xml.UnitType;
import org.geoserver.ows.util.ResponseUtils;
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

    @RequestMapping(
        value = "/{layer}/{proj}",
        method = {RequestMethod.GET, RequestMethod.POST},
        produces = MapMLConstants.MIME_TYPE
    )
    public Mapml mapML(
            HttpServletRequest request,
            @PathVariable("layer") String layer,
            @PathVariable("proj") String proj,
            @RequestParam("style") Optional<String> style)
            throws NoSuchAuthorityCodeException, TransformException, FactoryException {
        LayerInfo layerInfo = geoServer.getCatalog().getLayerByName(layer);
        if (layerInfo == null) {
            // TODO error handling
            throw new RuntimeException("Invalid layer name: " + layer);
        }

        ProjType projType;
        try {
            projType = ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException iae) {
            // TODO error handling
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
        String baseUrl = ResponseUtils.baseURL(request);
        String baseUrlPattern = baseUrl;
        
        // handle shard config
        Boolean enableSharding = layerMeta.get("mapml.enableSharding", Boolean.class);
        String shardListString = layerMeta.get("mapml.shardList", String.class);
        String[] shardArray = new String[0];
        if(shardListString != null) {
            shardArray = shardListString.split("[,\\s]+");
        }
        String shardServerPattern = layerMeta.get("mapml.shardServerPattern", String.class);
        if(shardArray.length < 1 || shardServerPattern == null || shardServerPattern.isEmpty()) {
            enableSharding = Boolean.FALSE;
        }
        // if we have a valid shard config
        if(Boolean.TRUE.equals(enableSharding)) {
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
        meta.setContent(MapMLConstants.MIME_TYPE + ";projection=" + projType.value());
        metas.add(meta);
        List<Link> links = head.getLinks();

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link link = new Link();
            link.setRel(RelType.LICENSE);
            if (licenseLink != null) {
                link.setHref(licenseLink);
            }
            if (licenseTitle != null) {
                link.setTitle(licenseTitle);
            }
            links.add(link);
        }
        // styles
        Set<StyleInfo> styles = layerInfo.getStyles();
        if (styles.size() > 1) {
            for (StyleInfo si : styles) {
                Link link = new Link();
                if (si.getName().equals(styleName)) {
                    link.setRel(RelType.SELF_STYLE);
                } else {
                    link.setRel(RelType.STYLE);
                }
                link.setHref(baseUrl + "mapml/" + layer + "/" + proj + "?style=" + si.getName());
                link.setTitle(si.getName());
                links.add(link);
            }
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
        if(Boolean.TRUE.equals(enableSharding)) {
            input = new Input();
            input.setName("s");
            input.setType(InputType.HIDDEN);
            input.setShard("true");
            input.setList("servers");
            extentList.add(input);
            Datalist datalist = new Datalist();
            datalist.setId("servers");
            List<Option> options = datalist.getOptions();
            for(int s = 0; s < shardArray.length; s++) {
                Option o = new Option();
                o.setValue(shardArray[s]);
                options.add(o);
            }
            extentList.add(datalist);
        }
        
        String dimension = layerMeta.get("mapml.dimension", String.class);
        if("Time".equalsIgnoreCase(dimension)) {
            
        } else if("Elevation".equalsIgnoreCase(dimension)) {
            
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
            input.setPosition(PositionType.TOP_LEFT); // TODO why is this the same as for xmin?
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // tile link
            Link link = new Link();
            link.setRel(RelType.TILE);
            link.setTref(
                    baseUrlPattern
                            + layerInfo.getResource().getStore().getWorkspace().getName()
                            + "/wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
                            + "&layers="
                            + layerInfo.getName()
                            + "&styles="
                            + styleName
                            + "&bbox={txmin},{tymin},{txmax},{tymax}&format=image/png&transparent=false&width=256&height=256");
            extentList.add(link);
        } else {
            // image inputs
            // xmin
            input = new Input();
            input.setName("xmin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS); // TODO
            input.setPosition(PositionType.TOP_LEFT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // ymin
            input = new Input();
            input.setName("ymin");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS); // TODO
            input.setPosition(PositionType.BOTTOM_LEFT);
            input.setAxis(AxisType.NORTHING);
            input.setMin(cbmBbox.getMinY());
            input.setMax(cbmBbox.getMaxY());
            extentList.add(input);

            // xmax
            input = new Input();
            input.setName("xmax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS); // TODO
            input.setPosition(PositionType.TOP_RIGHT);
            input.setAxis(AxisType.EASTING);
            input.setMin(cbmBbox.getMinX());
            input.setMax(cbmBbox.getMaxX());
            extentList.add(input);

            // ymax
            input = new Input();
            input.setName("ymax");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.PCRS); // TODO
            input.setPosition(PositionType.TOP_LEFT); // TODO why is this the same as for xmin?
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
            Link link = new Link();
            link.setRel(RelType.IMAGE);
            link.setTref(
                    baseUrlPattern
                            + layerInfo.getResource().getStore().getWorkspace().getName()
                            + "/wms?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:"
                            + projType.epsgCode
                            + "&layers="
                            + layerInfo.getName()
                            + "&styles="
                            + styleName
                            + "&bbox={xmin},{ymin},{xmax},{ymax}&format=image/png&transparent=false&width={w}&height={h}");
            extentList.add(link);
        }

        // query inputs
        if (layerInfo.isQueryable()) {
            // query i value (x)
            input = new Input();
            input.setName("i");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.MAP); // TODO
            input.setAxis(AxisType.I);
            extentList.add(input);

            // query j value (y)
            input = new Input();
            input.setName("j");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.MAP); // TODO
            input.setAxis(AxisType.J);
            extentList.add(input);

            // query link
            Link link = new Link();
            link.setRel(RelType.QUERY);
            link.setTref(
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
                            + "&bbox={xmin},{ymin},{xmax},{ymax}"
                            + "&info_format=text/mapml"
                            + "&transparent=false"
                            + "&width={w}&height={h}"
                            + "&x={i}&y={j}");
            extentList.add(link);
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
