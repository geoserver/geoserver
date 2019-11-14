/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.builders.impl.XpathHandler;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;

public class FilterFunction_xpath extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "xpath", parameter("result", Object.class), parameter("xpath", String.class));

    public FilterFunction_xpath() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String arg0;
        try { // attempt to evaluate xpath
            arg0 = (String) getExpression(0).evaluate(feature);
            XpathHandler xpathHandler = new XpathHandler();
            Object result = xpathHandler.evaluateXpath(RootBuilder.namespaces, feature, arg0);
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
}
