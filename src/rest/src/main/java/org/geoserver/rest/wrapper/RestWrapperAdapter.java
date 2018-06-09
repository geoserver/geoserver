/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.wrapper;

import freemarker.template.Template;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;

/** Default implementation of {@link RestWrapper} */
public class RestWrapperAdapter<T> implements RestWrapper<T> {

    Object object;
    Class<T> clazz;
    Template template;
    RestBaseController controller;

    public RestWrapperAdapter(
            Object object, Class<T> advertisedClass, RestBaseController controller) {
        this(object, advertisedClass, controller, null);
    }

    public RestWrapperAdapter(
            Object object,
            Class<T> advertisedClass,
            RestBaseController controller,
            Template template) {
        this.object = object;
        this.clazz = advertisedClass;
        this.template = template;
        this.controller = controller;
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
     * Default implementation. Calls {@link RestBaseController#configurePersister(XStreamPersister,
     * XStreamMessageConverter)}
     */
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        controller.configurePersister(persister, converter);
    }

    /**
     * Default implementation. Subclasses should override this to implement custom functionality
     *
     * @return freemarker template
     */
    @Override
    public Template getTemplate() {
        return template;
    }

    /**
     * Default implementation. Calls {@link
     * RestBaseController#configureFreemarker(FreemarkerHTMLMessageConverter, Template)}
     */
    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter) {
        controller.configureFreemarker(converter, getTemplate());
    }
}
