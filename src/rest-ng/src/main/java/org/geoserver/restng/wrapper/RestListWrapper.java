package org.geoserver.restng.wrapper;

import freemarker.template.Template;

import java.util.Collection;

/**
 * A wrapper for all Collection type responses using the {@link org.geoserver.restng.converters.XStreamCatalogListConverter}
 * (XML and JSON output).
 * Also supports Collection type responses using the {@link org.geoserver.restng.converters.FreemarkerHTMLMessageConverter},
 * but is not required for such responses.
 *
 * In the previous rest API this wasn't needed because in each individual rest request the Collections were aliased to
 */
public class RestListWrapper<T> extends RestWrapperAdapter<T> {


    public RestListWrapper(Collection<T> collection, Class<T> clazz, Template template) {
        super(collection, clazz, template);
    }

    /**
     * Alias for {@link #getObject()}
     */
    public Collection<T> getCollection() {
        return (Collection<T>) getObject();
    }
}
