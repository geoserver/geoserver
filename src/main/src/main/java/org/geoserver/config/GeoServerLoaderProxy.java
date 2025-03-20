/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static java.util.Objects.requireNonNull;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.config.datadir.DataDirectoryGeoServerLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * A proxy for {@link GeoServerLoader} that loads the actual loader instance based on the spring context.
 *
 * <p>This method will first attempt to lookup an instance of {@link GeoServerLoader} in the spring context and if none
 * found will fall back on {@link DefaultGeoServerLoader}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerLoaderProxy
        implements BeanPostProcessor,
                ApplicationListener<ContextClosedEvent>,
                ApplicationContextAware,
                GeoServerReinitializer {

    /** resource loader */
    protected GeoServerResourceLoader resourceLoader;

    /** the actual loader */
    GeoServerLoader loader;

    public GeoServerLoaderProxy(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.loader = lookupGeoServerLoader(applicationContext);
        loader.setApplicationContext(applicationContext);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (loader != null) {
            return loader.postProcessAfterInitialization(bean, beanName);
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (loader != null) {
            return loader.postProcessBeforeInitialization(bean, beanName);
        }
        return bean;
    }

    public void reload() throws Exception {
        if (loader != null) {
            loader.reload();
        }
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (loader != null) {
            loader.destroy();
        }
    }

    protected GeoServerLoader lookupGeoServerLoader(ApplicationContext appContext) {
        GeoServerLoader loader = GeoServerExtensions.bean(GeoServerLoader.class, appContext);
        if (loader == null) {
            loader = createDefaultLoader(appContext);
        }
        return loader;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        loader.initializeDefaultStyles(geoServer.getCatalog());
    }

    /**
     * @param appContext required for {@link DataDirectoryGeoServerLoader#isEnabled(ApplicationContext)}
     * @return a new instance of {@link DataDirectoryGeoServerLoader} if {@link DataDirectoryGeoServerLoader#isEnabled()
     *     enabled}, or {@link DefaultGeoServerLoader} otherwise.
     */
    protected GeoServerLoader createDefaultLoader(ApplicationContext appContext) {
        if (DataDirectoryGeoServerLoader.isEnabled(appContext)) {
            GeoServerDataDirectory dataDirectory = getBean(GeoServerDataDirectory.class);
            GeoServerSecurityManager securityManager = getBean(GeoServerSecurityManager.class);
            GeoServerConfigurationLock configLock = getBean(GeoServerConfigurationLock.class);
            return new DataDirectoryGeoServerLoader(dataDirectory, securityManager, configLock);
        }
        return new DefaultGeoServerLoader(resourceLoader);
    }

    protected <T> T getBean(Class<T> type) {
        return requireNonNull(GeoServerExtensions.bean(type));
    }
}
