/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp.predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CSPPredicateClassTest {

    @Test
    public void testConstructorClassNotAllowed() {
        String name = "java.util.Arrays";
        Exception e =
                assertThrows(IllegalArgumentException.class, () -> new CSPPredicateClass(name));
        assertEquals("Class name not allowed: " + name, e.getMessage());
    }

    @Test
    public void testPredicateTrue() {
        assertTrue(new CSPPredicateClass(CSPPredicateClass.class.getName()).test(null));
    }

    @Test
    public void testPredicateFalse() {
        assertFalse(new CSPPredicateClass("org.geoserver.foo.Bar").test(null));
    }
}
