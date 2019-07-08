/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.HashMap;
import java.util.Properties;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.script.wps.WpsHook;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.jsr223.PyScriptEngineFactory;
import org.python.util.PythonInterpreter;

/**
 * Python script plugin.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PythonPlugin extends ScriptPlugin {

    public PythonPlugin() {
        super("py", PyScriptEngineFactory.class);
    }

    @Override
    public void init(ScriptManager scriptMgr) throws Exception {
        // add lib to python.path
        Properties props = new Properties();
        props.put("python.path", scriptMgr.script("lib/" + "py").dir().getAbsolutePath());
        PythonInterpreter.initialize(null, props, null);

        //        codecs.register(new PyObject() {
        //            @Override
        //            public PyObject __call__(PyObject arg0) {
        //                if ("idna".equals(arg0.toString())) {
        //                    return new PyTuple(
        //                        new PyObject() {
        //                            public PyObject __call__(PyObject v) {
        //                                return new PyTuple(new
        // PyString(IDN.toUnicode(v.toString())), new PyInteger(0));
        //                            }
        //                        },
        //                        new PyObject() {
        //                            public PyObject __call__(PyObject v) {
        //                                return new PyTuple(new
        // PyString(IDN.toASCII(v.toString())), new PyInteger(0));
        //                            }
        //                        }
        //                    );
        //                }
        //                return Py.None;
        //            }
        //        });

    }

    @Override
    public String getId() {
        return "python";
    }

    @Override
    public String getDisplayName() {
        return "Python";
    }

    @Override
    public AppHook createAppHook() {
        return new PyAppHook(this);
    }

    @Override
    public WpsHook createWpsHook() {
        return new PyWpsHook(this);
    }

    @Override
    public WfsTxHook createWfsTxHook() {
        return new PyWfsTxHook(this);
    }

    static HashMap<Class<? extends PyObject>, Class> pyToJava = new HashMap();

    static {
        pyToJava.put(PyString.class, String.class);
        pyToJava.put(PyInteger.class, Integer.class);
        pyToJava.put(PyLong.class, Long.class);
        pyToJava.put(PyFloat.class, Double.class);
        pyToJava.put(PyBoolean.class, Boolean.class);
        // pyToJava.put(PyFile.class, File.class);
    }

    public static Class toJavaClass(PyType type) {
        Class clazz = null;
        try {
            Object o = Py.tojava(type, Object.class);
            if (o != null && o instanceof Class) {
                clazz = (Class) o;
            }
        } catch (PyException e) {
        }

        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            try {
                PyObject pyobj = (PyObject) clazz.getDeclaredConstructor().newInstance();
                Object obj = pyobj.__tojava__(Object.class);
                if (obj != null) {
                    clazz = obj.getClass();
                }
            } catch (Exception e) {
            }
        }

        if (clazz != null && PyObject.class.isAssignableFrom(clazz)) {
            Class jclass = pyToJava.get(clazz);
            if (jclass != null) {
                clazz = jclass;
            }
        }

        if (clazz != null && clazz.getName().startsWith("org.python.proxies")) {
            // get base type
            PyType base = (PyType) type.getBase();
            Class c = toJavaClass(base);
            if (c != null) {
                clazz = c;
            }
        }
        return clazz;
    }
}
