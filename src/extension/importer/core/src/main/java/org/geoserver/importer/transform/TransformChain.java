/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;

/**
 * Chain of transformations to apply during the import process.
 *  
 * @author Justin Deoliveira, OpenGeo
 * 
 * @see {@link VectorTransformChain}
 */
public abstract class TransformChain<T extends ImportTransform> implements Serializable {

    protected List<T> transforms;
    
    public TransformChain() {
        this(new ArrayList<T>(3));
    }

    public TransformChain(List<T> transforms) {
        this.transforms = transforms;
    }

    public TransformChain(T... transforms) {
        this.transforms = new ArrayList(Arrays.asList(transforms));
    }

    public List<T> getTransforms() {
        return transforms;
    }

    public <X extends T> void add(X tx) {
        transforms.add(tx);
    }

    public <X extends T> boolean remove(X tx) {
        return transforms.remove(tx);
    }

    public <X extends T> X get(Class<X> type) {
        for (T tx : transforms) {
            if (type.equals(tx.getClass())) {
                return (X) tx;
            }
        }
        return null;
    }

    public <X extends T> List<X> getAll(Class<X> type) {
        List<X> list = new ArrayList<X>();
        for (T tx : transforms) {
            if (type.isAssignableFrom(tx.getClass())) {
                list.add((X) tx);
            }
        }
        return list;
    }

    public <X extends T> void removeAll(Class<X> type) {
        for (Iterator<T> it = transforms.iterator(); it.hasNext(); ) {
            if (type.isAssignableFrom(it.next().getClass())) {
                it.remove();
            }
        }
    }
    
    public abstract void pre(ImportTask task, ImportData data) throws Exception;
    public abstract void post(ImportTask task, ImportData data) throws Exception;

    private Object readResolve() {
        if (transforms == null) {
            transforms = new ArrayList();
        }
        return this;
    }
}
