/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.CatalogReloadController;
import org.geoserver.rest.DispatcherCallback;
import org.springframework.stereotype.Controller;

/**
 * Protects catalog access from concurrent rest configuration calls. Will lock in write mode every
 * call modifying catalog resources, in read mode all others catalog resource related calls, no
 * locks will be performed on other rest requests.
 *
 * @author Andrea Aime - GeoSolutions
 */
@Controller
public class RestConfigurationLockCallback implements DispatcherCallback {

    GeoServerConfigurationLock locker;

    public RestConfigurationLockCallback(GeoServerConfigurationLock locker) {
        this.locker = locker;
    }

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        LockType type = locker.getCurrentLock();
        if (type != null) {
            throw new RuntimeException("The previous lock was not released: " + type);
        }
    }

    @Override
    public void dispatched(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object controller = DispatcherCallback.getControllerBean(handler);
        if (controller instanceof AbstractCatalogController
                || controller instanceof AbstractGeoServerController) {
            if (controller instanceof CatalogReloadController
                    || isWriteMethod(request.getMethod())) {
                // this requires a full lock, it affects part of GeoTools that are not thread safe
                locker.lock(LockType.WRITE);
            } else {
                locker.lock(LockType.READ);
            }
        }
    }

    private boolean isWriteMethod(String method) {
        return "PUT".equalsIgnoreCase(method)
                || "POST".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    @Override
    public void exception(
            HttpServletRequest request, HttpServletResponse response, Exception error) {
        // nothing to see here, move on
    }

    @Override
    public void finished(HttpServletRequest request, HttpServletResponse response) {
        LockType type = locker.getCurrentLock();
        if (type != null) {
            locker.unlock();
        }
    }
}
