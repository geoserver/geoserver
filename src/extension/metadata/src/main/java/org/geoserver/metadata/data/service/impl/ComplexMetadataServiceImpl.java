/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.metadata.data.dto.AttributeCollection;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.OccurrenceEnum;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation.
 *
 * <p>Node: values for templates that are lists are added in front of the user defined values in
 * order to keep the indexes in the description map constant even when the user modifies the list.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
@Repository
public class ComplexMetadataServiceImpl implements ComplexMetadataService {

    private static final Logger LOGGER = Logging.getLogger(ComplexMetadataServiceImpl.class);

    @Autowired ConfigurationService configService;

    @Override
    public void merge(
            ComplexMetadataMap destination,
            List<ComplexMetadataMap> sources,
            Map<String, List<Integer>> derivedAtts) {

        clearTemplateData(destination, derivedAtts);

        ArrayList<ComplexMetadataMap> reversed = new ArrayList<>(sources);
        Collections.reverse(reversed);
        for (ComplexMetadataMap source : reversed) {
            mergeAttributes(destination, source, derivedAtts);
        }
    }

    private void mergeAttributes(
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            Map<String, List<Integer>> derivedAtts) {
        for (AttributeConfiguration attribute :
                configService.getMetadataConfiguration().getAttributes()) {
            // keep track of the values that are from the template
            List<Integer> indexes =
                    derivedAtts.computeIfAbsent(attribute.getKey(), key -> new ArrayList<>());

            if (attribute.getFieldType() == FieldTypeEnum.COMPLEX) {
                mergeComplexField(attribute, destination, source, indexes);
            } else {
                mergeSimpleField(attribute, destination, source, indexes);
            }
        }
    }

    private void mergeSimpleField(
            AttributeConfiguration attribute,
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            List<Integer> indexes) {

        switch (attribute.getOccurrence()) {
            case SINGLE:
                Serializable sourceValue =
                        source.get(Serializable.class, attribute.getKey()).getValue();
                if (sourceValue != null) {
                    destination.get(Serializable.class, attribute.getKey()).setValue(sourceValue);
                    indexes.add(0);
                }
                break;
            case REPEAT:
                int startIndex = indexes.size();
                int sourceSize = source.size(attribute.getKey());
                if (sourceSize > 0) {
                    // SHIFT user content
                    for (int i = destination.size(attribute.getKey()) - 1; i >= startIndex; i--) {
                        Serializable value =
                                destination
                                        .get(Serializable.class, attribute.getKey(), i)
                                        .getValue();
                        if (!contains(source, attribute, value)) {
                            destination
                                    .get(Serializable.class, attribute.getKey(), i + sourceSize)
                                    .setValue(value);
                        } else {
                            // remove duplicates from templates
                            // shift everything one step backwards again
                            destination.delete(attribute.getKey(), i + sourceSize);
                        }
                    }
                }

                // insert template content
                for (int i = 0; i < sourceSize; i++) {
                    sourceValue = source.get(Serializable.class, attribute.getKey(), i).getValue();
                    int index = startIndex + i;
                    indexes.add(index);
                    destination
                            .get(Serializable.class, attribute.getKey(), index)
                            .setValue(sourceValue);
                }
        }
    }

    private boolean contains(
            ComplexMetadataMap source, AttributeConfiguration attribute, Serializable value) {
        for (int i = 0; i < source.size(attribute.getKey()); i++) {
            Serializable other = source.get(Serializable.class, attribute.getKey(), i).getValue();
            if (value == null && other == null || value != null && value.equals(other)) {
                return true;
            }
        }
        return false;
    }

