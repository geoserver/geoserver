/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.Iterator;
import java.util.List;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Jackson module that registers the {@link CloseableIteratorSerializer} for all {@link Iterator} types.
 *
 * <p>This module ensures that when Jackson serializes any Iterator, it will:
 *
 * <ul>
 *   <li>Serialize all elements in the iterator as a JSON array
 *   <li>Automatically close the iterator after serialization if it implements {@link AutoCloseable}
 * </ul>
 *
 * <p>This provides generic resource cleanup for any AutoCloseable Iterator, making it reusable for different types of
 * closeable iterators (database result sets, file streams, etc.).
 *
 * <p>This module is auto-discovered by Jackson via the Java ServiceLoader mechanism (see
 * {@code META-INF/services/tools.jackson.databind.JacksonModule}), ensuring it works in both vanilla GeoServer and
 * Spring Boot environments like GeoServer Cloud.
 *
 * @see JacksonJsonHttpMessageConverter
 * @see CloseableIteratorSerializer
 */
@SuppressWarnings("serial")
public class CloseableIteratorModule extends SimpleModule {

    public CloseableIteratorModule() {
        super("CloseableIteratorModule", new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addSerializerModifier(new CloseableIteratorBeanSerializerModifier());
    }

    /**
     * BeanSerializerModifier that replaces the serializer for bean properties returning Iterator with our custom
     * CloseableIteratorSerializer. This approach has higher priority than the default Iterator serializer.
     */
    private static class CloseableIteratorBeanSerializerModifier extends ValueSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(
                tools.jackson.databind.SerializationConfig config,
                BeanDescription.Supplier beanDesc,
                List<BeanPropertyWriter> beanProperties) {
            for (BeanPropertyWriter writer : beanProperties) {
                JavaType propertyType = writer.getType();

                // Replace serializer for any Iterator property
                if (Iterator.class.isAssignableFrom(propertyType.getRawClass())) {
                    JavaType elementType = propertyType.containedTypeOrUnknown(0);
                    @SuppressWarnings("unchecked")
                    ValueSerializer<Object> ser = (ValueSerializer<Object>)
                            (ValueSerializer<?>) new CloseableIteratorSerializer(elementType, false, null);
                    writer.assignSerializer(ser);
                }
            }
            return beanProperties;
        }
    }
}
