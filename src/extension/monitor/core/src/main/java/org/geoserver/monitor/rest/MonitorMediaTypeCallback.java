/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.util.Map;
import org.geoserver.rest.MediaTypeCallback;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

@Component
public class MonitorMediaTypeCallback implements MediaTypeCallback {

    @Override
    public void configure(ContentNegotiationConfigurer configurer) {
        // register media types via the new Spring API
        configurer.mediaType("csv", MonitorRequestController.CSV_MEDIATYPE);
        configurer.mediaType("zip", MonitorRequestController.ZIP_MEDIATYPE);
        configurer.mediaType("xls", MonitorRequestController.EXCEL_MEDIATYPE);
    }

    /**
     * Keep the old Map-based method for compatibility with older API variants. Note: not annotated with @Override
     * because the interface now uses the ContentNegotiationConfigurer variant.
     */
    public void configure(Map<String, MediaType> mediaTypes) {
        mediaTypes.put("csv", MonitorRequestController.CSV_MEDIATYPE);
        mediaTypes.put("zip", MonitorRequestController.ZIP_MEDIATYPE);
        mediaTypes.put("xls", MonitorRequestController.EXCEL_MEDIATYPE);
    }
}
