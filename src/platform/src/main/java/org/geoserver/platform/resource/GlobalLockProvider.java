/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import org.geoserver.platform.resource.Resource.Lock;

/**
 * A lock provider that delegates the work to another {@link LockProvider} instance, which needs to
 * be configured by calling {@link #setDelegate(LockProvider)}.
 *
 * <p>GeoServer is configured with a single globalLockProvider for use by the application. This
 * instance is configured with an appropriate bean instance as configured by the user.
 *
 * @author Andrea Aime - GeoSolutions
 * @author Jody Garnett (Boundless)
 */
public class GlobalLockProvider implements LockProvider {

    LockProvider delegate = new NullLockProvider();

    /**
     * Delegate used for lock creation
     *
     * @return delegate used for lock creation
     */
    public LockProvider getDelegate() {
        return delegate;
    }

    /**
     * Delegate to use for lock creation.
     *
     * @param delegate LockProvider configured for lock creation
     */
    public void setDelegate(LockProvider delegate) {
        if (delegate == null) {
            throw new NullPointerException("LockProvider delegate required");
        }
        this.delegate = delegate;
    }

    @Override
    public Lock acquire(String path) {
        return delegate.acquire(path);
    }

    @Override
    public String toString() {
        if (delegate instanceof NullLockProvider) {
            return "GlobalLock Provider";
        }
        return "GlobalLock Provider (" + delegate + ")";
    }
}
