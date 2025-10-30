/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Custom iterator serializer that extends Jackson's standard {@link IteratorSerializer} to add automatic resource
 * cleanup for {@link AutoCloseable} iterators.
 *
 * <p>This serializer ensures that if the iterator being serialized implements {@link AutoCloseable} (such as
 * {@link org.geoserver.catalog.util.CloseableIterator}), it will be automatically closed after serialization completes,
 * whether successful or not.
 *
 * <p>This is particularly useful for streaming data sources where the iterator wraps resources that need explicit
 * cleanup, such as database cursors, file handles, or other I/O resources.
 *
 * <p>This serializer works with any Iterator that implements AutoCloseable, making it generic and reusable across
 * different types of closeable iterators.
 *
 * <p>Example usage:
 *
 * <pre>
 * SimpleModule module = new SimpleModule();
 * module.addSerializer(Iterator.class,
 *     new CloseableIteratorSerializer());
 * objectMapper.registerModule(module);
 * </pre>
 *
 * @see IteratorSerializer
 * @see AutoCloseable
 * @see org.geoserver.catalog.util.CloseableIterator
 */
@SuppressWarnings("serial")
class CloseableIteratorSerializer extends IteratorSerializer {

    static final Logger LOGGER = Logging.getLogger(CloseableIteratorSerializer.class);

    @VisibleForTesting
    static final AtomicLong closed = new AtomicLong();

    /**
     * Default no-arg constructor required by Jackson when using {@code @JsonSerialize} annotation. Creates a serializer
     * with dynamic typing (no static element type).
     */
    public CloseableIteratorSerializer() {
        super(null, false, null);
    }

    /**
     * Main constructor for creating a new CloseableIteratorSerializer.
     *
     * @param elemType the type of elements in the iterator, or {@code null} for dynamic typing
     * @param staticTyping whether to use static typing for elements
     * @param vts type serializer for handling polymorphic types, or {@code null} if not needed
     */
    public CloseableIteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts) {
        super(elemType, staticTyping, vts);
    }

    /**
     * Copy constructor used for creating resolved instances with specific property context. This is called by Jackson
     * during serializer resolution to create context-specific serializer instances.
     *
     * @param src the source serializer to copy from
     * @param property the property being serialized, may be {@code null}
     * @param vts type serializer for handling polymorphic types, or {@code null} if not needed
     * @param valueSerializer pre-resolved value serializer, or {@code null} for dynamic resolution
     * @param unwrapSingle whether to unwrap single-element arrays, or {@code null} to use default
     */
    public CloseableIteratorSerializer(
            CloseableIteratorSerializer src,
            BeanProperty property,
            TypeSerializer vts,
            JsonSerializer<?> valueSerializer,
            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    /**
     * Factory method for creating a contextualized copy of this serializer with resolved component serializers and
     * configuration.
     *
     * <p>This method is called by Jackson during serializer resolution to create instances with proper property context
     * and element serializers.
     *
     * @param property the property being serialized, may be {@code null}
     * @param vts type serializer for handling polymorphic types, or {@code null} if not needed
     * @param elementSerializer the resolved serializer for elements, or {@code null} for dynamic
     * @param unwrapSingle whether to unwrap single-element arrays, or {@code null} to use default
     * @return a new instance of this serializer with the resolved configuration
     */
    @Override
    public IteratorSerializer withResolved(
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer, Boolean unwrapSingle) {
        return new CloseableIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

    /**
     * Serializes the contents of the iterator and ensures proper resource cleanup.
     *
     * <p>This method delegates to the parent implementation for actual serialization, but wraps it in a try-finally
     * block to ensure that if the iterator implements {@link AutoCloseable}, it will be closed after serialization
     * completes.
     *
     * <p>If an exception occurs during the close operation, it is caught to avoid masking any serialization exceptions.
     * The close exception is not rethrown as the primary concern is the serialization result.
     *
     * @param value the iterator to serialize
     * @param g the JSON generator to write to
     * @param provider the serializer provider for context and configuration
     * @throws IOException if an error occurs during serialization
     */
    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public void serializeContents(Iterator<?> value, JsonGenerator g, SerializerProvider provider) throws IOException {
        try {
            super.serializeContents(value, g, provider);
        } finally {
            if (value instanceof AutoCloseable) {
                try {
                    closed.incrementAndGet();
                    ((AutoCloseable) value).close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing resource: " + value, e);
                }
            }
        }
    }
}
