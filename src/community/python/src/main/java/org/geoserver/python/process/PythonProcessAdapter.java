/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.FileWatcher;
import org.geoserver.python.Python;
import org.geotools.data.Parameter;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySequenceList;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

public class PythonProcessAdapter {

    Map<String,PyObject> processes; 
    FileWatcher<Map<String,PyObject>> fw;
    
    public PythonProcessAdapter(File module, final Python py){
        fw = new FileWatcher<Map<String,PyObject>>(module) {
            @Override
            protected Map<String,PyObject> parseFileContents(InputStream in) throws IOException {
                PythonInterpreter pi = py.interpreter();
                pi.execfile(in);

                Map<String,PyObject> processes = new LinkedHashMap();
                PyStringMap locals = (PyStringMap) pi.getLocals();
                for (Object o : locals.keys()) {
                    String key = (String) o;
                    PyObject obj = locals.__getitem__(key);
                    if (obj instanceof PyFunction ) {
                        try {
                            if (obj.__getattr__("__process__") != null) {
                                processes.put(key, obj);
                            }
                        }
                        catch(PyException e) {}
                    }
                }
                return processes;
            }
        };
    }
    
    public List<String> getNames() {
        return new ArrayList(processes().keySet());
    }
    
    public String getTitle(String name) {
        return process(name).__getattr__("title").toString();
    }
    
    public String getDescription(String name) {
        return process(name).__getattr__("description").toString();
    }
    
    public String getVersion(String name) {
        return process(name).__getattr__("version").toString();
    }
    
    public Map<String,Parameter<?>> getInputParameters(String name) {
        try {
            PyList args = (PyList) process(name).__getattr__("args");
            return parameters(args);
        }
        catch(Exception e) {
            throw new RuntimeException("Error occurred looking up inputs for process " + name, e);
        }
    }
    
    public Map<String,Parameter<?>> getOutputParameters(String name) {
        try {
            PySequenceList args = (PySequenceList) process(name).__getattr__("result");
            PyList list = new PyList();
            list.add(args);
            return parameters(list);
        }
        catch(Exception e) {
            throw new RuntimeException("Error occurred looking up result for process " + name, e);
        }
    }
    
    public Map<String,Object> run(String name, Map<String,Object> inputs) throws Exception {
        PyObject run = process(name);
        
        List<PyObject> args = new ArrayList();
        List<String> kw = new ArrayList();
        
        for (Map.Entry<String,Object> input : inputs.entrySet()) {
            args.add(Py.java2py(input.getValue()));
            kw.add(input.getKey());
        }
        
        PyObject r = run.__call__(args.toArray(new PyObject[args.size()]), 
            kw.toArray(new String[kw.size()]));
        Collection result = null;
      
        if (r instanceof Collection) {
            result = (Collection) r;
        }
        else {
            result = new ArrayList();
            result.add(r);
        }
        
        Map<String,Parameter<?>> outputs = getOutputParameters(name);
        if (result.size() != outputs.size()) {
            throw new IllegalStateException(String.format("Process returned %d values, should have " +
                "returned %d", result.size(), outputs.size()));
        }
        
        Map<String,Object> results = new LinkedHashMap<String, Object>();
        Iterator it = result.iterator();
        for (Parameter output : outputs.values()) {
            results.put(output.key, ((PyObject)it.next()).__tojava__(output.type));
        }
            
        return results;
    }
    
    protected Map<String,Parameter<?>> parameters(PyList list) {
        LinkedHashMap<String, Parameter<?>> inputs = new LinkedHashMap();
        
        for (PyObject obj : (List<PyObject>) list) {
            String arg = obj.__getitem__(0).toString();
           
            Object type = null;
            try {
                type = obj.__getitem__(1);
            }
            catch(PyException e) {}
            
            Object desc = null;
            try {
                desc = obj.__getitem__(2);
            }
            catch(PyException e) {}
             
            Class clazz = null;
            if (type != null && type instanceof PyType) {
                clazz = Python.toJavaClass((PyType)type);
            }
            if (clazz == null) {
                clazz = Object.class;
            }

            if (desc != null) {
                desc = desc.toString();
            }
            else {
                desc = arg;
            }
            
            inputs.put(arg, new Parameter(arg, clazz, arg, desc.toString()));
        }
        return inputs;
    }
   
    
    protected Map<String,PyObject> processes() {
        if (fw.isModified()) {
            synchronized(this) {
                if (fw.isModified()) {
                    try {
                        processes = fw.read();
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        return processes;
    }
    
    protected PyObject process(String name) {
        PyObject obj = processes().get(name);
        if (obj == null) {
            throw new IllegalArgumentException("No such process '" + name + "'");
        }
        
        return obj;
    }
}
