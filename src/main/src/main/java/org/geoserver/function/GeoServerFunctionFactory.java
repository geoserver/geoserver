/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

/**
 * This class implements the {@link FunctionFactory} interface and can be used for creating a new
 * Function from the input parameters. Actually it implements only the {@link IsInstanceOf}
 * function.
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class GeoServerFunctionFactory implements FunctionFactory {

    @Override
    public List<FunctionName> getFunctionNames() {
        // Simple creation of the FunctionName list
        List<FunctionName> functionList = new ArrayList<FunctionName>();
        functionList.add(IsInstanceOf.NAME);
        return Collections.unmodifiableList(functionList);
    }

    @Override
    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        // Check if the name belongs to the IsInstanceOf NAME, otherwise null
        // is returned
        if (IsInstanceOf.NAME.getFunctionName().equals(name)) {
            return new IsInstanceOf(args, fallback);
        }
        return null;
    }
}
