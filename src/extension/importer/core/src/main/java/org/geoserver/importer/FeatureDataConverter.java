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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
        final HashSet<String> words = new HashSet<String>();
        final InputStream wordStream =
                FeatureDataConverter.class.getResourceAsStream("oracle_reserved_words.txt");
        final Reader wordReader = new InputStreamReader(wordStream, Charset.forName("UTF-8"));
        final BufferedReader bufferedWordReader = new BufferedReader(wordReader);

        String word;
        try {
            while ((word = bufferedWordReader.readLine()) != null) {
                words.add(word);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Oracle reserved words", e);
        } finally {
            try {
                bufferedWordReader.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error while closing Oracle reserved words file", e);
            }
        }
        ORACLE_RESERVED_WORDS = Collections.unmodifiableSet(words);
    }

    /**
     * characters we strip out of type names and attribute names because they can't be represented
     * as xml
     */
    static final Pattern UNSAFE_CHARS = Pattern.compile("(^[^a-zA-Z\\._]+)|([^a-zA-Z\\._0-9]+)");

    private FeatureDataConverter() {}

    public SimpleFeatureType convertType(
            SimpleFeatureType featureType, VectorFormat format, ImportData data, ImportTask task) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(
                convertTypeName(
                        task != null && task.getLayer().getName() != null
                                ? task.getLayer().getName()
                                : featureType.getTypeName()));

        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
            attBuilder.init(att);
            typeBuilder.add(attBuilder.buildDescriptor(convertAttributeName(att.getLocalName())));
        }

        return typeBuilder.buildFeatureType();
    }

    public void convert(SimpleFeature from, SimpleFeature to) {
        Set<String> fromAttrNames = attributeNames(from);
        Set<String> toAttrNames = attributeNames(to);
        Set<String> commonNames =
                fromAttrNames
                        .stream()
                        .filter(name -> toAttrNames.contains(convertAttributeName(name)))
                        .collect(Collectors.toSet());
        for (String attrName : commonNames) {
            to.setAttribute(convertAttributeName(attrName), from.getAttribute(attrName));
        }
    }

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
        Set<String> attrNames = new HashSet<String>(attributeDescriptors.size());
        for (AttributeDescriptor attr : attributeDescriptors) {
            attrNames.add(attr.getLocalName());
        }
        return attrNames;
    }

    public static FeatureDataConverter DEFAULT = new FeatureDataConverter();

    public static FeatureDataConverter TO_SHAPEFILE =
            new FeatureDataConverter() {
                @Override
                public SimpleFeatureType convertType(
                        SimpleFeatureType featureType,
                        VectorFormat format,
                        ImportData data,
                        ImportTask item) {

                    // for shapefile we always ensure the geometry is the first type, and we have to
                    // deal
                    // with the max field name length of 10
                    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                    typeBuilder.setName(convertTypeName(featureType.getTypeName()));

                    GeometryDescriptor gd = featureType.getGeometryDescriptor();
                    if (gd != null) {
                        Class binding = gd.getType().getBinding();
                        if (Geometry.class.equals(binding)) {
                            try {
                                FeatureReader r = (FeatureReader) format.read(data, item);
                                try {
                                    if (r.hasNext()) {
                                        SimpleFeature f = (SimpleFeature) r.next();
                                        if (f.getDefaultGeometry() != null) {
                                            binding = f.getDefaultGeometry().getClass();
                                        }
                                    }
                                } finally {
                                    r.close();
                                }
                            } catch (IOException e) {
                                LOGGER.warning("Unable to determine concrete geometry type");
                            }
                        }
                        typeBuilder.add(attName(gd.getLocalName()), binding);
                    }
                    for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                        if (att.equals(gd)) {
                            continue;
                        }
                        typeBuilder.add(attName(att.getLocalName()), att.getType().getBinding());
                    }
                    return typeBuilder.buildFeatureType();
                }

                @Override
                public void convert(SimpleFeature from, SimpleFeature to) {
                    for (AttributeDescriptor att : from.getType().getAttributeDescriptors()) {
                        Object obj = from.getAttribute(att.getLocalName());
                        if (att instanceof GeometryDescriptor) {
                            to.setDefaultGeometry(obj);
                        } else if (containsAttribute(to, attName(att.getLocalName()))) {
                            to.setAttribute(attName(att.getLocalName()), obj);
                        }
                    }
                }

                String attName(String name) {
                    name = convertAttributeName(name);
                    return name.length() > 10 ? name.substring(0, 10) : name;
                }

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

                @Override
                public SimpleFeatureType convertType(
                        SimpleFeatureType featureType,
                        VectorFormat format,
                        ImportData data,
                        ImportTask item) {
                    SimpleFeatureType converted =
                            DEFAULT.convertType(featureType, format, data, item);
                    String featureTypeName = convertTypeName(featureType.getTypeName());
                    // trim the length of the name
                    // by default, postgis table/index names need to fit in 64 characters
                    // with the "spatial_" prefix and "_geometry" suffix, that leaves 47 chars
                    // and we should leave room to append integers to make the name unique too
                    if (featureTypeName.length() > 45) {
                        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                        featureTypeName = featureTypeName.substring(0, 45);
                        typeBuilder.setName(featureTypeName);
                        typeBuilder.addAll(featureType.getAttributeDescriptors());
                        converted = typeBuilder.buildFeatureType();
                    }
                    return converted;
                }
            };

    public static final FeatureDataConverter TO_ORACLE =
            new FeatureDataConverter() {
                public void convert(SimpleFeature from, SimpleFeature to) {
                    // for oracle the target names are always uppercase
                    Set<String> fromAttrNames = attributeNames(from);
                    Set<String> toAttrNames = attributeNames(to);
                    for (String name : fromAttrNames) {
                        String toName = name.toUpperCase();
                        if (toAttrNames.contains(toName)) {
                            to.setAttribute(convertAttributeName(toName), from.getAttribute(name));
                        }
                    }
                };

                public SimpleFeatureType convertType(
                        SimpleFeatureType featureType,
                        VectorFormat format,
                        ImportData data,
                        ImportTask task) {
                    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                    AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
                    typeBuilder.setName(ensureOracleSafe(featureType.getTypeName()));
                    for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                        attributeBuilder.init(att);
                        final String name = (ensureOracleSafe(att.getName().getLocalPart()));
                        typeBuilder.add(attributeBuilder.buildDescriptor(name));
                    }
                    return typeBuilder.buildFeatureType();
                }

                private final String ensureOracleSafe(final String identifier) {
                    final String capitalized = convertTypeName(identifier).toUpperCase();
                    final String notReserved;
                    if (ORACLE_RESERVED_WORDS.contains(capitalized)) {
                        notReserved = capitalized + "_";
                    } else {
                        notReserved = capitalized;
                    }
                    return notReserved;
                }
            };
}
