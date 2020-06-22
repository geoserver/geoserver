/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link FreemarkerStaticsAccessRule}
 *
 * @author awaterme
 */
public class FreemarkerStaticsAccessRuleTest {
    // used for testing inner classes
    public static final class Dummy {
        public static final class String {};
    }

    public static final class String {};

    /** Tests most common case: no value set */
    @Test
    public void testNullEmpty() {
        FreemarkerStaticsAccessRule tmpRule = FreemarkerStaticsAccessRule.fromPattern(null);
        assertDisabled(tmpRule);

        tmpRule = FreemarkerStaticsAccessRule.fromPattern("");
        assertDisabled(tmpRule);

        tmpRule = FreemarkerStaticsAccessRule.fromPattern(" , ");
        assertDisabled(tmpRule);
    }

    /** Tests case with unrestricted access */
    @Test
    public void testUnrestricted() {
        FreemarkerStaticsAccessRule tmpRule = FreemarkerStaticsAccessRule.fromPattern("*");
        assertTrue(tmpRule.isUnrestricted());
    }

    /** tests invalid input */
    @Test
    public void testInvalid() {
        FreemarkerStaticsAccessRule tmpRule = FreemarkerStaticsAccessRule.fromPattern("a.b.C");
        assertDisabled(tmpRule);

        tmpRule = FreemarkerStaticsAccessRule.fromPattern("a.b.C, ..., .a.b,;");
        assertDisabled(tmpRule);

        tmpRule = FreemarkerStaticsAccessRule.fromPattern("true");
        assertDisabled(tmpRule);

        tmpRule = FreemarkerStaticsAccessRule.fromPattern("false");
        assertDisabled(tmpRule);
    }

    /** Tests valid input */
    @Test
    public void testValid() {
        // most simple
        FreemarkerStaticsAccessRule tmpRule =
                FreemarkerStaticsAccessRule.fromPattern("java.lang.String");
        assertFalse(tmpRule.isUnrestricted());
        assertEquals(1, tmpRule.getAllowedItems().size());
        assertEquals("java.lang.String", tmpRule.getAllowedItems().get(0).getClassName());

        // multiple, JDK and other
        tmpRule =
                FreemarkerStaticsAccessRule.fromPattern(
                        " java.lang.String  ,java.text.DecimalFormat  , "
                                + FeatureTemplate.class.getName());
        assertFalse(tmpRule.isUnrestricted());
        assertEquals(3, tmpRule.getAllowedItems().size());
        assertEquals("java.lang.String", tmpRule.getAllowedItems().get(0).getClassName());
        assertEquals("java.text.DecimalFormat", tmpRule.getAllowedItems().get(1).getClassName());
        assertEquals(
                FeatureTemplate.class.getName(), tmpRule.getAllowedItems().get(2).getClassName());

        // inner classes
        tmpRule =
                FreemarkerStaticsAccessRule.fromPattern(
                        getClass().getName() + "$" + Dummy.class.getSimpleName());
        assertFalse(tmpRule.isUnrestricted());
        assertEquals(1, tmpRule.getAllowedItems().size());
        assertEquals(Dummy.class.getName(), tmpRule.getAllowedItems().get(0).getClassName());

        // partially broken
        tmpRule =
                FreemarkerStaticsAccessRule.fromPattern(
                        " java.lang.String, not.existing.Class ,java.text.DecimalFormat");
        assertFalse(tmpRule.isUnrestricted());
        assertEquals(2, tmpRule.getAllowedItems().size());
        assertEquals("java.lang.String", tmpRule.getAllowedItems().get(0).getClassName());
        assertEquals("java.text.DecimalFormat", tmpRule.getAllowedItems().get(1).getClassName());
    }

    /** Tests number suffixes are consistent */
    @Test
    public void testNumberPostfixes() {
        java.lang.String lName1 = String.class.getName();
        java.lang.String lName2 = FreemarkerStaticsAccessRuleTest.String.class.getName();
        java.lang.String lName3 = FreemarkerStaticsAccessRuleTest.Dummy.String.class.getName();

        assertSuffixes(lName1, lName2, lName3);

        lName1 = FreemarkerStaticsAccessRuleTest.Dummy.String.class.getName();
        lName2 = FreemarkerStaticsAccessRuleTest.String.class.getName();
        lName3 = String.class.getName();

        assertSuffixes(lName1, lName2, lName3);
    }

    private void assertSuffixes(
            java.lang.String aName1, java.lang.String aName2, java.lang.String aName3) {

        FreemarkerStaticsAccessRule tmpRule =
                FreemarkerStaticsAccessRule.fromPattern(aName1 + "," + aName2 + "," + aName3);

        assertFalse(tmpRule.isUnrestricted());
        assertEquals(3, tmpRule.getAllowedItems().size());

        assertEquals("String", tmpRule.getAllowedItems().get(0).getAlias());
        assertEquals(aName1, tmpRule.getAllowedItems().get(0).getClassName());

        assertEquals("String2", tmpRule.getAllowedItems().get(1).getAlias());
        assertEquals(aName2, tmpRule.getAllowedItems().get(1).getClassName());

        assertEquals("String3", tmpRule.getAllowedItems().get(2).getAlias());
        assertEquals(aName3, tmpRule.getAllowedItems().get(2).getClassName());
    }

    private void assertDisabled(FreemarkerStaticsAccessRule aRule) {
        assertFalse(aRule.isUnrestricted());
        assertTrue(aRule.getAllowedItems().isEmpty());
    }
}
