/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.util;

import org.apache.wicket.model.IModel;

/**
 * A Model that can wrap another model returning eventually a String and map it to the specified
 * Enum value.
 *
 * @param <T>
 */
public class EnumAdapterModel<T extends Enum> implements IModel<T> {

    private final Class<T> binding;
    IModel<T> delegate;

    public EnumAdapterModel(IModel<T> delegate, Class<T> binding) {
        this.delegate = delegate;
        this.binding = binding;
    }

    @Override
    public T getObject() {
        Object object = delegate.getObject();
        if (object instanceof String) {
            String text = (String) object;
            if (text.isEmpty()) {
                return null;
            }
            for (Object constant : binding.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase(text)) {
                    return (T) constant;
                }
            }
        }
        return (T) object;
    }

    @Override
    public void setObject(T object) {
        delegate.setObject(object);
    }

    @Override
    public void detach() {
        // nothing to do
    }
}
