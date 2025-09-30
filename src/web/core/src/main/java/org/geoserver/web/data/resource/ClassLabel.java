/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/** A {@link Label} dealing with common attribute type bindings */
class ClassLabel extends Label {

    public ClassLabel(IModel<Class> model) {
        super("type", model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) new ClassNameConverter();
    }
}
