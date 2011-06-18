package org.geoserver.wms.svg;

import org.apache.batik.svggen.SVGGraphics2D;
import org.geoserver.wms.WMSMapContext;

public class BatikSVGMap extends org.geoserver.wms.WebMap {
    private SVGGraphics2D graphics;

    BatikSVGMap(WMSMapContext context, SVGGraphics2D graphics) {
        super(context);
        this.graphics = graphics;
        setMimeType(SVG.MIME_TYPE);
    }

    public SVGGraphics2D getGraphics() {
        return graphics;
    }

    @Override
    public void disposeInternal() {
        graphics.dispose();
    }
}