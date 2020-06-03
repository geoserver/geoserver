/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geotools.filter.function.CategorizeFunction;
import org.geotools.filter.function.RecodeFunction;
import org.geotools.renderer.style.StyleAttributeExtractor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/** Extracts all classification functions */
class ClassificationFunctionsVisitor extends StyleAttributeExtractor {

    Set<Function> classificationFunctions = new HashSet<>();
    boolean otherFunctions = false;

    public Set<Function> getClassificationFunctions() {
        return classificationFunctions;
    }

    public boolean hasRecode() {
        return classificationFunctions.stream().anyMatch(f -> f instanceof RecodeFunction);
    }

    public boolean hasCategorize() {
        return classificationFunctions.stream().anyMatch(f -> f instanceof CategorizeFunction);
    }

    public boolean hasOtherFunctions() {
        return otherFunctions;
    }

    @Override
    public Object visit(Function expression, Object data) {
        if (expression instanceof RecodeFunction || expression instanceof CategorizeFunction) {
            this.classificationFunctions.add(expression);
        } else {
            otherFunctions = true;
        }
        return null;
    }

    /**
     * Returns the set of property names used in the classification functions as first argument Will
     * throw exceptions if the first parameter is not a PropertyName
     *
     * @return
     */
    public Set<String> getClassificationProperty() {
        Set<String> result = new HashSet<>();
        for (Function function : classificationFunctions) {
            List<Expression> parameters = function.getParameters();
            PropertyName pn = (PropertyName) parameters.get(0);
            result.add(pn.getPropertyName());
        }

        return result;
    }

    /**
     * Extracts all recode keys. If there are no recode functions, or the keys are not literals,
     * exceptions will be thrown
     *
     * @return
     */
    public Set<List<Object>> getRecodeKeys() {
        Set<List<Object>> result = new HashSet<>();
        for (Function function : classificationFunctions) {
            List<Expression> parameters = function.getParameters();
            List<Object> values = new ArrayList<>();
            for (int i = 1; i < parameters.size(); i += 2) {
                Literal literal = (Literal) parameters.get(i);
                values.add(literal.evaluate(null));
            }
            result.add(values);
        }

        return result;
    }

    /**
     * Extracts all categorize keys. If there are no categorize functions, or the keys are not
     * literals, exceptions will be thrown
     *
     * @return
     */
    public Set<List<Object>> getCategorizeKeys() {
        Set<List<Object>> result = new HashSet<>();
        for (Function function : classificationFunctions) {
            List<Expression> parameters = function.getParameters();
            List<Object> values = new ArrayList<>();
            for (int i = 2; i < parameters.size(); i += 2) {
                Literal literal = (Literal) parameters.get(i);
                values.add(literal.evaluate(null));
            }
            result.add(values);
        }

        return result;
    }
}
