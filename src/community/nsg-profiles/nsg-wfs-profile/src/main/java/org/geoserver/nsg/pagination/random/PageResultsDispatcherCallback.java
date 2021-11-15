/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * This dispatcher manages service of type {@link PageResultsWebFeatureService} and sets the
 * parameter ResultSetID present on KVP map.
 *
 * <p>Dummy featureId value is added to KVP map to allow dispatcher to manage it as usual WFS 2.0
 * request.
 *
 * @author sandr
 */
public class PageResultsDispatcherCallback extends AbstractDispatcherCallback
        implements ApplicationListener<ContextRefreshedEvent> {

    static final String PAGE_RESULTS = "PageResults";

    private static final Logger LOGGER = Logging.getLogger(PageResultsDispatcherCallback.class);
    private final PageResultsWebFeatureService service;
    private GeoServer gs;

    public PageResultsDispatcherCallback(GeoServer gs, PageResultsWebFeatureService service) {
        this.gs = gs;
        this.service = service;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Object req = request.getKvp().get("REQUEST");
        if ("wfs".equals(service.getId().toLowerCase()) && PAGE_RESULTS.equals(req)) {
            // allow the request to be successfully parsed as a GetFeature (needs at least a
            // typename or a featureId)
            request.getKvp().put("featureId", Collections.singletonList("dummy"));
            // replace the service
            return new Service(
                    service.getId(), this.service, service.getVersion(), service.getOperations());
        }
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Operation newOperation = operation;
        // Change operation from PageResults to GetFeature to allow management of request as
        // standard get feature
        if (operation.getId().equals("PageResults")) {
            newOperation =
                    new Operation(
                            "GetFeature",
                            operation.getService(),
                            operation.getMethod(),
                            operation.getParameters());
        }
        return super.operationDispatched(request, newOperation);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // configure the extra operation in WFS 2.0
        List<Service> services = GeoServerExtensions.extensions(Service.class);
        for (Service s : services) {
            if ("wfs".equals(s.getId().toLowerCase())
                    && Integer.valueOf(2).equals(s.getVersion().getMajor())) {
                if (!s.getOperations().contains(PAGE_RESULTS)) {
                    s.getOperations().add(PAGE_RESULTS);
                }
            }
        }
    }
}
