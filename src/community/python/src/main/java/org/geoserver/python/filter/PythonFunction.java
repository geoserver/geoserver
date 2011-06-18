package org.geoserver.python.filter;

import java.util.List;

import org.geotools.filter.FunctionImpl;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

public class PythonFunction extends FunctionImpl implements VolatileFunction {

    PythonFilterFunctionAdapter adapter;
    
    public PythonFunction(String name, List<Expression> args, PythonFilterFunctionAdapter adapter) {
        setName(name);
        setParameters(args);
        this.adapter = adapter;
    }
    
    @Override
    public Object evaluate(Object object) {
        return adapter.evaluate(getName(), object, getParameters());
    }
    
}
