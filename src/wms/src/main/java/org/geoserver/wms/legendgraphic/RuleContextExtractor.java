/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.geotools.filter.function.InterpolateFunction.METHOD_COLOR;
import static org.geotools.filter.function.InterpolateFunction.METHOD_NUMERIC;
import static org.geotools.filter.function.InterpolateFunction.MODE_COSINE;
import static org.geotools.filter.function.InterpolateFunction.MODE_CUBIC;
import static org.geotools.filter.function.InterpolateFunction.MODE_LINEAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.function.CategorizeFunction;
import org.geotools.filter.function.InterpolateFunction;
import org.geotools.filter.function.RecodeFunction;

/**
 * Extracts the RuleContext information from a Rule, used to transform interpolate, recode and categorize functions into
 * multiple rules with literal values instead, for legend generation purposes.
 */
abstract class RuleContextExtractor {

    public static final String DASH_SEPARATOR = " - ";

    /**
     * A simple struct to hold the information we need to generate a new rule from an existing one after replacing the
     * transformation function with a literal value.
     */
    static final class RuleContext {
        private final String titleSuffix;
        private final Function sourceFunction;
        private final Expression replacementValue;
        private final String separator;
        /**
         * @param titleSuffix The suffix to append to the original rule title to generate the new rule title.
         * @param sourceFunction The original transformation function, used to generate the filter expression.
         * @param replacementValue The literal value to use in place of the transformation function.
         * @param separator The separator to use between the original rule title and the title suffix.
         */
        RuleContext(String titleSuffix, Function sourceFunction, Expression replacementValue, String separator) {
            this.titleSuffix = titleSuffix;
            this.sourceFunction = sourceFunction;
            this.replacementValue = replacementValue;
            this.separator = separator;
        }

        public String titleSuffix() {
            return titleSuffix;
        }

        public Function sourceFunction() {
            return sourceFunction;
        }

        public Expression replacementValue() {
            return replacementValue;
        }

        public String separator() {
            return separator;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            RuleContext that = (RuleContext) obj;
            return Objects.equals(this.titleSuffix, that.titleSuffix)
                    && Objects.equals(this.sourceFunction, that.sourceFunction)
                    && Objects.equals(this.replacementValue, that.replacementValue)
                    && Objects.equals(this.separator, that.separator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(titleSuffix, sourceFunction, replacementValue, separator);
        }

        @Override
        public String toString() {
            return "RuleContext[" + "titleSuffix="
                    + titleSuffix + ", " + "sourceFunction="
                    + sourceFunction + ", " + "replacementValue="
                    + replacementValue + ", " + "separator="
                    + separator + ']';
        }
    }

    /**
     * Extracts the RuleContext from the given rule, if it contains a single transformation function (Recode,
     * Categorize, Interpolate). If the rule contains no transformation functions, or multiple ones, null is returned.
     *
     * @param transformation The transformation function to extract the RuleContext from.
     * @return The RuleContext, or null if the rule does not contain exactly one transformation function.
     */
    static List<RuleContext> extract(Function transformation) {
        if (transformation instanceof RecodeFunction) {
            return new RecodeExtractor((RecodeFunction) transformation).extract();
        } else if (transformation instanceof CategorizeFunction) {
            return new CategorizeExtractor((CategorizeFunction) transformation).extract();
        } else if (transformation instanceof InterpolateFunction) {
            return new InterpolateExtractor((InterpolateFunction) transformation).extract();
        } else {
            throw new IllegalStateException("Unexpected transformation function type: "
                    + transformation.getClass().getName());
        }
    }

    Function transformation;

    private RuleContextExtractor(Function transformation) {
        this.transformation = transformation;
    }

    /**
     * Extracts the RuleContext from the transformation function.
     *
     * @return A list of RuleContext, one for each literal value the transformation function can produce.
     */
    public abstract List<RuleContext> extract();

    /** Extracts the RuleContext from a list of key->value pairs, as used by Recode but also by Interpolate. */
    protected List<RuleContext> extractFromTable(List<Expression> parameters) {
        List<RuleContext> contexts = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i += 2) {
            Expression from = parameters.get(i);
            Expression to = parameters.get(i + 1);
            contexts.add(new RuleContext(toString(from), transformation, to, DASH_SEPARATOR));
        }
        return contexts;
    }

    /**
     * Converts an expression to a string, using its evaluate method with a String return type. This works for literals
     * and property names, but not for more complex expressions (e.g. arithmetic ones).
     */
    protected static String toString(Expression expression) {
        if (expression == null) return null;
        String result = expression.evaluate(null, String.class);
        if (result == null) {
            throw new IllegalStateException("Cannot convert expression to string: " + expression);
        }
        return result;
    }

    /** Extractor for Recode functions. */
    private static class RecodeExtractor extends RuleContextExtractor {
        RecodeExtractor(RecodeFunction recode) {
            super(recode);
        }

