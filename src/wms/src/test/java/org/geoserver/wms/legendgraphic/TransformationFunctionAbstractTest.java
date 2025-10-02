/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.Rule;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.CategorizeFunction;
import org.geotools.filter.function.InterpolateFunction;
import org.geotools.filter.function.RecodeFunction;
import org.geotools.xml.styling.SLDParser;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;

/**
 * Base class for tests related to transformation functions (Recode, Categorize, Interpolate) in styles. It provides a
 * mechanism to load styles from the test resources and access them by name, as well as some common matchers.
 */
public abstract class TransformationFunctionAbstractTest {
    // read the styles from the test data directory, apply the TransformationFunctionCollector to them,
    // and verify that the expected transformation functions are found on the expected rules.
    static final Map<String, StyledLayerDescriptor> STYLES = new HashMap<>();
    public static final StyleFactory SF = CommonFactoryFinder.getStyleFactory();
    public static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    @BeforeClass
    public static void loadStyles() throws Exception {
        STYLES.clear();
        File stylesDir = new File("src/test/resources/org/geoserver/wms/legendgraphic/transformation");
        for (File f : stylesDir.listFiles((d, name) -> name.endsWith(".sld"))) {
            String styleName = f.getName().substring(0, f.getName().length() - 4);
            StyledLayerDescriptor sld = new SLDParser(SF, f).parseSLD();
            STYLES.put(styleName, sld);
        }
    }

    protected static Matcher<Function> isRecode() {
        return instanceOf(RecodeFunction.class);
    }

    protected static Matcher<Function> isInterpolate() {
        return instanceOf(InterpolateFunction.class);
    }

    protected static Matcher<Function> isCategorize() {
        return instanceOf(CategorizeFunction.class);
    }

    /** Little helper to match a rule by its name (title) */
    static Matcher<Rule> ruleNameMatches(String expected) {
        return new FeatureMatcher<>(is(expected), "ruleName", "ruleName") {
            @Override
            protected String featureValueOf(Rule actual) {
                return actual.getDescription().getTitle().toString();
            }
        };
    }
}
