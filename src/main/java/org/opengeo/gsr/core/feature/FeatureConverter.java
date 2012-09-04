/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

import java.util.Iterator;

import org.opengeo.gsr.core.geometry.Geometry;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 *         TODO: Implement unmarshall; change marshal so that attribute values can be of any type, not only strings
 */
public class FeatureConverter implements Converter {

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(Feature.class);
    }

    @Override
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        Feature feature = (Feature) obj;
        Geometry geometry = feature.getGeometry();
        writer.startNode("geometry");
        writer.setValue(geometry.toString());
        writer.endNode();
        if (!feature.getAttributes().isEmpty()) {
            writer.startNode("attributes");
            Iterator<String> keys = feature.getAttributes().keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = feature.getAttributes().get(key);
                writer.startNode(key);
                writer.setValue(value.toString());
                writer.endNode();
            }
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // TODO Auto-generated method stub
        return null;
    }

}
