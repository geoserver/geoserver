/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wfstemplating.builders.AbstractTemplateBuilder;
import org.geoserver.wfstemplating.builders.SourceBuilder;
import org.geoserver.wfstemplating.builders.TemplateBuilder;
import org.geoserver.wfstemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.wfstemplating.builders.impl.StaticBuilder;
import org.geoserver.wfstemplating.expressions.JsonLdCQLManager;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.ows.ServiceException;
import org.geotools.util.logging.Logging;
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

    private int currentEl;
    private String currentSource;
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    static final Logger LOGGER = Logging.getLogger(JsonPathVisitor.class);
    boolean isSimple;

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
                findXpathArg(newExpression);
                if (newExpression != null) {
                    return newExpression;
                }
            } catch (Throwable ex) {
                LOGGER.log(
                        Level.INFO,
                        "Unable to evaluate the json-ld path against"
                                + "the json-ld template. Cause: {0}",
                        ex.getMessage());
            }
        }
        return getFactory(extraData)
                .property(expression.getPropertyName(), expression.getNamespaceContext());
    }

    private void findXpathArg(Object newExpression) {
        DefaultFilterVisitor defaultFilterVisitor =
                new DefaultFilterVisitor() {
                    @Override
                    public Object visit(PropertyName filter, Object extraData) {
                        if (filter instanceof AttributeExpressionImpl) {
                            AttributeExpressionImpl pn = (AttributeExpressionImpl) filter;
                            pn.setPropertyName(completeXPath(pn.getPropertyName()));
                        }
                        return extraData;
                    }
                };
        if (newExpression instanceof Expression) {
            ((Expression) newExpression).accept(defaultFilterVisitor, null);
        } else if (newExpression instanceof Filter) {
            ((Filter) newExpression).accept(defaultFilterVisitor, null);
        }
    }

    /**
     * Find the corresponding function to which json-ld path is pointing, by iterating over
     * builder's tree
     */
    public Object findFunction(List<TemplateBuilder> children, String[] eles)
            throws ServiceException {
        if (children != null) {
            for (TemplateBuilder jb : children) {
                String key = ((AbstractTemplateBuilder) jb).getKey();
                if (key == null || key.equals(eles[currentEl])) {
                    if (jb instanceof SourceBuilder) {
                        String source = ((SourceBuilder) jb).getStrSource();
                        if (source != null) {
                            if (currentSource != null) {
                                source = "/" + source;
                                currentSource += source;
                            } else {
                                currentSource = source;
                            }
                        }
                    }
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
                    } else {
                        if (key != null) currentEl++;
                        Object result = findFunction(jb.getChildren(), eles);
                        if (result != null) {
                            return result;
                        }
                    }
                } else {
                    if (jb.getChildren() != null) {
                        Object result = findFunction(jb.getChildren(), eles);
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
}
