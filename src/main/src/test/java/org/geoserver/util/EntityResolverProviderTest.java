/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.junit.Test;

public class EntityResolverProviderTest {

    @Test
    public void testAllowListDefaults() throws Exception {
        Set<String> allowed = EntityResolverProvider.entityResolutionAllowlist("");
        assertNotNull(allowed);
        assertEquals(4, allowed.size());
        assertTrue(allowed.contains(AllowListEntityResolver.W3C));
        assertTrue(allowed.contains(AllowListEntityResolver.INSPIRE));
        assertTrue(allowed.containsAll(Arrays.asList(AllowListEntityResolver.OGC.split("\\|"))));

        allowed = EntityResolverProvider.entityResolutionAllowlist(null);
        assertNotNull(allowed);
        assertEquals(4, allowed.size());
        assertTrue(allowed.contains(AllowListEntityResolver.W3C));
        assertTrue(allowed.contains(AllowListEntityResolver.INSPIRE));
        assertTrue(allowed.containsAll(Arrays.asList(AllowListEntityResolver.OGC.split("\\|"))));
    }

    @Test
    public void testAllowListWildCard() throws Exception {
        Set<String> allowed = EntityResolverProvider.entityResolutionAllowlist("*");
        assertNull(allowed);
    }

    @Test
    public void testAllowListDomains() throws Exception {
        Set<String> allowed = EntityResolverProvider.entityResolutionAllowlist("how2map.com");

        assertNotNull(allowed);
        assertEquals(5, allowed.size());
        assertTrue(allowed.contains(AllowListEntityResolver.W3C));
        assertTrue(allowed.contains(AllowListEntityResolver.INSPIRE));
        assertTrue(allowed.containsAll(Arrays.asList(AllowListEntityResolver.OGC.split("\\|"))));
        assertTrue(allowed.contains("how2map.com"));
    }
}
