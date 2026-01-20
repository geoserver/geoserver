/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AuthFilterChainTest {

    private static final String PROP = "geoserver.security.allowedAuthFilterChainClasses";

    private static final String CLASS_NAME = "org.geoserver.rest.security.DummyRequestFilterChain";

    private static String previousAllowed;

    @BeforeClass
    public static void saveAndSetProps() {
        previousAllowed = System.getProperty(PROP);
        System.setProperty(PROP, CLASS_NAME);
    }

    @AfterClass
    public static void restoreProps() {
        if (previousAllowed == null) {
            System.clearProperty(PROP);
        } else {
            System.setProperty(PROP, previousAllowed);
        }
    }

    @Test(expected = AuthenticationFilterChainRestController.CannotMakeChain.class)
    public void testUnallowedRequestFilterChain() {
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
        AuthFilterChain afc = new AuthFilterChain();
        afc.setClassName(CLASS_NAME);
        afc.setName("reflection-test-filter-chain-allowed");
        afc.setPatterns(Collections.singletonList("/x/**"));
        afc.setFilters(Collections.singletonList("anonymous"));
        afc.setDisabled(false);
        RequestFilterChain rfc = afc.toRequestFilterChain();
        assertEquals(DummyRequestFilterChain.class, rfc.getClass());
    }
}
