/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.function;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Predicates;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.VolatileFunction;

/**
 * This class implements the {@link Function} interface and can be used for checking if an object is
 * an instance of the provided input class.
 *
 * <p>Users can call this function using the {@link Predicates} class:
 *
 * <p>Predicates.isInstanceOf(Class clazz);
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class IsInstanceOf implements VolatileFunction, Function {

    /** Function name and related parameters */
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "isInstanceOf",
                    Boolean.class,
                    FunctionNameImpl.parameter("class", Class.class));

    /** Function parameters */
    private List<Expression> parameters;

    /** Fallback value used as default */
    private Literal fallback;

    public IsInstanceOf() {
        this.parameters = new ArrayList<Expression>();
        this.fallback = null;
    }

    protected IsInstanceOf(List<Expression> parameters, Literal fallback) {
        this.parameters = parameters;
        this.fallback = fallback;
        // Check on the parameters
        if (parameters == null) {
            throw new NullPointerException("parameter required");
        }
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("isInstanceOf(class) requires one parameter only");
        }
    }

    @Override
    public Object evaluate(Object object) {
        return evaluate(object, Boolean.class);
    }

    @Override
    public <T> T evaluate(Object object, Class<T> context) {
        // Selection of the first expression
        Expression clazzExpression = parameters.get(0);

        // Getting the defined class
        Class clazz = clazzExpression.evaluate(object, Class.class);

        // Checking the result
        boolean result = false;

        // If the input class is Object, the function always returns true
        if (clazz != null) {
            if (clazz == Object.class) {
                result = true;
            } else {
                // Otherwise the function checks if the class is an instance of the
                // input class
                result = clazz.isAssignableFrom(object.getClass());
            }
        }

        // Finally the result is converted to the defined context class
        return Converters.convert(result, context);
    }

    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public String getName() {
        return NAME.getName();
    }

    @Override
    public FunctionName getFunctionName() {
        return NAME;
    }

    @Override
    public List<Expression> getParameters() {
        return parameters;
    }

    @Override
    public Literal getFallbackValue() {
        return fallback;
    }

    @Override
    public String toString() {
        List<Expression> params = getParameters();
        if (params == null || params.size() == 0) {
            return "IsInstanceOf([INVALID])";
        } else {
            return "IsInstanceOf(" + params.get(0) + ")";
        }
    }
}
