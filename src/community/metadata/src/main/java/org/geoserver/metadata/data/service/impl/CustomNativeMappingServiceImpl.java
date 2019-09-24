/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.metadata.data.dto.CustomNativeMappingConfiguration;
import org.geoserver.metadata.data.dto.CustomNativeMappingsConfiguration;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.data.service.CustomNativeMappingService;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Allows the geoserver-native attributes keywords, metadatalinks, and identifiers to be
 * automatically updated based on custom attributes.
 *
 * @author Niels Charlier
 */
@Service
public class CustomNativeMappingServiceImpl implements CustomNativeMappingService {

    private static enum MappingTypeEnum {
        KEYWORDS {
            @Override
            List<?> list(LayerInfo layer) {
                return layer.getResource().getKeywords();
            }

            @Override
            Object create(String value) {
                return new Keyword(value);
            }

            @Override
            String getValue(Object object) {
                return ((Keyword) object).getValue();
            }
        },
        IDENTIFIERS {
            @Override
            List<?> list(LayerInfo layer) {
                return layer.getIdentifiers();
            }

            @Override
            Object create(String value) {
                LayerIdentifier li = new LayerIdentifier();
                li.setIdentifier(value);
                return li;
            }

            @Override
            String getValue(Object object) {
                return ((LayerIdentifier) object).getIdentifier();
            }
        },
        METADATALINKS {
            @Override
            List<?> list(LayerInfo layer) {
                return layer.getResource().getMetadataLinks();
            }

            @Override
            Object create(String value) {
                MetadataLinkInfoImpl mli = new MetadataLinkInfoImpl();
                mli.setContent(value);
                return mli;
            }

            @Override
            String getValue(Object object) {
                return ((MetadataLinkInfo) object).getContent();
            }
        };

        abstract List<?> list(LayerInfo layer);

        abstract Object create(String value);

        abstract String getValue(Object object);
    }

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(CustomNativeMappingServiceImpl.class);

    private static final String VALUE = "value";

    @Autowired private ConfigurationService configService;

    @Override
    public void mapCustomToNative(LayerInfo layer) {
        CustomNativeMappingsConfiguration config =
                configService.getCustomNativeMappingsConfiguration();

        Map<String, List<Integer>> indexMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> custom =
                convert(
                        (Map<String, Serializable>)
                                layer.getResource()
                                        .getMetadata()
                                        .get(MetadataConstants.CUSTOM_METADATA_KEY),
                        indexMap);

        Set<MappingTypeEnum> cleared = new HashSet<>();
        for (CustomNativeMappingConfiguration mapping : config.getCustomNativeMappings()) {
            MappingTypeEnum mappingType = MappingTypeEnum.valueOf(mapping.getType());

            if (!cleared.contains(mappingType)) {
                mappingType.list(layer).clear();
                cleared.add(mappingType);
            }
            mappingType
                    .list(layer)
                    .addAll(build(mapping.getMapping(), mappingType, custom, indexMap));
        }
    }

