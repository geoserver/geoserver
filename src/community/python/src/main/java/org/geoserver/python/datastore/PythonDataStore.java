/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.data.Query;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.python.core.PyList;
import org.python.core.PyMethod;
import org.python.core.PyObject;

/**
 * A DataStore implementation that adapts a geoscript workspace.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PythonDataStore extends ContentDataStore {

    Map<String,Object> parameters;
    PythonDataStoreAdapter adapter;
    
    public PythonDataStore(Map<String,Object> parameters, PythonDataStoreAdapter adapter) {
        this.parameters = parameters;
        this.adapter = adapter;
    }
    
    @Override
    protected PythonFeatureStore createFeatureSource(ContentEntry entry) throws IOException {
        return new PythonFeatureStore(entry, Query.ALL);
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        PyObject workspace = getWorkspace();
        
        PyMethod layers = (PyMethod) workspace.__findattr__("layers");
        if (layers == null) {
            layers = (PyMethod) workspace.__findattr__("keys");
        }
        PyList result = (PyList) layers.__call__();
        
        List<Name> typeNames = new ArrayList<Name>();
        for (Object o : result.toArray()) {
            typeNames.add(new NameImpl(o.toString()));
        }
        return typeNames;
    }

    PyObject getWorkspace() {
        return adapter.getWorkspace(parameters);
    }
}
