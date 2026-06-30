/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Set;
import org.junit.Test;

public class AccessLimitsTest {

    @Test
    public void testSecurityTagWithCommaRejected() {
        AccessLimits limits = new AccessLimits(CatalogMode.HIDE);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> limits.setSecurityTags(Set.of("a,b")));
        assertThat(e.getMessage(), containsString("a,b"));
    }

    @Test
    public void testSecurityTagsAccepted() {
        AccessLimits limits = new AccessLimits(CatalogMode.HIDE);
        limits.setSecurityTags(Set.of("tenant-a", "tenant-b"));
        assertEquals(Set.of("tenant-a", "tenant-b"), limits.getSecurityTags());
    }
}
