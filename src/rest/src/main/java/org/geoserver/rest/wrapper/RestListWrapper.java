/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.wrapper;

import freemarker.template.Template;
import java.util.Collection;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamCatalogListConverter;

/**
 * A wrapper for all Collection type responses using the {@link XStreamCatalogListConverter} (XML
 * and JSON output). Also supports Collection type responses using the {@link
 * FreemarkerHTMLMessageConverter}, but is not required for such responses.
 *
 * <p>In the previous rest API this wasn't needed because in each individual rest request the
 * Collections were aliased to
 */
public class RestListWrapper<T> extends RestWrapperAdapter<T> {

    String itemAttributeName = "name";

    public RestListWrapper(
            Collection<T> collection,
            Class<T> clazz,
            RestBaseController controller,
            Template template) {
        super(collection, clazz, controller, template);
    }

    public RestListWrapper(
            Collection<T> collection,
            Class<T> clazz,
            RestBaseController controller,
            String itemAttributeName,
            Template template) {
        super(collection, clazz, controller, template);
        this.itemAttributeName = itemAttributeName;
    }

    /** Alias for {@link #getObject()} */
    public Collection<T> getCollection() {
        return (Collection<T>) getObject();
    }

    /** The item attribute name, identifying the object, typically "name" */
    public String getItemAttributeName() {
        return itemAttributeName;
    }
}
