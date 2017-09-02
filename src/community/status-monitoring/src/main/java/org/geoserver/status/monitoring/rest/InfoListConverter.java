package org.geoserver.status.monitoring.rest;

import org.geoserver.status.monitoring.collector.SystemInfoProperty;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class InfoListConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Infos.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        Infos infos = (Infos) source;
        for (SystemInfoProperty data : infos.getData()) {
            context.convertAnother(data);
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return null;
    }

}
