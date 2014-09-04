/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.request.target.component.IBookmarkablePageRequestTarget;
import org.apache.wicket.request.target.component.IPageRequestTarget;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;

/**
 * Protects the catalog and configuration from concurrent access from the web GUI side (will stay
 * here until the catalog and configution will become thread safe).
 * <p>
 * It locks in write mode all {@link GeoServerSecuredPage} subclasses, as those have some
 * possibility to write on the configuration/catalog, all other pages are locked in read mode.
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class WicketConfigurationLockCallback implements WicketCallback {

    GeoServerConfigurationLock locker;

    static ThreadLocal<LockType> THREAD_LOCK = new ThreadLocal<GeoServerConfigurationLock.LockType>();

    public WicketConfigurationLockCallback(GeoServerConfigurationLock locker) {
        this.locker = locker;
    }

    @Override
    public void onBeginRequest() {
        // nothing to do here
    }

    @Override
    public void onAfterTargetsDetached() {
        // who cares?
    }

    @Override
    public void onEndRequest() {
        LockType type = THREAD_LOCK.get();
        if (type != null) {
            THREAD_LOCK.remove();
            locker.unlock(type);
        }
    }

    @Override
    public void onRequestTargetSet(IRequestTarget target) {
        // we can have many of these calls per http call, avoid locking multiple times,
        // onEndRequest will be called just once
        LockType type = THREAD_LOCK.get();
        if (type != null) {
            return;
        }

        // setup a write lock for secured pages, a read one for the others
        if (target instanceof IPageRequestTarget) {
            IPageRequestTarget pt = (IPageRequestTarget) target;
            Page page = pt.getPage();
            if (page instanceof GeoServerSecuredPage) {
                type = LockType.WRITE;
            }
        } else if (target instanceof IBookmarkablePageRequestTarget) {
            IBookmarkablePageRequestTarget bt = (IBookmarkablePageRequestTarget) target;

            if (GeoServerSecuredPage.class.isAssignableFrom(bt.getPageClass())) {
                type = LockType.WRITE;
            }
        }
        if (type == null) {
            type = LockType.READ;
        }

        // and lock
        THREAD_LOCK.set(type);
        locker.lock(type);
    }

    @Override
    public void onRuntimeException(Page page, RuntimeException e) {
        // nothing to do
    }

}
