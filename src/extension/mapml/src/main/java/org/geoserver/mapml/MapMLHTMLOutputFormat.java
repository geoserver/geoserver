/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/** Handles a GetMap request that for a map in MapML HTML format. */
public class MapMLHTMLOutputFormat implements GetMapOutputFormat {
    private WMS wms;
    private final Set<String> OUTPUT_FORMATS =
            Collections.unmodifiableSet(new HashSet<>(List.of(MapMLConstants.MAPML_HTML_MIME_TYPE)));
    static final MapProducerCapabilities MAPML_CAPABILITIES = new MapProducerCapabilities(false, true, true);

    /**
     * Constructor
     *
     * @param wms the WMS
     */
    public MapMLHTMLOutputFormat(WMS wms) {
        this.wms = wms;
    }

    @Override
    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        Request request = Dispatcher.REQUEST.get();
        HttpServletRequest httpServletRequest = request.getHttpRequest();
        MapMLDocumentBuilder mapMLDocumentBuilder = new MapMLDocumentBuilder(mapContent, wms, httpServletRequest);
        return new MapMLHTMLMap(mapContent, mapMLDocumentBuilder.getMapMLHTMLDocument());
    }

    @Override
    public Set<String> getOutputFormatNames() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MapMLConstants.MAPML_HTML_MIME_TYPE;
    }

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return MAPML_CAPABILITIES;
    }
}
