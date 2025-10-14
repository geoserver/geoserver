/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
 * Base class for adapting {@link Response} objects for a given response type and HttpMessageConverter interface
 *
 * @param <T>
 */
public class MessageConverterResponseAdapter<T> implements HttpMessageConverter<T>, ApplicationContextAware {

    protected final Class<T> valueClass;
    protected final Class<?> responseBinding;
    protected List<Response> responses;
    protected List<MediaType> supportedMediaTypes;

    public MessageConverterResponseAdapter(Class<T> valueClass, Class<?> responseBinding) {
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
        if (response.isEmpty()) {
            throw new IllegalArgumentException(
                    "Could not find a Response handling " + mediaType + " for binding " + valueClass);
        }

        Request dr = Dispatcher.REQUEST.get();
        Operation operation = getOperation(value, dr, mediaType);
        writeResponse(value, httpOutputMessage, operation, response.get());
    }

    protected void writeResponse(T value, HttpOutputMessage httpOutputMessage, Operation operation, Response response)
            throws IOException {
        setHeaders(value, operation, response, httpOutputMessage);
        response.write(value, httpOutputMessage.getBody(), operation);
    }

    protected Operation getOperation(T result, Request dr, MediaType mediaType) {
        return dr.getOperation();
    }

    public Optional<Response> getResponse(MediaType mediaType) {
        @SuppressWarnings("unchecked")
        T result = (T) APIRequestInfo.get().getResult();
        Request dr = Dispatcher.REQUEST.get();
        Operation originalOperation = dr.getOperation();
        Operation op = result != null ? getOperation(result, dr, mediaType) : originalOperation;
        Predicate<MediaType> matchMediaType = mt -> mediaType.isCompatibleWith(mt);
        Predicate<Response> matchResponse =
                r -> r.canHandle(op) && (getMediaTypeStream(r).anyMatch(matchMediaType));
        return responses.stream().filter(matchResponse).findFirst();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Predicate<Response> predicate = getResponseFilterPredicate();
        this.responses = GeoServerExtensions.extensions(Response.class, applicationContext).stream()
                .filter(predicate)
                .collect(Collectors.toList());
        this.supportedMediaTypes = this.responses.stream()
                .flatMap(r -> getMediaTypeStream(r))
                .distinct()
                .collect(Collectors.toList());
    }

    protected Predicate<Response> getResponseFilterPredicate() {
        return r -> responseBinding.isAssignableFrom(r.getBinding());
    }

    protected Stream<MediaType> getMediaTypeStream(Response r) {
        return r.getOutputFormats().stream()
                .filter(f -> f.contains("/"))
                .filter(f -> {
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

    /**
     * Allows response to set headers like in the OGC Dispatcher, controls content disposition override based on query
     * parameters too
     */
    protected void setHeaders(Object result, Operation operation, Response response, HttpOutputMessage message) {
        // get the basics using the new api
        String disposition = response.getPreferredDisposition(result, operation);
        String filename = response.getAttachmentFileName(result, operation);

        // get user overrides, if any
        Request request = Dispatcher.REQUEST.get();
        if (request != null && request.getRawKvp() != null) {
            Map rawKvp = request.getRawKvp();
            // check if the filename and content disposition were provided
            if (rawKvp.get("FILENAME") != null) {
                filename = (String) rawKvp.get("FILENAME");
            }
            if (rawKvp.get("CONTENT-DISPOSITION") != null) {
                disposition = (String) rawKvp.get("CONTENT-DISPOSITION");
            }
        }

        // make sure the disposition obtained so far is valid
        // check and prevent invalid header injection
        if (disposition != null
                && !Response.DISPOSITION_ATTACH.equals(disposition)
                && !Response.DISPOSITION_INLINE.equals(disposition)) {
            disposition = null;
        }

        // set any extra headers, other than the mime-type
        String[][] headers = response.getHeaders(result, operation);
        boolean contentDispositionProvided = false;
        if (headers != null) {
            for (String[] header : headers) {
                if (header[0].equalsIgnoreCase("Content-Disposition")) {
                    contentDispositionProvided = true;
                    if (disposition == null) {
                        message.getHeaders().set(header[0], header[1]);
                    }
                } else {
                    message.getHeaders().set(header[0], header[1]);
                }
            }
        }

        // default disposition value and set if not forced by the user and not set
        // directly by the response
        if (!contentDispositionProvided) {
            if (disposition == null) {
                disposition = Response.DISPOSITION_INLINE;
            }

            // override any existing header
            String disp = disposition + "; filename=" + filename;
            message.getHeaders().set("Content-Disposition", disp);
        }
    }
}
