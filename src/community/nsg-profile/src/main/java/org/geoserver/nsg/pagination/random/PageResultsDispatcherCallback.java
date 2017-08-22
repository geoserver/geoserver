/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.util.Collections;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/**
 *
 * This dispatcher manages service of type {@link PageResultsWebFeatureService} and sets the
 * parameter ResultSetID present on KVP map.
 * <p>
 * Dummy featureId value is added to KVP map to allow dispatcher to manage it as usual WFS 2.0
 * request.
 *
 * @author sandr
 * 
 */

public class PageResultsDispatcherCallback extends AbstractDispatcherCallback {

    private final static Logger LOGGER = Logging.getLogger(PageResultsDispatcherCallback.class);

    private GeoServer gs;

    public PageResultsDispatcherCallback(GeoServer gs) {
        this.gs = gs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        if (service.getService() instanceof PageResultsWebFeatureService) {
            PageResultsWebFeatureService prService = (PageResultsWebFeatureService) service
                    .getService();
            String resultSetId = (String) request.getKvp().get("resultSetID");
            prService.setResultSetId(resultSetId);
            request.getKvp().put("featureId", Collections.singletonList("dummy"));

        }
        return super.serviceDispatched(request, service);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Operation newOperation = operation;
        // Change operation from PageResults to GetFeature to allow management of request as
        // standard get feature
        if (operation.getId().equals("PageResults")) {
            newOperation = new Operation("GetFeature", operation.getService(),
                    operation.getMethod(), operation.getParameters());
        }
        return super.operationDispatched(request, newOperation);
    }

}
