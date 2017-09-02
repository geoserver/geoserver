package org.geoserver.status.monitoring.rest;

import org.geoserver.status.monitoring.collector.SystemInfoProperty;
import org.geoserver.status.monitoring.collector.SystemPropertyValue;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SystemInfoConverter implements Converter {

    private String DEFAULT_VALUE = "NOT AVAILABLE";

    @Override
    public boolean canConvert(Class type) {
        return type.equals(SystemInfoProperty.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        SystemInfoProperty proeprty = (SystemInfoProperty) source;
        if (!proeprty.getAvailable() || proeprty.getValues() == null
                || proeprty.getValues().isEmpty()) {
            writer.startNode(proeprty.name());
            addOtherProperty(proeprty, writer);
            writer.endNode();
        } else if (proeprty.getValues().size() == 1) {
            writer.startNode(proeprty.name());
            addOtherProperty(proeprty, writer);
            writer.startNode("value");
            writer.setValue(proeprty.getValues().get(0).getValue());
            writer.endNode();
            writer.endNode();
        } else {
            for (SystemPropertyValue p : proeprty.getValues()) {
                writer.startNode(proeprty.name());
                addOtherProperty(proeprty, writer);
                writer.startNode(p.getCountName());
                writer.setValue(p.getCount());
                writer.endNode();
                writer.startNode(p.getValueName());
                writer.setValue(p.getValue());
                writer.endNode();
                writer.endNode();
            }
        }
    }

    private void addOtherProperty(SystemInfoProperty proeprty, HierarchicalStreamWriter writer) {
        writer.startNode("available");
        writer.setValue(proeprty.getAvailable().toString().toUpperCase());
        writer.endNode();
        writer.startNode("description");
        writer.setValue(proeprty.getDescription());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return null;
    }

}
