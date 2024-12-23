/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.ExtensionPriority;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;

/**
 * A converter used when the {@link HTMLResponseBody} is found, to simply apply a template to the object returned by the
 * controller method
 *
 * @param <T>
 */
@Component
public class AnnotatedHTMLMessageConverter<T> extends AbstractHTMLMessageConverter<T> implements ExtensionPriority {

    private static final String ANNOTATION = "annotation";
    private static final String BASE_CLASS = "baseClass";

    private static final String SERVICE_INFO_CLASS = "serviceInfoClass";

    /** Builds the message converter */
    public AnnotatedHTMLMessageConverter(FreemarkerTemplateSupport support, GeoServer geoServer) {
        super(support, geoServer);
    }

    /**
     * Looks up an eventual {@link HTMLResponseBody} annotation from the current handler method, and stores them in
     * request attributes so that they can be re-used later by this class. Normally invoked by the {@link APIDispatcher}
     * during request processing.
     *
     * @param handler
     */
    public static void processAnnotation(HandlerMethod handler) {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        HTMLResponseBody htmlResponseBody = handler.getMethod().getAnnotation(HTMLResponseBody.class);
        if (attributes != null && htmlResponseBody != null) {
            attributes.setAttribute(ANNOTATION, htmlResponseBody, SCOPE_REQUEST);

            Class<?> baseClass = htmlResponseBody.baseClass();
            if (baseClass == Object.class) {
                baseClass = handler.getBean().getClass();
            }
            attributes.setAttribute(BASE_CLASS, baseClass, SCOPE_REQUEST);

            APIService apiService =
                    APIDispatcher.getApiServiceAnnotation(handler.getBean().getClass());
            if (apiService != null) {
                attributes.setAttribute(SERVICE_INFO_CLASS, apiService.serviceClass(), SCOPE_REQUEST);
            }
        }
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // decision is actually made by canWriten
        return true;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return getOptionalRequestAttribute(ANNOTATION, HTMLResponseBody.class).isPresent()
                && super.canWrite(clazz, mediaType);
    }

    private static <T> T getRequestAttribute(String name, Class<T> type) {
        return getOptionalRequestAttribute(name, type).orElseThrow();
    }

    private static <T> Optional<T> getOptionalRequestAttribute(String name, Class<T> type) {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(a -> a.getAttribute(name, SCOPE_REQUEST))
                .filter(type::isInstance)
                .map(type::cast);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<? extends ServiceInfo> getServiceConfigurationClass() {
        return getRequestAttribute(SERVICE_INFO_CLASS, Class.class);
    }

    @Override
    protected void writeInternal(T value, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            HashMap<String, Object> model = setupModel(value);
            Charset defaultCharset = getDefaultCharset();
            if (outputMessage != null && outputMessage.getBody() != null && defaultCharset != null) {
                templateSupport.processTemplate(
                        null,
                        getRequestAttribute(ANNOTATION, HTMLResponseBody.class).templateName(),
                        getRequestAttribute(BASE_CLASS, Class.class),
                        model,
                        new OutputStreamWriter(outputMessage.getBody(), defaultCharset),
                        defaultCharset);
            } else {
                LOGGER.warning("Either the default character set, output message or body was null, so the "
                        + "template could not be processed.");
            }
        } finally {
            // the model can be working over feature collections, make sure they are cleaned up
            purgeIterators();
        }
    }

    @Override
    public int getPriority() {
        // in case the client does not provide an indication of the preferred format, give priority
        // to machine oriented responses (classic case of "curl" from command line without headers)
        // Annoying bit: LOWEST is also the default, does not make this response any less preferred
        // than the ones not implementing ExtensionPriority, need to go further (higher value, lower
        // priority)
        return ExtensionPriority.LOWEST * 2;
    }
}
