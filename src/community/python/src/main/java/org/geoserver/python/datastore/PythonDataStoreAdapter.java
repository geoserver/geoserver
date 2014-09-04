/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.FileWatcher;
import org.geoserver.python.Python;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Parameter;
import org.geotools.data.DataAccessFactory.Param;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyList;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

public class PythonDataStoreAdapter {

    Python py;
    FileWatcher<PyType> fw;
    PyType type;
    
    public PythonDataStoreAdapter(File file, Python py) {
        this.py = py;
        fw = new FileWatcher<PyType>(file) {
            @Override
            protected PyType parseFileContents(InputStream in) throws IOException {
                PythonInterpreter pi = PythonDataStoreAdapter.this.py.interpreter();
                pi.execfile(in);
                
                PyStringMap locals = (PyStringMap) pi.getLocals();
                for (Object obj : locals.values()) {
                    if (obj instanceof PyType) {
                        PyType pobj = (PyType) obj;
                        try {
                            PyObject init = pobj.__getattr__("__init__");
                            if (init != null) {
                                if (init.__getattr__("__datastore__") != null) {
                                    return (PyType) pobj;
                                }
                            }
                        }
                        catch(PyException e) {}
                    }
                }
                
                return null;
            }
        };
    }
    
    public String getTitle() {
        return type().__getattr__("__init__").__getattr__("title").toString();
    }
    
    public String getDescription() {
        PyObject desc = type().__getattr__("__init__").__getattr__("description");
        if (desc.getType() == PyNone.TYPE) {
            return getTitle();
        }
        
        return desc.toString();
    }
    
    public List<Param> getParameters() {
        PyType type = type();
        
        PythonInterpreter pi = py.interpreter();
        pi.exec("from inspect import getargspec");
        
        PyFunction getargspec = (PyFunction) pi.get("getargspec"); 
        PyObject init =  type.__getattr__("__init__");
        
        PySequence argspec = 
            (PySequence) getargspec.__call__(init.__getattr__("wrapped")).__getitem__(0);
        PyDictionary params = (PyDictionary) init.__getattr__("params");
        
        List<Param> list = new ArrayList();
        for (Object item : (Collection) argspec) {
            String pname = item.toString();
            if ("self".equals(pname)) continue;
            
            PySequence pinfo = (PySequence) params.get(pname);
            
            String pdesc = pinfo != null ? pinfo.__getitem__(0).toString() : pname;
            Object ptype = null;
            try {
                ptype = pinfo != null ? pinfo.__getitem__(1) : null;
            }
            catch(PyException e) {}
             
            Class clazz = null;
            if (ptype != null && ptype instanceof PyType) {
                clazz = Python.toJavaClass((PyType)ptype);
            }
            if (clazz == null) {
                clazz = Object.class;
            }

            Map metadata = null;
            if ("passwd".equalsIgnoreCase(pname) || "password".equalsIgnoreCase(pname)) {
                metadata = Collections.singletonMap(Parameter.IS_PASSWORD, true);
            }
            list.add(new Param(pname, clazz, pdesc, true, null, metadata ));
        }
        
        return list;
    }
    
    public DataStoreFactorySpi getDataStoreFactory() {
        return new PythonDataStoreFactory(this);
    }
    
    public PythonDataStore getDataStore(Map<String,Object> parameters) throws IOException {
       return new PythonDataStore(parameters, this);
    }

    PyObject getWorkspace(Map<String,Object> parameters) {
        List<PyObject> args = new ArrayList();
        List<String> kw = new ArrayList();
        
        int i = 0;
        for (Map.Entry<String,Object> e : parameters.entrySet()) {
            if (PythonDataStoreFactory.TYPE.key.equals(e.getKey())) 
                continue;
            
            args.add(Py.java2py(e.getValue()));
            kw.add(e.getKey());
        }
        
        return type().__call__(args.toArray(new PyObject[args.size()]), 
                kw.toArray(new String[kw.size()]));
    }
    
    PyType type() {
        if (fw.isModified()) {
            synchronized(this) {
                if (fw.isModified()) {
                    try {
                        type = fw.read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        return type;
    }
}
