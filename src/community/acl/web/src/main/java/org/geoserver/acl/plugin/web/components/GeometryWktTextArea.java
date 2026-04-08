/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import java.util.Locale;
import lombok.NonNull;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.springframework.util.StringUtils;

@SuppressWarnings({"serial", "rawtypes"})
public class GeometryWktTextArea<T extends Geometry> extends TextArea<T> {

    private final Class<T> geomType;

    public static GeometryWktTextArea<Geometry> of(@NonNull String id) {
        return new GeometryWktTextArea<>(id, Geometry.class, Model.of());
    }

    public GeometryWktTextArea(@NonNull String id, @NonNull Class<T> geomType, @NonNull IModel<T> model) {
        super(id, model);
        this.geomType = geomType;
        add(new IValidator<>() {
            public @Override void validate(IValidatable<T> validatable) {
                try {
                    validatable.getValue();
                } catch (Exception e) {
                    validatable.error(new ValidationError(e.getMessage()));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        super.convertInput();
        Object convertedInput = super.getConvertedInput();
        if (convertedInput instanceof String string) {
            T geom = (T) createConverter(geomType).convertToObject(string, getLocale());
            setConvertedInput(geom);
        }
    }

    @Override
    protected IConverter<?> createConverter(Class<?> type) {
        if (geomType.isAssignableFrom(type)) return new GeometryConverter(geomType);
        return null;
    }

    private static class GeometryConverter implements IConverter<Geometry> {

        private final Class<? extends Geometry> type;

        public GeometryConverter(Class<? extends Geometry> geomType) {
            this.type = geomType;
        }

        @Override
        public Geometry convertToObject(String value, Locale locale) throws ConversionException {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            Geometry<?> fromWkt;
            try {
                fromWkt = Wkt.fromWkt(value);
            } catch (Exception e) {
                throw new ConversionException("Unable to parse WKT: " + e.getMessage(), e);
            }
            if (!type.isInstance(fromWkt)) {
                throw new ConversionException("Expected %s, got %s"
                        .formatted(type.getSimpleName(), fromWkt.getClass().getSimpleName()));
            }
            return fromWkt;
        }

        @Override
        public String convertToString(Geometry value, Locale locale) {
            return value == null ? null : Wkt.toWkt(value);
        }
    }
}
