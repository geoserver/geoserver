/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import freemarker.core.ParseException;
import freemarker.template.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geoserver.template.TemplateUtils;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/**
 * Base class for all rest controllers
 *
 * <p>Extending classes should be annotated with {@link
 * org.springframework.web.bind.annotation.RestController} so that they are automatically
 * instantiated as a Controller bean.
 *
 * <p>Custom configuration can be added to XStreamPersister by overriding {@link
 * #configurePersister(XStreamPersister, XStreamMessageConverter)} Custom configuration can be added
 * to Freemarker by calling {@link #configureFreemarker(FreemarkerHTMLMessageConverter, Template)}
 *
 * <p>Any extending classes which override {@link #configurePersister(XStreamPersister,
 * XStreamMessageConverter)}, and require this configuration for reading objects from incoming
 * requests must also be annotated with {@link
 * org.springframework.web.bind.annotation.ControllerAdvice} and override the {@link
 * #supports(MethodParameter, Type, Class)} method.
 *
 * <p>Any response objects that should be encoded using either {@link XStreamMessageConverter} or
 * {@link FreemarkerHTMLMessageConverter} should be wrapped in a {@link RestWrapper} by calling
 * {@link #wrapObject(Object, Class)}. Any response objects that should be encoded using {@link
 * org.geoserver.rest.converters.XStreamCatalogListConverter} should be wrapped by calling {@link
 * #wrapList(Collection, Class)}
 */
public abstract class RestBaseController implements RequestBodyAdvice {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.rest");

    /** Root path of the rest api */
    public static final String ROOT_PATH = "/rest";

    /** Default encoding for the freemarker {@link Configuration} */
    protected String encoding = "UTF-8";

    /** Name of the folder containing freemarker templates */
    protected String pathPrefix = "ftl-templates";

    /**
     * Constructs the freemarker {@link Configuration}
     *
     * @param clazz Class of the object being wrapped
     */
    protected <T> Configuration createConfiguration(Class<T> clazz) {
        Configuration cfg = TemplateUtils.getSafeConfiguration();
        cfg.setObjectWrapper(createObjectWrapper(clazz));
        cfg.setClassForTemplateLoading(getClass(), pathPrefix);
        if (encoding != null) {
            cfg.setDefaultEncoding(encoding);
        }
        return cfg;
    }

