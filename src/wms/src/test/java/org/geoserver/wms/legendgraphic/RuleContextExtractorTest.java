/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.geoserver.wms.legendgraphic.RuleContextExtractor.RuleContext;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;

public class RuleContextExtractorTest {

    private static final String DASH = " - ";
    private static final String SPACE = " ";
    FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    private void assertContext(
            RuleContext context,
            String expectedTitleSuffix,
            Function expectedFunction,
            Object expectedReplacementValue,
            String expectedSeparator) {
        assertEquals(expectedTitleSuffix, context.titleSuffix());
        assertEquals(expectedFunction, context.sourceFunction());
        assertEquals(expectedReplacementValue, context.replacementValue());
        assertEquals(expectedSeparator, context.separator());
    }

    @Test
    public void testRecode() {
        Function recode = ff.function(
                "Recode",
                ff.property("REGION"),
                ff.literal("First"),
                ff.literal("#6495ED"),
                ff.literal("Second"),
                ff.literal("#B0C4DE"),
                ff.literal("Third"),
                ff.literal("#00FFFF"));

        List<RuleContext> contexts = RuleContextExtractor.extract(recode);
        assertNotNull(contexts);
        assertEquals(3, contexts.size());
        assertContext(contexts.get(0), "First", recode, ff.literal("#6495ED"), DASH);
        assertContext(contexts.get(1), "Second", recode, ff.literal("#B0C4DE"), DASH);
        assertContext(contexts.get(2), "Third", recode, ff.literal("#00FFFF"), DASH);
    }

    @Test
    public void testInterpolate() {
        Function interpolate = ff.function(
                "Interpolate",
                ff.property("ELEVATION"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"));

        assertCommonInterpolateContexts(interpolate);
    }

    @Test
    public void testInterpolateMode() {
        Function interpolate = ff.function(
                "Interpolate",
                ff.property("ELEVATION"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"),
                ff.literal("linear"));

        assertCommonInterpolateContexts(interpolate);
    }

    @Test
    public void testInterpolateMethod() {
        Function interpolate = ff.function(
                "Interpolate",
                ff.property("ELEVATION"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"),
                ff.literal("numeric"));

        assertCommonInterpolateContexts(interpolate);
    }

    @Test
    public void testInterpolateMethodMode() {
        Function interpolate = ff.function(
                "Interpolate",
                ff.property("ELEVATION"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"),
                ff.literal("linear"),
                ff.literal("numeric"));

        assertCommonInterpolateContexts(interpolate);
    }

    private void assertCommonInterpolateContexts(Function interpolate) {
        List<RuleContext> contexts = RuleContextExtractor.extract(interpolate);
        assertNotNull(contexts);
        assertEquals(3, contexts.size());
        assertContext(contexts.get(0), "0", interpolate, ff.literal("#0000FF"), DASH);
        assertContext(contexts.get(1), "100", interpolate, ff.literal("#00FF00"), DASH);
        assertContext(contexts.get(2), "200", interpolate, ff.literal("#FF0000"), DASH);
    }

    @Test
    public void testCategorize() throws SchemaException {
        Function categorize = ff.function(
                "Categorize",
                ff.property("ELEVATION"),
                ff.literal("#000000"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"));

        List<RuleContext> contexts = RuleContextExtractor.extract(categorize);
        assertNotNull(contexts);
        assertEquals(4, contexts.size());
        // check the contexts and double-check the categorize default behavior (succeeding)
        assertContext(contexts.get(0), "< 0", categorize, ff.literal("#000000"), SPACE);
        assertEquals("#0000FF", categorize.evaluate(featureElevation(0)));
        assertContext(contexts.get(1), ">= 0 & < 100", categorize, ff.literal("#0000FF"), SPACE);
        assertEquals("#00FF00", categorize.evaluate(featureElevation(100)));
        assertContext(contexts.get(2), ">= 100 & < 200", categorize, ff.literal("#00FF00"), SPACE);
        assertEquals("#FF0000", categorize.evaluate(featureElevation(200)));
        assertContext(contexts.get(3), ">= 200", categorize, ff.literal("#FF0000"), SPACE);
    }

    @Test
    public void testCategorizePreceding() throws SchemaException {
        Function categorize = ff.function(
                "Categorize",
                ff.property("ELEVATION"),
                ff.literal("#000000"),
                ff.literal(0),
                ff.literal("#0000FF"),
                ff.literal(100),
                ff.literal("#00FF00"),
                ff.literal(200),
                ff.literal("#FF0000"),
                ff.literal("preceding"));

        List<RuleContext> contexts = RuleContextExtractor.extract(categorize);
        assertNotNull(contexts);
        assertEquals(4, contexts.size());
        // check the contexts and double-check the categorize behavior with succeeding
        assertContext(contexts.get(0), "<= 0", categorize, ff.literal("#000000"), SPACE);
        assertEquals("#000000", categorize.evaluate(featureElevation(0)));
        assertContext(contexts.get(1), "> 0 & <= 100", categorize, ff.literal("#0000FF"), SPACE);
        assertEquals("#0000FF", categorize.evaluate(featureElevation(100)));
        assertContext(contexts.get(2), "> 100 & <= 200", categorize, ff.literal("#00FF00"), SPACE);
        assertEquals("#00FF00", categorize.evaluate(featureElevation(200)));
        assertContext(contexts.get(3), "> 200", categorize, ff.literal("#FF0000"), SPACE);
    }

    /** Returns a simple feature with an ELEVATION attribute set to the provided value. */
    private SimpleFeature featureElevation(double value) throws SchemaException {
        SimpleFeatureType ft = DataUtilities.createType("test", "ELEVATION:Double");
        return SimpleFeatureBuilder.build(ft, new Object[] {value}, null);
    }
}
