/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import com.google.common.collect.ImmutableList;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** An {@link AutoCompleteTextField} dealing with common attribute type bindings */
class ClassTextField extends AutoCompleteTextField<Class> {

    /** List of well known class names this component will auto-complete to */
    private static final List<Class> BINDINGS =
            Arrays.asList(
                            // alphanumeric
                            String.class,
                            Boolean.class,
                            Byte.class,
                            Short.class,
                            Integer.class,
                            Long.class,
                            Float.class,
                            Double.class,
                            // dates
                            Time.class,
                            Date.class,
                            Timestamp.class,
                            // geometries
                            Geometry.class,
                            Point.class,
                            MultiPoint.class,
                            LineString.class,
                            MultiLineString.class,
                            Polygon.class,
                            MultiPolygon.class)
                    .stream()
                    .sorted(Comparator.comparing(Class::getSimpleName))
                    .collect(
                            Collectors.collectingAndThen(
                                    Collectors.toList(), ImmutableList::copyOf));

    // Has a simplified setup thanks to class simple names having no overlaps
    private static final Map<String, Class> NAME_TO_CLASS =
            BINDINGS.stream().collect(Collectors.toMap(c -> c.getSimpleName(), c -> c));
    private static final Map<Class, String> CLASS_TO_NAME =
            BINDINGS.stream().collect(Collectors.toMap(c -> c, c -> c.getSimpleName()));

    public ClassTextField(String id, IModel<Class> model) {
        super("type", model, new ClassNameRenderer());
    }

    @Override
    protected Iterator<Class> getChoices(String s) {
        return BINDINGS.iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) new ClassNameConverter();
    }

    private static class ClassNameConverter implements IConverter<Class> {
        @Override
        public Class convertToObject(String s, Locale locale) throws ConversionException {
            Class result = NAME_TO_CLASS.get(s);
            // allow for user selected type names too
            if (result == null)
                try {
                    result = Class.forName(s);
                } catch (ClassNotFoundException e) {
                    throw new ConversionException(e);
                }
            return result;
        }

        @Override
        public String convertToString(Class c, Locale locale) {
            return ClassTextField.getClassName(c);
        }
    }

    private static String getClassName(Class c) {
        String name = CLASS_TO_NAME.get(c);
        if (name == null) return c.getName(); // if not a common one, use the full name
        return name;
    }

    private static class ClassNameRenderer extends AbstractAutoCompleteTextRenderer<Class> {
        @Override
        protected String getTextValue(Class c) {
            return getClassName(c);
        }
    }
}
