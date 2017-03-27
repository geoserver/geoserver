package org.geoserver.rest.wrapper;

import freemarker.template.Template;
import org.geoserver.config.util.XStreamPersister;

/**
 * Default implementation of {@link RestWrapper}
 */
public class RestWrapperAdapter<T> implements RestWrapper<T> {

    Object object;
    Class<T> clazz;
    Template template;

    public RestWrapperAdapter(Object object, Class<T> advertisedClass) {
        this(object, advertisedClass, null);
    }

    public RestWrapperAdapter(Object object, Class<T> advertisedClass, Template template) {
        this.object = object;
        this.clazz = advertisedClass;
        this.template = template;
    }

    @Override
    public Class<T> getObjectClass() {
        return clazz;
    }

    @Override
    public Object getObject() {
        return object;
    }

    /**
     * Default (empty) implementation. Subclasses should override this to implement custom functionality
     */
    @Override
    public void configurePersister(XStreamPersister persister) { }

    /**
     * @return freemarker template
     */
    @Override
    public Template getTemplate() {
        return template;
    }
}
