/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.datastore.PythonDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

public class Python {

    public static Logger LOGGER = Logging.getLogger("org.geoserver.jython");
    
    static HashMap<Class<? extends PyObject>, Class> pyToJava = new HashMap();
    static {
        pyToJava.put(PyString.class, String.class);
        pyToJava.put(PyInteger.class, Integer.class);
        pyToJava.put(PyLong.class, Long.class);
        pyToJava.put(PyFloat.class, Double.class);
        pyToJava.put(PyBoolean.class, Boolean.class);
        //pyToJava.put(PyFile.class, File.class);
    }
    
    public static Class toJavaClass(PyType type) {
        Class clazz = null;
        try {
            Object o = Py.tojava(type, Object.class);
            if (o != null && o instanceof Class) {
                clazz = (Class) o;
            }
        }
        catch(PyException e) {}
        
        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            try {
                PyObject pyobj = (PyObject) clazz.newInstance();
                Object obj = pyobj.__tojava__(Object.class);
                if (obj != null) {
                    clazz = obj.getClass();
                }
            }
            catch(Exception e) {}
        }
        
        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            Class jclass = pyToJava.get(clazz);
            if (jclass != null) {
                clazz = jclass;
            }
        }
        
        if (clazz != null && clazz.getName().startsWith("org.python.proxies")) {
            //get base type
            PyType base = (PyType) type.getBase();
            Class c = toJavaClass(base);
            if (c != null) {
                clazz = c;
            }
        }
         return clazz;
    }
    
    GeoServerResourceLoader resourceLoader;
    
    boolean initialized = false;
    
    public Python(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
     public PythonInterpreter interpreter() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialize();
                    initialized = true;
                }
            }
        }
        
        return new PythonInterpreter();
    }
    
    void initialize() {
        //copy libs into <DATA_DIR>/python/lib
        try {
            initLibs();
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        //initialize the python path
        ArrayList<String> pythonPath = new ArrayList();
        
        //look for a jython installation on the system
        /*String jythonHome = System.getenv("JYTHON_HOME");
        if (jythonHome != null) {
            pythonPath.add(jythonHome+File.separator+"/Lib");
        }*/
        
        //add <GEOSERVER_DATA_DIR>/jython/lib to path
        try {
            pythonPath.add(getLibRoot().getCanonicalPath());
            pythonPath.add(getDataStoreRoot().getCanonicalPath());
        } 
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to access Jython lib directory", e);
        }
        
        StringBuffer path = new StringBuffer();
        for (String loc : pythonPath) {
            path.append(loc).append(File.pathSeparator);
        }
        path.setLength(path.length()-1);
        
        Properties props = new Properties();
        props.put("python.path", path.toString());
        
        PythonInterpreter.initialize(null, props, null);
    }
    
    void initLibs() throws IOException {
        File libRoot = getLibRoot();
        //File gsPyRoot = resourceLoader.findOrCreateDirectory(libRoot, "geoserver");
        
        ClassLoader cl = getClass().getClassLoader(); 
        //IOUtils.copyStream(cl.getResourceAsStream("geoserver/__init__.py"), 
        //    new FileOutputStream(new File(gsPyRoot, "__init__.py")), true, true);
        //IOUtils.copyStream(cl.getResourceAsStream("geoserver/catalog.py"), 
        //        new FileOutputStream(new File(gsPyRoot, "catalog.py")), true, true);
        //IOUtils.copyStream(cl.getResourceAsStream("geoserver/layer.py"), 
        //        new FileOutputStream(new File(gsPyRoot, "layer.py")), true, true);
        
        //ZipFile f = new ZipFile(new File(cl.getResource("geoscript.zip").getFile()));
        //new File(libRoot, "geoscript").mkdir();
        //IOUtils.inflate(f, libRoot, null);
        
    }
    
    public File getRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python");
    }
    
    public File getScriptRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "script");
    }
    
    public File getAppRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "app"); 
    }
    
    public String getLibPath() throws IOException {
        return "python" + File.separator + "lib";
    }
    
    public File getLibRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory(getLibPath());
    }

    public File getDataStoreRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "datastore");
    }
    
    public File getProcessRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "process");
    }
    
    public File getFormatRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "format");
    }
    
    public File getFilterRoot() throws IOException {
        return resourceLoader.findOrCreateDirectory("python", "filter");
    }
    
}
