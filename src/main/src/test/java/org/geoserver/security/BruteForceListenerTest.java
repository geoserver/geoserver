/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.geoserver.security.config.BruteForcePreventionConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class BruteForceListenerTest {

    @Before
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // just a test, not a real IP
    public void setup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("128.0.0.1");
        GeoServerSecurityFilterChainProxy.REQUEST.set(request);
    }

    @After
    public void tearDown() {
        GeoServerSecurityFilterChainProxy.REQUEST.remove();
    }

    @Test
    public void testThrottlingDisabled() throws Exception {
        // Setup mock security manager and config
        GeoServerSecurityManager sm = mock(GeoServerSecurityManager.class);
        SecurityManagerConfig smConfig = mock(SecurityManagerConfig.class);
        BruteForcePreventionConfig bfConfig = new BruteForcePreventionConfig();
        bfConfig.setEnabled(true);
        bfConfig.setMinDelaySeconds(1);
        bfConfig.setMaxDelaySeconds(1);

        when(sm.getSecurityConfig()).thenReturn(smConfig);
        when(smConfig.getBruteForcePrevention()).thenReturn(bfConfig);

        // We use a spy to avoid actual waits while allowing call count verification
        BruteForceListener listener = spy(new BruteForceListener(sm));
        doReturn(0L).when(listener).computeDelay(any());

        // prepare the authentication failed event
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "wrong-password");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new AuthenticationException("bad") {});

        // 1. Verify it actually tries to delay when NOT disabled, computeDelay is called once
        listener.onApplicationEvent(event);
        verify(listener, times(1)).computeDelay(any());

        // 2. Verify it does NOT try to delay when disabled (computeDelay not called, count remains 1)
        BruteForceListener.withThrottlingDisabled(() -> {
            listener.onApplicationEvent(event);
            return null;
        });
        verify(listener, times(1)).computeDelay(any());

        // 3. Verify it's re-enabled after cleanup, causing a second call to computeDelay
        listener.onApplicationEvent(event);
        verify(listener, times(2)).computeDelay(any());
    }
}
