package org.geoserver.rest.wrapper;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.XStreamMessageConverter;

import freemarker.template.Template;

/**
 * Wrapper for objects returned by MVC Rest endpoints
 */
public interface RestWrapper<T> {
    /**
     * Get the class of object or collection contents, if the object is a collection
     *
     * @return class of the object
     */
    Class<T> getObjectClass();

    /**
     * Get the wrapped object
     *
     * May return either an instace of {@link #getObjectClass()} or a collection of instances of that class.
     *
     * @return wrapped object
     */
    Object getObject();

    /**
     * Apply configuration to the XStreamPersister based on the converter
     *
     * @param persister The XStream persister
     * @param xStreamMessageConverter The XStream converter
     */
    void configurePersister(XStreamPersister persister, XStreamMessageConverter xStreamMessageConverter);

    /**
     * Get the freemarker template associated with this response
     *
     * @return the freemarker template
     */
    Template getTemplate();
}
