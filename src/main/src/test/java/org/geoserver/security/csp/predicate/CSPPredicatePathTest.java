/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.regex.PatternSyntaxException;
import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CSPPredicatePathTest {

    private MockHttpServletRequest request = null;

    private CSPHttpRequestWrapper wrapper = null;

    @Before
    public void setUp() {
        this.request = new MockHttpServletRequest();
        this.wrapper = new CSPHttpRequestWrapper(this.request, null);
    }

    @Test
    public void testConstructorInvalidRegex() {
        assertThrows(PatternSyntaxException.class, () -> new CSPPredicatePath("["));
    }

    @Test
    public void testPredicateMatches() {
        CSPPredicatePath predicate = new CSPPredicatePath("^/([^/]+/){0,2}wms/?$");
        this.request.setPathInfo("/wms");
        assertTrue(predicate.test(this.wrapper));
        this.request.setPathInfo("/wms/");
        assertTrue(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/wms");
        assertTrue(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/wms/");
        assertTrue(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/bar/wms");
        assertTrue(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/bar/wms/");
        assertTrue(predicate.test(this.wrapper));
    }

    @Test
    public void testPredicateNotMatches() {
        CSPPredicatePath predicate = new CSPPredicatePath("^/([^/]+/){0,2}wms/?$");
        this.request.setPathInfo("");
        assertFalse(predicate.test(this.wrapper));
        this.request.setPathInfo("/");
        assertFalse(predicate.test(this.wrapper));
        this.request.setPathInfo("/wfs");
        assertFalse(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/wfs/");
        assertFalse(predicate.test(this.wrapper));
        this.request.setPathInfo("/foo/bar/wfs");
        assertFalse(predicate.test(this.wrapper));
    }
}
