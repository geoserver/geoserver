/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.JdbcMultipleValue;
import org.geotools.data.complex.config.MultipleValue;
import org.geotools.data.complex.filter.MultipleValueExtractor;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.util.logging.Logging;

/**
 * This class provides a visitor to allow the usage of property directly referencing the domain
 * model when dealing with templates of complex features. It extracts the attribute names from
 * DynamicValueBuilders, sources and filters to map them to the correct xpath. Source are mapped to
 * the JDBC feature sources name while ${propertyName} and $${cql} directives to the target Xpath of
 * the matched AttributeMapping.
 */
public class SimplifiedPropertyReplacer extends DefaultTemplateVisitor {

    static final Logger LOGGER = Logging.getLogger(SimplifiedPropertyReplacer.class);

    public static final String RELATION_MARK = "->";

    private Stack<FeatureTypeMapping> mappingsStack;

    public SimplifiedPropertyReplacer(FeatureTypeMapping ftm) {
        this.mappingsStack = new Stack<>();
        mappingsStack.add(ftm);
    }

    @Override
    public Object visit(SourceBuilder sourceBuilder, Object extradata) {
        String source = sourceBuilder.getStrSource();
        if (source != null && !mappingsStack.isEmpty()) {
            FeatureTypeMapping context = mappingsStack.peek();
            String xpath = getXpathForSource(context, source);
            if (xpath != null) sourceBuilder.setSource(xpath);
        }
        replaceSimplifiedPropertyNamesInFilter(sourceBuilder, new FilterAttributeExtractor());
        sourceBuilder.getChildren().forEach(c -> c.accept(this, extradata));
        if (source != null && !mappingsStack.isEmpty()) mappingsStack.pop();
        return extradata;
    }

    @Override
    public Object visit(IteratingBuilder iteratingBuilder, Object extradata) {
        visit((SourceBuilder) iteratingBuilder, extradata);
        return extradata;
    }

    @Override
    public Object visit(CompositeBuilder compositeBuilder, Object extradata) {
        visit((SourceBuilder) compositeBuilder, extradata);
        return extradata;
    }

    @Override
    public Object visit(StaticBuilder staticBuilder, Object extradata) {
        replaceSimplifiedPropertyNamesInFilter(staticBuilder, new FilterAttributeExtractor());
        return super.visit(staticBuilder, extradata);
    }

    @Override
    public Object visit(DynamicValueBuilder dynamicBuilder, Object extradata) {
        if (!mappingsStack.isEmpty()) {
            Expression expression =
                    dynamicBuilder.getCql() != null
                            ? dynamicBuilder.getCql()
                            : dynamicBuilder.getXpath();
            FilterAttributeExtractor extractor = new FilterAttributeExtractor();
            expression.accept(extractor, null);
            List<String> xpaths =
                    convertSimplifiedPropertyNamesToXpaths(
                            extractor.getAttributeNames(), dynamicBuilder.getContextPos());
            replaceWithXpath(xpaths, expression, dynamicBuilder);

            replaceSimplifiedPropertyNamesInFilter(dynamicBuilder, extractor);
        }
        return super.visit(dynamicBuilder, extradata);
    }

    private void replaceSimplifiedPropertyNamesInFilter(
            AbstractTemplateBuilder builder, FilterAttributeExtractor extractor) {
        Filter filter = builder.getFilter();
        if (filter != null && !mappingsStack.isEmpty()) {
            extractor.clear();
            filter.accept(extractor, null);
            List<String> filterXpaths =
                    convertSimplifiedPropertyNamesToXpaths(
                            extractor.getAttributeNames(), builder.getFilterContextPos());
            replaceWithXpath(filterXpaths, filter, builder);
        }
    }

