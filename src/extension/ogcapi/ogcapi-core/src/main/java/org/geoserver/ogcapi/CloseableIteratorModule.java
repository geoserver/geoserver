/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.util.Iterator;
import java.util.List;

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
 * {@code META-INF/services/com.fasterxml.jackson.databind.Module}), ensuring it works in both vanilla GeoServer and
 * Spring Boot environments like GeoServer Cloud.
 *
 * @see MappingJackson2HttpMessageConverter
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
        context.addBeanSerializerModifier(new CloseableIteratorBeanSerializerModifier());
    }

    /**
     * BeanSerializerModifier that replaces the serializer for bean properties returning Iterator with our custom
     * CloseableIteratorSerializer. This approach has higher priority than the default Iterator serializer.
     */
    private static class CloseableIteratorBeanSerializerModifier extends BeanSerializerModifier {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public List<BeanPropertyWriter> changeProperties(
                SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
            for (BeanPropertyWriter writer : beanProperties) {
                JavaType propertyType = writer.getType();

                // Replace serializer for any Iterator property
                if (Iterator.class.isAssignableFrom(propertyType.getRawClass())) {
                    JavaType elementType = propertyType.containedTypeOrUnknown(0);
                    JsonSerializer ser = new CloseableIteratorSerializer(elementType, false, null);
                    writer.assignSerializer(ser);
                }
            }
            return beanProperties;
        }
    }
}
