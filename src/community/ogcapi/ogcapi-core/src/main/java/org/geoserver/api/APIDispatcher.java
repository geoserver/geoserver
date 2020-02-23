/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.ClientStreamAbortedException;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;
import org.xml.sax.SAXException;

/**
 * A dispatcher for OGC API requests (and suitable for implementation restful services in general.
 * Supports {@link DispatcherCallback} and {@link Dispatcher#REQUEST} to properly play with the
 * existing GeoServer ecosystem
 */
public class APIDispatcher extends AbstractController {

    static final String RESPONSE_OBJECT = "ResponseObject";

    public static final String ROOT_PATH = "ogc";

    static final Charset UTF8 = Charset.forName("UTF-8");

    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.api");

    // SHARE
    /** list of callbacks */
    protected List<DispatcherCallback> callbacks = Collections.EMPTY_LIST;

    protected RequestMappingHandlerMapping mappingHandler;

    protected RequestMappingHandlerAdapter handlerAdapter;
    protected HandlerMethodReturnValueHandlerComposite returnValueHandlers;
    protected ContentNegotiationManager contentNegotiationManager =
            new APIContentNegotiationManager();
    private List<HttpMessageConverter<?>> messageConverters;
    private List<APIExceptionHandler> exceptionHandlers;

    public APIDispatcher() {
        // allow delete and put
        super(false);
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) {
        // load life cycle callbacks
        callbacks = GeoServerExtensions.extensions(DispatcherCallback.class, context);
        exceptionHandlers = GeoServerExtensions.extensions(APIExceptionHandler.class, context);

        this.mappingHandler =
                new RequestMappingHandlerMapping() {
                    @Override
                    protected boolean isHandler(Class<?> beanType) {
                        return hasAnnotation(beanType, APIService.class);
                    }
                };
        this.mappingHandler.setApplicationContext(context);
        this.mappingHandler.afterPropertiesSet();
        // do we really want this? The REST API uses it though
        this.mappingHandler.getUrlPathHelper().setAlwaysUseFullPath(true);

        // create the one handler adapter we need similar to how DispatcherServlet does it
        // but with a special implementation that supports callbacks for the operation
        APIConfigurationSupport configurationSupport =
                context.getAutowireCapableBeanFactory().createBean(APIConfigurationSupport.class);
        configurationSupport.setCallbacks(callbacks);
        handlerAdapter = configurationSupport.requestMappingHandlerAdapter();
        handlerAdapter.setApplicationContext(context);
        handlerAdapter.afterPropertiesSet();
        // force json as the first choice
        handlerAdapter.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter());
        handlerAdapter.getMessageConverters().add(0, new MappingJackson2YAMLMessageConverter());
        // add all registered converters before the Spring ones too
        List<HttpMessageConverter> extensionConverters =
                GeoServerExtensions.extensions(HttpMessageConverter.class);
        addToListBackwards(extensionConverters, handlerAdapter.getMessageConverters());
        this.messageConverters = handlerAdapter.getMessageConverters();

        // add custom argument resolvers
        List<HandlerMethodArgumentResolver> pluginResolvers =
                GeoServerExtensions.extensions(HandlerMethodArgumentResolver.class);
        List<HandlerMethodArgumentResolver> adapterResolvers = new ArrayList<>();
        List<HandlerMethodArgumentResolver> existingResolvers =
                handlerAdapter.getArgumentResolvers();
        if (existingResolvers != null) {
            adapterResolvers.addAll(existingResolvers);
        }
        addToListBackwards(pluginResolvers, adapterResolvers);
        handlerAdapter.setArgumentResolvers(adapterResolvers);

        // default treatment of "f" parameter and headers, defaulting to JSON if nothing else has
        // been provided
        List<HandlerMethodReturnValueHandler> returnValueHandlers =
                Optional.ofNullable(handlerAdapter.getReturnValueHandlers())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(
                                f -> {
                                    if (f instanceof RequestResponseBodyMethodProcessor) {
                                        // replace with custom version that can do HTML output based
                                        // on method annotations and does generic OGC API content
                                        // negotiation
                                        return new APIBodyMethodProcessor(
                                                handlerAdapter.getMessageConverters(),
                                                contentNegotiationManager,
                                                GeoServerExtensions.bean(
                                                        GeoServerResourceLoader.class),
                                                GeoServerExtensions.bean(GeoServer.class),
                                                callbacks);
                                    } else {
                                        return f;
                                    }
                                })
                        .collect(Collectors.toList());

