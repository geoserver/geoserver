/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.MapMLProjection;
import org.geoserver.mapml.tcrs.Point;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.proj.PROJFormattable;
import org.geotools.referencing.proj.PROJFormatter;

/** Class delegated to build an HTML Document embedding a MapML Viewer. */
public class MapMLHTMLOutput {

    private String layerLabel;
    private HttpServletRequest request;
    private MapMLProjection projType;
    private String sourceUrL;
    private int zoom = 0;
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    private ReferencedEnvelope projectedBbox;
    private String templateHeader;

    private MapMLHTMLOutput(HTMLOutputBuilder builder) {
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.zoom = builder.zoom;
        this.layerLabel = builder.layerLabel;
        this.request = builder.request;
        this.projType = builder.projType;
        this.sourceUrL = builder.sourceUrL;
        this.projectedBbox = builder.projectedBbox;
        this.templateHeader = builder.templateHeader;
    }

    public static class HTMLOutputBuilder {
        private String layerLabel;
        private HttpServletRequest request;
        private MapMLProjection projType;
        private String sourceUrL;
        private ReferencedEnvelope projectedBbox;
        private int zoom = 0;
        private Double latitude = 0.0;
        private Double longitude = 0.0;
        private String templateHeader = "";

        public HTMLOutputBuilder setLayerLabel(String layerLabel) {
            this.layerLabel = layerLabel;
            return this;
        }

        public HTMLOutputBuilder setRequest(HttpServletRequest request) {
            this.request = request;
            return this;
        }

        public HTMLOutputBuilder setProjType(MapMLProjection projType) {
            this.projType = projType;
            return this;
        }

        public HTMLOutputBuilder setSourceUrL(String sourceUrL) {
            this.sourceUrL = sourceUrL;
            return this;
        }

        public HTMLOutputBuilder setTemplateHeader(String templateHeader) {
            this.templateHeader = templateHeader;
            return this;
        }

        public HTMLOutputBuilder setProjectedBbox(ReferencedEnvelope projectedBbox) {
            this.projectedBbox = projectedBbox;
            return this;
        }

        public HTMLOutputBuilder setLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public HTMLOutputBuilder setLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public MapMLHTMLOutput build() {
            return new MapMLHTMLOutput(this);
        }
    }

