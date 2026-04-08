/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.ows.util.PropertyCopyPolicy;
import org.junit.After;
import org.junit.Test;

public class PatchContextTest {

    // Dummy owner class for keys
    private static class Dummy {}

    @After
    public void cleanup() {
        // Avoid leaks if a test fails midway
        if (PatchContext.isActive()) {
            PatchContext.stop();
        }
    }

    @Test
    public void testPendingNullResolvedWhenMemberRegistered() {
        PatchContext.start();
        PatchContext pc = PatchContext.get();
        assertNotNull(pc);

        // Simulate reader detecting xsi:nil before mapper resolves real member
        pc.setCurrentSerializedPath("/dummy/user");
        pc.markCurrentAsExplicitNull(); // should go to pendingNullPaths

        // Now simulate mapper resolving current path to real member name
        pc.registerCurrentMember(Dummy.class, "user");

        // OwsUtils passes property names starting with uppercase
        assertTrue(pc.isExplicitNull(Dummy.class, "User"));

        // (optional) also true if called with already-normalized name
        assertTrue(pc.isExplicitNull(Dummy.class, "user"));
    }

    @Test
    public void testAcronymIsNotDecapitalized() {
        PatchContext.start();
        PatchContext pc = PatchContext.get();

        pc.setCurrentSerializedPath("/dummy/URL");
        pc.markCurrentAsExplicitNull();
        pc.registerCurrentMember(Dummy.class, "URL");

        // Must stay URL, not U RL / uRL / url
        assertTrue(pc.isExplicitNull(Dummy.class, "URL"));
        assertFalse("Acronyms should not be decapitalized", pc.isExplicitNull(Dummy.class, "Url"));
    }

    @Test
    public void testCopyPolicyForcesNullEvenIfNewValueIsNonNull() {
        PatchContext.start();
        PatchContext pc = PatchContext.get();

        // Mark a property as explicitly nulled
        pc.setCurrentSerializedPath("/dummy/watermark");
        pc.markCurrentAsExplicitNull();
        pc.registerCurrentMember(Dummy.class, "watermark");

        // Simulate XStream creating a default object (non-null) despite xsi:nil
        Object newValue = new Object();

        PropertyCopyPolicy policy = PatchContext.getCopyPolicy();

        assertTrue(
                "Non-null newValue should be copied",
                policy.shouldCopy("Watermark", new Dummy(), new Dummy(), newValue));

        Object mapped = policy.mapValue("Watermark", new Dummy(), new Dummy(), newValue);
        assertNull("Explicit-null must win and force mapped value to null", mapped);
    }

    @Test
    public void testDefaultPolicySkipsNulls() {
        PropertyCopyPolicy p = PropertyCopyPolicy.DEFAULT_POLICY;
        assertTrue(p.shouldCopy("X", new Dummy(), new Dummy(), "value"));
        assertFalse(p.shouldCopy("X", new Dummy(), new Dummy(), null));
    }
}
