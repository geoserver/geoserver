/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContext;

/**
 * Handles a GetMap request that spects a map in PDF format.
 * 
 * @author Pierre-Emmanuel Balageas, ALCER (http://www.alcer.com)
 * @author Simone Giannecchini - GeoSolutions
 * @author Gabriel Roldan
 * @version $Id$
 */
public class PDFMapOutputFormat extends AbstractMapOutputFormat {

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "application/pdf";

    public static class PDFMap extends org.geoserver.wms.WebMap {

        public PDFMap(final WMSMapContext mapContext) {
            super(mapContext);
        }

        public WMSMapContext getContext() {
            return mapContext;
        }
    }

    public PDFMapOutputFormat() {
        super(MIME_TYPE);
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContext)
     */
    public PDFMap produceMap(final WMSMapContext mapContext) throws ServiceException, IOException {

        PDFMap result = new PDFMap(mapContext);
        result.setContentDispositionHeader(mapContext, ".pdf");
        result.setMimeType(MIME_TYPE);
        return result;
    }

}
