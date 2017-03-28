package org.geoserver.rest.wrapper;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;

import freemarker.template.Template;

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
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) { }

    /**
     * @return freemarker template
     */
    @Override
    public Template getTemplate() {
        return template;
    }

    /**
     * Default (empty) implementation. Subclasses should override this to implement custom functionality
     */
        @Override
        public void configureFreemarker(FreemarkerHTMLMessageConverter converter) { }
   
}
