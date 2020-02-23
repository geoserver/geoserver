/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.apache.wicket.RuntimeConfigurationType.DEPLOYMENT;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.Application;
import org.apache.wicket.ConverterLocator;
import org.apache.wicket.DefaultExceptionMapper;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.protocol.http.CsrfPreventionRequestCycleListener;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IExceptionMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestHandlerDelegate;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.RequestCycleSettings.RenderStrategy;
import org.apache.wicket.util.IProvider;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.util.DataDirectoryConverterLocator;
import org.geoserver.web.util.GeoToolsConverterAdapter;
import org.geoserver.web.util.converters.StringBBoxConverter;
import org.geotools.data.util.MeasureConverterFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.measure.Measure;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;

/**
 * The GeoServer application, the main entry point for any Wicket application. In particular, this
 * one sets up, among the others, custom resource loader, custom localizers, and custom converters
 * (wrapping the GeoTools ones), as well as providing some convenience methods to access the
 * GeoServer Spring context and principal GeoServer objects.
 *
 * @author Andrea Aaime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 */
public class GeoServerApplication extends WebApplication
        implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {

    /** logger for web application */
    public static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    public static boolean DETECT_BROWSER =
            Boolean.valueOf(System.getProperty("org.geoserver.web.browser.detect", "true"));

    public static final String GEOSERVER_CSRF_DISABLED = "GEOSERVER_CSRF_DISABLED";
    public static final String GEOSERVER_CSRF_WHITELIST = "GEOSERVER_CSRF_WHITELIST";

    ApplicationContext applicationContext;

    /**
     * Default redirect mode. Determines whether default webUIMode setting means redirect or not
     * (default is true).
     */
    protected boolean defaultIsRedirect = true;

    /**
     * Get default redirect mode.
     *
     * @return default redirect mode.
     */
    public boolean isDefaultIsRedirect() {
        return defaultIsRedirect;
    }

    /**
     * Set default redirect mode. (must be called before init method, usually by Spring
     * PropertyOverriderConfigurer)
     */
    public void setDefaultIsRedirect(boolean defaultIsRedirect) {
        this.defaultIsRedirect = defaultIsRedirect;
    }

    /** The {@link GeoServerHomePage}. */
    public Class<GeoServerHomePage> getHomePage() {
        return GeoServerHomePage.class;
    }

    public static GeoServerApplication get() {
        return (GeoServerApplication) Application.get();
    }

    /** Returns the spring application context. */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /** Returns the geoserver configuration instance. */
    public GeoServer getGeoServer() {
        return getBeanOfType(GeoServer.class);
    }

    /** Returns the catalog. */
    public Catalog getCatalog() {
        return getGeoServer().getCatalog();
    }

    /** Returns the security manager. */
    public GeoServerSecurityManager getSecurityManager() {
        return getBeanOfType(GeoServerSecurityManager.class);
    }

    /** Returns the geoserver resource loader. */
    public GeoServerResourceLoader getResourceLoader() {
        return getBeanOfType(GeoServerResourceLoader.class);
    }

    /**
     * Loads a bean from the spring application context of a specific type.
     *
     * <p>If there are multiple beans of the specfied type in the context an exception is thrown.
     *
     * @param type The class of the bean to return.
     */
    public <T> T getBeanOfType(Class<T> type) {
        return GeoServerExtensions.bean(type, getApplicationContext());
    }

    /**
     * Loads a bean from the spring application context with a specific name.
     *
     * @param name The name of the bean to return.
     */
    public Object getBean(String name) {
        return GeoServerExtensions.bean(name);
    }

    /**
     * Loads beans from the spring application context of a specific type.
     *
     * @param type The type of beans to return.
     * @return A list of objects of the specified type, possibly empty.
     * @see {@link GeoServerExtensions#extensions(Class, ApplicationContext)}
     */
    public <T> List<T> getBeansOfType(Class<T> type) {
        return GeoServerExtensions.extensions(type, getApplicationContext());
    }

    /** Clears all the wicket caches so that resources and localization files will be re-read */
    public void clearWicketCaches() {
        getResourceSettings().getPropertiesFactory().clearCache();
        getResourceSettings().getLocalizer().clearCache();
    }

    /** Initialization override which sets up a locator for i18n resources. */
    protected void init() {
        // enable GeoServer custom resource locators
        getResourceSettings().setUseMinifiedResources(false);
        getResourceSettings().setResourceStreamLocator(new GeoServerResourceStreamLocator());

        /*
         * The order string resource loaders are added to IResourceSettings is of importance so we need to add any contributed loader prior to the
         * standard ones so it takes precedence. Otherwise it won't be hit due to GeoServerStringResourceLoader never resolving to null but falling
         * back to the default language
         */
        List<IStringResourceLoader> alternateResourceLoaders =
                getBeansOfType(IStringResourceLoader.class);
        for (IStringResourceLoader loader : alternateResourceLoaders) {
            LOGGER.info("Registering alternate resource loader: " + loader);
            getResourceSettings().getStringResourceLoaders().add(loader);
        }

        getResourceSettings()
                .getStringResourceLoaders()
                .add(0, new GeoServerStringResourceLoader());
        getDebugSettings().setAjaxDebugModeEnabled(false);

        getApplicationSettings().setPageExpiredErrorPage(GeoServerExpiredPage.class);
        // generates infinite redirections, commented out for the moment
        // getSecuritySettings().setCryptFactory(GeoserverWicketEncrypterFactory.get());

        // theoretically, this replaces the old GeoServerRequestEncodingStrategy
        // by making the URLs encrypted at will
        GeoServerSecurityManager securityManager = getBeanOfType(GeoServerSecurityManager.class);
        setRootRequestMapper(
                new DynamicCryptoMapper(getRootRequestMapper(), securityManager, this));

        getRequestCycleListeners().add(new CallbackRequestCycleListener(this));

        // Csrf Protection
        Boolean geoserverCsrfDisabled =
                Boolean.valueOf(GeoServerExtensions.getProperty(GEOSERVER_CSRF_DISABLED));
        String geoserverCsrfWhitelist = GeoServerExtensions.getProperty(GEOSERVER_CSRF_WHITELIST);

        // Don't add a new lister each time init() is called
        List<IRequestCycleListener> csrfListenersToRemove = new ArrayList<>();
        for (IRequestCycleListener listener : getRequestCycleListeners()) {
            if (listener instanceof CsrfPreventionRequestCycleListener) {
                csrfListenersToRemove.add(listener);
            }
        }
        for (IRequestCycleListener listener : csrfListenersToRemove) {
            getRequestCycleListeners().remove(listener);
        }
        CsrfPreventionRequestCycleListener csrfListener = new CsrfPreventionRequestCycleListener();
        if (!geoserverCsrfDisabled) {
            if (geoserverCsrfWhitelist != null && !"".equals(geoserverCsrfWhitelist.trim())) {
                for (String origin : geoserverCsrfWhitelist.split(",")) {
                    csrfListener.addAcceptedOrigin(origin.trim());
                }
            }
            getRequestCycleListeners().add(csrfListener);
        }

        WebUIMode webUIMode = getGeoServer().getGlobal().getWebUIMode();
        if (webUIMode == null) {
            webUIMode = WebUIMode.DEFAULT;
        }
        switch (webUIMode) {
            case DO_NOT_REDIRECT:
                getRequestCycleSettings().setRenderStrategy(RenderStrategy.ONE_PASS_RENDER);
                break;
            case REDIRECT:
                getRequestCycleSettings().setRenderStrategy(RenderStrategy.REDIRECT_TO_BUFFER);
                break;
            case DEFAULT:
                getRequestCycleSettings()
                        .setRenderStrategy(
                                defaultIsRedirect
                                        ? RenderStrategy.REDIRECT_TO_BUFFER
                                        : RenderStrategy.ONE_PASS_RENDER);
        }
    }

    @Override
    public IProvider<IExceptionMapper> getExceptionMapperProvider() {
        // IProvider is functional, remove a bit of boilerplate
        return () ->
                new DefaultExceptionMapper() {
                    @Override
                    protected IRequestHandler mapUnexpectedExceptions(
                            Exception e, Application application) {

                        return createPageRequestHandler(
                                new PageProvider(new GeoServerErrorPage(e)));
                    }
                };
    }

    @Override
    public RuntimeConfigurationType getConfigurationType() {
        String config =
                GeoServerExtensions.getProperty(
                        "wicket." + Application.CONFIGURATION, getApplicationContext());
        if (config == null) {
            return DEPLOYMENT;
        } else if (!"DEPLOYMENT".equalsIgnoreCase(config)
                && !"DEVELOPMENT".equalsIgnoreCase(config)) {
            LOGGER.warning(
                    "Unknown Wicket configuration value '"
                            + config
                            + "', defaulting to DEPLOYMENT");
            return DEPLOYMENT;
        } else {
            return RuntimeConfigurationType.valueOf(config.toUpperCase());
        }
    }

    @Override
    public Session newSession(Request request, Response response) {
        Session s = new GeoServerSession(request);
        if (s.getLocale() == null) s.setLocale(Locale.ENGLISH);
        return s;
    }

    /*
     * Overrides to return a custom converter locator which loads converters from the GeoToools converter subsystem.
     */
    protected IConverterLocator newConverterLocator() {
        // TODO: load converters from application context
        ConverterLocator locator = new ConverterLocator();
        locator.set(
                ReferencedEnvelope.class,
                new GeoToolsConverterAdapter(new StringBBoxConverter(), ReferencedEnvelope.class));
        DataDirectoryConverterLocator dd = new DataDirectoryConverterLocator(getResourceLoader());
        locator.set(File.class, dd.getConverter(File.class));
        locator.set(URI.class, dd.getConverter(URI.class));
        locator.set(URL.class, dd.getConverter(URL.class));
        locator.set(
                Measure.class,
                new GeoToolsConverterAdapter(MeasureConverterFactory.CONVERTER, Measure.class));

        return locator;
    }

    // static class RequestCycleProcessor extends WebRequestCycleProcessor {
    //
    // public IRequestTarget resolve(RequestCycle requestCycle,
    // RequestParameters requestParameters) {
    // IRequestTarget target = super.resolve(requestCycle,
    // requestParameters);
    // if (target != null) {
    // return target;
    // }
    //
    // STILL HAVE TO FIGURE OUT HOW TO SEND THE USER BACK TO THE HOME PAGE IN THE NEW WICKET
    // return resolveHomePageTarget(requestCycle, requestParameters);
    // }
    //
    //
    // }

    static class CallbackRequestCycleListener extends AbstractRequestCycleListener {
        private List<WicketCallback> callbacks;

        public CallbackRequestCycleListener(GeoServerApplication app) {
            callbacks = app.getBeansOfType(WicketCallback.class);
        }

        @Override
        public void onBeginRequest(org.apache.wicket.request.cycle.RequestCycle cycle) {
            for (WicketCallback callback : callbacks) {
                callback.onBeginRequest();
            }
        }

        @Override
        public void onEndRequest(org.apache.wicket.request.cycle.RequestCycle cycle) {
            for (WicketCallback callback : callbacks) {
                callback.onEndRequest();
            }
        }

        @Override
        public void onDetach(org.apache.wicket.request.cycle.RequestCycle cycle) {
            for (WicketCallback callback : callbacks) {
                callback.onAfterTargetsDetached();
            }
        }

        @Override
        public void onRequestHandlerScheduled(RequestCycle cycle, IRequestHandler handler) {
            processHandler(cycle, handler);
        }

        private void processHandler(RequestCycle cycle, IRequestHandler handler) {
            if (handler instanceof IPageRequestHandler) {
                IPageRequestHandler pageHandler = (IPageRequestHandler) handler;
                Class<? extends IRequestablePage> pageClass = pageHandler.getPageClass();
                for (WicketCallback callback : callbacks) {
                    callback.onRequestTargetSet(cycle, pageClass);
                }
            } else if (handler instanceof IRequestHandlerDelegate) {
                IRequestHandlerDelegate delegator = (IRequestHandlerDelegate) handler;
                processHandler(cycle, delegator.getDelegateHandler());
            }
        }

        @Override
        public void onRequestHandlerResolved(
                org.apache.wicket.request.cycle.RequestCycle cycle, IRequestHandler handler) {
            processHandler(cycle, handler);
        }

        @Override
        public IRequestHandler onException(
                org.apache.wicket.request.cycle.RequestCycle cycle, Exception ex) {
            for (WicketCallback callback : callbacks) {
                callback.onRuntimeException(cycle, ex);
            }
            return null;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Convenience method to get the underlying servlet request backing the current wicket request.
     *
     * <p>The request is obtained from the current RequestCycle.
     */
    public HttpServletRequest servletRequest() {
        RequestCycle cycle = RequestCycle.get();
        if (cycle == null) {
            throw new IllegalStateException("Method must be called from a wicket request thread");
        }

        return servletRequest(cycle.getRequest());
    }

    /**
     * Convenience method to get the underlying servlet request backing the current wicket request.
     */
    public HttpServletRequest servletRequest(Request req) {
        if (req == null || !(req instanceof ServletWebRequest)) {
            throw new IllegalStateException(
                    "Request not of type ServletWebRequest, was: "
                            + (req == null ? "null" : req.getClass().getName()));
        }

        return ((ServletWebRequest) req).getContainerRequest();
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AuthenticationSuccessEvent
                || event instanceof InteractiveAuthenticationSuccessEvent) {
            if (Session.exists()) {
                WebSession.get().replaceSession();
            }
        }
    }
}
