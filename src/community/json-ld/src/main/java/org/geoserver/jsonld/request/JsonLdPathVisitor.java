/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.jsonld.builders.AbstractJsonBuilder;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.SourceBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.StaticBuilder;
import org.geoserver.jsonld.expressions.XPathFunction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.ows.ServiceException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * This visitor search for a Filter in {@link JsonBuilder} tree using the json-ld path provided as a
 * guideline.
 */
public class JsonLdPathVisitor extends DuplicatingFilterVisitor {

    private int currentEl;
    private String currentSource;
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    static final Logger LOGGER = Logging.getLogger(JsonLdPathVisitor.class);

    public Object visit(PropertyName expression, Object extraData) {
        String propertyValue = expression.getPropertyName();
        Object newExpression = null;
        if (extraData instanceof JsonBuilder) {
            String[] elements;
            if (propertyValue.indexOf(".") != -1) {
                elements = propertyValue.split("\\.");
            } else {
                elements = propertyValue.split("/");
            }
            JsonBuilder builder = (JsonBuilder) extraData;
            try {
                currentSource = null;
                currentEl = 0;
                newExpression = findXpath(builder.getChildren(), elements);
                workExpression(newExpression);
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

    private void workExpression(Object newExpression) {
        if (newExpression instanceof AttributeExpressionImpl) {
            AttributeExpressionImpl pn = (AttributeExpressionImpl) newExpression;
            pn.setPropertyName(
                    currentSource != null
                            ? currentSource + "/" + pn.getPropertyName()
                            : pn.getPropertyName());
        } else if (newExpression instanceof XPathFunction) {
            XPathFunction xpath = (XPathFunction) newExpression;
            LiteralExpressionImpl param = (LiteralExpressionImpl) xpath.getParameters().get(0);
            param.setValue(
                    currentSource != null
                            ? currentSource + "/" + param.getValue()
                            : param.getValue());
        } else if (newExpression instanceof FunctionExpressionImpl) {
            FunctionExpressionImpl function = (FunctionExpressionImpl) newExpression;
            for (Expression ex : function.getParameters()) {
                workExpression(ex);
            }
        }
    }

    public Object findXpath(List<JsonBuilder> children, String[] eles) throws ServiceException {
        if (children != null) {
            for (JsonBuilder jb : children) {
                if (((AbstractJsonBuilder) jb).getKey() != null
                        && ((AbstractJsonBuilder) jb).getKey().equals(eles[currentEl])) {
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
                        else return super.visit(dvb.getCql(), null);
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
                        currentEl++;
                        Object result = findXpath(jb.getChildren(), eles);
                        if (result != null) {
                            return result;
                        }
                    }
                } else {
                    if (jb.getChildren() != null) {
                        Object result = findXpath(jb.getChildren(), eles);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }
}
