/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ServerBusyPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** @author Alessio Fabiani, GeoSolutions */
public class ServerBusyPageTest extends GeoServerWicketTestSupport {

    long defaultTimeout;

    @Before
    public void clearTimeout() {
        defaultTimeout = GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS;
        GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS = 1;
    }

    @After
    public void reinstateTimeout() {
        GeoServerConfigurationLock.DEFAULT_TRY_LOCK_TIMEOUT_MS = defaultTimeout;
    }

    @Test
    public void testStoreEditServerBusyPage() throws Exception {
        login();

        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        l.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));

        final LockType type = LockType.WRITE;
        final GeoServerConfigurationLock locker =
                (GeoServerConfigurationLock) GeoServerExtensions.bean("configurationLock");
        locker.setEnabled(true);
        locker.unlock(); // just to be on the safe side

        AtomicBoolean acquired = new AtomicBoolean(false);
        AtomicBoolean release = new AtomicBoolean(false);
        if (locker != null) {
            Thread configWriter =
                    new Thread("Config-writer") {

                        public void run() {
                            // Acquiring Configuration Lock as another user
                            locker.lock(type);
                            acquired.set(true);

                            try {
                                while (!release.get()) {
                                    Thread.sleep(50);
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } finally {
                                locker.unlock();
                            }
                        }
                    };
            configWriter.start();

            try {
                while (!acquired.get()) {
                    Thread.sleep(50);
                }

                tester.startPage(
                        DataAccessEditPage.class,
                        new PageParameters().add("wsName", "cite").add("storeName", "cite"));
                tester.assertRenderedPage(ServerBusyPage.class);
                tester.assertNoErrorMessage();
            } finally {
                release.set(true);
            }
        }
    }
}
