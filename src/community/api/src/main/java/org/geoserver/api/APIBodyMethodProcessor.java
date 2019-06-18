/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *  
 */

/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.api;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.print.attribute.standard.RequestingUserName;
import javax.servlet.http.HttpServletRequest;

/**
 * Customized {@link RequestResponseBodyMethodProcessor} that uses its own content negotiation
 * manager and can handle HTML annotated responses
 */
public class APIBodyMethodProcessor extends RequestResponseBodyMethodProcessor {

    private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

    private final ContentNegotiationManager contentNegotiationManager;
    protected final GeoServerResourceLoader loader;
    protected final GeoServer geoServer;

    public APIBodyMethodProcessor(
            List<HttpMessageConverter<?>> converters,
            GeoServerResourceLoader loader,
            GeoServer geoServer) {
        this(converters, new APIContentNegotiationManager(), loader, geoServer);
    }

    public APIBodyMethodProcessor(
            List<HttpMessageConverter<?>> converters,
            ContentNegotiationManager contentNegotiationManager,
            GeoServerResourceLoader loader,
            GeoServer geoServer) {
        super(
                converters,
                new APIContentNegotiationManager(), // this is the customized bit
                Collections.singletonList(new JsonViewResponseBodyAdvice()));
        this.contentNegotiationManager = contentNegotiationManager;
        this.loader = loader;
        this.geoServer = geoServer;
    }

    protected <T> void writeWithMessageConverters(
            @Nullable T value,
            MethodParameter returnType,
            ServletServerHttpRequest inputMessage,
            ServletServerHttpResponse outputMessage)
            throws IOException, HttpMediaTypeNotAcceptableException,
                    HttpMessageNotWritableException {
        HTMLResponseBody htmlResponseBody = returnType.getMethodAnnotation(HTMLResponseBody.class);
        MediaType mediaType = getMediaTypeToUse(value, returnType, inputMessage, outputMessage);
        if (htmlResponseBody != null
                && MediaType.TEXT_HTML.isCompatibleWith(mediaType)) {
            // direct HTML encoding based on annotations
            SimpleHTTPMessageConverter converter =
                    new SimpleHTTPMessageConverter(
                            value.getClass(),
                            returnType.getContainingClass(),
                            loader,
                            geoServer,
                            htmlResponseBody.templateName());
            converter.write(value, MediaType.TEXT_HTML, outputMessage);
        } else {
            super.writeWithMessageConverters(value, returnType, inputMessage, outputMessage);
        }
    }

    private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request)
            throws HttpMediaTypeNotAcceptableException {

        return contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
    }

    protected <T> MediaType getMediaTypeToUse(
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
            targetType =
                    GenericTypeResolver.resolveType(
                            getGenericType(returnType), returnType.getContainingClass());
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
            List<MediaType> producibleTypes =
                    getProducibleMediaTypes(request, valueType, targetType);
            // we want to check if HTML is the first producible without using converters, adding it
            // to the mix
            producibleTypes.add(MediaType.TEXT_HTML);

            if (body != null && producibleTypes.isEmpty()) {
                throw new HttpMessageNotWritableException(
                        "No converter found for return value of type: " + valueType);
            }
            List<MediaType> mediaTypesToUse = new ArrayList<>();
            for (MediaType requestedType : acceptableTypes) {
                for (MediaType producibleType : producibleTypes) {
                    if (requestedType.isCompatibleWith(producibleType)) {
                        mediaTypesToUse.add(
                                getMostSpecificMediaType(requestedType, producibleType));
                    }
                }
            }
            if (mediaTypesToUse.isEmpty()) {
                if (body != null) {
                    throw new HttpMediaTypeNotAcceptableException(producibleTypes);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "No match for " + acceptableTypes + ", supported: " + producibleTypes);
                }
                return null;
            }

            MediaType.sortBySpecificityAndQuality(mediaTypesToUse);

            for (MediaType mediaType : mediaTypesToUse) {
                if (mediaType.isConcrete()) {
                    selectedMediaType = mediaType;
                    break;
                } else if (mediaType.equals(MediaType.ALL)
                        || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
                    selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Using '"
                                + selectedMediaType
                                + "', given "
                                + acceptableTypes
                                + " and supported "
                                + producibleTypes);
            }
        }
        return selectedMediaType;
    }

    private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
        MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
        return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceTypeToUse) <= 0
                ? acceptType
                : produceTypeToUse);
    }

    /**
     * Return the generic type of the {@code returnType} (or of the nested type if it is an {@link
     * HttpEntity}).
     */
    private Type getGenericType(MethodParameter returnType) {
        if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return ResolvableType.forType(returnType.getGenericParameterType())
                    .getGeneric()
                    .getType();
        } else {
            return returnType.getGenericParameterType();
        }
    }
}
