package org.geoserver.importer.rest;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by tbarsballe on 2017-03-31.
 */
public class ImportContextCollectionWrapper<T> {
    private Object collection;
    private Class<T> clazz;

    public ImportContextCollectionWrapper(Iterator<T> iterator, Class<T> clazz) {
        collection = iterator;
        this.clazz = clazz;
    }

    public ImportContextCollectionWrapper(Collection<T> collection, Class<T> clazz) {
        this.collection = collection;
        this.clazz = clazz;
    }

    public Object getCollection() {
        return collection;
    }

    public Class<T> getCollectionClass() {
        return clazz;
    }
}
