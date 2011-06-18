/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.batik.svggen.SVGGraphics2D;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;

/**
 * Renders svg using the Batik SVG Toolkit. An SVG context is created for a map and then passed of
 * to {@link org.geotools.renderer.lite.StreamingRenderer}.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public final class SVGBatikMapResponse extends AbstractMapResponse {

    public SVGBatikMapResponse() {
        super(BatikSVGMap.class, SVG.OUTPUT_FORMATS);
    }

    /**
     * 
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {

        BatikSVGMap map = (BatikSVGMap) value;
        try {
            SVGGraphics2D graphics = map.getGraphics();
            graphics.stream(new OutputStreamWriter(output, "UTF-8"));
        } finally {
            map.dispose();
        }
    }
}
