/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.expressions.aggregate.StreamFunction;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

/**
 * This visitor search for a Filter in {@link TemplateBuilder} tree using the path provided as a
 * guideline.
 */
public class TemplatePathVisitor extends DuplicatingFilterVisitor {

    protected int currentEl;
    protected Stack<String> currentSource = new Stack<>();
    private int contextPos = 0;
    boolean isSimple;
    private List<Filter> filters = new ArrayList<>();

    public TemplatePathVisitor(FeatureType type) {
        this(type instanceof SimpleFeatureType);
    }

    TemplatePathVisitor(boolean isSimple) {
        this.isSimple = isSimple;
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        String propertyName = expression.getPropertyName();
        if (extraData instanceof TemplateBuilder) {
            TemplateBuilder builder = (TemplateBuilder) extraData;
            Object newExpression = mapPropertyThroughBuilder(propertyName, builder);
            if (newExpression != null) return newExpression;
        }
        return getFactory(extraData)
                .property(expression.getPropertyName(), expression.getNamespaceContext());
    }

    /**
     * Back maps a given property through the template, to find out if it originates via an
     * expression.
     *
     * @param propertyName
     * @param builder
     * @return
     */
    protected Expression mapPropertyThroughBuilder(String propertyName, TemplateBuilder builder) {
        String[] elements;
        if (propertyName.indexOf(".") != -1) {
            elements = propertyName.split("\\.");
        } else {
            elements = propertyName.split("/");
        }

        try {
            currentSource = new Stack<>();
            currentEl = 0;
            Expression expression = findFunction(builder, Arrays.asList(elements));
            if (expression != null) {
                // we found a function hence we may need to extract the property,
                // otherwise we would fallback on the provided property name
                Expression newExpression = findXpathArg(expression);
                if (newExpression != null) {
                    return newExpression;
                }
            }
        } catch (Throwable ex) {
            throw new RuntimeException(
                    "Unable to evaluate template path against"
                            + "the template. Cause: "
                            + ex.getMessage());
        }
        return null;
    }

    private Expression findXpathArg(Expression expression) {
        PropertyNameCompleterVisitor completerVisitor = new PropertyNameCompleterVisitor();
        return (Expression) expression.accept(completerVisitor, null);
    }

    private Filter findXpathArg(Filter expression) {
        PropertyNameCompleterVisitor completerVisitor = new PropertyNameCompleterVisitor();
        return (Filter) expression.accept(completerVisitor, null);
    }

