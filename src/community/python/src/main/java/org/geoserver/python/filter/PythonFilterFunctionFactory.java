/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.filter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.python.Python;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.util.SoftValueHashMap;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

public class PythonFilterFunctionFactory implements FunctionFactory {

    Python py;
    SoftValueHashMap<Name, PythonFilterFunctionAdapter> adapters = new SoftValueHashMap(10);
    
    public List<FunctionName> getFunctionNames() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        Python py = py();
        
        List<FunctionName> names = new ArrayList<FunctionName>();
        try {
            File filterRoot = py.getFilterRoot();
            for (String file : filterRoot.list()) {
                if (file.endsWith(".py")) {
                    PythonFilterFunctionAdapter adapter = 
                        new PythonFilterFunctionAdapter(new File(filterRoot, file), py);
                    for(String name : adapter.getNames()) {
                        FunctionName fname = 
                            ff.functionName(name, adapter.getParameterNames(name).size());
                        names.add(fname);
                    }
                }
            }
        }
        catch (IOException e) {
            Python.LOGGER.log(Level.WARNING, "Error looking up filter functions", e);
        }
        
        return names;
    }
    
    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    public Function function(Name name, List<Expression> args, Literal fallback) {
        PythonFilterFunctionAdapter adapter = adapter(name);
        if (adapter == null) {
            return null;
        }
        return new PythonFunction(name, args, adapter);
    };
    
    Python py() {
        if (py == null) {
            py = GeoServerExtensions.bean(Python.class);
        }
        return py;
    }
    
    PythonFilterFunctionAdapter adapter(Name name) {
        PythonFilterFunctionAdapter adapter = adapters.get(name);
        if (adapter == null) {
            synchronized(this) {
                adapter = adapters.get(name);
                if (adapter == null) {
                    try {
                        adapter = createFilterFunctionAdapter(name);
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    
                    if (adapter != null) {
                        adapters.put(name, adapter);
                    }
                }
            }
        }
        return adapter;
    }

    private PythonFilterFunctionAdapter createFilterFunctionAdapter(Name name) 
        throws IOException {
        
        for (File f : py().getFilterRoot().listFiles()) {
            PythonFilterFunctionAdapter adapter = new PythonFilterFunctionAdapter(f, py());
            if (adapter.getNames().contains(name.getLocalPart())) {
                return adapter; 
            }
        }
        
        return null;
    }
}
