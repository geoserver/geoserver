/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContext;
import org.springframework.util.Assert;

/**
 * Handles a GetMap request that produces a map in HTMLImageMap format.
 * 
 * @author Mauro Bartolomeoli
 */
public class HTMLImageMapMapProducer implements GetMapOutputFormat {

    /**
     * The ImageMap is served as text/html: it is an HTML fragment, after all.
     */
    static final String MIME_TYPE = "text/html";

    static final MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(false, false,
            true, true, null);

    public HTMLImageMapMapProducer() {
        //
    }

    /**
     * Renders the map.
     * 
     * @throws ServiceException if an error occurs during rendering
     * @see GetMapOutputFormat#produceMap(WMSMapContext)
     */
    public EncodeHTMLImageMap produceMap(WMSMapContext mapContext) throws ServiceException,
            IOException {
        Assert.notNull(mapContext, "mapContext is not set");
        return new EncodeHTMLImageMap(mapContext);
    }

    /**
     * @see GetMapOutputFormat#getOutputFormatNames()
     * @see #getMimeType()
     */
    public Set<String> getOutputFormatNames() {
        return Collections.singleton(MIME_TYPE);
    }

    /**
     * @return {@code text/html}
     * @see GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return MIME_TYPE;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }

}
