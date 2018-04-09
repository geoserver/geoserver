/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

import java.util.function.Supplier;

public class WFS3DispatcherCallback extends AbstractDispatcherCallback {

    private Lazy<Service> wfs3 = new Lazy<>();
    private Lazy<Service> fallback = new Lazy<>();

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        Service wfs3 = this.wfs3.getOrCompute(() -> (Service) GeoServerExtensions.bean
                ("wfsService-3.0"));
        Service fallback = this.fallback.getOrCompute(() -> (Service) GeoServerExtensions.bean
                ("wfsService-2.0"));
        if (wfs3.equals(service) && "GetCapabilities".equals(request.getRequest())) {
            request.setServiceDescriptor(fallback);
            return fallback;
        }
        return service;
    }

    /**
     * Thread safe lazy calculation, used to avoid bean circular dependencies
     * @param <T>
     */
    public final class Lazy<T> {
        private volatile T value;

        public T getOrCompute(Supplier<T> supplier) {
            final T result = value; // Just one volatile read
            return result == null ? maybeCompute(supplier) : result;
        }

        private synchronized T maybeCompute(Supplier<T> supplier) {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
