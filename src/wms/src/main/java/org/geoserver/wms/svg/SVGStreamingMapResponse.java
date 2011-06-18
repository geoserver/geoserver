/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;

/**
 * Handles a GetMap request that expects a map in SVG format.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * @see SVGStreamingMapResponse
 * @see StreamingSVGMap
 */
public final class SVGStreamingMapResponse extends AbstractMapResponse {

    public SVGStreamingMapResponse() {
        super(StreamingSVGMap.class, SVG.OUTPUT_FORMATS);
    }

    /**
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        StreamingSVGMap map = (StreamingSVGMap) value;
        try {
            map.encode(output);
        } finally {
            map.dispose();
        }
    }
}
