/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.util.Map;
import org.geoserver.rest.MediaTypeCallback;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class MonitorMediaTypeCallback implements MediaTypeCallback {

    @Override
    public void configure(Map<String, MediaType> mediaTypes) {
        mediaTypes.put("csv", MonitorRequestController.CSV_MEDIATYPE);
        mediaTypes.put("zip", MonitorRequestController.ZIP_MEDIATYPE);
        mediaTypes.put("xls", MonitorRequestController.EXCEL_MEDIATYPE);
    }
}
