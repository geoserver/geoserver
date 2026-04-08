/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import org.apache.batik.svggen.SVGGraphics2D;
import org.geoserver.wms.WMSMapContent;

public class BatikSVGMap extends org.geoserver.wms.WebMap {
    private SVGGraphics2D graphics;

    BatikSVGMap(WMSMapContent context, SVGGraphics2D graphics) {
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
