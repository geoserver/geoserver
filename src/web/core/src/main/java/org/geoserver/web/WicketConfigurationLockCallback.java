/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;

/**
 * Protects the catalog and configuration from concurrent access from the web GUI side (will stay
 * here until the catalog and configution will become thread safe).
 *
 * <p>It locks in write mode all {@link GeoServerSecuredPage} subclasses, as those have some
 * possibility to write on the configuration/catalog, all other pages are locked in read mode.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WicketConfigurationLockCallback implements WicketCallback {

    GeoServerConfigurationLock locker;

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
        // the code will just skip if no lock is owned
        locker.unlock();
    }

    @Override
    public void onRequestTargetSet(
            RequestCycle cycle, Class<? extends IRequestablePage> requestTarget) {

        if (!GeoServerUnlockablePage.class.isAssignableFrom(requestTarget)) {
            LockType type = locker.getCurrentLock();
            if (type != null || requestTarget == null) {
                return;
            }

            boolean lockTaken = false;
            if (type == null) {
                // lock read mode, it will be upgraded to write as soon
                // as a write operation on the catalog is attempted
                lockTaken = locker.tryLock(LockType.READ);
            }

            // Check if the configuration is locked and the page is safe...
            if (cycle != null && !lockTaken) {
                cycle.setResponsePage(ServerBusyPage.class);
            }
        }
    }

    @Override
    public void onRuntimeException(RequestCycle cycle, Exception ex) {
        // nothing to do
    }
}
