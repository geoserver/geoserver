package org.geoserver.restng.wrapper;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Wraps an {@link Object} and a {@link Configuration} for use by {@link org.geoserver.restng.converters.FreemarkerHTMLMessageConverter}
 */
public class FreemarkerConfigurationWrapper {

    Object object;
    Template template;

    public FreemarkerConfigurationWrapper(Object object, Template template) {
        this.object = object;
        this.template = template;
    }

    public Object getObject() {
        return object;
    }

    public Template getTemplate() {
        return template;
    }
}