    public String toHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>")
                .append(escapeHtml4(layerLabel))
                .append("</title>\n")
                .append("<meta charset='utf-8'>\n")
                .append("<script type=\"module\"  src=\"")
                .append(buildViewerPath(request, "viewer/widget/mapml.js"))
                .append("\"></script>\n")
                .append("<style>\n")
                .append("html, body { height: 100%; }\n")
                .append("* { margin: 0; padding: 0; }\n")
                .append(
                        "mapml-viewer:defined { max-width: 100%; width: 100%; height: 100%; border: none; vertical-align: middle }\n")
                .append("mapml-viewer:not(:defined) > * { display: none; } n")
                .append("map-layer { display: none; }\n")
                .append("</style>\n");
        appendProjectionScript(projType, sb);
        zoom = computeZoom(projType, projectedBbox);
        sb.append("<noscript>\n")
                .append("<style>\n")
                .append("mapml-viewer:not(:defined) > :not(map-layer) { display: initial; }\n")
                .append("</style>\n")
                .append("</noscript>\n")
                .append(templateHeader)
                .append("</head>\n")
                .append("<body>\n")
                .append("<mapml-viewer projection=\"")
                .append(projType.getTiledCRS().getParams().getName())
                .append("\" ")
                .append("zoom=\"")
                .append(zoom)
                .append("\" lat=\"")
                .append(latitude)
                .append("\" ")
                .append("lon=\"")
                .append(longitude)
                .append("\" controls controlslist=\"geolocation\">\n")
                .append("<map-layer label=\"")
                .append(escapeHtml4(layerLabel))
                .append("\" ")
                .append("src=\"")
                .append(sourceUrL)
                .append("\" checked></map-layer>\n")
                .append("</mapml-viewer>\n");
        appendProjectionText(projType, sb);
        sb.append("</body>\n").append("</html>");
        return sb.toString();
    }

    private void appendProjectionScript(MapMLProjection projType, StringBuilder sb) {
        if (!projType.isBuiltIn()) {
            sb.append("<script type=\"module\" src=\"")
                    .append(buildViewerPath(request, "js/custom-projection.js"))
                    .append("\"></script>\n");
        }
    }

    private void appendProjectionText(MapMLProjection projType, StringBuilder sb) {
        if (!projType.isBuiltIn()) {
            sb.append("<textarea id=\"customProjection\" style=\"display: none;\">\n")
                    .append(escapeHtml4(buildDefinition(projType.getTiledCRS(), 10)))
                    .append("</textarea>");
        }
    }

    private String buildDefinition(TiledCRS tiledCRS, int indentChars) {
        TiledCRSParams params = tiledCRS.getParams();
        int tileSize = params.getTILE_SIZE();
        String name = params.getName();
        Point origin = params.getOrigin();
        String indent = " ".repeat(indentChars);
        String originString = String.format(Locale.ENGLISH, "[%.8f, %.8f]", origin.getX(), origin.getY());

        double[] resolutions = params.getResolutions();
        StringBuilder resolutionsString = new StringBuilder("[");
        for (int i = 0; i < resolutions.length; i++) {
            resolutionsString.append(resolutions[i]);
            if (i != resolutions.length - 1) {
                resolutionsString.append(", ");
            }
        }
        resolutionsString.append("]");

        Bounds bounds = params.getBounds();
        String boundsString = String.format(
                Locale.ENGLISH,
                "[[%.8f, %.8f], [%.8f, %.8f]]",
                bounds.getMin().getX(),
                bounds.getMin().getY(),
                bounds.getMax().getX(),
                bounds.getMax().getY());

        CoordinateReferenceSystem crs = tiledCRS.getCRS();
        PROJFormatter formatter = new PROJFormatter();
        String projString = formatter.toPROJ((PROJFormattable) crs);
        StringBuilder sb = new StringBuilder("{\n")
                .append("\"projection\": \"")
                .append(name)
                .append("\",\n")
                .append(indent)
                .append("\"origin\": ")
                .append(originString)
                .append(",\n")
                .append(indent)
                .append("\"resolutions\": ")
                .append(resolutionsString)
                .append(",\n")
                .append(indent)
                .append("\"bounds\": ")
                .append(boundsString)
                .append(",\n")
                .append(indent)
                .append("\"tilesize\": ")
                .append(tileSize)
                .append(",\n")
                .append(indent)
                .append("\"proj4string\" : \"")
                .append(projString)
                .append("\"\n")
                .append("}\n");
        return sb.toString();
    }

    private String buildViewerPath(HttpServletRequest request, String path) {
        String base = ResponseUtils.baseURL(request);
        return ResponseUtils.buildURL(base, "/mapml/" + path, null, URLMangler.URLType.RESOURCE);
    }

    public static int computeZoom(
            MapMLProjection projType, ReferencedEnvelope projectedBbox, double pixelWidth, double pixelHeight) {
        TiledCRS tcrs = projType.getTiledCRS();
        boolean flipAxis =
                CRS.getAxisOrder(projectedBbox.getCoordinateReferenceSystem()).equals(CRS.AxisOrder.NORTH_EAST);
        double minX = flipAxis ? projectedBbox.getMinY() : projectedBbox.getMinX();
        double maxX = flipAxis ? projectedBbox.getMaxY() : projectedBbox.getMaxX();
        double minY = flipAxis ? projectedBbox.getMinX() : projectedBbox.getMinY();
        double maxY = flipAxis ? projectedBbox.getMaxX() : projectedBbox.getMaxY();
        final Bounds pb = new Bounds(new Point(minX, minY), new Point(maxX, maxY));
        // allowing for the data to be displayed at a certain size (WxH) in pixels,
        // figure out the zoom level at which the projected bounds fits into that,
        // in both dimensions
        Bounds displayBounds = new Bounds(new Point(0, 0), new Point(pixelWidth, pixelHeight));
        return tcrs.fitProjectedBoundsToDisplay(pb, displayBounds);
    }

    public static int computeZoom(MapMLProjection projType, ReferencedEnvelope projectedBbox) {
        double width = MapMLConstants.DISPLAY_BOUNDS_DESKTOP_LANDSCAPE.getMax().getX();
        double height = MapMLConstants.DISPLAY_BOUNDS_DESKTOP_LANDSCAPE.getMax().getY();
        return computeZoom(projType, projectedBbox, width, height);
    }
}
