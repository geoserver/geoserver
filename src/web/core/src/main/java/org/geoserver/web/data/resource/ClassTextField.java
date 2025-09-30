/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.Iterator;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

/** An {@link AutoCompleteTextField} dealing with common attribute type bindings */
class ClassTextField extends AutoCompleteTextField<Class> {

    public ClassTextField(IModel<Class> model) {
        super("type", model, new ClassNameRenderer());
    }

    @Override
    protected Iterator<Class> getChoices(String s) {
        return ClassNameConverter.getBindings().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) new ClassNameConverter();
    }

    private static class ClassNameRenderer extends AbstractAutoCompleteTextRenderer<Class> {
        @Override
        protected String getTextValue(Class c) {
            return ClassNameConverter.getClassName(c);
        }
    }
}