    @Override
    public void mapNativeToCustom(LayerInfo layer, List<Integer> indexes) {
        CustomNativeMappingsConfiguration config =
                configService.getCustomNativeMappingsConfiguration();

        @SuppressWarnings("unchecked")
        Map<String, Serializable> custom =
                (Map<String, Serializable>)
                        layer.getResource()
                                .getMetadata()
                                .computeIfAbsent(
                                        MetadataConstants.CUSTOM_METADATA_KEY,
                                        key -> new HashMap<>());

        for (int i = 0; i < config.getCustomNativeMappings().size(); i++) {
            if (indexes == null || indexes.contains(i)) {
                CustomNativeMappingConfiguration mapping = config.getCustomNativeMappings().get(i);
                MappingTypeEnum mappingType = MappingTypeEnum.valueOf(mapping.getType());

                for (Object item : mappingType.list(layer)) {
                    Map<String, String> mappedRecord = new HashMap<>();
                    for (Entry<String, String> mappingEntry : mapping.getMapping().entrySet()) {
                        try {
                            Map<String, String> mappedProperties =
                                    PlaceHolderUtil.reversePlaceHolders(
                                            mappingEntry.getValue(),
                                            VALUE.equals(mappingEntry.getKey())
                                                    ? mappingType.getValue(item)
                                                    : BeanUtils.getProperty(
                                                            item, mappingEntry.getKey()));
                            if (mappedProperties == null) {
                                mappedRecord = null;
                                break;
                            }
                            mappedRecord.putAll(mappedProperties);
                        } catch (IllegalAccessException
                                | InvocationTargetException
                                | NoSuchMethodException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                    if (mappedRecord != null) {
                        merge(custom, mappedRecord);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> build(
            Map<String, String> mapping,
            MappingTypeEnum mappingType,
            Map<String, List<String>> custom,
            Map<String, List<Integer>> indexMap) {
        List<String> values = PlaceHolderUtil.replacePlaceHolder(mapping.get(VALUE), custom);
        List<T> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null) {
                result.add((T) mappingType.create(values.get(i)));
            }
        }
        List<Integer> indexList = indexMap.get(PlaceHolderUtil.getPlaceHolder(mapping.get(VALUE)));
        for (Entry<String, String> entry : mapping.entrySet()) {
            if (!VALUE.equals(entry.getKey())) {
                values = PlaceHolderUtil.replacePlaceHolder(entry.getValue(), custom);
                for (int i = 0;
                        i < result.size()
                                && (values == null || indexList != null || i < values.size());
                        i++) {
                    try {
                        BeanUtils.setProperty(
                                result.get(i),
                                entry.getKey(),
                                values == null
                                        ? entry.getValue()
                                        : indexList == null || values.size() == result.size()
                                                ? values.get(i)
                                                : values.get(indexList.get(i)));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    private static Map<String, List<String>> convert(
            Map<String, Serializable> map, Map<String, List<Integer>> indexMap) {
        Map<String, List<String>> result = new HashMap<>();
        for (Entry<String, Serializable> entry : map.entrySet()) {
            List<String> list = new ArrayList<>();
            if (entry.getValue() instanceof List<?>) {
                List<?> items = (List<?>) entry.getValue();
                if (items.size() > 0 && items.get(0) instanceof List<?>) {
                    // two dimensions, make index map for cross-dimensional mapping
                    List<Integer> indexList = new ArrayList<Integer>();
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i) instanceof List<?>) {
                            for (Object item : (List<?>) items.get(i)) {
                                list.add(Converters.convert(item, String.class));
                                indexList.add(i);
                            }
                        }
                    }
                    indexMap.put(entry.getKey(), indexList);
                } else {
                    for (Object item : items) {
                        list.add(Converters.convert(item, String.class));
                    }
                }
            } else if (entry.getValue() != null) {
                list.add(Converters.convert(entry.getValue(), String.class));
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void merge(Map<String, Serializable> dest, Map<String, String> map) {
        // check if nothing is being duplicated
        boolean exists = true;
        for (Entry<String, String> entry : map.entrySet()) {
            Serializable target = dest.get(entry.getKey());
            if (target instanceof List) {
                exists = ((List<Serializable>) target).contains(entry.getValue());
            } else {
                exists = target != null && target.equals(entry.getValue());
            }
            if (!exists) {
                break;
            }
        }
        if (exists) {
            return;
        }
        for (Entry<String, String> entry : map.entrySet()) {
            Serializable target = dest.get(entry.getKey());
            if (target instanceof List) {
                ((List<Serializable>) target).add(entry.getValue());
            } else {
                dest.put(
                        entry.getKey(),
                        target == null
                                ? entry.getValue()
                                : Lists.newArrayList(target, entry.getValue()));
            }
        }
    }
}
