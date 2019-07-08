/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.PropertyUtils;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Extracts a property from a {@link Info} object.
 *
 * <p>The property can be nested (p1.p2.p3), indexed (p1[3]), collection (colProp), or a combination
 * (colProp1.nonColProp.colProp2[1]).
 *
 * <p>In the later case, indicates {@code colProp1} is a collection property and a list of all the
 * id values from all the objects in the p1 property shall be returned.
 */
public class CatalogPropertyAccessor implements PropertyAccessor {

    private static final Logger LOGGER = Logging.getLogger(CatalogPropertyAccessor.class);

    @Override
    public boolean canHandle(Object object, String xpath, Class<?> target) {
        return object instanceof Info;
    }

    @Override
    public <T> void set(Object object, String xpath, T value, Class<T> target)
            throws IllegalArgumentException {

        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object object, String xpath, Class<T> target) throws IllegalArgumentException {
        Object value = getProperty(object, xpath);
        T result;
        if (null != target && null != value) {
            result = Converters.convert(value, target);
        } else {
            result = (T) value;
        }
        return result;
    }

    /**
     * @param input the object to extract the (possibly nested,indexed, or collection) property from
     * @param propertyName the property to extract from {@code input}
     * @return the evaluated value of the given property, or {@code null} if a prior nested property
     *     in the path is null;
     * @throws IllegalArgumentException if no such property exists for the given object
     */
    public Object getProperty(final Object input, final String propertyName)
            throws IllegalArgumentException {

        if (input instanceof Info && Predicates.ANY_TEXT.getPropertyName().equals(propertyName)) {
            return getAnyText((Info) input);
        }
        String[] propertyNames = propertyName.split("\\.");
        return getProperty(input, propertyNames, 0);
    }

    /** @param input */
    @SuppressWarnings("unchecked")
    private List<String> getAnyText(final Info input) {

        final Set<String> propNames = fullTextProperties(input);
        List<String> textProps = new ArrayList<String>(propNames.size());
        for (String propName : propNames) {
            Object property = getProperty(input, propName);
            if (property instanceof Collection) {
                textProps.addAll(((Collection<String>) property));
            } else if (property != null) {
                textProps.add(String.valueOf(property));
            }
        }
        return textProps;
    }

    public Object getProperty(final Object input, final String[] propertyNames, final int offset)
            throws IllegalArgumentException {

        if (offset < 0 || offset > propertyNames.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "offset: " + offset + ", properties: " + propertyNames.length);
        }

        if (offset == propertyNames.length) {
            return input;
        }

        final String propName = propertyNames[offset];

        if (null == input) {
            throw new IllegalArgumentException(
                    "Property not found: "
                            + Joiner.on('.').join(Arrays.copyOf(propertyNames, offset + 1)));
        }

        // indexed property?
        if (propName.indexOf('[') > 0 && propName.endsWith("]")) {
            return getIndexedProperty(input, propertyNames, offset);
        }
        if (input instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> col = (Collection<Object>) input;
            List<Object> result = new ArrayList<Object>(col.size());
            for (Object o : col) {
                if (o == null) {
                    continue;
                }
                // if one of the nested properties is not found just ignore and move
                // to the next one, we can have mixed collections (e.g., layer group layers)
                try {
                    Object value = getProperty(o, propName);
                    Object nested = getProperty(value, propertyNames, offset + 1);
                    result.add(nested);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Skipping nested property not found", e);
                }
            }
            return result;
        }

        Object value;
        if (input instanceof Map) {
            if (!((Map<?, ?>) input).containsKey(propName)) {
                throw new IllegalArgumentException(
                        "Property "
                                + propName
                                + " does not exist in Map property "
                                + (offset > 0 ? propertyNames[offset - 1] : ""));
            }
            value = ((Map<?, ?>) input).get(propName);
        } else {
            // special case for ResourceInfo bounding box, used the derived property
            if ("boundingBox".equalsIgnoreCase(propName) && input instanceof ResourceInfo) {
                try {
                    value = ((ResourceInfo) input).boundingBox();
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                value = OwsUtils.get(input, propName);
            }
        }

        // if our nested access stumbles onto a null, we return a null value to allow
        // for full text searches to work (e.g., workspace.name, but workspace can be null
        // in both layer groups and styles
        if (value == null) {
            return null;
        }

        return getProperty(value, propertyNames, offset + 1);
    }

