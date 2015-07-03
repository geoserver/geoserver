/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.FileWatcher;
import org.geoserver.python.Python;
import org.geotools.data.DataAccessFactory.Param;
import org.opengis.filter.expression.Expression;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.modules.synchronize;
import org.python.util.PythonInterpreter;

public class PythonFilterFunctionAdapter {

    protected Python py;
    protected FileWatcher<Map<String,PyObject>> fw;
    protected Map<String,PyObject> functions;
    
    public PythonFilterFunctionAdapter(File module, Python py) {
        this.py = py;
        this.fw = new FileWatcher<Map<String,PyObject>>(module) {
            @Override
            protected Map<String,PyObject> parseFileContents(InputStream in) throws IOException {
                PythonInterpreter pi = PythonFilterFunctionAdapter.this.py.interpreter();
                pi.execfile(in);
                
                Map<String,PyObject> functions = new LinkedHashMap();
                PyStringMap locals = (PyStringMap) pi.getLocals();
                for (Object o : locals.keys()) {
                    String key = (String) o;
                    PyObject obj = locals.__getitem__(key);
                    try {
                        if (obj.__getattr__("__filter_function__") != null) {
                            functions.put(key, obj);
                        }
                    }
                    catch(PyException e) {}

                }
                 
                return functions;
            }
        };
    }
    
    public List<String> getNames() {
        return new ArrayList(functions().keySet());
    }
    
    public List<String> getParameterNames(String name) {
        PyObject obj = function(name);
        PythonInterpreter pi = py.interpreter();
        pi.exec("from inspect import getargspec");
        
        PyFunction getargspec = (PyFunction) pi.get("getargspec"); 
        PySequence argspec = 
            (PySequence) getargspec.__call__(obj.__getattr__("wrapped")).__getitem__(0);
        
        List<String> list = new ArrayList();
        int i = 0;
        for (Object item : (Collection) argspec) {
            //skip first argument because that is what gets evaluated by 
            // the filter and is not considered an "argument"
            if (i++ == 0) continue;
            String pname = item.toString();
            list.add(pname);
        } 
        return list;
    }
    
    protected PyObject function(String name) {
        return functions().get(name);
    }
    
    protected Map<String,PyObject> functions() {
        if (fw.isModified()) {
            synchronized(this) {
                if (fw.isModified()) {
                    try {
                        functions = fw.read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        return functions;
    }

    public Object evaluate(String name, Object object, List<Expression> args) {
        if (args == null) args = Collections.EMPTY_LIST;
        
        PyObject func = function(name);
        PyObject[] params = new PyObject[args.size()+1];
        params[0] = Py.java2py(object);
        
        for (int i = 0; i < args.size(); i++) {
            params[i+1] = Py.java2py(args.get(i).evaluate(object));
        }
        
        return func.__call__(params).__tojava__(Object.class);
    }
}
