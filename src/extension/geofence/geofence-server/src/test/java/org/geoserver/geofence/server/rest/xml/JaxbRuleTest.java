/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.server.rest.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public class JaxbRuleTest {

    @Test
    public void testSetArea() {
        JaxbRule.LayerDetails details = new JaxbRule.LayerDetails();
        // used to NPE here
        details.setAllowedArea(null);
        assertNull(details.getAllowedArea());
    }

    /** A partial update leaves out allowedStyle/attribute; those must survive, not be wiped. */
    @Test
    public void testPartialUpdateKeepsUnmentionedDetails() {
        org.geofence.core.model.LayerDetails stored = new org.geofence.core.model.LayerDetails();
        stored.getAllowedStyles().add("style1");
        org.geofence.core.model.LayerAttribute attribute = new org.geofence.core.model.LayerAttribute();
        attribute.setName("attr1");
        stored.getAttributes().add(attribute);

        // only the catalog mode is being changed
        JaxbRule.LayerDetails sent = new JaxbRule.LayerDetails();
        sent.setCatalogMode("HIDE");
        org.geofence.core.model.LayerDetails updated = sent.toLayerDetails(stored);

        assertEquals(Set.of("style1"), updated.getAllowedStyles());
        assertEquals(1, updated.getAttributes().size());
    }

    /** Sending the element empty is a deliberate "remove them all", and must still work. */
    @Test
    public void testExplicitlyEmptyDetailsAreCleared() {
        org.geofence.core.model.LayerDetails stored = new org.geofence.core.model.LayerDetails();
        stored.getAllowedStyles().add("style1");

        JaxbRule.LayerDetails sent = new JaxbRule.LayerDetails();
        sent.setAllowedStyles(Set.of());
        org.geofence.core.model.LayerDetails updated = sent.toLayerDetails(stored);

        assertTrue(updated.getAllowedStyles().isEmpty());
    }
}
