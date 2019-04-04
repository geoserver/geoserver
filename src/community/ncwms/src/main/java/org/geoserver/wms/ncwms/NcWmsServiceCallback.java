/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.util.Map;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/** Hook to replace the service when the GetTimeSeries operation is requested */
public class NcWmsServiceCallback extends AbstractDispatcherCallback {
    private NcWmsService service;

    public NcWmsServiceCallback(final NcWmsService service) {
        this.service = service;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Object req = request.getKvp().get("REQUEST");
        if ("wms".equals(service.getId().toLowerCase())
                && NcWmsService.GET_TIME_SERIES_REQUEST.equals(req)) {
            /*
             * HACK: we are using GetFeatureInfoRequest and GetFeatureInfoKvpReader for parsing a GetTimeSeries. As the valid INFO_FORMATs are
             * different, we need to fool the GetFeatureInfoKvpReader
             */
            Map kvp = request.getKvp();
            String requestedFormat = (String) kvp.get("INFO_FORMAT");
            kvp.put("INFO_FORMAT", "text/plain");
            kvp.put(NcWmsService.TIME_SERIES_INFO_FORMAT_PARAM_NAME, requestedFormat);
            return new Service(
                    service.getId(), this.service, service.getVersion(), service.getOperations());
        }
        return service;
    }
}
