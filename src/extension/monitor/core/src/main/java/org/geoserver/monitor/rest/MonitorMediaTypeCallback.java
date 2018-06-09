/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import org.geoserver.rest.MediaTypeCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

@Component
public class MonitorMediaTypeCallback implements MediaTypeCallback {

    @Override
    public void configure(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("csv", MonitorRequestController.CSV_MEDIATYPE);
        configurer.mediaType("zip", MonitorRequestController.ZIP_MEDIATYPE);
        configurer.mediaType("xls", MonitorRequestController.EXCEL_MEDIATYPE);
    }
}
