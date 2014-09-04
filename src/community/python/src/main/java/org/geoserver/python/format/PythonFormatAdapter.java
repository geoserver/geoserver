/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.geoserver.platform.FileWatcher;
import org.geoserver.python.Python;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.util.PythonInterpreter;

public abstract class PythonFormatAdapter {

    protected Python py;
    protected FileWatcher<PyObject> fw;
    protected PyObject pobj;

    public PythonFormatAdapter(File module, Python py) {
        this.py = py;
        this.fw = new FileWatcher<PyObject>(module) {
            @Override
            protected PyObject parseFileContents(InputStream in) throws IOException {
                PythonInterpreter pi = PythonFormatAdapter.this.py.interpreter();
                pi.execfile(in);
                
                PyStringMap locals = (PyStringMap) pi.getLocals();
                for (Object o : locals.keys()) {
                    String key = (String) o;
                    PyObject obj = locals.__getitem__(key);
                    if (obj instanceof PyFunction ) {
                        try {
                            if (obj.__getattr__(getMarker()) != null) {
                                return obj;
                            }
                        }
                        catch(PyException e) {}
                    }
                }
                
                throw new IllegalStateException("No output format function found in " 
                    + fw.getFile().getAbsolutePath());
            }
        };
    }
    
    public String getName() {
        return pyObject().__getattr__("name").toString();
    }
    
    public String getMimeType() {
        return pyObject().__getattr__("mime").toString();
    }

    protected PyObject pyObject() {
        if (fw.isModified()) {
            synchronized (fw) {
                if (fw.isModified()) {
                    try {
                        pobj = fw.read();
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        
        return pobj;
    }

    protected abstract String getMarker();
}
