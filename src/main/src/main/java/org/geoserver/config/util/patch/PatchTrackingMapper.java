/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import com.thoughtworks.xstream.mapper.MapperWrapper;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper wrapper that tracks the member names being deserialized and registers them in the current {@link PatchContext}
 * if available. This allows the patch deserialization process to correlate the XML/JSON paths with the real member
 * names being set, which is necessary to determine if a property was explicitly set to null in the XML/JSON (as XStream
 * turns nulls into empty strings).
 */
public class PatchTrackingMapper extends MapperWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatchTrackingMapper.class);

    private static final ConcurrentMap<Class<?>, Set<String>> PROPERTIES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, Map<String, String>> FIELD_TO_PROPERTY = new ConcurrentHashMap<>();

    private static final Set<String> NO_MATCH_LOGGED = ConcurrentHashMap.newKeySet();

    public PatchTrackingMapper(MapperWrapper next) {
        super(next);
    }

    @Override
    public String realMember(Class type, String serialized) {
        String real = super.realMember(type, serialized);

        PatchContext pc = PatchContext.get();
        if (pc != null) {
            String normalized = resolve(type, real);
            pc.registerCurrentMember(type, normalized);
        }

        return real;
    }

    /**
     * Resolves the given XStream member name to the corresponding bean property name, using the following rules:
     *
     * <ol>
     *   <li>If there is a field annotated with @PatchProperty whose value matches the member name, use the
     *       corresponding property name.
     *   <li>If the member name matches a bean property, use it as is.
     *   <li>If the member name is all lower case, and there is a bean property with the same name but in upper case
     *       (e.g URI vs uri), use the upper case property name.
     *   <li>If no match is found, return the original member name and log a debug message (only once per type/member
     *       combination) to help identify missing mappings.
     * </ol>
     */
    private static String resolve(Class<?> ownerType, String xstreamMember) {
        if (ownerType == null || xstreamMember == null || xstreamMember.isEmpty()) {
            return xstreamMember;
        }

        // 1) Field annotation: field name -> property name
        String viaFieldAnno = fieldToProperty(ownerType).get(xstreamMember);
        if (viaFieldAnno != null) {
            return viaFieldAnno;
        }

        // 2) Exact property exists
        if (hasProperty(ownerType, xstreamMember)) {
            return xstreamMember;
        }

        // 3) Automatic: only ALL-CAPS acronym properties
        if (isAllLowerAscii(xstreamMember)) {
            String upper = xstreamMember.toUpperCase(Locale.ROOT);
            if (hasProperty(ownerType, upper)) {
                return upper;
            }
        }

        // 4) No match -> log once
        logNoMatch(ownerType, xstreamMember);
        return xstreamMember;
    }

    private static boolean hasProperty(Class<?> type, String name) {
        return propertiesOf(type).contains(name);
    }

    private static Set<String> propertiesOf(Class<?> type) {
        return PROPERTIES.computeIfAbsent(type, PatchTrackingMapper::introspectProperties);
    }

    private static Set<String> introspectProperties(Class<?> type) {
        try {
            BeanInfo bi = Introspector.getBeanInfo(type);
            Set<String> props = new HashSet<>();
            for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                String n = pd.getName();
                if (n == null || "class".equals(n)) continue;
                if (pd.getReadMethod() != null || pd.getWriteMethod() != null) {
                    props.add(n);
                }
            }
            return Collections.unmodifiableSet(props);
        } catch (Exception e) {
            LOGGER.debug("Failed to introspect bean properties for {}", type.getName(), e);
            return Collections.emptySet();
        }
    }

    private static Map<String, String> fieldToProperty(Class<?> type) {
        return FIELD_TO_PROPERTY.computeIfAbsent(type, PatchTrackingMapper::introspectFieldMappings);
    }

    private static Map<String, String> introspectFieldMappings(Class<?> type) {
        Map<String, String> map = new HashMap<>();

        // property -> list(fields) for diagnostics
        Map<String, List<String>> reverse = new HashMap<>();

        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                PatchProperty pp = f.getAnnotation(PatchProperty.class);
                if (pp == null) continue;

                String fieldName = f.getName();
                String propName = pp.value();

                if (propName == null || propName.isBlank()) {
                    LOGGER.warn("@PatchProperty on {}.{} has blank value", c.getName(), fieldName);
                    continue;
                }

                map.put(fieldName, propName);
                reverse.computeIfAbsent(propName, k -> new ArrayList<>()).add(c.getName() + "#" + fieldName);
            }
        }

        // Validate: annotated properties should exist as bean properties
        Set<String> props = propertiesOf(type);
        for (Map.Entry<String, List<String>> e : reverse.entrySet()) {
            String prop = e.getKey();
            List<String> fields = e.getValue();

            if (!props.contains(prop)) {
                LOGGER.warn(
                        "@PatchProperty refers to missing bean property '{}' on {} (annotated fields: {})",
                        prop,
                        type.getName(),
                        fields);
            }

            if (fields.size() > 1) {
                LOGGER.info(
                        "Multiple fields are annotated as @PatchProperty('{}') on {}: {}",
                        prop,
                        type.getName(),
                        fields);
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private static boolean isAllLowerAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    private static void logNoMatch(Class<?> type, String member) {
        String key = type.getName() + "#" + member;
        if (NO_MATCH_LOGGED.add(key)) {
            LOGGER.debug(
                    "Patch member name '{}' does not match any bean property in {} (no @PatchProperty and no ALL-CAPS"
                            + " property).",
                    member,
                    type.getName());
        }
    }
}
