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
import java.util.function.Consumer;
import java.util.function.Function;
import org.geoserver.ows.util.OwsUtils;

public class RuleConverter implements Converter {
    @Override
    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Rule rule = (Rule) source;
        String[] properties =
                new String[] {
                    "id",
                    "activated",
                    "position",
                    "match",
                    "activation",
                    "parameter",
                    "transform",
                    "remove",
                    "combine",
                    "repeat"
                };
        for (String property : properties) {
            Object value = OwsUtils.get(rule, property);
            if (value == null) continue;
            if (!(value instanceof String)) {
                value = value.toString();
            }
            writer.addAttribute(property, (String) value);
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        RuleBuilder builder = new RuleBuilder();
        builder.withId(
                Optional.ofNullable(reader.getAttribute("id"))
                        .orElse(UUID.randomUUID().toString()));
        addParameter(reader, "activated", builder::withActivated, Boolean::valueOf);
        addParameter(reader, "position", builder::withPosition, Integer::valueOf);
        addParameter(reader, "match", builder::withMatch);
        addParameter(reader, "activation", builder::withActivation);
        addParameter(reader, "parameter", builder::withParameter);
        addParameter(reader, "transform", builder::withTransform);
        addParameter(reader, "remove", builder::withRemove, Integer::valueOf);
        addParameter(reader, "combine", builder::withCombine);
        addParameter(reader, "repeat", builder::withRepeat, Boolean::valueOf);
        return builder.build();
    }

    private <T> void addParameter(
            HierarchicalStreamReader reader, String attributeName, Consumer<T> consumer) {
        addParameter(reader, attributeName, consumer, null);
    }

    private <T> void addParameter(
            HierarchicalStreamReader reader,
            String attributeName,
            Consumer<T> consumer,
            Function<String, T> converter) {
        String value = reader.getAttribute(attributeName);
        if (value != null) {
            T converted;
            if (converter != null) {
                converted = converter.apply(value);
            } else {
                converted = (T) value;
            }
            consumer.accept(converted);
        }
    }

    @Override
    public boolean canConvert(Class type) {
        return Rule.class.equals(type);
    }
}
