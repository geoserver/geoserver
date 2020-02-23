/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.impl.WrappingProxy;

/**
 * Performs lock upgrades on the fly
 *
 * @author Andrea Aime - GeoSolutions
 */
public class LockingCatalogFacade implements InvocationHandler, WrappingProxy {

    GeoServerConfigurationLock configurationLock;
    CatalogFacade delegate;

    public LockingCatalogFacade(
            CatalogFacade delegate, GeoServerConfigurationLock configurationLock) {
        this.configurationLock = configurationLock;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LockType lockType = configurationLock.getCurrentLock();
        if (lockType == LockType.READ && isWriteMethod(method)) {
            configurationLock.tryUpgradeLock();
        }
        return method.invoke(delegate, args);
    }

    private boolean isWriteMethod(Method method) {
        final String name = method.getName();
        // ignoring setCatalog because it does not actually happens during
        return name.startsWith("set")
                || name.startsWith("remove")
                || name.startsWith("add")
                || name.startsWith("save");
    }

    /**
     * Returns a wrapped {@link CatalogFacade} that will upgrade read locks to write before
     * attempting any write operation
     */
    public static CatalogFacade create(
            CatalogFacade facade, GeoServerConfigurationLock configurationLock) {
        return ProxyUtils.createProxy(
                facade, CatalogFacade.class, new LockingCatalogFacade(facade, configurationLock));
    }

    @Override
    public Object getProxyObject() {
        return delegate;
    }
}
