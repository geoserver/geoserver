/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/** GeoSever initialization hook to register the non-standard ncWMS methods in the WMS Service */
public class NcWmsInitializer implements ApplicationListener<ContextRefreshedEvent> {
    public static final String GET_TIME_SERIES_OP_NAME = "GetTimeSeries";

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        List<Service> services = GeoServerExtensions.extensions(Service.class);
        for (Service s : services) {
            if ("wms".equals(s.getId().toLowerCase())) {
                if (!s.getOperations().contains(GET_TIME_SERIES_OP_NAME)) {
                    s.getOperations().add(GET_TIME_SERIES_OP_NAME);
                }
            }
        }
    }
}