    private void mergeComplexField(
            AttributeConfiguration attribute,
            ComplexMetadataMap destination,
            ComplexMetadataMap source,
            List<Integer> indexes) {

        switch (attribute.getOccurrence()) {
            case SINGLE:
                if (source.size(attribute.getKey()) > 0) {
                    ComplexMetadataMap sourceMap = source.subMap(attribute.getKey());
                    ComplexMetadataMap destinationMap = destination.subMap(attribute.getKey());
                    copy(sourceMap, destinationMap, attribute.getTypename());
                    indexes.add(0);
                }
                break;
            case REPEAT:
                int startIndex = indexes.size();
                int sourceSize = source.size(attribute.getKey());
                if (sourceSize > 0) {
                    // SHIFT user content
                    for (int i = destination.size(attribute.getKey()) - 1; i >= startIndex; i--) {
                        ComplexMetadataMap orig = destination.subMap(attribute.getKey(), i);
                        if (!containsComplex(source, attribute, orig)) {
                            ComplexMetadataMap shifted =
                                    destination.subMap(attribute.getKey(), i + sourceSize);
                            copy(orig, shifted, attribute.getTypename());
                        } else {
                            // remove duplicates from templates
                            // shift everything one step backwards again
                            destination.delete(attribute.getKey(), i + sourceSize);
                        }
                    }
                }

                // insert template content
                for (int i = 0; i < source.size(attribute.getKey()); i++) {
                    ComplexMetadataMap sourceMap = source.subMap(attribute.getKey(), i);
                    int index = startIndex + i;
                    ComplexMetadataMap destinationMap =
                            destination.subMap(attribute.getKey(), index);
                    indexes.add(index);
                    copy(sourceMap, destinationMap, attribute.getTypename());
                }
        }
    }

