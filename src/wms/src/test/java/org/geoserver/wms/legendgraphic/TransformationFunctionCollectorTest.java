/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.Rule;
import org.geotools.api.style.StyledLayerDescriptor;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Checks the functionality of TransformationFunctionCollector by loading a few styles from the test resources, applying
 * the visitor to them, and checking that the expected transformation functions are found on the expected rules. It does
 * not check the transformation functions contents, just their presence,as the collector does nothing to transform them
 */
public class TransformationFunctionCollectorTest extends TransformationFunctionAbstractTest {

    private static Map<Rule, Set<Function>> getTransformations(String styleName) {
        StyledLayerDescriptor sld = STYLES.get(styleName);
        TransformationFunctionCollector collector = new TransformationFunctionCollector();
        sld.accept(collector);
        return collector.getRuleFunctions();
    }

    @Test
    public void testRecode() {
        Map<Rule, Set<Function>> transformations = getTransformations("recode");
        // expect 1 rule with 1 recode function
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isRecode())));
    }

    @Test
    public void testRecodeTwice() {
        Map<Rule, Set<Function>> transformations = getTransformations("recodeTwice");
        // expect 1 rule with 1 recode function (same recode in the same symbolizer)
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isRecode())));
    }

    @Test
    public void testRecodeInconsistent() {
        Map<Rule, Set<Function>> transformations = getTransformations("recodeInconsistent");
        // expect 1 rule with 2 recode function
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isRecode(), isRecode())));
    }

    @Test
    public void testRecodeTwoRules() {
        Map<Rule, Set<Function>> transformations = getTransformations("recodeTwoRules");
        // expect 2 rule with 2 recode function
        assertEquals(2, transformations.size());
        assertThat(
                transformations,
                allOf(
                        hasEntry(ruleNameMatches("Region Fill"), contains(isRecode())),
                        hasEntry(ruleNameMatches("Region Border"), contains(isRecode()))));
    }

    @Test
    public void testInterpolate() {
        Map<Rule, Set<Function>> transformations = getTransformations("interpolate");
        // expect 1 rule with 1 interpolate function
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isInterpolate())));
    }

    @Test
    public void testInterpolateRecodeMixed() {
        StyledLayerDescriptor sld = STYLES.get("recodeInterpolateMixed");
        TransformationFunctionCollector collector = new TransformationFunctionCollector();
        sld.accept(collector);
        Map<Rule, Set<Function>> transformations = collector.getRuleFunctions();
        // expect 1 rule with 1 recode and 1 interpolate function
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isRecode(), isInterpolate())));
        // not transformable as a consequence
        assertThat(collector.getTargetRule(), Matchers.anEmptyMap());
    }

    @Test
    public void testCategorize() {
        Map<Rule, Set<Function>> transformations = getTransformations("categorize");
        // expect 1 rule with 1 interpolate function
        assertEquals(1, transformations.size());
        assertThat(transformations, hasEntry(ruleNameMatches("Region"), contains(isCategorize())));
    }

    @Test
    public void testNoTransformation() {
        Map<Rule, Set<Function>> transformations = getTransformations("notransformation");
        // expect empty
        assertTrue(transformations.isEmpty());
    }
}
