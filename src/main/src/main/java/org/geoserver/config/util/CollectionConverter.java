/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionConverter
        extends com.thoughtworks.xstream.converters.collections.CollectionConverter {

    public static final String UNMODIFIABLE_LIST = "java.util.Collections$UnmodifiableList";
    public static final String UNMODIFIABLE_SET = "java.util.Collections$UnmodifiableSet";
    public static final String ARRAY_LIST = "java.util.Arrays$ArrayList";

    public CollectionConverter(Mapper mapper) {
        super(mapper);
    }

    public CollectionConverter(Mapper mapper, Class type) {
        super(mapper, type);
    }

    @Override
    public boolean canConvert(Class type) {
        if (type != null) {
            String typeName = type.getName();
            if (typeName.equals(ARRAY_LIST)
                    || typeName.equals(UNMODIFIABLE_LIST)
                    || typeName.equals(UNMODIFIABLE_SET)) {
                return true;
            }
        }
        return super.canConvert(type);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Class requiredType = context.getRequiredType();
        if (requiredType != null) {
            String typeName = requiredType.getName();
            if (UNMODIFIABLE_LIST.equals(typeName)) {
                List list = new ArrayList<>();
                populateCollection(reader, context, list);
                return Collections.unmodifiableList(list);
            } else if (UNMODIFIABLE_SET.equals(typeName)) {
                Set set = new HashSet<>();
                populateCollection(reader, context, set);
                return Collections.unmodifiableSet(set);
            } else if (ARRAY_LIST.equals(typeName)) {
                List list = new ArrayList<>();
                populateCollection(reader, context, list);
                return Arrays.asList(list.toArray());
            }
        }
        return super.unmarshal(reader, context);
    }
}
