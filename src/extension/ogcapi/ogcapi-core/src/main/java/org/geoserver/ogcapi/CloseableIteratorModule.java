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
import com.fasterxml.jackson.databind.ser.Serializers;
import org.geoserver.catalog.util.CloseableIterator;

/**
 * Jackson module that registers the {@link CloseableIteratorSerializer} for {@link CloseableIterator} types and other
 * AutoCloseable iterators.
 *
 * <p>This module ensures that when Jackson serializes any Iterator that implements {@link AutoCloseable} (such as
 * {@link CloseableIterator}), it will:
 *
 * <ul>
 *   <li>Serialize all elements in the iterator as a JSON array
 *   <li>Automatically close the iterator after serialization completes
 * </ul>
 *
 * <p>This provides generic resource cleanup for any AutoCloseable Iterator, making it reusable for different types of
 * closeable iterators (database result sets, file streams, etc.).
 *
 * @see MappingJackson2HttpMessageConverter
 */
@SuppressWarnings("serial")
class CloseableIteratorModule extends SimpleModule {

    public CloseableIteratorModule() {
        super("CloseableIteratorModule", new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addSerializers(new CloseableIteratorSerializers());
    }

    /**
     * Custom serializers provider that returns {@link CloseableIteratorSerializer} for {@link CloseableIterator} types.
     */
    private static class CloseableIteratorSerializers extends Serializers.Base {
        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
            if (CloseableIterator.class.isAssignableFrom(type.getRawClass())) {
                JavaType elementType = type.containedTypeOrUnknown(0);
                return new CloseableIteratorSerializer(elementType, false, null);
            }
            return null;
        }
    }
}
