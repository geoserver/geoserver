package org.geoserver.restng.catalog.wrapper;

import java.util.Collection;

/**
 * A wrapper for all Collection type responses using the {@link org.geoserver.restng.converters.XStreamCatalogListConverter}
 * (XML and JSON output).
 *
 * In the previous rest API this wasn't needed because in each individual rest request the Collections were aliased to
 */
public class XStreamListWrapper<T> {

    Collection<T> collection;
    Class<T> clazz;

    public XStreamListWrapper(Collection<T> collection, Class<T> clazz) {
        this.collection = collection;
        this.clazz = clazz;
    }

    /**
     * Get the class of the collection contents
     *
     * @return class of the collection contents
     */
    public Class<T> getObjectClass() {
        return clazz;
    }

    /**
     * Get the wrapped collection
     *
     * @return wrapped collection
     */
    public Collection<T> getCollection() {
        return collection;
    }
}
