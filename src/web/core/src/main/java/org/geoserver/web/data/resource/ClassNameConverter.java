/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import com.google.common.collect.ImmutableList;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

class ClassNameConverter implements IConverter<Class> {

    /** List of well known class names this component will convert */
    private static final List<Class> BINDINGS = Arrays.asList(
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
            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

    // Has a simplified setup thanks to class simple names having no overlaps
    private static final Map<String, Class> NAME_TO_CLASS =
            BINDINGS.stream().collect(Collectors.toMap(Class::getSimpleName, c -> c));

    private static final Map<Class, String> CLASS_TO_NAME =
            BINDINGS.stream().collect(Collectors.toMap(c -> c, Class::getSimpleName));

    public static List<Class> getBindings() {
        return new ArrayList<>(BINDINGS);
    }

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
        return getClassName(c);
    }

    public static String getClassName(Class c) {
        String name = CLASS_TO_NAME.get(c);
        if (name == null) return c.getName(); // if not a common one, use the full name
        return name;
    }
}
