/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.wicket.Application;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.request.IRequestCodingStrategy;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.spring.SpringWebApplication;
import org.apache.wicket.util.convert.ConverterLocator;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.util.DataDirectoryConverterLocator;
import org.geoserver.web.util.GeoToolsConverterAdapter;
import org.geoserver.web.util.converters.StringBBoxConverter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.wicketstuff.htmlvalidator.HtmlValidationResponseFilter;

/**
 * The GeoServer application, the main entry point for any Wicket application. In particular, this
 * one sets up, among the others, custom resource loader, custom localizers, and custom converters
 * (wrapping the GeoTools ones), as well as providing some convenience methods to access the
 * GeoServer Spring context and principal GeoServer objects.
 * 
 * @author Andrea Aaime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 */
public class GeoServerApplication extends SpringWebApplication {

    /**
     * logger for web application
     */
    public static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    public static boolean DETECT_BROWSER = Boolean.valueOf(System.getProperty(
            "org.geoserver.web.browser.detect", "true"));

    /**
     * The {@link GeoServerHomePage}.
     */
    public Class<GeoServerHomePage> getHomePage() {
        return GeoServerHomePage.class;
    }

    public static GeoServerApplication get() {
        return (GeoServerApplication) Application.get();
    }

    /**
     * Returns the spring application context.
     */
    public ApplicationContext getApplicationContext() {
        return internalGetApplicationContext();
    }

    /**
     * Returns the geoserver configuration instance.
     */
    public GeoServer getGeoServer() {
        return getBeanOfType(GeoServer.class);
    }

    /**
     * Returns the catalog.
     */
    public Catalog getCatalog() {
        return getGeoServer().getCatalog();
    }

    /**
     * Returns the security manager.
     */
    public GeoServerSecurityManager getSecurityManager() {
        return getBeanOfType(GeoServerSecurityManager.class);
    }


    /**
     * Returns the geoserver resource loader.
     */
    public GeoServerResourceLoader getResourceLoader() {
        return getBeanOfType(GeoServerResourceLoader.class);
    }

    /**
     * Loads a bean from the spring application context of a specific type.
     * <p>
     * If there are multiple beans of the specfied type in the context an exception is thrown.
     * </p>
     * 
     * @param type The class of the bean to return.
     */
    public <T> T getBeanOfType(Class<T> type) {
        return GeoServerExtensions.bean(type, getApplicationContext());
    }

    /**
     * Loads a bean from the spring application context with a specific name.
     * 
     * @param type The class of the bean to return.
     */
    public Object getBean(String name) {
        return GeoServerExtensions.bean(name);
    }

    /**
     * Loads beans from the spring application context of a specific type.
     * 
     * @param type The type of beans to return.
     * 
     * @return A list of objects of the specified type, possibly empty.
     * @see {@link GeoServerExtensions#extensions(Class, ApplicationContext)}
     */
    public <T> List<T> getBeansOfType(Class<T> type) {
        return GeoServerExtensions.extensions(type, getApplicationContext());
    }

    /**
     * Clears all the wicket caches so that resources and localization files will be re-read
     */
    public void clearWicketCaches() {
        getResourceSettings().getPropertiesFactory().clearCache();
        getResourceSettings().getLocalizer().clearCache();
    }

    /**
     * Initialization override which sets up a locator for i18n resources.
     */
    protected void init() {
        // enable GeoServer custom resource locators
        getResourceSettings().setResourceStreamLocator(new GeoServerResourceStreamLocator());

        /*
         * The order string resource loaders are added to IResourceSettings is of importance so we
         * need to add any contributed loader prior to the standard ones so it takes precedence.
         * Otherwise it won't be hit due to GeoServerStringResourceLoader never resolving to null
         * but falling back to the default language
         */
        List<IStringResourceLoader> alternateResourceLoaders = getBeansOfType(IStringResourceLoader.class);
        for (IStringResourceLoader loader : alternateResourceLoaders) {
            LOGGER.info("Registering alternate resource loader: " + loader);
            getResourceSettings().addStringResourceLoader(loader);
        }

        getResourceSettings().addStringResourceLoader(0, new GeoServerStringResourceLoader());
        // getResourceSettings().addStringResourceLoader(new ComponentStringResourceLoader());
        // getResourceSettings().addStringResourceLoader(new
        // ClassStringResourceLoader(this.getClass()));

        // we have our own application wide gzip compression filter
        getResourceSettings().setDisableGZipCompression(true);

        // enable toggable XHTML validation
        if (DEVELOPMENT.equalsIgnoreCase(getConfigurationType())) {
            getMarkupSettings().setStripWicketTags(true);
            HtmlValidationResponseFilter htmlvalidator = new GeoServerHTMLValidatorResponseFilter();
            htmlvalidator.setIgnoreAutocomplete(true);
            htmlvalidator.setIgnoreKnownWicketBugs(true);
            getRequestCycleSettings().addResponseFilter(htmlvalidator);
        }
        getDebugSettings().setAjaxDebugModeEnabled(false);

        getApplicationSettings().setPageExpiredErrorPage(GeoServerExpiredPage.class);
        getSecuritySettings().setCryptFactory(GeoserverWicketEncrypterFactory.get());

        // figure out which browser we're running against
        getRequestCycleSettings().setGatherExtendedBrowserInfo(DETECT_BROWSER);
    }

