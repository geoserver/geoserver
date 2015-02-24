/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.python.Python;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Parameter;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

public class PythonDataStoreFactory implements DataStoreFactorySpi {

    static Param TYPE = new Param("__type__", String.class, "", true, "python");
    
    PythonDataStoreAdapter adapter;
    
    public PythonDataStoreFactory(PythonDataStoreAdapter adapter) {
        this.adapter = adapter;
    }
    
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return adapter.getDataStore((Map)params);
    }

    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return null;
    }

    public boolean canProcess(Map<String, Serializable> params) {
        for (Parameter p : adapter.getParameters()) {
            if (!params.containsKey(p.key)) {
                return false;
            }
        }
        
        return "python".equals(params.get(TYPE.key));
    }

    public String getDisplayName() {
        return adapter.getTitle();
    }
    
    public String getDescription() {
        return adapter.getDescription();
    }

    public Param[] getParametersInfo() {
        List<Param> params = adapter.getParameters();
        Param[] info = new Param[params.size()+1];
        for (int i = 0; i < params.size(); i++) {
            info[i] = params.get(i);
        }
        
        info[params.size()] = TYPE;
        return info;
    }

    public boolean isAvailable() {
        return true;
    }

    public Map<Key, ?> getImplementationHints() {
        return null;
    }

}
