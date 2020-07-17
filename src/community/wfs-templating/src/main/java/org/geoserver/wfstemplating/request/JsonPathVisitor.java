/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.wfstemplating.builders.AbstractTemplateBuilder;
import org.geoserver.wfstemplating.builders.SourceBuilder;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.wfstemplating.builders.impl.StaticBuilder;
import org.geoserver.wfstemplating.expressions.JsonLdCQLManager;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.ows.ServiceException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * This visitor search for a Filter in {@link TemplateBuilder} tree using the path provided as a
 * guideline.
 */
public class JsonPathVisitor extends DuplicatingFilterVisitor {

    protected int currentEl;
    protected String currentSource;
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    boolean isSimple;
    private List<Filter> filters = new ArrayList<>();

    public JsonPathVisitor(FeatureType type) {
        this.isSimple = type instanceof SimpleFeatureType;
    }

    public Object visit(PropertyName expression, Object extraData) {
        String propertyValue = expression.getPropertyName();
        Object newExpression = null;
        if (extraData instanceof TemplateBuilder) {
            String[] elements;
            if (propertyValue.indexOf(".") != -1) {
                elements = propertyValue.split("\\.");
            } else {
                elements = propertyValue.split("/");
            }
            TemplateBuilder builder = (TemplateBuilder) extraData;
            try {
                currentSource = null;
                currentEl = 0;
                newExpression = findFunction(builder.getChildren(), elements);
                newExpression = findXpathArg(newExpression);
                if (newExpression != null) {
                    return newExpression;
                }
            } catch (Throwable ex) {
                throw new RuntimeException(
                        "Unable to evaluate the json-ld path against"
                                + "the json-ld template. Cause: "
                                + ex.getMessage());
            }
        }
        return getFactory(extraData)
                .property(expression.getPropertyName(), expression.getNamespaceContext());
    }

    private Object findXpathArg(Object newExpression) {
        DuplicatingFilterVisitor duplicatingFilterVisitor =
                new DuplicatingFilterVisitor() {
                    @Override
                    public Object visit(PropertyName filter, Object extraData) {
                        filter = (PropertyName) super.visit(filter, extraData);
                        if (filter instanceof AttributeExpressionImpl) {
                            AttributeExpressionImpl pn = (AttributeExpressionImpl) filter;
                            pn.setPropertyName(completeXPath(pn.getPropertyName()));
                            filter = pn;
                        }
                        return filter;
                    }
                };
        if (newExpression instanceof Expression) {
            return ((Expression) newExpression).accept(duplicatingFilterVisitor, null);
        } else if (newExpression instanceof Filter) {
            return ((Filter) newExpression).accept(duplicatingFilterVisitor, null);
        }
        return null;
    }

    /**
     * Find the corresponding function to which json-ld path is pointing, by iterating over
     * builder's tree
     */
    public Object findFunction(List<TemplateBuilder> children, String[] eles)
            throws ServiceException {
        TemplateBuilder jb = findBuilder(children, eles);
        if (jb != null) {
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder dvb = (DynamicValueBuilder) jb;
                if (currentEl + 1 != eles.length) throw new ServiceException("error");
                if (dvb.getXpath() != null) return super.visit(dvb.getXpath(), null);
                else {
                    return super.visit(dvb.getCql(), null);
                }
            } else if (jb instanceof StaticBuilder) {
                JsonNode staticNode = ((StaticBuilder) jb).getStaticValue();
                while (currentEl < eles.length) {
                    JsonNode child = staticNode.get(eles[currentEl]);
                    staticNode = child != null ? child : staticNode;
                    currentEl++;
                }
                if (currentEl != eles.length) throw new ServiceException("error");
                return FF.literal(staticNode.asText());
            }
        }
        return null;
    }

    /**
     * Find the corresponding function to which json-ld path is pointing, by iterating over
     * builder's tree
     */
    public TemplateBuilder findBuilder(List<TemplateBuilder> children, String[] eles) {
        if (children != null) {
            for (TemplateBuilder jb : children) {
                String key = ((AbstractTemplateBuilder) jb).getKey();
                if (key == null || key.equals(eles[currentEl])) {
                    if (jb instanceof SourceBuilder) {
                        SourceBuilder sb = (SourceBuilder) jb;
                        String source = sb.getStrSource();
                        if (source != null) {
                            if (currentSource != null) {
                                source = "/" + source;
                                currentSource += source;
                            } else {
                                currentSource = source;
                            }
                        }
                        addFilter(sb.getFilter());
                    }
                    if (jb instanceof DynamicValueBuilder || jb instanceof StaticBuilder) {
                        return jb;
                    } else {
                        if (key != null) currentEl++;
                        TemplateBuilder result = findBuilder(jb.getChildren(), eles);
                        if (result != null) {
                            return result;
                        }
                    }
                } else {
                    if (jb.getChildren() != null) {
                        TemplateBuilder result = findBuilder(jb.getChildren(), eles);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Add to the xpath, xpath parts taken from the $source attribute. This is done for Complex
     * Features only
     */
    private String completeXPath(String xpath) {
        if (currentSource != null && !isSimple) xpath = currentSource + "/" + xpath;
        return JsonLdCQLManager.quoteXpathAttribute(xpath);
    }

    private void addFilter(Filter filter) {
        if (filter != null) {
            String f = CQL.toCQL(filter);
            if (!f.contains("@")) {
                filter = (Filter) findXpathArg(filter);
                filters.add(filter);
            }
        }
    }

    public List<Filter> getFilters() {
        return filters;
    }
}
