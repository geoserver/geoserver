/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.csp.CSPHttpRequestWrapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class CSPPredicateMethodTest {

    private MockHttpServletRequest request = null;

    private CSPHttpRequestWrapper wrapper = null;

    @Before
    public void setUp() {
        this.request = new MockHttpServletRequest();
        this.wrapper = new CSPHttpRequestWrapper(this.request, null);
    }

    @Test
    public void testConstructorMethodNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new CSPPredicateMethod("FOO"));
    }

    @Test
    public void testPredicateTrue() {
        CSPPredicateMethod predicate = new CSPPredicateMethod("GET,HEAD,POST");
        this.request.setMethod("GET");
        assertTrue(predicate.test(this.wrapper));
        this.request.setMethod("HEAD");
        assertTrue(predicate.test(this.wrapper));
        this.request.setMethod("POST");
        assertTrue(predicate.test(this.wrapper));
    }

    @Test
    public void testPredicateFalse() {
        CSPPredicateMethod predicate = new CSPPredicateMethod("GET,HEAD");
        this.request.setMethod("POST");
        assertFalse(predicate.test(this.wrapper));
        this.request.setMethod("PUT");
        assertFalse(predicate.test(this.wrapper));
        this.request.setMethod("DELETE");
        assertFalse(predicate.test(this.wrapper));
    }
}