        @Override
        public List<RuleContext> extract() {
            // function signature: recode(<lookup value>, <key1>, <value1>, <key2>, <value2>, ..., )
            // we can assume the function parameters are valid, otherwise the function would not have been created
            // we are interested in the key->value pairs only, so we skip the first parameter
            return extractFromTable(transformation
                    .getParameters()
                    .subList(1, transformation.getParameters().size()));
        }
    }

    /** Extractor for Interpolate functions. */
    private static class InterpolateExtractor extends RuleContextExtractor {
        static final Set<String> METHODS = Collections.newSetFromMap(new CaseInsensitiveMap<>());
        static final Set<String> MODES = Collections.newSetFromMap(new CaseInsensitiveMap<>());

        static {
            METHODS.add(METHOD_NUMERIC);
            METHODS.add(METHOD_COLOR);
            METHODS.add("colour"); // not exposed in InterpolateFunction, but accepted as input

            MODES.add(MODE_LINEAR);
            MODES.add(MODE_COSINE);
            MODES.add(MODE_CUBIC);
        }

        InterpolateExtractor(InterpolateFunction interpolate) {
            super(interpolate);
        }

        @Override
        public List<RuleContext> extract() {
            // function signature: interpolate(<lookup value>, <key1>, <value1>, <key2>, <value2>, ..., <mode>,
            // <method>)
            // we can assume the function parameters are valid, otherwise the function would not have been created
            // besides skipping the first, we need to figure out if the last two optional parameters are present or not
            int skipAtEnd = 0;
            if (isExpectedValueSpecified(transformation.getParameters(), MODES)) {
                skipAtEnd++;
            }
            if (isExpectedValueSpecified(transformation.getParameters(), METHODS)) {
                skipAtEnd++;
            }

            return extractFromTable(transformation
                    .getParameters()
                    .subList(1, transformation.getParameters().size() - skipAtEnd));
        }

        /**
         * Checks if a given set of expected values are found in the last two parameters of the function call. Can be
         * used to check if mode or method are specified, matching the behavior of InterpolateFunction, but with much
         * simpler code (and yes, the original code is a bit convoluted and won't verify if method is specified after
         * mode or vice versa).
         */
        private boolean isExpectedValueSpecified(List<Expression> parameters, Set<String> expectedValues) {
            // If mode is specified it will be the last or second last parameter
            final int n = parameters.size();
            for (int i = 2; i >= 1; i--) {
                int index = n - i;
                if (index > 1) {
                    Expression expr = parameters.get(index);
                    if (expr instanceof Literal && ((Literal) expr).getValue() instanceof String) {
                        String value = (String) ((Literal) expr).getValue();
                        if (expectedValues.contains(value)) return true;
                    }
                }
            }

            return false;
        }
    }

    private static class CategorizeExtractor extends RuleContextExtractor {

        public static final String SPACE = " ";

        private CategorizeExtractor(CategorizeFunction categorize) {
            super(categorize);
        }

        @Override
        public List<RuleContext> extract() {
            // function signature: categorize(<lookup value>, <value1>, <threshold1>, <values2>, <threshold2>, ...,
            // <valueN>, <succeeding/preceding>)
            // we can assume the function parameters are valid, otherwise the function would not have been created
            // besides skipping the first, we need to figure out the succeeding/preceding optional parameter to
            // report the right title suffix

            // if the number of parameters is odd, the last one is the succeeding/preceding parameter
            boolean succeeding = true;
            int skipAtEnd = transformation.getParameters().size() % 2;
            if (skipAtEnd == 1) {
                Expression last = transformation
                        .getParameters()
                        .get(transformation.getParameters().size() - 1);
                String lastValue = toString(last);
                if (CategorizeFunction.PRECEDING.equalsIgnoreCase(lastValue)) {
                    succeeding = false;
                }
            }

            List<Expression> categorizeTable = transformation
                    .getParameters()
                    .subList(1, transformation.getParameters().size() - skipAtEnd);
            List<RuleContext> contexts = new ArrayList<>();
            // first range is the infinite range below the first threshold
            String titleSuffix = (succeeding ? "< " : "<= ") + toString(categorizeTable.get(1));
            contexts.add(new RuleContext(titleSuffix, transformation, categorizeTable.get(0), SPACE));
            // internal rules are all finite ranges
            for (int i = 1; i < categorizeTable.size() - 2; i += 2) {
                Expression before = categorizeTable.get(i);
                Expression value = categorizeTable.get(i + 1);
                Expression after = categorizeTable.get(i + 2);
                String[] comparators = succeeding ? new String[] {">= ", "< "} : new String[] {"> ", "<= "};
                titleSuffix = comparators[0] + toString(before) + " & " + comparators[1] + toString(after);
                contexts.add(new RuleContext(titleSuffix, transformation, value, SPACE));
            }
            // last range is the infinite range above the last threshold
            int n = categorizeTable.size();
            titleSuffix = (succeeding ? ">= " : "> ") + toString(categorizeTable.get(n - 2));
            contexts.add(new RuleContext(titleSuffix, transformation, categorizeTable.get(n - 1), SPACE));

            return contexts;
        }
    }
}
