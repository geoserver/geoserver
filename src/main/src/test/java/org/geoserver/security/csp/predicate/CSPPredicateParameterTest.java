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

public class CSPPredicateParameterTest {

    private MockHttpServletRequest request = null;

    private CSPHttpRequestWrapper wrapper = null;

    @Before
    public void setUp() {
        this.request = new MockHttpServletRequest();
        this.wrapper = new CSPHttpRequestWrapper(this.request, null);
    }

    @Test
    public void testConstructorInvalidKeyRegex() {
        assertThrows(PatternSyntaxException.class, () -> new CSPPredicateParameter("[", ".*"));
    }

    @Test
    public void testConstructorInvalidValueRegex() {
        assertThrows(PatternSyntaxException.class, () -> new CSPPredicateParameter(".*", "["));
    }

    @Test
    public void testPredicateMatches() {
        CSPPredicateParameter predicate = new CSPPredicateParameter("(?i)^service$", "(?i)^(wms)?$");
        this.request.removeAllParameters();
        assertTrue(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("SeRvIcE", "");
        assertTrue(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("service", "wms");
        assertTrue(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("SERVICE", "WMS");
        assertTrue(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("service", "wms");
        this.request.setParameter("SERVICE", "WMS");
        assertTrue(predicate.test(this.wrapper));
    }

    @Test
    public void testPredicateNotMatches() {
        CSPPredicateParameter predicate = new CSPPredicateParameter("(?i)^service$", "(?i)^(wms)?$");
        this.request.setParameter("service", "wfs");
        assertFalse(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("SERVICE", "WFS");
        assertFalse(predicate.test(this.wrapper));
        this.request.removeAllParameters();
        this.request.setParameter("service", "wms");
        this.request.setParameter("SERVICE", "WFS");
        assertFalse(predicate.test(this.wrapper));
    }
}