    private Object getIndexedProperty(
            Object input, final String[] propertyNames, final int offset) {

        final String indexedPropName = propertyNames[offset];

        final String colPropName = indexedPropName.substring(0, indexedPropName.indexOf('['));

        final int index;
        {
            final int beginIndex = indexedPropName.indexOf('[') + 1;
            final int endIndex = indexedPropName.length() - 1;
            final String indexStr = indexedPropName.substring(beginIndex, endIndex);
            index = Integer.parseInt(indexStr);
            Preconditions.checkArgument(
                    index > 0, "Illegal indexed property, index shall be > 0: " + indexedPropName);
        }

        Collection<Object> col = getCollectionProperty(input, colPropName);
        Object indexedValue;
        if (col == null) {
            try {
                indexedValue = PropertyUtils.getIndexedProperty(input, colPropName, index - 1);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                indexedValue = null;
            }
        } else {
            if (!(col instanceof List)) {
                throw new RuntimeException(
                        "Indexed property access is not valid for property " + colPropName);
            }
            List<Object> list = (List<Object>) col;
            if (index > list.size()) {
                return null;
            }
            indexedValue = list.get(index - 1);
        }
        return indexedValue == null ? false : getProperty(indexedValue, propertyNames, offset + 1);
    }

    private Collection<Object> getCollectionProperty(Object input, String colPropName) {
        Object colProp;
        if (input instanceof Map) {
            colProp = ((Map<?, ?>) input).get(colPropName);
        } else {
            try {
                colProp = OwsUtils.get(input, colPropName);
            } catch (Exception e) {
                return null;
            }
        }
        if (null == colProp) {
            return null;
        }
        if (colProp.getClass().isArray()) {
            int length = Array.getLength(colProp);
            List<Object> array = new ArrayList<Object>(length);
            for (int j = 0; j < length; j++) {
                array.add(Array.get(colProp, j));
            }
            colProp = array;
        }
        if (!(colProp instanceof Collection)) {
            throw new IllegalArgumentException(
                    "Specified property "
                            + colPropName
                            + " is not a collection or array: "
                            + colProp);
        }
        @SuppressWarnings("unchecked")
        Collection<Object> col = (Collection<Object>) colProp;
        return col;
    }

    private static Map<Class<?>, Set<String>> FULL_TEXT_PROPERTIES = Maps.newHashMap();

    private static Set<String> fullTextProperties(Info obj) {
        Set<String> props = ImmutableSet.of();
        if (obj != null) {
            Class<?> clazz = ModificationProxy.unwrap(obj).getClass();
            ClassMappings classMappings = ClassMappings.fromImpl(clazz);
            checkState(
                    classMappings != null, "No class mappings found for class " + clazz.getName());
            Class<?> interf = classMappings.getInterface();
            props = fullTextProperties(interf);
        }
        return props;
    }

    public static Set<String> fullTextProperties(Class<?> type) {
        if (FULL_TEXT_PROPERTIES.isEmpty()) {
            loadFullTextProperties();
        }
        Set<String> props = FULL_TEXT_PROPERTIES.get(type);
        if (props == null) {
            props = ImmutableSet.of();
        }
        return props;
    }

    /** */
    private static synchronized void loadFullTextProperties() {
        if (!FULL_TEXT_PROPERTIES.isEmpty()) {
            return;
        }
        final String resource = "CatalogPropertyAccessor_FullTextProperties.properties";
        Properties properties = new Properties();
        InputStream stream = CatalogPropertyAccessor.class.getResourceAsStream(resource);
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Closeables.close(stream, false);
            } catch (IOException e) {
                LOGGER.log(
                        Level.FINE,
                        "Ignoring exception thrown while closing "
                                + resource
                                + " in CatalogPropertyAccessor",
                        e);
            }
        }
        Map<String, String> map = Maps.fromProperties(properties);
        for (Map.Entry<String, String> e : map.entrySet()) {
            Class<?> key;
            try {
                key = Class.forName(e.getKey());
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            String[] split = e.getValue().split(",");
            Set<String> set = Sets.newHashSet();
            for (String s : split) {
                set.add(s.trim());
            }

            FULL_TEXT_PROPERTIES.put(key, ImmutableSet.copyOf(set));
        }
    }
}
