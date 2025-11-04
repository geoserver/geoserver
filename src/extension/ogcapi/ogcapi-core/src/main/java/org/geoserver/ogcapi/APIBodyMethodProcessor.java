/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * Customized {@link RequestResponseBodyMethodProcessor} that allows full support of {@link DispatcherCallback}, and has
 * a customized content negotiation strategy that allows to set up the default media type by using the defaultMediaType
 * annotation on the controller and prefer JSON producing converters in case no default has been set up.
 */
public class APIBodyMethodProcessor extends RequestResponseBodyMethodProcessor {

    private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

    private final ContentNegotiationManager contentNegotiationManager;
    protected List<DispatcherCallback> callbacks;

    public APIBodyMethodProcessor(
            List<HttpMessageConverter<?>> converters,
            ContentNegotiationManager contentNegotiationManager,
            List<DispatcherCallback> callbacks) {
        super(converters, contentNegotiationManager, Collections.singletonList(new JsonViewResponseBodyAdvice()));
        this.contentNegotiationManager = contentNegotiationManager;
        this.callbacks = callbacks;
        // allow converter overrides, the Spring internal machinery picks the first one matching
        // but does not seem to be respecting the @Order and Ordered interfaces
        Collections.sort(this.messageConverters, AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    protected <T> void writeWithMessageConverters(
            @Nullable T value,
            MethodParameter returnType,
            ServletServerHttpRequest inputMessage,
            ServletServerHttpResponse outputMessage)
            throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
        // handle case of null value returned by controller methods
        HttpServletResponse servletResponse = outputMessage.getServletResponse();
        if (value == null) {
            servletResponse.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        HttpMessageConverter<T> converter = getMessageConverter(value, returnType, inputMessage, outputMessage);

        // DispatcherCallback bridging
        MediaType mediaType = getMediaTypeToUse(value, returnType, inputMessage, outputMessage);
        Response response = new Response(value.getClass()) {

            @Override
            public String getMimeType(Object value, Operation operation) throws ServiceException {
                return mediaType.toString();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void write(Object value, OutputStream output, Operation operation)
                    throws IOException, ServiceException {
                converter.write((T) value, mediaType, outputMessage);
            }
        };

        Request dr = Dispatcher.REQUEST.get();
        response = fireResponseDispatchedCallback(dr, dr.getOperation(), value, response);

        // write using the response provided by the callbacks
        String contentType = response.getMimeType(value, dr.getOperation());
        outputMessage.getHeaders().setContentType(MediaType.parseMediaType(contentType));
        servletResponse.setContentType(contentType);
        String responseCharset = response.getCharset(dr.getOperation());
        if (responseCharset != null) {
            servletResponse.setCharacterEncoding(response.getCharset(dr.getOperation()));
        }
        response.write(value, servletResponse.getOutputStream(), dr.getOperation());
    }

    private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request)
            throws HttpMediaTypeNotAcceptableException {

        return contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
    }

    private <T> MediaType getMediaTypeToUse(
            @Nullable T value,
            MethodParameter returnType,
            ServletServerHttpRequest inputMessage,
            ServletServerHttpResponse outputMessage)
            throws HttpMediaTypeNotAcceptableException {
        Object body;
        Class<?> valueType;
        Type targetType;

        if (value instanceof CharSequence) {
            body = value.toString();
            valueType = String.class;
            targetType = String.class;
        } else {
            body = value;
            valueType = getReturnValueType(body, returnType);
            targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
        }

        if (isResourceType(value, returnType)) {
            return null;
        }

        MediaType selectedMediaType = null;
        MediaType contentType = outputMessage.getHeaders().getContentType();
        if (contentType != null && contentType.isConcrete()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found 'Content-Type:" + contentType + "' in response");
            }
            selectedMediaType = contentType;
        } else {
            HttpServletRequest request = inputMessage.getServletRequest();
            List<MediaType> acceptableTypes = getAcceptableMediaTypes(request);
            // if we got no indication, see if the method has a default content type, and
            // if not, default to JSON as per OGC API expectations
            List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType, value);
            if (ContentNegotiationManager.MEDIA_TYPE_ALL_LIST.equals(acceptableTypes)) {
                MediaType defaultMediaType = Optional.ofNullable(
                                returnType.getMethodAnnotation(DefaultContentType.class))
                        .map(t -> MediaType.parseMediaType(t.value()))
                        .orElse(null);
                if (defaultMediaType != null) {
                    acceptableTypes = Collections.singletonList(defaultMediaType);
                } else if (producibleTypes.contains(MediaType.APPLICATION_JSON)) {
                    acceptableTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
                } // otherwise let it be free
            }
            // we want to check if HTML is the first producible without using converters, adding it
            // to the mix
            HTMLResponseBody htmlResponseBody = returnType.getMethodAnnotation(HTMLResponseBody.class);
            if (htmlResponseBody != null) {
                producibleTypes.add(MediaType.TEXT_HTML);
            }

            if (body != null && producibleTypes.isEmpty()) {
                throw new HttpMessageNotWritableException("No converter found for return value of type: " + valueType);
            }
            List<MediaType> mediaTypesToUse = new ArrayList<>();
            for (MediaType requestedType : acceptableTypes) {
                for (MediaType producibleType : producibleTypes) {
                    if (requestedType.isCompatibleWith(producibleType)) {
                        mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
                    }
                }
            }
            if (mediaTypesToUse.isEmpty()) {
                if (body != null) {
                    throw new HttpMediaTypeNotAcceptableException(producibleTypes);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("No match for " + acceptableTypes + ", supported: " + producibleTypes);
                }
                return null;
            }

            // if there is an exact match, go for it
            for (MediaType mediaType : mediaTypesToUse) {
                for (MediaType acceptableType : acceptableTypes) {
                    if (mediaType.equals(acceptableType)) {
                        selectedMediaType = mediaType;
                        break;
                    }
                }
                if (selectedMediaType != null) break;
            }

            // otherwise find something compatible
            if (selectedMediaType == null) {
                MimeTypeUtils.sortBySpecificity(mediaTypesToUse);

                for (MediaType mediaType : mediaTypesToUse) {
                    if (mediaType.isConcrete()) {
                        selectedMediaType = mediaType;
                        break;
                    } else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
                        selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
                        break;
                    }
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Using '"
                        + selectedMediaType
                        + "', given "
                        + acceptableTypes
                        + " and supported "
                        + producibleTypes);
            }
        }
        return selectedMediaType;
    }

    @Override
    protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> valueClass) {
        return getProducibleMediaTypes(request, valueClass, null);
    }

    /**
     * Returns the media types that can be produced. The resulting media types are:
     *
     * <ul>
     *   <li>The producible media types specified in the request mappings, or
     *   <li>Media types of configured converters that can write the specific return value, or
     *   <li>{@link MediaType#ALL}
     *   <li>Specific GeoServer converters that can declare a mediatype only when given the actual response value
     * </ul>
     */
    @SuppressWarnings("unchecked")
    protected List<MediaType> getProducibleMediaTypes(
            HttpServletRequest request, Class<?> valueClass, @Nullable Type targetType, Object value) {

        // Not accessing request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE),
        // the producible media types are being set by some other Spring machinery that is not
        // accounting for the capabilities of this class
        List<MediaType> result = new ArrayList<>();
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            if (converter instanceof ResponseMessageConverter messageConverter1
                    && converter.canWrite(valueClass, null)) {
                result.addAll(messageConverter1.getSupportedMediaTypes(valueClass, value));
            } else if (converter instanceof GenericHttpMessageConverter<?> messageConverter && targetType != null) {
                if (messageConverter.canWrite(targetType, valueClass, null)) {
                    result.addAll(converter.getSupportedMediaTypes());
                }
            } else if (converter.canWrite(valueClass, null)) {
                result.addAll(converter.getSupportedMediaTypes());
            }
        }
        return (result.isEmpty() ? Collections.singletonList(MediaType.ALL) : result);
    }

    private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
        MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
        return (acceptType.isLessSpecific(produceTypeToUse) ? produceTypeToUse : acceptType);
    }

    /** Return the generic type of the {@code returnType} (or of the nested type if it is an {@link HttpEntity}). */
    private Type getGenericType(MethodParameter returnType) {
        if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return ResolvableType.forType(returnType.getGenericParameterType())
                    .getGeneric()
                    .getType();
        } else {
            return returnType.getGenericParameterType();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> HttpMessageConverter<T> getMessageConverter(
            @Nullable T value,
            MethodParameter returnType,
            ServletServerHttpRequest inputMessage,
            ServletServerHttpResponse outputMessage)
            throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
        Object body;
        Class<?> valueType;
        Type targetType;
        if (value instanceof CharSequence) {
            body = value.toString();
            valueType = String.class;
            targetType = String.class;
        } else {
            body = value;
            valueType = this.getReturnValueType(value, returnType);
            targetType =
                    GenericTypeResolver.resolveType(this.getGenericType(returnType), returnType.getContainingClass());
        }

        MediaType selectedMediaType = getMediaTypeToUse(value, returnType, inputMessage, outputMessage);

        if (selectedMediaType != null) {
            selectedMediaType = selectedMediaType.removeQualityValue();
            for (HttpMessageConverter<?> converter : this.messageConverters) {
                if (converter instanceof ResponseMessageConverter messageConverter
                        && messageConverter.canWrite(value, selectedMediaType)) {
                    return (HttpMessageConverter<T>) converter;
                }
                if (converter instanceof GenericHttpMessageConverter messageConverter
                        && messageConverter.canWrite(targetType, valueType, selectedMediaType)) {
                    return (HttpMessageConverter<T>) converter;
                } else if (converter.canWrite(valueType, selectedMediaType)) {
                    return (HttpMessageConverter<T>) converter;
                }
            }
        }

        if (body != null) {
            throw new HttpMediaTypeNotAcceptableException(this.getSupportedMediaTypes(Object.class));
        }
        return null;
    }

    Response fireResponseDispatchedCallback(Request req, Operation op, Object result, Response response) {
        for (DispatcherCallback cb : callbacks) {
            Response r = cb.responseDispatched(req, op, result, response);
            response = r != null ? r : response;
        }
        return response;
    }
}
