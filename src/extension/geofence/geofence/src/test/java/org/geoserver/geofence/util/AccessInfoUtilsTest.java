/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.geofence.core.services.dto.AccessTypeDTO;
import org.geofence.core.services.dto.LayerAttributeDTO;
import org.junit.Test;

/** Unit tests for {@link AccessInfoUtils}. */
public class AccessInfoUtilsTest {

    @Test
    public void testIntersectAttributesWithDisjointNames() {
        // "common" is in both sets; "onlyA" only in s1; "onlyB" only in s2.
        LayerAttributeDTO common1 = new LayerAttributeDTO("common", AccessTypeDTO.READWRITE);
        LayerAttributeDTO common2 = new LayerAttributeDTO("common", AccessTypeDTO.READONLY);
        LayerAttributeDTO onlyA = new LayerAttributeDTO("onlyA", AccessTypeDTO.READWRITE);
        LayerAttributeDTO onlyB = new LayerAttributeDTO("onlyB", AccessTypeDTO.READONLY);

        Set<LayerAttributeDTO> s1 = Set.of(common1, onlyA);
        Set<LayerAttributeDTO> s2 = Set.of(common2, onlyB);

        Set<LayerAttributeDTO> result = AccessInfoUtils.intersectAttributes(s1, s2);

        assertEquals(3, result.size());
        assertTrue(result.contains(onlyA));
        assertTrue(result.contains(onlyB));
        assertTrue(result.stream()
                .anyMatch(la -> "common".equals(la.getName()) && la.getAccess() == AccessTypeDTO.READONLY));
    }

    @Test
    public void testIntersectAttributesNullSets() {
        Set<LayerAttributeDTO> s1 = Set.of(new LayerAttributeDTO("a", AccessTypeDTO.READWRITE));

        assertEquals(s1, AccessInfoUtils.intersectAttributes(s1, null));
        assertEquals(s1, AccessInfoUtils.intersectAttributes(null, s1));
    }
}