    /**
     * Constructs the freemarker {@link ObjectWrapper}
     *
     * @param clazz Class of the object being wrapped
     */
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz);
    }

    /**
     * Finds a freemarker {@link Template} based on the object and {@link Configuration}
     *
     * @param o Object being serialized
     * @param clazz Class of the object
     * @return Freemarker template
     */
    protected Template getTemplate(Object o, Class clazz) {
        Template template = null;
        Configuration configuration = createConfiguration(clazz);

        // first try finding a name directly
        String templateName = getTemplateName(o);
        if (templateName != null) {
            template = tryLoadTemplate(configuration, templateName);
            if (template == null) template = tryLoadTemplate(configuration, templateName + ".ftl");
        }
        final RequestInfo requestInfo = RequestInfo.get();

        // next look up by the resource being requested
        if (template == null && requestInfo != null) {
            // could not find a template bound to the class directly, search by the resource
            // being requested
            String pagePath = requestInfo.getPagePath();
            String r = pagePath.substring(pagePath.lastIndexOf('/') + 1);
            // trim trailing slash
            if (r.equals("")) {
                pagePath = pagePath.substring(0, pagePath.length() - 1);
                r = pagePath.substring(pagePath.lastIndexOf('/') + 1);
            }
            int i = r.lastIndexOf(".");
            if (i != -1) {
                r = r.substring(0, i);
            }

            template = tryLoadTemplate(configuration, r + ".ftl");
        }

        // finally try to find by class
        while (template == null && clazz != null) {

            template = tryLoadTemplate(configuration, clazz.getSimpleName() + ".ftl");
            if (template == null) {
                template =
                        tryLoadTemplate(
                                configuration, clazz.getSimpleName().toLowerCase() + ".ftl");
            }
            if (template == null) {
                for (Class<?> interfaze : clazz.getInterfaces()) {
                    template = tryLoadTemplate(configuration, interfaze.getSimpleName() + ".ftl");
                    if (template != null) break;
                }
            }

            // move up the class hierarchy to continue to look for a matching template
            if (clazz.getSuperclass() == Object.class) {
                break;
            }
            clazz = clazz.getSuperclass();
        }

        if (template != null) {
            templateName = template.getName();
        } else {
            // use a fallback
            templateName = "Object.ftl";
        }
        return tryLoadTemplate(configuration, templateName);
    }

    /**
     * Tries to load a template, will return null if it's not found. If the template exists but it
     * contains syntax errors an exception will be thrown instead
     *
     * @param configuration The template configuration.
     * @param templateName The name of the template to load.
     */
    protected Template tryLoadTemplate(Configuration configuration, String templateName) {
        try {
            return configuration.getTemplate(templateName);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException io) {
            LOGGER.log(Level.FINE, "Failed to lookup template " + templateName, io);
            return null;
        }
    }

    /**
     * Template method to get a custom template name
     *
     * @param object The object being serialized.
     */
    protected String getTemplateName(Object object) {
        return null;
    }

    /**
     * Wraps the passed collection in a {@link RestListWrapper}
     *
     * @param list The collection to wrap
     * @param clazz The advertised class to use for the collection contents
     */
    protected <T> RestWrapper<T> wrapList(Collection<T> list, Class<T> clazz) {
        return new RestListWrapper<>(list, clazz, this, getTemplate(list, clazz));
    }

    /**
     * Wraps the passed object in a {@link RestWrapperAdapter}
     *
     * @param object The object to wrap
     * @param clazz The advertised class to use for the collection contents
     */
    protected <T> RestWrapper<T> wrapObject(T object, Class<T> clazz) {
        return new RestWrapperAdapter<>(object, clazz, this, getTemplate(object, clazz));
    }

    /**
     * Wraps the passed object in a {@link RestWrapperAdapter}
     *
     * @param object The object to wrap
     * @param clazz The advertised class to use for the collection contents
     * @param errorMessage The error message to return if the object is null.
     * @param quietOnNotFound The value of the quietOnNotFound parameter
     */
    // TODO: Remove this once all references have been removed (should just use
    // ResourceNotFoundExceptions)
    protected <T> RestWrapper<T> wrapObject(
            T object, Class<T> clazz, String errorMessage, Boolean quietOnNotFound) {
        errorMessage = quietOnNotFound != null && quietOnNotFound ? "" : errorMessage;
        if (object == null) {
            throw new RestException(errorMessage, HttpStatus.NOT_FOUND);
        }
        return new RestWrapperAdapter<>(object, clazz, this, getTemplate(object, clazz));
    }

    @Override
    /**
     * Any subclass that implements {@link #configurePersister(XStreamPersister,
     * XStreamMessageConverter)} and require this configuration for reading objects from incoming
     * requests should override this method to return true when called from the appropriate
     * controller, and should also be annotated with {@link
     * org.springframework.web.bind.annotation.ControllerAdvice}
     */
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return false;
    }

    @Override
    public Object handleEmptyBody(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        if (!(inputMessage instanceof RestHttpInputWrapper)) {
            return new RestHttpInputWrapper(inputMessage, this);
        }
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /**
     * Default (empty) implementation of configurePersister. This will be called by the default
     * implementations of {@link RestWrapper#configurePersister(XStreamPersister,
     * XStreamMessageConverter)} and {@link
     * RestHttpInputWrapper#configurePersister(XStreamPersister, XStreamMessageConverter)}
     * constructed by {@link #wrapObject(Object, Class)}, {@link #wrapList(Collection, Class)}, and
     * {@link #beforeBodyRead(HttpInputMessage, MethodParameter, Type, Class)}
     *
     * <p>Subclasses should override this to implement custom functionality
     */
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {}

    /**
     * Default (empty) implementation of configurePersister. This will be called by the default
     * implementation of {@link RestWrapper#configurePersister(XStreamPersister,
     * XStreamMessageConverter)}, constructed by {@link #wrapObject(Object, Class)}, and {@link
     * #wrapList(Collection, Class)}
     *
     * <p>Subclasses should override this to implement custom functionality
     */
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {}

    /**
     * Returns the result of RequestContextHolder#getRequestAttributes() making sure the result is
     * not null, throwing an {@link NullPointerException} with an explanation otherwise
     */
    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    protected RequestAttributes getNonNullRequestAttributes() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new NullPointerException(
                    "Could not get request attributes in the current request");
        }
        return requestAttributes;
    }

    /** Returns a map with the URI template variables. */
    protected Map<String, String> getURITemplateVariables() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) return Collections.emptyMap();

        Map<String, String> result =
                (Map<String, String>)
                        attributes.getAttribute(
                                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                                RequestAttributes.SCOPE_REQUEST);
        if (result == null) return Collections.emptyMap();

        return result;
    }
}
