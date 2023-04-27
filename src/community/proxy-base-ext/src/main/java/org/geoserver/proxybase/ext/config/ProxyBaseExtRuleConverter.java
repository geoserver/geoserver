/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.config;

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
import org.geoserver.proxybase.ext.ProxyBaseExtensionRuleBuilder;

/** Converts a {@link ProxyBaseExtensionRule} to and from XML. */
public class ProxyBaseExtRuleConverter implements Converter {
    @Override
    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        ProxyBaseExtensionRule rule = (ProxyBaseExtensionRule) source;
        String[] properties = {"id", "activated", "matcher", "transformer", "position"};
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
        ProxyBaseExtensionRuleBuilder builder = new ProxyBaseExtensionRuleBuilder();
        builder.withId(
                Optional.ofNullable(reader.getAttribute("id"))
                        .orElse(UUID.randomUUID().toString()));
        addParameter(reader, "activated", builder::withActivated, Boolean::valueOf);
        addParameter(reader, "matcher", builder::withMatcher);
        addParameter(reader, "transformer", builder::withTransformer);
        addParameter(reader, "position", builder::withPosition, Integer::valueOf);
        return builder.build();
    }

    private <T> void addParameter(
            HierarchicalStreamReader reader, String attributeName, Consumer<T> consumer) {
        addParameter(reader, attributeName, consumer, null);
    }

    @SuppressWarnings("unchecked")
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
        return ProxyBaseExtensionRule.class.equals(type);
    }
}