    @Override
    public String getConfigurationType() {
        String config = GeoServerExtensions.getProperty("wicket." + Application.CONFIGURATION,
                getApplicationContext());
        if (config == null) {
            return DEPLOYMENT;
        } else if (!DEPLOYMENT.equalsIgnoreCase(config) && !DEVELOPMENT.equalsIgnoreCase(config)) {
            LOGGER.warning("Unknown Wicket configuration value '" + config
                    + "', defaulting to DEPLOYMENT");
            return DEPLOYMENT;
        } else {
            return config;
        }
    }

    @Override
    public Session newSession(Request request, Response response) {
        Session s = new GeoServerSession(request);
        if (s.getLocale() == null)
            s.setLocale(Locale.ENGLISH);
        return s;
    }

    /*
     * Overrides to return a custom request cycle processor. This is done in order to support
     * "dynamic dispatching" from web.xml.
     */
    protected IRequestCycleProcessor newRequestCycleProcessor() {
        return new RequestCycleProcessor();
    }

    public final RequestCycle newRequestCycle(final Request request, final Response response) {
        return new RequestCycle(this, (WebRequest) request, (WebResponse) response);
    }

    /*
     * Overrides to return a custom converter locator which loads converters from the GeoToools
     * converter subsystem.
     */
    protected IConverterLocator newConverterLocator() {
        // TODO: load converters from application context
        ConverterLocator locator = new ConverterLocator();
        locator.set(ReferencedEnvelope.class, new GeoToolsConverterAdapter(
                new StringBBoxConverter(), ReferencedEnvelope.class));
        DataDirectoryConverterLocator dd = new DataDirectoryConverterLocator(getResourceLoader());
        locator.set(File.class, dd.getConverter(File.class));
        locator.set(URI.class, dd.getConverter(URI.class));
        locator.set(URL.class, dd.getConverter(URL.class));

        return locator;
    }

    
    static class RequestCycleProcessor extends WebRequestCycleProcessor {
        
        public IRequestTarget resolve(RequestCycle requestCycle,
                RequestParameters requestParameters) {
            IRequestTarget target = super.resolve(requestCycle,
                    requestParameters);
            if (target != null) {
                return target;
            }

            return resolveHomePageTarget(requestCycle, requestParameters);
        }
        @Override
        protected IRequestCodingStrategy newRequestCodingStrategy() {            
              return new GeoServerRequestEncodingStrategy();
        }

    }

    static class RequestCycle extends WebRequestCycle {
        private List<WicketCallback> callbacks;

        public RequestCycle(GeoServerApplication app, WebRequest req, WebResponse resp) {
            super(app, req, resp);
            callbacks = app.getBeansOfType(WicketCallback.class);
        }

        @Override
        protected void onBeginRequest() {
            for (WicketCallback callback : callbacks) {
                callback.onBeginRequest();
            }
        }

        @Override
        protected void onAfterTargetsDetached() {
            for (WicketCallback callback : callbacks) {
                callback.onAfterTargetsDetached();
            }
        }

        @Override
        protected void onEndRequest() {
            for (WicketCallback callback : callbacks) {
                callback.onEndRequest();
            }
        }

        @Override
        public final Page onRuntimeException(final Page cause, final RuntimeException ex) {
            for (WicketCallback callback : callbacks) {
                callback.onRuntimeException(cause, ex);
            }
            if (ex instanceof PageExpiredException) {
                return super.onRuntimeException(cause, ex);
            } else {
                return new GeoServerErrorPage(cause, ex);
            }
        }

        @Override
        protected void onRequestTargetSet(IRequestTarget requestTarget) {
            for (WicketCallback callback : callbacks) {
                callback.onRequestTargetSet(requestTarget);
            }
            super.onRequestTargetSet(requestTarget);
        }
    }

    private IConverterLocator buildConverterLocator() {
        ConverterLocator locator = new ConverterLocator();

        return locator;
    }
    
    
}
