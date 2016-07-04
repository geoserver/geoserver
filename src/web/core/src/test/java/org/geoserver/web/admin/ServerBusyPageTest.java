/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ServerBusyPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class ServerBusyPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testStoreEditServerBusyPage() throws Exception {
        login();

        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        l.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        
        final LockType type = LockType.WRITE;
        final GeoServerConfigurationLock locker = (GeoServerConfigurationLock) GeoServerExtensions.bean("configurationLock");

        if (locker != null) {
            Thread configWriter = new Thread(){

                public void run() {
                    // Acquiring Configuration Lock as another user
                    locker.setEnabled(true);
                    locker.setAuth(new UsernamePasswordAuthenticationToken("anonymousUser","", l));
                    locker.lock(type);

                    try {
                        // Consider the TIMEOUT time spent when checking for the lock...
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    } finally {
                        try {
                            locker.unlock(type);
                            locker.setAuth(null);
                        } catch(Exception e) {
                            
                        }
                    }
                }

                
            };
            configWriter.start();

            tester.startPage(DataAccessEditPage.class, new PageParameters().add("wsName", "cite").add("storeName", "cite"));
            if (!locker.tryLock(type)) {
                tester.assertRenderedPage(ServerBusyPage.class);
            } else {
                tester.assertRenderedPage(DataAccessEditPage.class);
            }
            tester.assertNoErrorMessage();

            // Waste some time just to be sure the lock has been released...
            Thread.sleep(5000);
            
            try {
                locker.unlock(type);
                locker.setAuth(null);
            } catch(Exception e) {
                
            }
        }
    }
    
}
