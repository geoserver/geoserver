/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.filter;

import java.util.List;

import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

public class PythonFunction extends FunctionImpl implements VolatileFunction {

    PythonFilterFunctionAdapter adapter;
    
    public PythonFunction(Name name, List<Expression> args, PythonFilterFunctionAdapter adapter) {
        setName(name.getLocalPart());
        setParameters(args);
        functionName = new FunctionNameImpl(name, args.size());
        this.adapter = adapter;
    }
    
    @Override
    public Object evaluate(Object object) {
        return adapter.evaluate(getName(), object, getParameters());
    }
    
}