    private List<String> convertSimplifiedPropertyNamesToXpaths(String[] strProps, int contextPos) {
        List<String> xpaths = new ArrayList<>(strProps.length);
        try {
            int lastEl = mappingsStack.size() - 1;
            FeatureTypeMapping context;
            // handle the .. notation to refer previous context
            // and gets the correct FeatureTypeMapping
            if (contextPos > 0) {
                int index = lastEl >= contextPos ? lastEl - contextPos : 0;
                context = mappingsStack.get(index);
            } else context = mappingsStack.peek();
            for (String p : strProps) {
                String currentXpath = "";
                String[] splitted = p.split("/");
                boolean isNested = false;
                for (String part : splitted) {
                    if (isNested) {
                        // pop the featureTypeMapping this is not a SourceBuilder
                        // and should not set context for other builders
                        context = mappingsStack.pop();
                        isNested = false;
                    }
                    String xpathPart = null;
                    if (part.startsWith(RELATION_MARK)) {
                        // then we need to map the source local to the builder
                        part = part.replace(RELATION_MARK, "").replace(" ", "");
                        xpathPart =
                                findMatchingXpathFromTableName(
                                        part, context.getAttributeMappings());
                    }

                    if (xpathPart != null) {
                        isNested = true;
                    } else {
                        List<AttributeMapping> filteredMappings =
                                context.getAttributeMappings().stream()
                                        .filter(m -> !(m instanceof NestedAttributeMapping))
                                        .collect(Collectors.toList());
                        xpathPart = findMatchingXpathInAttributeMappingList(part, filteredMappings);
                    }
                    currentXpath = concatXpathPart(currentXpath, xpathPart);
                }
                if (currentXpath != null && !currentXpath.equals("")) xpaths.add(currentXpath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return xpaths;
    }

    private String getXpathForSource(FeatureTypeMapping context, String source) {
        String result;
        if (context.getSource().getName().getLocalPart().equals(source)) {
            result = strName(context.getTargetFeature().getName());
        } else {
            List<AttributeMapping> attributeMappings = context.getAttributeMappings();
            try {
                result = findMatchingXpathFromTableName(source, attributeMappings);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to map source " + source + " to the proper xpath");
            }
        }
        return result;
    }

    private String findMatchingXpathFromTableName(String pathPart, List<AttributeMapping> mappings)
            throws IOException {
        String result = null;
        for (AttributeMapping mapping : mappings) {
            if (mapping instanceof NestedAttributeMapping) {
                result = findMatchingXpathInNested(pathPart, (NestedAttributeMapping) mapping);
            } else {
                result = findMatchingXpathInJdbcMultipleValue(pathPart, mapping);
            }

            if (result != null) break;
        }
        return result;
    }

    private String findMatchingXpathInNested(String pathPart, NestedAttributeMapping nested)
            throws IOException {
        FeatureTypeMapping ftm = nested.getFeatureTypeMapping(null);
        String result = null;
        String sourceName = ftm.getSource().getName().getLocalPart();
        if (sourceName.equals(pathPart)) {
            result =
                    nested.getTargetXPath().toString()
                            + "/"
                            + strName(ftm.getTargetFeature().getName());
            mappingsStack.add(ftm);
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine(
                        "Source " + pathPart + " mapped to " + sourceName + "  in NestedMapping");
        }
        return result;
    }

    private String findMatchingXpathInJdbcMultipleValue(
            String pathPart, AttributeMapping attributeMapping) throws IOException {
        MultipleValueExtractor mvExtractor = new MultipleValueExtractor();
        attributeMapping.getSourceExpression().accept(mvExtractor, null);
        String result = null;
        List<MultipleValue> mvList = mvExtractor.getMultipleValues();
        if (mvList != null && !mvList.isEmpty()) {
            List<MultipleValue> jdbcMvList =
                    mvList.stream()
                            .filter(mv -> mv instanceof JdbcMultipleValue)
                            .collect(Collectors.toList());
            for (MultipleValue mv : jdbcMvList) {
                String tableName = ((JdbcMultipleValue) mv).getTargetTable();
                if (pathPart.equals(tableName)) {
                    String targetXpath = attributeMapping.getTargetXPath().toString();
                    result = targetXpath;
                    // since here the source is mapped to a jdbcMultipleValue
                    // readd the ftm to avoid the correct context being popped
                    // when this SourceBuilder visiting process ends.
                    FeatureTypeMapping ftm = mappingsStack.peek();
                    mappingsStack.add(ftm);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine(
                                "Source "
                                        + pathPart
                                        + " mapped to "
                                        + targetXpath
                                        + " in JdbcMultipleValue directive");
                    break;
                }
            }
        }
        return result;
    }

    private String findMatchingXpathInAttributeMappingList(
            String pathPart, List<AttributeMapping> mappings) {
        String result = null;
        MultipleValueExtractor extractor = new MultipleValueExtractor();
        for (AttributeMapping mapping : mappings) {
            result = xpathFromAttributeMappingExpr(pathPart, extractor, mapping);
            if (result == null) {
                result = xpathFromClientProperties(pathPart, extractor, mapping);
            }
            if (result != null) break;
        }
        return result;
    }

    private String xpathFromClientProperties(
            String pathPart, MultipleValueExtractor extractor, AttributeMapping mapping) {
        extractor.clear();
        String result = null;
        Map<Name, Expression> cProps = mapping.getClientProperties();
        for (Name xpath : cProps.keySet()) {
            Expression expression = cProps.get(xpath);
            expression.accept(extractor, null);
            String[] attributeNames = extractor.getAttributeNames();
            if (attributeNames.length > 0 && attributeNames[0].equals(pathPart)) {
                result = "@" + strName(xpath);
                mapping.getSourceExpression().accept(extractor, null);
                if (!(mapping.getSourceExpression() instanceof JdbcMultipleValue))
                    result = mapping.getTargetXPath().toString() + "/" + result;
                break;
            }
        }
        return result;
    }

    private String xpathFromAttributeMappingExpr(
            String pathPart, MultipleValueExtractor extractor, AttributeMapping mapping) {
        mapping.getSourceExpression().accept(extractor, null);
        String result = null;
        for (String attr : extractor.getAttributeNameSet()) {
            if (attr != null && attr.equals(pathPart)) {
                result = mapping.getTargetXPath().toString();
            }
        }
        List<MultipleValue> mvList = extractor.getMultipleValues();
        if (result == null && mvList != null && !mvList.isEmpty()) {
            MultipleValue mv = mvList.get(0);
            if (mv instanceof JdbcMultipleValue) {
                Expression column = ((JdbcMultipleValue) mv).getTargetValue();
                extractor.clear();
                column.accept(extractor, null);
                String[] attrs = extractor.getAttributeNames();
                if (attrs.length > 0 && attrs[0].equals(pathPart)) {
                    result = ".";
                }
            }
        }
        Expression idExpr = mapping.getIdentifierExpression();
        if (result == null && idExpr != null & !idExpr.equals(Expression.NIL)) {
            extractor.clear();
            idExpr.accept(extractor, null);
            String[] attrs = extractor.getAttributeNames();
            if (attrs.length > 0 && attrs[0].equals(pathPart)) result = "@gml:id";
        }
        return result;
    }

    private void replaceWithXpath(
            List<String> xpaths, Object toReplace, AbstractTemplateBuilder builder) {
        if (!xpaths.isEmpty()) {
            DuplicatingFilterVisitor dupVisitor =
                    new DuplicatingFilterVisitor() {
                        private int i = 0;

                        @Override
                        public Object visit(PropertyName expression, Object extraData) {
                            AttributeExpressionImpl pn =
                                    new AttributeExpressionImpl(
                                            xpaths.get(i), expression.getNamespaceContext());
                            i++;
                            return pn;
                        }
                    };
            if (toReplace instanceof Expression) {
                replacePropertyNamesInExpression(
                        (Expression) toReplace, dupVisitor, (DynamicValueBuilder) builder);
            } else {
                replacePropertyNamesInFilter((Filter) toReplace, dupVisitor, builder);
            }
        }
    }

    private void replacePropertyNamesInExpression(
            Expression expression, DuplicatingFilterVisitor visitor, DynamicValueBuilder builder) {
        Expression withXpath = (Expression) expression.accept(visitor, null);
        if (builder.getXpath() != null) builder.setXpath((AttributeExpressionImpl) withXpath);
        else builder.setCql(withXpath);
    }

    private void replacePropertyNamesInFilter(
            Filter filter, DuplicatingFilterVisitor visitor, AbstractTemplateBuilder builder) {
        Filter withXpath = (Filter) filter.accept(visitor, null);
        builder.setFilter(withXpath);
    }

    private String concatXpathPart(String xpath, String xpathPart) {
        if (xpathPart != null) {
            if (!xpath.equals("")) xpath += "/";
            xpath += xpathPart;
        }
        return xpath;
    }

    private String strName(Name name) {
        String prefix = mappingsStack.peek().getNamespaces().getPrefix(name.getNamespaceURI());
        return prefix + name.getSeparator() + name.getLocalPart();
    }
}