    /**
     * Find the corresponding function to which the template path is pointing, by iterating over
     * builder's tree
     */
    public Expression findFunction(TemplateBuilder builder, List<String> pathElements) {
        int lastElI = pathElements.size() - 1;
        String lastEl = pathElements.get(lastElI);
        char[] charArr = lastEl.toCharArray();
        int index = extractArrayIndexIfPresent(charArr);
        // we might have a path like path.to.an.array1 pointing
        // to a template array attribute eg "array":["${xpath}","$${xpath}", "static value"]
        if (index != 0) {
            lastEl = String.valueOf(charArr);
            pathElements.set(lastElI, lastEl.substring(0, charArr.length - 1));
        }
        // find the builder to which the path is pointing
        TemplateBuilder jb = findBuilder(builder, pathElements);
        if (jb != null) {
            if (jb instanceof IteratingBuilder && index != 0) {
                // retrieve the builder based on the position
                IteratingBuilder itb = (IteratingBuilder) jb;
                jb = getChildFromIterating(itb, index - 1);
            }

            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder dvb = (DynamicValueBuilder) jb;
                addFilter(dvb.getFilter());
                this.contextPos = dvb.getContextPos();
                if (dvb.getXpath() != null) return (PropertyName) super.visit(dvb.getXpath(), null);
                else {
                    return super.visit(dvb.getCql(), null);
                }
            } else if (jb instanceof StaticBuilder) {
                StaticBuilder staticBuilder = (StaticBuilder) jb;
                addFilter(staticBuilder.getFilter());
                Expression retExpr;
                if (staticBuilder.getStaticValue() != null) {
                    JsonNode staticNode = staticBuilder.getStaticValue();
                    while (currentEl < pathElements.size()) {
                        JsonNode child = staticNode.get(pathElements.get(currentEl - 1));
                        staticNode = child != null ? child : staticNode;
                        currentEl++;
                    }
                    retExpr = ff.literal(staticNode.asText());
                } else {
                    retExpr = ff.literal(staticBuilder.getStrValue());
                }
                return retExpr;
            }
        }
        return null;
    }

    private int extractArrayIndexIfPresent(char[] charArr) {
        int lastIdx = charArr.length - 1;
        char lastElem = charArr[lastIdx];
        if (Character.isDigit(lastElem)) {
            return Character.getNumericValue(lastElem);
        }
        return 0;
    }

    /**
     * Find the corresponding function to which the template path is pointing, by iterating over
     * builder's tree
     */
    private TemplateBuilder findBuilder(TemplateBuilder parent, List<String> pathElements) {
        List<TemplateBuilder> children = parent.getChildren();
        int length = pathElements.size();
        if (children != null) {
            for (TemplateBuilder tb : children) {
                String key = ((AbstractTemplateBuilder) tb).getKey(null);
                if (tb instanceof DynamicIncludeFlatBuilder) {
                    // go get the including node that the dynamic build inglobated
                    tb = ((DynamicIncludeFlatBuilder) tb).getIncludingNodeBuilder(null);
                }
                if (matchBuilder(tb, key, pathElements, parent)) {
                    boolean isLastEl = currentEl == length;
                    if (isLastEl || tb instanceof StaticBuilder) {
                        return tb;
                    } else if (tb instanceof SourceBuilder) {
                        pickSourceAndFilter((SourceBuilder) tb);
                        TemplateBuilder result = findBuilder(tb, pathElements);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    // In case a path specifying an element in an array has been specified
    // eg. path.to.array.1
    private TemplateBuilder getChildFromIterating(IteratingBuilder itb, int position) {
        List<TemplateBuilder> children = itb.getChildren();
        if (position < children.size()) return children.get(position);
        return null;
    }

    private boolean keyMatched(TemplateBuilder jb, String key, List<String> pathElements) {
        // CompositeBuilder (if children of an Iterating builder) have null key
        boolean allowNullKey = jb instanceof CompositeBuilder && key == null;
        // the key matched one element of the path
        boolean keyMatchedOtherBuilder = (key != null && key.equals(pathElements.get(currentEl)));

        if (keyMatchedOtherBuilder) currentEl++;

        return allowNullKey || keyMatchedOtherBuilder;
    }

    // takes source and filter from the SourceBuilder
    private void pickSourceAndFilter(SourceBuilder sb) {
        String source = sb.getStrSource();
        if (source != null) {
            if (source.indexOf(".") != -1) source = source.replace(".", "/");
            currentSource.add(source);
        }
        addFilter(sb.getFilter());
    }

    private void addFilter(Filter filter) {
        if (filter != null) {
            filter = (Filter) findXpathArg(filter);
            filters.add(filter);
        }
    }

    public List<Filter> getFilters() {
        return filters;
    }

    private boolean matchBuilder(
            TemplateBuilder current,
            String key,
            List<String> pathElements,
            TemplateBuilder parent) {
        boolean result = keyMatched(current, key, pathElements);
        if (!result) {
            if (parent instanceof RootBuilder) result = true;
            else if (parent instanceof SourceBuilder && !((SourceBuilder) parent).hasOwnOutput())
                result = true;
        }
        return result;
    }

    /**
     * A visitor that completes the property name extracted for backwards mapping, using the sources
     * if present.
     */
    private class PropertyNameCompleterVisitor extends DuplicatingFilterVisitor {
        @Override
        public Object visit(PropertyName filter, Object extraData) {
            filter = (PropertyName) super.visit(filter, extraData);
            if (filter instanceof AttributeExpressionImpl) {
                AttributeExpressionImpl pn = (AttributeExpressionImpl) filter;
                String property;
                if (canCompleteXpath(extraData)) property = completeXPath(pn.getPropertyName());
                else property = pn.getPropertyName();
                pn.setPropertyName(property);
                filter = pn;
            }
            return filter;
        }

        /**
         * Add to the xpath, xpath parts taken from the $source attribute. This is done for Complex
         * Features only
         */
        private String completeXPath(String xpath) {
            if (!currentSource.isEmpty() && !isSimple) {
                Stack<String> sourceParts = new Stack<>();
                sourceParts.addAll(currentSource);
                xpath = completeXpath(sourceParts, xpath);
            }
            return xpath;
        }

        /**
         * Check if extradata is the flag sent by the Function visit method for Stream Function.
         *
         * @param extradata
         * @return true if extradata is true, false otherwise.
         */
        private boolean canCompleteXpath(Object extradata) {
            return extradata == null
                    || (extradata instanceof Boolean && ((Boolean) extradata).booleanValue());
        }

        private String completeXpath(Stack<String> source, String xpath) {
            String result = null;
            int ctxPos = contextPos;
            while (!source.isEmpty()) {
                String sourcePart = source.pop();
                if (ctxPos <= 0)
                    if (result == null) result = sourcePart.concat("/");
                    else result = sourcePart.concat("/").concat(result);
                ctxPos--;
            }
            if (result == null) result = xpath;
            else result = result.concat(xpath);
            return result;
        }

        @Override
        public Object visit(Function expression, Object extraData) {
            // Stream Function is a special case since after the first property name
            // all the next ones evaluate on the result of the first one.
            if (expression instanceof StreamFunction) {
                StreamFunction streamFunction = (StreamFunction) expression;
                List<Expression> expressions = streamFunction.getParameters();
                int size = expressions.size();
                int pnCounter = 0;
                for (int i = 0; i < size; i++) {
                    Expression e = expressions.get(i);
                    if (e instanceof PropertyName) {
                        e = (Expression) visit((PropertyName) e, pnCounter == 0);
                        pnCounter++;
                    } else e = visit(e, extraData);
                    expressions.set(i, e);
                }
                return expression;
            }
            return super.visit(expression, extraData);
        }
    }
}