    private boolean containsComplex(
            ComplexMetadataMap source, AttributeConfiguration attribute, ComplexMetadataMap map) {
        for (int i = 0; i < source.size(attribute.getKey()); i++) {
            ComplexMetadataMap other = source.subMap(attribute.getKey(), i);
            if (map == null && other == null || equals(map, other, attribute.getTypename())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(ComplexMetadataMap map, ComplexMetadataMap other, String typeName) {
        AttributeCollection typeConfiguration = getAttributeCollection(typeName);

        for (AttributeConfiguration config : typeConfiguration.getAttributes()) {
            if (config.getFieldType() != FieldTypeEnum.COMPLEX) {
                Serializable value = map.get(Serializable.class, config.getKey()).getValue();
                Serializable otherValue = other.get(Serializable.class, config.getKey()).getValue();
                if (!(value == null && otherValue == null
                        || value != null && value.equals(otherValue))) {
                    return false;
                }
            } else {
                if (!equals(
                        map.subMap(config.getKey()),
                        other.subMap(config.getKey()),
                        config.getTypename())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void clearTemplateData(
            ComplexMetadataMap destination, Map<String, List<Integer>> derivedAtts) {
        if (derivedAtts != null) {
            for (String key : derivedAtts.keySet()) {
                AttributeConfiguration attConfig =
                        configService.getMetadataConfiguration().findAttribute(key);
                if (attConfig != null && attConfig.getOccurrence() == OccurrenceEnum.REPEAT) {
                    ArrayList<Integer> reversed = new ArrayList<>(derivedAtts.get(key));
                    Collections.sort(reversed);
                    Collections.reverse(reversed);
                    for (Integer index : reversed) {
                        destination.delete(key, index);
                    }
                } else if (!derivedAtts.get(key).isEmpty()) {
                    destination.delete(key);
                }
            }
            derivedAtts.clear();
        }
    }

    @Override
    public void init(ComplexMetadataMap map, String typeName) {
        AttributeCollection attCollection = getAttributeCollection(typeName);

        if (attCollection != null) {
            for (AttributeConfiguration config : attCollection.getAttributes()) {
                int size;
                if (config.getFieldType() != FieldTypeEnum.COMPLEX) {
                    if (config.getOccurrence() == OccurrenceEnum.REPEAT
                            && (size = map.size(config.getKey())) > 0) {
                        for (int i = 0; i < size; i++) {
                            map.get(Serializable.class, config.getKey(), i).init();
                        }
                    } else {
                        map.get(Serializable.class, config.getKey()).init();
                    }
                } else {
                    if (config.getOccurrence() == OccurrenceEnum.REPEAT
                            && (size = map.size(config.getKey())) > 0) {
                        for (int i = 0; i < size; i++) {
                            init(map.subMap(config.getKey(), i), config.getTypename());
                        }
                    } else {
                        init(map.subMap(config.getKey()), config.getTypename());
                    }
                }
            }
        }
    }

    @Override
    public void copy(
            ComplexMetadataMap source,
            ComplexMetadataMap dest,
            String typeName,
            boolean ignoreUUID) {
        AttributeCollection attCollection = getAttributeCollection(typeName);

        for (AttributeConfiguration config : attCollection.getAttributes()) {
            if (config.getFieldType() != FieldTypeEnum.COMPLEX) {
                if (!ignoreUUID || config.getFieldType() != FieldTypeEnum.UUID) {
                    dest.get(Serializable.class, config.getKey())
                            .setValue(
                                    ComplexMetadataMapImpl.dimCopy(
                                            source.get(Serializable.class, config.getKey())
                                                    .getValue()));
                }
            } else {
                copy(
                        source.subMap(config.getKey()),
                        dest.subMap(config.getKey()),
                        config.getTypename());
            }
        }
    }

    @Override
    public void clean(ComplexMetadataMap map) {
        ComplexMetadataMap copy = map.clone();
        map.delete("");
        copy(copy, map, null);
    }

    @Override
    public void derive(ComplexMetadataMap map) {
        derive(map, null);
    }

    private void derive(ComplexMetadataMap map, String typeName) {
        AttributeCollection attCollection = getAttributeCollection(typeName);

        if (attCollection != null) {
            for (AttributeConfiguration config : attCollection.getAttributes()) {
                if (config.getFieldType() == FieldTypeEnum.DERIVED) {
                    AttributeConfiguration derivedFrom =
                            attCollection.findAttribute(config.getDerivedFrom());
                    if (derivedFrom != null) {
                        if (config.getOccurrence() == OccurrenceEnum.REPEAT) {
                            map.delete(config.getKey());
                            for (int i = 0; i < map.size(derivedFrom.getKey()); i++) {
                                String value =
                                        map.get(String.class, config.getDerivedFrom(), i)
                                                .getValue();
                                int indexValue = derivedFrom.getValues().indexOf(value);
                                String realValue =
                                        indexValue >= 0 && indexValue < config.getValues().size()
                                                ? config.getValues().get(indexValue)
                                                : null;
                                map.get(String.class, config.getKey(), i).setValue(realValue);
                            }
                        } else {
                            String value =
                                    map.get(String.class, config.getDerivedFrom()).getValue();
                            int indexValue = derivedFrom.getValues().indexOf(value);
                            String actualValue =
                                    indexValue >= 0 && indexValue < config.getValues().size()
                                            ? config.getValues().get(indexValue)
                                            : null;
                            map.get(String.class, config.getKey()).setValue(actualValue);
                        }
                    }
                } else if (config.getFieldType() == FieldTypeEnum.COMPLEX) {
                    if (config.getOccurrence() == OccurrenceEnum.REPEAT) {
                        for (int i = 0; i < map.size(config.getKey()); i++) {
                            derive(map.subMap(config.getKey(), i), config.getTypename());
                        }
                    } else {
                        derive(map.subMap(config.getKey()), config.getTypename());
                    }
                }
            }
        }
    }

    private AttributeCollection getAttributeCollection(String typeName) {
        AttributeCollection coll =
                typeName == null
                        ? configService.getMetadataConfiguration()
                        : configService.getMetadataConfiguration().findType(typeName);

        if (coll == null) {
            LOGGER.log(
                    Level.WARNING, "Could not find complex attribute type with name " + typeName);
        }

        return coll;
    }
}
