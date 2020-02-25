/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geotools.util.logging.Logging;

/**
 * Chain of transformations to apply during the import process.
 *
 * @author Justin Deoliveira, OpenGeo
 * @see {@link VectorTransformChain} {@link RasterTransformChain}
 */
public abstract class TransformChain<T extends ImportTransform> implements Serializable {

    private static final long serialVersionUID = 4090734786225748502L;

    static Logger LOGGER = Logging.getLogger(TransformChain.class);

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

    /** Runs all {@link PreTransform} in the chain */
    public void pre(ImportTask item, ImportData data) throws Exception {
        for (PreTransform tx : filter(transforms, PreTransform.class)) {
            try {
                tx.apply(item, data);
            } catch (Exception e) {
                error(tx, e);
            }
        }
    }

    /** Runs all {@link PostTransform} in the chain */
    public void post(ImportTask task, ImportData data) throws Exception {
        for (PostTransform tx : filter(transforms, PostTransform.class)) {
            try {
                tx.apply(task, data);
            } catch (Exception e) {
                error(tx, e);
            }
        }
    }

    protected Object readResolve() {
        if (transforms == null) {
            transforms = new ArrayList();
        }
        return this;
    }

    protected void error(ImportTransform tx, Exception e) throws Exception {
        if (tx.stopOnError(e)) {
            throw e;
        } else {
            // log and continue
            LOGGER.log(Level.WARNING, "Transform " + tx + " failed", e);
        }
    }

    protected <T> List<T> filter(List<? extends ImportTransform> transforms, Class<T> type) {
        List<T> filtered = new ArrayList<T>();
        for (ImportTransform tx : transforms) {
            if (type.isInstance(tx)) {
                filtered.add((T) tx);
            }
        }
        return filtered;
    }
}
