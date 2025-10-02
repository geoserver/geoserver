/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.Symbolizer;
import org.hamcrest.Matchers;
import org.junit.Test;

public class TransformationFunctionTranslatorTest extends TransformationFunctionAbstractTest {

    private static FeatureTypeStyle transformSingleFTS(String styleName) {
        StyledLayerDescriptor sld = STYLES.get(styleName);
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        Style style = TransformationFunctionTranslator.translate(layer.getStyles()[0]);
        assertEquals(1, style.featureTypeStyles().size());
        return style.featureTypeStyles().get(0);
    }

    @Test
    public void testRecode() throws Exception {
        FeatureTypeStyle fts = transformSingleFTS("recode");
        // one rule per recode entry (3)
        List<Rule> rules = fts.rules();
        assertEquals(3, rules.size());
        // rule names have been generated from the original rule name and recode keys
        Rule r0 = rules.get(0);
        assertThat(r0, ruleNameMatches("Region - First"));
        checkSinglePolygonFill(rules.get(0), "#6495ED");
        Rule r1 = rules.get(1);
        assertThat(r1, ruleNameMatches("Region - Second"));
        checkSinglePolygonFill(r1, "#B0C4DE");
        Rule r2 = rules.get(2);
        assertThat(r2, ruleNameMatches("Region - Third"));
        checkSinglePolygonFill(r2, "#00FFFF");
    }

    @Test
    public void testRecodeTwice() throws Exception {
        FeatureTypeStyle fts = transformSingleFTS("recodeTwice");
        // one rule per recode entry (3)
        List<Rule> rules = fts.rules();
        assertEquals(3, rules.size());
        // rule names have been generated from the original rule name and recode keys
        Rule r0 = rules.get(0);
        assertThat(r0, ruleNameMatches("Region - First"));
        checkSinglePolygonFill(rules.get(0), "#6495ED");
        checkSinglePolygonStroke(rules.get(0), "#6495ED");
        Rule r1 = rules.get(1);
        assertThat(r1, ruleNameMatches("Region - Second"));
        checkSinglePolygonFill(r1, "#B0C4DE");
        checkSinglePolygonStroke(r1, "#B0C4DE");
        Rule r2 = rules.get(2);
        assertThat(r2, ruleNameMatches("Region - Third"));
        checkSinglePolygonFill(r2, "#00FFFF");
        checkSinglePolygonStroke(r2, "#00FFFF");
    }

    @Test
    public void testRecodeTwoRules() throws Exception {
        FeatureTypeStyle fts = transformSingleFTS("recodeTwoRules");
        // expect 7 rules, 3 for the first rule, 4 for the second
        List<Rule> rules = fts.rules();
        assertEquals(7, rules.size());
        // rule names have been generated from the original rule name and recode keys
        assertThat(
                rules,
                contains(
                        ruleNameMatches("Region Fill - First"),
                        ruleNameMatches("Region Fill - Second"),
                        ruleNameMatches("Region Fill - Third"),
                        ruleNameMatches("Region Border - First"),
                        ruleNameMatches("Region Border - Second"),
                        ruleNameMatches("Region Border - Third"),
                        ruleNameMatches("Region Border - Fourth")));
        // the first three rules have only a fill
        checkSinglePolygonFill(rules.get(0), "#6495ED");
        checkSinglePolygonFill(rules.get(1), "#B0C4DE");
        checkSinglePolygonFill(rules.get(2), "#00FFFF");
        // the last four rules have only a stroke
        checkSingleLineStroke(rules.get(3), "#6495ED");
        checkSingleLineStroke(rules.get(4), "#B0C4DE");
        checkSingleLineStroke(rules.get(5), "#00FFFF");
        checkSingleLineStroke(rules.get(6), "#AAAAAA");
    }

    @Test
    public void testRecodeInconsistent() throws Exception {
        StyledLayerDescriptor sld = STYLES.get("recodeInconsistent");
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        Style sourceStyle = layer.getStyles()[0];
        Style translatedStyle = TransformationFunctionTranslator.translate(sourceStyle);
        // could not translate, style is the same
        assertSame(translatedStyle, sourceStyle);
    }

    @Test
    public void testInterpolate() throws Exception {
        FeatureTypeStyle fts = transformSingleFTS("interpolate");
        // one rule per recode entry (3)
        List<Rule> rules = fts.rules();
        assertEquals(3, rules.size());
        // rule names have been generated from the original rule name and interpolate keys
        Rule r0 = rules.get(0);
        assertThat(r0, ruleNameMatches("Region - 10000"));
        checkSinglePolygonFill(rules.get(0), "#6495ED");
        Rule r1 = rules.get(1);
        assertThat(r1, ruleNameMatches("Region - 20000"));
        checkSinglePolygonFill(r1, "#B0C4DE");
        Rule r2 = rules.get(2);
        assertThat(r2, ruleNameMatches("Region - 30000"));
        checkSinglePolygonFill(r2, "#00FFFF");
    }

    @Test
    public void testCategorize() throws Exception {
        FeatureTypeStyle fts = transformSingleFTS("categorize");
        // defines 4 ranges (2 unbounded and 2 bounded)
        List<Rule> rules = fts.rules();
        assertEquals(4, rules.size());
        // rule names have been generated from the original rule name and thresholds
        Rule r0 = rules.get(0);
        assertThat(r0, ruleNameMatches("Region < 10000"));
        checkSinglePolygonFill(rules.get(0), "#FF0000");
        Rule r1 = rules.get(1);
        assertThat(r1, ruleNameMatches("Region >= 10000 & < 20000"));
        checkSinglePolygonFill(r1, "#6495ED");
        Rule r2 = rules.get(2);
        assertThat(r2, ruleNameMatches("Region >= 20000 & < 30000"));
        checkSinglePolygonFill(r2, "#B0C4DE");
        Rule r3 = rules.get(3);
        assertThat(r3, ruleNameMatches("Region >= 30000"));
        checkSinglePolygonFill(r3, "#00FFFF");
    }

    private static void checkSinglePolygonFill(Rule rule, String expectedColor) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        assertEquals(1, symbolizers.size());
        assertThat(symbolizers.get(0), Matchers.instanceOf(PolygonSymbolizer.class));
        PolygonSymbolizer ps = (PolygonSymbolizer) symbolizers.get(0);
        assertEquals(FF.literal(expectedColor), ps.getFill().getColor());
    }

    private static void checkSinglePolygonStroke(Rule rule, String expectedColor) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        assertEquals(1, symbolizers.size());
        assertThat(symbolizers.get(0), Matchers.instanceOf(PolygonSymbolizer.class));
        PolygonSymbolizer ps = (PolygonSymbolizer) symbolizers.get(0);
        assertEquals(FF.literal(expectedColor), ps.getStroke().getColor());
    }

    private static void checkSingleLineStroke(Rule rule, String expectedColor) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        assertEquals(1, symbolizers.size());
        assertThat(symbolizers.get(0), Matchers.instanceOf(LineSymbolizer.class));
        LineSymbolizer ls = (LineSymbolizer) symbolizers.get(0);
        assertEquals(FF.literal(expectedColor), ls.getStroke().getColor());
    }
}
