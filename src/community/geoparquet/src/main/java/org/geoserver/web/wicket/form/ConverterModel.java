/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.geotools.util.Converters;

/**
 * A model that converts values from one type to another, typically used for form components that require specific
 * types.
 *
 * <p>In GeoServer datastores, connection parameters are often stored as strings even when they represent numeric or
 * boolean values. This model handles the conversion between these string representations and their appropriate Java
 * types when used with form components that require specific types.
 *
 * <p>This is particularly useful for:
 *
 * <ul>
 *   <li>Parameters that may be stored as strings but need to be used as numbers in form components like
 *       {@link NumberTextField}
 *   <li>Parameters that weren't originally entered through the UI and may have inconsistent types
 *   <li>Handling null values gracefully
 * </ul>
 *
 * <p>The conversion is performed using GeoTools' {@link Converters} utility, which provides a robust framework for
 * converting between different types.
 *
 * @param <T> The target type to convert to/from (e.g., Integer, Double, Boolean)
 */
@SuppressWarnings("serial")
public class ConverterModel<T> implements IModel<T> {

    private final IModel<Object> model;
    private final Class<T> targetType;

    /**
     * Creates a new ConverterModel that will convert the source model's value to the target type.
     *
     * @param model The model whose value needs conversion
     * @param targetType The class of the target type
     */
    public ConverterModel(IModel<Object> model, Class<T> targetType) {
        this.model = model;
        this.targetType = targetType;
    }

    @Override
    public T getObject() {
        Object object = model.getObject();
        // Try to convert using GeoTools Converters
        T result = Converters.convert(object, targetType);
        return result;
    }

    @Override
    public void setObject(T object) {
        model.setObject(object);
    }

    @Override
    public void detach() {
        model.detach();
    }
}
