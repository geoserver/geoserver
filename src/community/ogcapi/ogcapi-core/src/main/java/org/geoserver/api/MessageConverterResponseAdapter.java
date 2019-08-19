/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Base class for adapting {@link Response} objects for a given response type and
 * HttpMessageConverter interface
 *
 * @param <T>
 */
public class MessageConverterResponseAdapter<T>
        implements HttpMessageConverter<T>, ApplicationContextAware {

    Class<T> valueClass;
    Class responseBinding;
    List<Response> responses;
    private List<MediaType> supportedMediaTypes;

    public MessageConverterResponseAdapter(Class<T> valueClass, Class responseBinding) {
        this.valueClass = valueClass;
        this.responseBinding = responseBinding;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        // write only
        return false;
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        return valueClass.isAssignableFrom(aClass)
                && (mediaType == null || getResponse(mediaType).isPresent());
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    public T read(Class<? extends T> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(T value, MediaType mediaType, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        Optional<Response> response = getResponse(mediaType);
        if (!response.isPresent()) {
            throw new IllegalArgumentException(
                    "Could not find a Response handling "
                            + mediaType
                            + " for binding "
                            + valueClass);
        }

        Request dr = Dispatcher.REQUEST.get();
        Operation operation = getOperation(value, dr);
        writeResponse(value, httpOutputMessage, operation, response.get());
    }

    protected void writeResponse(
            T value, HttpOutputMessage httpOutputMessage, Operation operation, Response response)
            throws IOException {
        response.write(value, httpOutputMessage.getBody(), operation);
    }

    protected Operation getOperation(T featuresResponse, Request dr) {
        return dr.getOperation();
    }

    public Optional<Response> getResponse(MediaType mediaType) {
        return responses
                .stream()
                .filter(r -> getMediaTypeStream(r).anyMatch(mt -> mediaType.isCompatibleWith(mt)))
                .findFirst();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.responses =
                GeoServerExtensions.extensions(Response.class, applicationContext)
                        .stream()
                        .filter(getResponseFilterPredicate())
                        .collect(Collectors.toList());
        this.supportedMediaTypes =
                this.responses
                        .stream()
                        .flatMap(r -> getMediaTypeStream(r))
                        .distinct()
                        .collect(Collectors.toList());
    }

    protected Predicate<Response> getResponseFilterPredicate() {
        return r -> responseBinding.isAssignableFrom(r.getBinding());
    }

    private Stream<MediaType> getMediaTypeStream(Response r) {
        return r.getOutputFormats()
                .stream()
                .filter(f -> f.contains("/"))
                .filter(
                        f -> {
                            // GML2 content type is not really valid, this is here to filter rough
                            // content types
                            try {
                                MediaType.parseMediaType(f);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                .map(f -> MediaType.parseMediaType(f));
    }
}
