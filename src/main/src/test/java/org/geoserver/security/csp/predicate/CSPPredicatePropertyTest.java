/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.regex.PatternSyntaxException;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.geoserver.security.csp.CSPUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CSPPredicatePropertyTest {

    private static final String KEY = CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS;

    private MockHttpServletRequest request = null;

    private CSPHttpRequestWrapper wrapper = null;

    private CSPConfiguration config = null;

    @Before
    public void setUp() {
        this.request = new MockHttpServletRequest();
        this.config = new CSPConfiguration();
        this.wrapper = new CSPHttpRequestWrapper(this.request, this.config);
    }

    @After
    public void resetProperty() {
        System.clearProperty(KEY);
    }

    @Test
    public void testConstructorPropertyNotAllowed() {
        String name = "java.version";
        Exception e = assertThrows(IllegalArgumentException.class, () -> new CSPPredicateProperty(name, "^$"));
        assertEquals("Property key not allowed: " + name, e.getMessage());
    }

    @Test
    public void testConstructorInvalidRegex() {
        assertThrows(PatternSyntaxException.class, () -> new CSPPredicateProperty(KEY, "["));
    }

    @Test
    public void testPredicatePropertySetMatches() {
        System.setProperty(KEY, "true");
        assertTrue(new CSPPredicateProperty(KEY, "^true$").test(null));
    }

    @Test
    public void testPredicatePropertySetNotMatches() {
        System.setProperty(KEY, "false");
        assertFalse(new CSPPredicateProperty(KEY, "^true$").test(null));
    }

    @Test
    public void testPredicateFieldSetMatches() {
        System.clearProperty(KEY);
        this.config.setFrameAncestors("true");
        assertTrue(new CSPPredicateProperty(KEY, "^true$").test(this.wrapper));
    }

    @Test
    public void testPredicateFieldSetNotMatches() {
        System.clearProperty(KEY);
        this.config.setFrameAncestors("false");
        assertFalse(new CSPPredicateProperty(KEY, "^true$").test(this.wrapper));
    }

    @Test
    public void testPredicatePropertyAndFieldUnsetMatches() {
        System.clearProperty(KEY);
        assertTrue(new CSPPredicateProperty(KEY, "^$").test(this.wrapper));
    }

    @Test
    public void testPredicatePropertyAndFieldUnsetNotMatches() {
        System.clearProperty(KEY);
        assertFalse(new CSPPredicateProperty(KEY, "^true$").test(this.wrapper));
    }
}
