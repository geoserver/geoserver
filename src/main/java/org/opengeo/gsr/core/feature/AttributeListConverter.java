/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

import java.util.List;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class AttributeListConverter implements Converter {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean canConvert(Class clazz) {
        return clazz.equals(AttributeList.class);
    }

    @Override
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        AttributeList attributes = (AttributeList) obj;
        List<Attribute> attrs = attributes.getAttributes();
        for (Attribute attr : attrs) {
            writer.startNode(attr.getName());
            // TODO: This encodes all values as Strings, which passes Schema validation but is not desirable
            writer.setValue(attr.getValue().toString());
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // TODO Auto-generated method stub
        return null;
    }

}
