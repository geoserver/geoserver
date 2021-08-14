/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_MIME_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.Point;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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

/**
 * Spring MVC controller for MapML requests
 *
 * @author Chris Hodgson
 * @author prushforth
 *     <p>This controller has two methods which map requests for layers depending on the MIME media
 *     type requested by the client. The first returns HTML (text/html) representing a layer
 *     preview. This preview uses the Web-Map-Custom-Element viewer polyfill, which is bundled as
 *     static resources in the built project. The second method returns MapML (text/mapml) for a
 *     layer, which is how the layer polyfill embedded in the text/html response "requests itself".
 */
@RestController
@RequestMapping(path = "/mapml")
@CrossOrigin
public class MapMLController {

    @Autowired GeoServer geoServer;
    @Autowired WMS wms;

    public static final HashMap<String, TiledCRS> previewTcrsMap = new HashMap<>();

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

    /**
     * Return an HTML representation of a layer, by embedding the Web-Map-Custom-Element viewer
     * reference in the generated HTML.
     *
     * @param request
     * @param response
     * @param layer layer or layer group name
     * @param proj TCRS name
     * @param style A named WMS style
     * @param transparent boolean corresponding to WMS' transparent parameter
     * @param format string corresponding to WMS' format parameter
     * @return a text/html representation
     */
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
        boolean isLayerGroup = (layerInfo == null);
        String layerName = "";
        String styleName = style.orElse("");
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
        TiledCRS TCRS = previewTcrsMap.get(projType.value());

        Double longitude = bbox.centre().getX();
        Double latitude = bbox.centre().getY();

        final ReferencedEnvelope bbbox;
        int zoom = 0;
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

        } catch (Exception e) {
            // what to do?
            // log.info("Exception occured retrieving bbox for "+ layer
            // );
        }
        String base = ResponseUtils.baseURL(request);
        String viewerPath =
                ResponseUtils.buildURL(
                        base,
                        "/mapml/viewer/widget/mapml-viewer.js",
                        null,
                        URLMangler.URLType.RESOURCE);
        String title = "GeoServer MapML preview " + layerName;
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>")
                .append(title)
                .append("</title>\n")
                .append("<meta charset='utf-8'>\n")
                .append("<script type=\"module\"  src=\"")
                .append(viewerPath)
                .append("\"></script>\n")
                .append("<style>\n")
                .append("html, body { height: 100%; }\n")
                .append("* { margin: 0; padding: 0; }\n")
                .append("mapml-viewer:defined { max-width: 100%; width: 100%; height: 100%; }\n")
                .append("mapml-viewer:not(:defined) > * { display: none; } n")
                .append("layer- { display: none; }\n")
                .append("</style>\n")
                .append("<noscript>\n")
                .append("<style>\n")
                .append("mapml-viewer:not(:defined) > :not(layer-) { display: initial; }\n")
                .append("</style>\n")
                .append("</noscript>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<mapml-viewer projection=\"")
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
                .append("</mapml-viewer>\n")
                .append("</body>\n")
                .append("</html>");
        return sb.toString();
    }

    /**
     * Return a MapML representation of a layer
     *
     * @param request
     * @param response
     * @param layer
     * @param proj
     * @param style
     * @param transparent boolean corresponding to WMS' transparent parameter
     * @param format string corresponding to WMS' format parameter
     * @return a text/mapml representation
     * @throws NoSuchAuthorityCodeException - NoSuchAuthorityCodeException
     * @throws TransformException - TransformException
     * @throws FactoryException - FactoryException
     * @throws IOException - IOException
     */
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
        MapMLDocumentBuilder mb =
                new MapMLDocumentBuilder(
                        this, request, response, layer, proj, style, transparent, format);
        return mb.getMapMLDocument();
    }
}
