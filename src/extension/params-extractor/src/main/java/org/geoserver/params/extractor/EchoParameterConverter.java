/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Optional;
import java.util.UUID;

/** Custom XStream converter for {@link EchoParameter} */
public class EchoParameterConverter implements Converter {
    @Override
    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        EchoParameter param = (EchoParameter) source;
        writer.addAttribute("id", param.getId());
        writer.addAttribute("parameter", param.getParameter());
        writer.addAttribute("activated", String.valueOf(param.getActivated()));
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return new EchoParameterBuilder()
                .withId(
                        Optional.ofNullable(reader.getAttribute("id"))
                                .orElse(UUID.randomUUID().toString()))
                .withActivated(Boolean.valueOf(reader.getAttribute("activated")))
                .withParameter(reader.getAttribute("parameter"))
                .build();
    }

    @Override
    public boolean canConvert(Class type) {
        return EchoParameter.class.equals(type);
    }
}