        // split handling of response  in two to respect the Dispatcher Operation/Response
        // architecture
        this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
        this.returnValueHandlers.addHandlers(returnValueHandlers);
        handlerAdapter.setReturnValueHandlers(
                Arrays.asList(
                        new HandlerMethodReturnValueHandler() {
                            @Override
                            public boolean supportsReturnType(MethodParameter returnType) {
                                return true;
                            }

                            @Override
                            public void handleReturnValue(
                                    Object returnValue,
                                    MethodParameter returnType,
                                    ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest)
                                    throws Exception {
                                mavContainer.getModel().put(RESPONSE_OBJECT, returnValue);
                            }
                        }));
    }

    private void addToListBackwards(List source, List target) {
        // add them in reverse order to the head, so that they will have the same order as extension
        // priority commands
        ListIterator arIterator = source.listIterator(source.size());
        while (arIterator.hasPrevious()) {
            target.add(0, arIterator.previous());
        }
    }

    // SHARE
    protected void preprocessRequest(HttpServletRequest request) throws Exception {
        // set the charset
        Charset charSet = null;

        // TODO: make this server settable
        charSet = UTF8;
        if (request.getCharacterEncoding() != null)
            try {
                charSet = Charset.forName(request.getCharacterEncoding());
            } catch (Exception e) {
                // ok, we tried...
            }

        request.setCharacterEncoding(charSet.name());
    }

    @Override
    protected ModelAndView handleRequestInternal(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {

        preprocessRequest(httpRequest);

        // create a new request instance compatible with DispatcherCallback and other mechanisms
        // in GeoServer
        Request dr = new Request();
        Dispatcher.REQUEST.set(dr);
        dr.setHttpRequest(httpRequest);
        dr.setHttpResponse(httpResponse);
        dr.setGet("GET".equalsIgnoreCase(httpRequest.getMethod()));

        // setup a API specific contex object providing info about the current request
        APIRequestInfo requestInfo = new APIRequestInfo(httpRequest, httpResponse, this);
        APIRequestInfo.set(requestInfo);

        // perform request execution
        try {
            // initialize the request and allow callbacks to override it
            // store it in the thread local used by the
            dr = init(dr);
            requestInfo.setRequestedMediaTypes(
                    contentNegotiationManager.resolveMediaTypes(
                            new ServletWebRequest(dr.getHttpRequest())));

            // lookup the handler adapter (same as service and operation)
            HandlerMethod handler = getHandlerMethod(httpRequest, dr);
            dispatchService(dr, handler);

            // this is actually "execute", internaly
            ModelAndView mav =
                    handlerAdapter.handle(dr.getHttpRequest(), dr.getHttpResponse(), handler);

            ModelAndViewContainer mavContainer = new ModelAndViewContainer();
            mavContainer.addAllAttributes(
                    RequestContextUtils.getInputFlashMap(dr.getHttpRequest()));

            // and this is response handling
            Object returnValue = mav != null ? mav.getModel().get(RESPONSE_OBJECT) : null;
            returnValue = fireOperationExecutedCallback(dr, dr.getOperation(), returnValue);

            returnValueHandlers.handleReturnValue(
                    returnValue,
                    new ReturnValueMethodParameter(handler.getMethod(), returnValue),
                    mavContainer,
                    new DispatcherServletWebRequest(dr.getHttpRequest(), dr.getHttpResponse()));
            // TODO: fire the methods for response written
        } catch (Throwable t) {
            // make Spring security exceptions flow so that exception transformer filter can handle
            // them
            if (isSecurityException(t)) throw (Exception) t;
            exception(t, requestInfo);
        } finally {
            fireFinishedCallback(dr);
            Dispatcher.REQUEST.remove();
        }

        return null;
    }

    private void dispatchService(Request dr, HandlerMethod handler) {
        // get the annotations and set service, version and request
        APIService annotation = getApiServiceAnnotation(handler.getBeanType());
        dr.setService(annotation.service());
        dr.setVersion(annotation.version());
        dr.setRequest(getOperationName(handler.getMethod()));

        // comply with DispatcherCallback and fire a service dispatched callback
        Service service =
                new Service(
                        annotation.service(),
                        handler.getBean(),
                        new Version(annotation.service()),
                        Collections.emptyList());
        dr.setServiceDescriptor(service);
        service = fireServiceDispatchedCallback(dr, service);
        // replace in case callbacks have replaced it
        dr.setServiceDescriptor(service);
    }

    /**
     * Returns the {@link APIService} annotation from the class, or if not found, from a superclass
     *
     * @param clazz The class to look {@link APIService} on
     * @return The first {@link APIService} found walking up the inheritance hierarchy, or null if
     *     not found
     */
    static APIService getApiServiceAnnotation(Class clazz) {
        APIService annotation = null;
        while (annotation == null && clazz != null) {
            annotation = (APIService) clazz.getAnnotation(APIService.class);
            if (annotation == null) {
                clazz = clazz.getSuperclass();
            }
        }

        return annotation;
    }

    private HandlerMethod getHandlerMethod(HttpServletRequest httpRequest, Request dr)
            throws Exception {
        HandlerExecutionChain chain = mappingHandler.getHandler(dr.getHttpRequest());
        if (chain == null) {
            String msg =
                    "No mapping for " + httpRequest.getMethod() + " " + getRequestUri(httpRequest);
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(msg);
            }
            throw new APIException(null, msg, HttpStatus.NOT_FOUND);
        }
        Object handler = chain.getHandler();
        if (!handlerAdapter.supports(handler)) {
            String msg =
                    "Mapping for "
                            + httpRequest.getMethod()
                            + " "
                            + getRequestUri(httpRequest)
                            + " found but it's not supported by the HandlerAdapter. Check for mis-setup of service beans";
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(msg);
            }
            throw new APIException(null, msg, HttpStatus.NOT_FOUND);
        }
        return (HandlerMethod) handler;
    }

    private void exception(Throwable t, APIRequestInfo request) throws IOException {
        HttpServletResponse response = request.getResponse();

        // eliminate ClientStreamAbortedException
        Throwable current = t;
        while (current != null
                && !(current instanceof ClientStreamAbortedException)
                && !isSecurityException(current)
                && !(current instanceof HttpErrorCodeException)) {
            if (current instanceof SAXException) current = ((SAXException) current).getException();
            else current = current.getCause();
        }
        if (current instanceof ClientStreamAbortedException) {
            LOGGER.log(Level.FINER, "Client has closed stream", t);
            return;
        }

        // make sure we don't eat security exceptions, they have their own handling
        if (isSecurityException(current)) {
            throw (RuntimeException) current;
        }

        LOGGER.log(Level.SEVERE, "Failed to dispatch API request", t);

        // is it meant to be a simple and straight answer?
        if (current instanceof HttpErrorCodeException) {
            HttpErrorCodeException hec = (HttpErrorCodeException) current;
            response.setContentType(
                    hec.getContentType() != null ? hec.getContentType() : "text/plain");
            if (hec.getErrorCode() >= 400) {
                response.sendError(hec.getErrorCode(), hec.getMessage());
            } else {
                response.setStatus(hec.getErrorCode());
                response.getOutputStream().print(hec.getMessage());
            }
        } else {
            APIExceptionHandler handler = getExceptionHandler(t, request);
            if (handler == null) {
                response.sendError(500, t.getMessage());
            } else {
                handler.handle(t, response);
            }
        }
    }

    private APIExceptionHandler getExceptionHandler(Throwable t, APIRequestInfo request) {
        return exceptionHandlers
                .stream()
                .filter(h -> h.canHandle(t, request))
                .findFirst()
                .orElse(null);
    }

    Request init(Request request) throws ServiceException, IOException {
        // parse the request path into two components. (1) the 'path' which
        // is the string after the last '/', and the 'context' which is the
        // string before the last '/'
        String ctxPath = request.getHttpRequest().getContextPath();
        String reqPath = request.getHttpRequest().getRequestURI();
        reqPath = reqPath.substring(ctxPath.length());

        // strip off leading and trailing slashes
        if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1, reqPath.length());
        }

        if (reqPath.endsWith("/")) {
            reqPath = reqPath.substring(0, reqPath.length() - 1);
        }

        String context = reqPath;
        String path = null;
        int index = context.lastIndexOf('/');
        if (index != -1) {
            path = context.substring(index + 1);
            context = context.substring(0, index);
        } else {
            path = reqPath;
            context = null;
        }
        request.setContext(context);
        request.setPath(path);

        // TODO: MVC will handle these from the request, do we need to wrap the HTTP request?
        // most likely...

        // unparsed kvp set
        Map kvp = request.getHttpRequest().getParameterMap();

        if (kvp == null || kvp.isEmpty()) {
            request.setKvp(new HashMap());
            request.setRawKvp(new HashMap());
        } else {
            // track parsed kvp and unparsd
            Map parsedKvp = KvpUtils.normalize(kvp);
            Map rawKvp = new KvpMap(parsedKvp);

            request.setKvp(parsedKvp);
            request.setRawKvp(rawKvp);
        }

        return fireInitCallback(request);
    }

    // SHARE
    Request fireInitCallback(Request req) {
        for (DispatcherCallback cb : callbacks) {
            Request r = cb.init(req);
            req = r != null ? r : req;
        }
        return req;
    }

    // SHARE (or move to a callback handler/list class of sort?)
    void fireFinishedCallback(Request req) {
        for (DispatcherCallback cb : callbacks) {
            try {
                cb.finished(req);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Error firing finished callback for " + cb.getClass(), t);
            }
        }
    }

    /**
     * Examines a {@link Throwable} object and returns true if it represents a security exception.
     *
     * @param t Throwable
     * @return true if t is a security exception
     */
    // SHARE
    protected static boolean isSecurityException(Throwable t) {
        return t != null
                && t.getClass().getPackage().getName().startsWith("org.springframework.security");
    }

    // SHARE
    Service fireServiceDispatchedCallback(Request req, Service service) {
        for (DispatcherCallback cb : callbacks) {
            Service s = cb.serviceDispatched(req, service);
            service = s != null ? s : service;
        }
        return service;
    }

    // SHARE
    Object fireOperationExecutedCallback(Request req, Operation op, Object result) {
        for (DispatcherCallback cb : callbacks) {
            Object r = cb.operationExecuted(req, op, result);
            result = r != null ? r : result;
        }
        return result;
    }

    /**
     * This comes from {@link org.springframework.web.servlet.DispatcherServlet}, it's private and
     * thus not reusable
     */
    private static String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return uri;
    }

    /** Returns a read only list of available {@link HttpMessageConverter} */
    public List<HttpMessageConverter<?>> getConverters() {
        return Collections.unmodifiableList(messageConverters);
    }

    /** Returns a {@link List} of media types that can be produced for a given response object */
    public List<MediaType> getProducibleMediaTypes(Class<?> responseType, boolean addHTML) {
        List<MediaType> result = new ArrayList<>();
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            if (converter instanceof GenericHttpMessageConverter) {
                if (((GenericHttpMessageConverter<?>) converter)
                        .canWrite(responseType, responseType, null)) {
                    result.addAll(converter.getSupportedMediaTypes());
                }
            } else if (converter.canWrite(responseType, null)) {
                result.addAll(converter.getSupportedMediaTypes());
            }
        }
        if (addHTML) {
            result.add(MediaType.TEXT_HTML);
        }

        return result.stream()
                .filter(mt -> mt.isConcrete())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns the name of a given handler method, using the name found in {@link RequestMapping} or
     * one of its method specific sub-annotations. If not found, falls back on the method name.
     */
    public static String getOperationName(Method m) {
        return Arrays.stream(m.getAnnotations())
                .filter(a -> isRequestMapping(a))
                .map(
                        a -> {
                            try {
                                return (String) a.getClass().getMethod("name").invoke(a);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Failed to get name from request mapping annotation, unexpected",
                                        e);
                            }
                            return "";
                        })
                .filter(name -> name != null && !name.isEmpty())
                .findFirst()
                .orElse(m.getName()); // fallback on the method name if needs be
    }

    /**
     * Returns true if the method in question is a service method, annotated with a {@link
     * RequestMapping} or one of its method specific sub-annotations
     */
    public static boolean hasRequestMapping(Method m) {
        return Arrays.stream(m.getAnnotations()).anyMatch(a -> isRequestMapping(a));
    }

    private static boolean isRequestMapping(Annotation a) {
        return a instanceof RequestMapping
                || a.annotationType().getAnnotation(RequestMapping.class) != null;
    }
}
