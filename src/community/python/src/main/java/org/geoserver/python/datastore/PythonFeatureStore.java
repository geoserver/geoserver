/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.python.core.PyInstance;
import org.python.core.PyJavaType;
import org.python.core.PyList;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

public class PythonFeatureStore extends ContentFeatureSource implements SimpleFeatureSource {

    PyObject layer;
    PyObject schema;
    
    public PythonFeatureStore(ContentEntry entry, Query query) {
        super(entry, query);
        
        PythonDataStore dataStore = (PythonDataStore) entry.getDataStore();
        PyObject workspace = dataStore.getWorkspace();
        
        PyMethod get = (PyMethod) workspace.__findattr__("get");
        if (get == null) {
            get = (PyMethod) workspace.__findattr__("__getitem__");
        }
        this.layer = get.__call__(new PyObject[]{new PyString(entry.getName().getLocalPart())});
        this.schema = layer.__findattr__("schema");
    
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(((PyString)schema.__findattr__("name")).asString());

        PyObject proj = schema.__findattr__("proj");
        if (proj != null) {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem)
                proj.__getattr__("_crs").__tojava__(CoordinateReferenceSystem.class);
            if (crs != null) {
                tb.setCRS(crs);
            }
        }
       
        PyList fields = (PyList) schema.__findattr__("fields");
        for (Object o : fields.toArray()) {
            PyObject field = (PyObject) o;
            
            String name = ((PyString)field.__findattr__("name")).asString();
            
            PyType type = (PyType)field.__findattr__("typ");
            Class clazz = (Class) type.__tojava__(Object.class);
            
            tb.add(name, unwrapClass(clazz));
        }
        return tb.buildFeatureType();
    }

    Class unwrapClass(Class clazz) {
        if (PyString.class.isAssignableFrom(clazz)) {
            return String.class;
        }
        return clazz;
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        PyMethod bounds = (PyMethod) layer.__findattr__("bounds");
        PyObject filter = convertFilter(query.getFilter());
        
        PyObject result = bounds.__call__(filter != null ? new PyObject[]{filter} : new PyObject[]{});
        
        //do not return the actual envelope synce it is a python object
        return new ReferencedEnvelope((ReferencedEnvelope) result.__tojava__(ReferencedEnvelope.class));
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        PyMethod count = (PyMethod) layer.__findattr__("count");
        
        PyObject filter = convertFilter(query.getFilter());
        PyObject result = count.__call__(filter != null ? new PyObject[]{filter} : new PyObject[]{});
        return (Integer) result.__tojava__(Integer.class);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        return new PythonFeatureReader(getSchema(), layer);
    }
    
    PyObject convertFilter(Filter f) {
        PyString filter = null;
        if (f != Filter.INCLUDE) {
            //TODO: figure out how to pass filter object in directly
            filter = new PyString(CQL.toCQL(f));
        }
        return filter;
    }

}
