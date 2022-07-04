/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geotools.data.FeatureReader;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Converts feature between two feature data sources.
 *
 * <p>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FeatureDataConverter {

    static Logger LOGGER = Logging.getLogger(Importer.class);

    private static final Set<String> ORACLE_RESERVED_WORDS;

    static {
        final HashSet<String> words = new HashSet<>();
        try (InputStream wordStream =
                        FeatureDataConverter.class.getResourceAsStream(
                                "oracle_reserved_words.txt");
                Reader wordReader = new InputStreamReader(wordStream, StandardCharsets.UTF_8);
                BufferedReader bufferedWordReader = new BufferedReader(wordReader)) {
            String word;
            while ((word = bufferedWordReader.readLine()) != null) {
                words.add(word);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Oracle reserved words", e);
        }
        ORACLE_RESERVED_WORDS = Collections.unmodifiableSet(words);
    }

    /**
     * characters we strip out of type names and attribute names because they can't be represented
     * as xml
     */
    static final Pattern UNSAFE_CHARS = Pattern.compile("(^[^a-zA-Z\\._]+)|([^a-zA-Z\\._0-9]+)");

    private FeatureDataConverter() {}

    /**
     * Convert the provided featureType to follow the limitations of the vector format such as name
     * length and data types supported.
     *
     * <p>Type name precedence based on native name, requested layer name, or provided type name.
     *
     * @param featureType Input schema resulting from source data (with any processing applied)
     * @param format vector format
     * @param data Import data details
     * @param task Import task details including requested native name and layer name
     * @return Resulting schema which may be used with vector format
     */
    public SimpleFeatureType convertType(
            SimpleFeatureType featureType, VectorFormat format, ImportData data, ImportTask task) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(convertTypeName(requestedTypeName(task, featureType)));

        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
            String attributeName = convertAttributeName(att.getLocalName());

            attBuilder.init(att);
            typeBuilder.add(attBuilder.buildDescriptor(attributeName));
        }

        return typeBuilder.buildFeatureType();
    }

    /**
     * Type name precedence based on native name, requested layer name, or provided type name.
     *
     * @param task Used to check native name and layer name
     * @param featureType Used to check input featureType (possibly subject to some processing) type
     *     name
     * @return type name
     */
    protected String requestedTypeName(ImportTask task, SimpleFeatureType featureType) {
        if (task != null && task.getLayer().getResource().getNativeName() != null) {
            return task.getLayer().getResource().getNativeName();
        } else if (task != null && task.getLayer().getResource().getName() != null) {
            return task.getLayer().getResource().getName();
        } else {
            return featureType.getTypeName();
        }
    }

    public void convert(SimpleFeature from, SimpleFeature to) {
        Set<String> fromAttrNames = attributeNames(from);
        Set<String> toAttrNames = attributeNames(to);
        Set<String> commonNames =
                fromAttrNames.stream()
                        .filter(name -> toAttrNames.contains(convertAttributeName(name)))
                        .collect(Collectors.toSet());
        for (String attrName : commonNames) {
            to.setAttribute(convertAttributeName(attrName), from.getAttribute(attrName));
        }
    }

    /**
     * Clean up typeName to ensure valid xml type (for GML output) taking care to remove leading
     * digit and replace unsupported characters with {@code _} character. See {@link #UNSAFE_CHARS}.
     *
     * <p>Override to allow for additional restrictions such as max length.
     *
     * @param typeName
     * @return Converted typename, should be valid for XML documents also.
     */
    protected String convertTypeName(String typeName) {
        StringBuilder builder = new StringBuilder();
        // instead of allowing prefix digits to 'collapse', prepend a valid char
        if (Character.isDigit(typeName.charAt(0))) {
            builder.append('_');
        }
        builder.append(typeName);
        return UNSAFE_CHARS.matcher(builder).replaceAll("_");
    }

    protected String convertAttributeName(String attName) {
        return convertTypeName(attName);
    }

    protected Set<String> attributeNames(SimpleFeature feature) {
        List<AttributeDescriptor> attributeDescriptors =
                feature.getType().getAttributeDescriptors();
        Set<String> attrNames = new HashSet<>(attributeDescriptors.size());
        for (AttributeDescriptor attr : attributeDescriptors) {
            attrNames.add(attr.getLocalName());
        }
        return attrNames;
    }

    public static FeatureDataConverter DEFAULT = new FeatureDataConverter();

    /**
     * For shapefile we need to ensure the geometry is the first attribute, and we have to deal with
     * the max field name length of 10.
     */
    public static FeatureDataConverter TO_SHAPEFILE =
            new FeatureDataConverter() {
                @Override
                public SimpleFeatureType convertType(
                        SimpleFeatureType featureType,
                        VectorFormat format,
                        ImportData data,
                        ImportTask item) {

                    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                    typeBuilder.setName(convertTypeName(featureType.getTypeName()));

                    GeometryDescriptor gd = featureType.getGeometryDescriptor();
                    if (gd != null) {
                        Class<?> binding = gd.getType().getBinding();
                        if (Geometry.class.equals(binding)) {
                            // Shapefile does not support abstract geometry binding, check
                            // contents to determine geometry type stored
                            try {
                                try (FeatureReader r = format.read(data, item)) {
                                    if (r.hasNext()) {
                                        SimpleFeature f = (SimpleFeature) r.next();
                                        if (f.getDefaultGeometry() != null) {
                                            binding = f.getDefaultGeometry().getClass();
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                LOGGER.warning("Unable to determine concrete geometry type");
                            }
                        }
                        typeBuilder.add(convertAttributeName(gd.getLocalName()), binding);
                    }
                    for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                        if (att.equals(gd)) {
                            continue;
                        }
                        typeBuilder.add(
                                convertAttributeName(att.getLocalName()),
                                att.getType().getBinding());
                    }
                    return typeBuilder.buildFeatureType();
                }

                @Override
                public void convert(SimpleFeature from, SimpleFeature to) {
                    for (AttributeDescriptor att : from.getType().getAttributeDescriptors()) {

                        Object obj = from.getAttribute(att.getLocalName());
                        if (att instanceof GeometryDescriptor) {
                            to.setDefaultGeometry(obj);
                        } else if (containsAttribute(
                                to, convertAttributeName(att.getLocalName()))) {
                            to.setAttribute(convertAttributeName(att.getLocalName()), obj);
                        }
                    }
                }

                /**
                 * Overide to enfore 10 character limit on attribute names.
                 *
                 * @param attName
                 * @return attribute name truncated to 10 char limit
                 */
                @Override
                protected String convertAttributeName(String attName) {
                    String name = super.convertAttributeName(attName);
                    return name.length() > 10 ? name.substring(0, 10) : name;
                }

                /**
                 * Quick check if feature type contains attribute name.
                 *
                 * @param ft
                 * @param attName
                 * @return true if attribute with local name matching attName found
                 */
                private boolean containsAttribute(SimpleFeature ft, String attName) {
                    for (AttributeDescriptor att : ft.getType().getAttributeDescriptors()) {
                        if (att.getLocalName().equals(attName)) {
                            return true;
                        }
                    }
                    return false;
                }
            };

    public static final FeatureDataConverter TO_POSTGIS =
            new FeatureDataConverter() {
                /**
                 * Ensure resulting table name fits within PostGIS table/index limit of 64
                 * characters (making allowance for {@code spatial_} prefix and {@code _geometry}
                 * suffix (and appending any numbers required to make the name unique).
                 *
                 * @param typeName Proposed feature type name
                 * @return type name, ensuring this is a valid table name for PostGIS
                 */
                @Override
                protected String convertTypeName(String typeName) {
                    typeName = super.convertTypeName(typeName);

                    // trim the length of the name
                    // by default, postgis table/index names need to fit in 64 characters
                    // with the "spatial_" prefix and "_geometry" suffix, that leaves 47 chars
                    // and we should leave room to append integers to make the name unique too
                    if (typeName.length() > 45) {
                        return typeName.substring(0, 45);
                    }
                    return typeName;
                }
            };

    public static final FeatureDataConverter TO_ORACLE =
            new FeatureDataConverter() {
                @Override
                public void convert(SimpleFeature from, SimpleFeature to) {
                    // for oracle the target names are always uppercase
                    // See ensureOracleSafe for conversion

                    Set<String> fromAttrNames = attributeNames(from);
                    Set<String> toAttrNames = attributeNames(to);

                    for (String name : fromAttrNames) {
                        String toName = ensureOracleSafe(name);
                        if (toAttrNames.contains(toName)) {
                            to.setAttribute(convertAttributeName(toName), from.getAttribute(name));
                        }
                    }
                }

                /**
                 * Override to check that typeName is {@link #ensureOracleSafe(String)}.
                 *
                 * @param typeName
                 * @return type name, ensuring this is a valid table name for oracle
                 */
                @Override
                protected String convertTypeName(String typeName) {
                    typeName = super.convertTypeName(typeName);
                    return ensureOracleSafe(typeName);
                }

                /**
                 * Override to ensure attribute name is {@link #ensureOracleSafe(String)}.
                 *
                 * @param attName proposed attribute name
                 * @return attribtue name, ensuring this is a valid column name for oracle
                 */
                @Override
                protected String convertAttributeName(String attName) {
                    attName = super.convertAttributeName(attName);

                    return ensureOracleSafe(attName);
                }
                /**
                 * Capitalize and ensure identifier does not conflict with Oracle reserved words.
                 *
                 * @param identifier Proposed table or column name
                 * @return identifier for use as table or column name
                 */
                private final String ensureOracleSafe(final String identifier) {
                    final String capitalized = identifier.toUpperCase();
                    if (ORACLE_RESERVED_WORDS.contains(capitalized)) {
                        return capitalized + "_";
                    } else {
                        return capitalized;
                    }
                }
            };
}
