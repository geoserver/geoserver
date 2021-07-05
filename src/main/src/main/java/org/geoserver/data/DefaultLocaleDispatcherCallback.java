/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.util.Locale;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.GeoServerDefaultLocale;

/**
 * Abstract DispatcherCallback that set the default locale for the current thread.
 *
 * @param <T> the specific {@link ServiceInfo} implementation
 */
public abstract class DefaultLocaleDispatcherCallback<T extends ServiceInfo>
        extends AbstractDispatcherCallback {

    protected GeoServer geoServer;

    public DefaultLocaleDispatcherCallback(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        ServiceInfo serviceInfo = getService(request);
        Locale defaultLocale = null;

        // check if a default locale was set in the ServiceInfo configuration.
        if (serviceInfo != null) defaultLocale = serviceInfo.getDefaultLocale();

        // check if the default locale was set in the Global settings.
        if (defaultLocale == null) defaultLocale = geoServer.getSettings().getDefaultLocale();

        if (defaultLocale != null) GeoServerDefaultLocale.set(defaultLocale);
        return super.serviceDispatched(request, service);
    }

    @Override
    public void finished(Request request) {
        GeoServerDefaultLocale.remove();
        super.finished(request);
    }

    /**
     * @param request the {@link Request} object
     * @return the ServiceInfo implementation. Subclasses must ovveride it.
     */
    protected abstract T getService(Request request);
}
