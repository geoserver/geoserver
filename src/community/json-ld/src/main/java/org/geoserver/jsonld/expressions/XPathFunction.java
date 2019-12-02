/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/** Cql functions that allows to evaluate an xpath against the object provided */
public class XPathFunction extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "xpath", parameter("result", Object.class), parameter("xpath", String.class));
    private NamespaceSupport namespaces;

    public XPathFunction() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String arg0;
        try { // attempt to evaluate xpath
            arg0 = (String) getExpression(0).evaluate(feature);
            Expression xpath = new AttributeExpressionImpl(arg0, namespaces);
            Object result = xpath.evaluate(feature);
            if (result != null) {
                if (!(result instanceof Geometry)) {
                    feature = ((GeometryAttributeImpl) result).getValue();
                } else {
                    feature = result;
                }
            }
        } catch (Exception e) // probably a type error
        {
            throw new IllegalArgumentException(
                    "Filter Function problem for function strEndsWith argument #0 - expected type String");
        }
        return feature;
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(NamespaceSupport namespaces) {
        this.namespaces = namespaces;
    }
}
