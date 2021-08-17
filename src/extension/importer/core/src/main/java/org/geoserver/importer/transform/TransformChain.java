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
        this(new ArrayList<>(3));
    }

    public TransformChain(List<T> transforms) {
        this.transforms = transforms;
    }

    @SafeVarargs
    public TransformChain(T... transforms) {
        this.transforms = new ArrayList<>(Arrays.asList(transforms));
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

    @SuppressWarnings("unchecked")
    public <X> X get(Class<X> type) {
        for (T tx : transforms) {
            if (type.equals(tx.getClass())) {
                return (X) tx;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <X> List<X> getAll(Class<X> type) {
        List<X> list = new ArrayList<>();
        for (T tx : transforms) {
            if (type.isAssignableFrom(tx.getClass())) {
                list.add((X) tx);
            }
        }
        return list;
    }

    public void removeAll(Class<?> type) {
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
                LOGGER.log(
                        Level.FINE,
                        "Task {0}, pre-transform {1} running on data {2}",
                        new Object[] {item, tx, data});
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
                LOGGER.log(
                        Level.FINE,
                        "Task {0}, post-transform {1} running using data {2}",
                        new Object[] {task, tx, data});
                tx.apply(task, data);
            } catch (Exception e) {
                error(tx, e);
            }
        }
    }

    protected Object readResolve() {
        if (transforms == null) {
            transforms = new ArrayList<>();
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

    @SuppressWarnings("unchecked")
    protected <T> List<T> filter(List<? extends ImportTransform> transforms, Class<T> type) {
        List<T> filtered = new ArrayList<>();
        for (ImportTransform tx : transforms) {
            if (type.isInstance(tx)) {
                filtered.add((T) tx);
            }
        }
        return filtered;
    }
}
