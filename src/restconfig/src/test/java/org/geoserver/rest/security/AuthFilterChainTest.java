/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.junit.After;
import org.junit.Test;

public class AuthFilterChainTest {

    private static final String PROP = "geoserver.security.allowedAuthFilterChainClasses";

    private static final String CLASS_NAME = "org.geoserver.rest.security.DummyRequestFilterChain";
    private static final String PREFIX = "org.geoserver.rest.security.*";

    @After
    public void restoreProps() {
        System.clearProperty(PROP);
    }

    @Test(expected = AuthenticationFilterChainRestController.CannotMakeChain.class)
    public void testUnallowedRequestFilterChain() {
        System.clearProperty(PROP);
        AuthFilterChain afc = new AuthFilterChain();
        afc.setClassName("java.lang.Runtime");
        afc.setName("reflection-test-filter-chain-unallowed");
        afc.setPatterns(Collections.singletonList("/x/**"));
        afc.setFilters(Collections.singletonList("anonymous"));
        afc.setDisabled(false);
        afc.toRequestFilterChain();
    }

    @Test
    public void testAllowedRequestFilterChain() {
        System.setProperty(PROP, CLASS_NAME);
        AuthFilterChain afc = new AuthFilterChain();
        afc.setClassName(CLASS_NAME);
        afc.setName("reflection-test-filter-chain-allowed");
        afc.setPatterns(Collections.singletonList("/x/**"));
        afc.setFilters(Collections.singletonList("anonymous"));
        afc.setDisabled(false);
        RequestFilterChain rfc = afc.toRequestFilterChain();
        assertEquals(DummyRequestFilterChain.class, rfc.getClass());
    }

    @Test
    public void testAllowedRequestFilterChainByPrefix() {
        System.setProperty(PROP, PREFIX);
        AuthFilterChain afc = new AuthFilterChain();
        afc.setClassName(CLASS_NAME);
        afc.setName("reflection-test-filter-chain-prefix");
        afc.setPatterns(Collections.singletonList("/x/**"));
        afc.setFilters(Collections.singletonList("anonymous"));
        afc.setDisabled(false);
        RequestFilterChain rfc = afc.toRequestFilterChain();
        assertEquals(DummyRequestFilterChain.class, rfc.getClass());
    }

    @Test(expected = AuthenticationFilterChainRestController.CannotMakeChain.class)
    public void testAllowListReloadsAfterPropertyChange() {
        System.setProperty(PROP, CLASS_NAME);
        AuthFilterChain allowed = new AuthFilterChain();
        allowed.setClassName(CLASS_NAME);
        allowed.setName("reflection-test-filter-chain-allowed-once");
        allowed.setPatterns(Collections.singletonList("/x/**"));
        allowed.setFilters(Collections.singletonList("anonymous"));
        allowed.setDisabled(false);
        allowed.toRequestFilterChain();

        System.clearProperty(PROP);

        AuthFilterChain blocked = new AuthFilterChain();
        blocked.setClassName(CLASS_NAME);
        blocked.setName("reflection-test-filter-chain-blocked-after-change");
        blocked.setPatterns(Collections.singletonList("/x/**"));
        blocked.setFilters(Collections.singletonList("anonymous"));
        blocked.setDisabled(false);
        blocked.toRequestFilterChain();
    }
}
