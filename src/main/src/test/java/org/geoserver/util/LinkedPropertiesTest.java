/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.Test;

public class LinkedPropertiesTest {

    @Test
    public void testDefaultSorting() throws IOException {
        // SortedProperties is the explicit class for alphabetically-sorted output
        SortedProperties props = new SortedProperties();
        props.setProperty("zebra", "last");
        props.setProperty("alpha", "first");
        props.setProperty("beta", "second");
        props.setProperty("gamma", "third");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);

        String content = out.toString(StandardCharsets.ISO_8859_1);

        // Keys should be sorted alphabetically
        int alphaIdx = content.indexOf("alpha=");
        int betaIdx = content.indexOf("beta=");
        int gammaIdx = content.indexOf("gamma=");
        int zebraIdx = content.indexOf("zebra=");

        assertTrue("alpha should appear before beta", alphaIdx < betaIdx);
        assertTrue("beta should appear before gamma", betaIdx < gammaIdx);
        assertTrue("gamma should appear before zebra", gammaIdx < zebraIdx);
    }

    @Test
    public void testPreserveOrder() throws IOException {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("zebra", "last");
        props.setProperty("alpha", "first");
        props.setProperty("beta", "second");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // LinkedProperties preserves insertion order by default
        props.store(out, null);

        String content = out.toString(StandardCharsets.ISO_8859_1);

        // Keys should be in insertion order
        int zebraIdx = content.indexOf("zebra=");
        int alphaIdx = content.indexOf("alpha=");
        int betaIdx = content.indexOf("beta=");

        assertTrue("zebra should appear before alpha", zebraIdx < alphaIdx);
        assertTrue("alpha should appear before beta", alphaIdx < betaIdx);
    }

    @Test
    public void testUnicodeEscaping() throws IOException {
        LinkedProperties props = new LinkedProperties();
        // Unicode character that should be escaped when stored
        props.setProperty("unicode.key", "value with \u00f1"); // ñ character
        props.setProperty("special", "equals=colon:");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);

        String content = out.toString(StandardCharsets.ISO_8859_1);

        // Built-in Properties.store() should handle escaping
        assertTrue(content.contains("unicode.key="));
        assertTrue(content.contains("special="));
        // Properties.store() in Java escapes non-ASCII Unicode
        // Check for the escaped form by looking for backslash-u pattern
        assertTrue("Unicode should be escaped", content.matches("(?s).*\\\\u00[0-9a-fA-F]{2}.*"));
        // Verify it doesn't crash and produces valid output
        assertTrue(content.length() > 0);
    }

    @Test
    public void testInsertionOrderMaintained() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("first", "1");
        props.setProperty("second", "2");
        props.setProperty("third", "3");

        // Verify iteration order matches insertion order
        Object[] keys = props.keySet().toArray();
        assertEquals("first", keys[0]);
        assertEquals("second", keys[1]);
        assertEquals("third", keys[2]);
    }

    @Test
    public void testConstructorWithDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("default.key", "default.value");

        LinkedProperties props = new LinkedProperties(defaults);

        assertEquals("default.value", props.getProperty("default.key"));
        assertTrue(props.containsKey("default.key"));
    }

    @Test
    public void testPutAll() {
        Properties source = new Properties();
        source.setProperty("key1", "value1");
        source.setProperty("key2", "value2");

        LinkedProperties props = new LinkedProperties();
        props.putAll(source);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
        assertEquals(2, props.size());
    }

    @Test
    public void testCommentPreserved() throws IOException {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("key", "value");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, "Test Comment");

        String content = out.toString(StandardCharsets.ISO_8859_1);

        assertTrue("Comment should be present", content.contains("Test Comment"));
    }

    @Test
    public void testMultipleStoresSortIndependently() throws IOException {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("z", "1");
        props.setProperty("a", "2");

        // First store: use SortedProperties for alphabetical sorting
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        SortedProperties sp = new SortedProperties();
        sp.setProperty("z", "1");
        sp.setProperty("a", "2");
        sp.store(out1, null);
        String sorted = out1.toString(StandardCharsets.ISO_8859_1);
        assertTrue(sorted.indexOf("a=") < sorted.indexOf("z="));

        // Second store: insertion-order is the default for LinkedProperties
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        LinkedProperties lp = new LinkedProperties();
        lp.setProperty("z", "1");
        lp.setProperty("a", "2");
        lp.store(out2, null);
        String ordered = out2.toString(StandardCharsets.ISO_8859_1);
        assertTrue(ordered.indexOf("z=") < ordered.indexOf("a="));
    }

    @Test
    public void testGetPropertyWithDefault() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("existing", "value");

        assertEquals("value", props.getProperty("existing", "default"));
        assertEquals("default", props.getProperty("nonexistent", "default"));
    }

    @Test
    public void testContainsValue() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("key", "value");

        assertTrue(props.containsValue("value"));
        assertFalse(props.containsValue("nonexistent"));
    }

    @Test
    public void testClear() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        assertEquals(2, props.size());

        props.clear();

        assertEquals(0, props.size());
        assertNull(props.getProperty("key1"));
    }

    @Test
    public void testRESTSecurityRuleOrder() throws IOException {
        // Simulate REST access rules where order matters
        LinkedProperties props = new LinkedProperties();

        // Add rules in specific insertion order (not alphabetical)
        props.setProperty("zrule", "VALUE_Z");
        props.setProperty("arule", "VALUE_A");
        props.setProperty("mrule", "VALUE_M");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, "Test Order");

        String content = out.toString(StandardCharsets.ISO_8859_1);

        // Find positions of each rule
        int zPos = content.indexOf("zrule=");
        int aPos = content.indexOf("arule=");
        int mPos = content.indexOf("mrule=");

        // Verify insertion order is maintained (not alphabetical)
        assertTrue("zrule should come first", zPos > 0 && zPos < aPos);
        assertTrue("arule should come second", aPos < mPos);

        // Verify it's NOT alphabetically sorted
        // If alphabetical, arule would come before zrule
        assertFalse("Should not be alphabetically sorted", aPos < zPos);
    }

    @Test
    public void testEquals() {
        LinkedProperties props1 = new LinkedProperties();
        props1.setProperty("key1", "value1");
        props1.setProperty("key2", "value2");

        LinkedProperties props2 = new LinkedProperties();
        props2.setProperty("key1", "value1");
        props2.setProperty("key2", "value2");

        LinkedProperties props3 = new LinkedProperties();
        props3.setProperty("key1", "different");
        props3.setProperty("key2", "value2");

        assertEquals(props1, props2);
        assertNotEquals(props1, props3);
        assertNotEquals(props1, null);
    }

    @Test
    public void testHashCode() {
        LinkedProperties props1 = new LinkedProperties();
        props1.setProperty("key", "value");

        LinkedProperties props2 = new LinkedProperties();
        props2.setProperty("key", "value");

        assertEquals(props1.hashCode(), props2.hashCode());
    }
}
