package com.boundlessgeo.gsr.core;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.Map;

/**
 * Converts {@link Map}s into JSON objects instead of Arrays.
 *
 * Used with {@link com.boundlessgeo.gsr.api.GeoServicesJSONConverter}
 */
public class MapJsonConverter implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext context) {
        Map map = (Map) o;
        for (Object key : ((Map) o).keySet()) {
            writer.startNode(key.toString());
            context.convertAnother(map.get(key));
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return null;
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }
}
