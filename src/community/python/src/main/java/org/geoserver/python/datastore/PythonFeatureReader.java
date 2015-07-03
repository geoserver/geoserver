/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.python.core.PyGenerator;
import org.python.core.PyMethod;
import org.python.core.PyObject;

public class PythonFeatureReader implements SimpleFeatureReader {

    SimpleFeatureType featureType;
    PyGenerator features;
    Iterator it;
    
    public PythonFeatureReader(SimpleFeatureType featureType, PyObject layer) {
        this.featureType = featureType;
        
        PyMethod features = (PyMethod) layer.__findattr__("features");
        
        this.features = (PyGenerator) features.__call__();
        this.it = this.features.iterator();
    }
    
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    public boolean hasNext() throws IOException {
        return it.hasNext();
    }

    public SimpleFeature next() throws IOException, IllegalArgumentException,
            NoSuchElementException {
        PyObject f = (PyObject) it.next();
        PyObject feature = f.__findattr__("_feature");
        return (SimpleFeature) feature.__tojava__(SimpleFeature.class);
    }
    
    public void close() throws IOException {
        it = null;
        features.close();
    }


}
