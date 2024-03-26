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

public class CSPPredicateQueryTest {

    private MockHttpServletRequest request = null;

    private CSPHttpRequestWrapper wrapper = null;

    @Before
    public void setUp() {
        this.request = new MockHttpServletRequest();
        this.wrapper = new CSPHttpRequestWrapper(this.request, null);
    }

    @Test
    public void testConstructorInvalidRegex() {
        assertThrows(PatternSyntaxException.class, () -> new CSPPredicateQuery("["));
    }

    @Test
    public void testPredicateMatchEmptyString() {
        CSPPredicateQuery predicate = new CSPPredicateQuery("^$");
        this.request.setQueryString(null);
        assertTrue(predicate.test(this.wrapper));
        this.request.setQueryString("");
        assertTrue(predicate.test(this.wrapper));
        this.request.setQueryString("foo=bar");
        assertFalse(predicate.test(this.wrapper));
    }
}
