/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.geoserver.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ui.context.Theme;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Element;

/**
 * A spring application context used for GeoServer testing.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GeoServerTestApplicationContext extends ClassPathXmlApplicationContext
        implements WebApplicationContext {
    ServletContext servletContext;

    boolean useLegacyGeoServerLoader = true;

    final File contextTmp;

    public GeoServerTestApplicationContext(String[] configLocation, ServletContext servletContext)
            throws BeansException {
        super(configLocation, false);
        try {
            contextTmp = IOUtils.createRandomDirectory("./target", "mock", "tmp");
            servletContext.setAttribute("javax.servlet.context.tempdir", contextTmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public Theme getTheme(String themeName) {
        return null;
    }

    public void setUseLegacyGeoServerLoader(boolean useLegacyGeoServerLoader) {
        this.useLegacyGeoServerLoader = useLegacyGeoServerLoader;
    }

    /*
     * JD: Overriding manually and playing with bean definitions. We do this
     * because we have not ported all the mock test data to a 2.x style configuration
     * directory, so we need to force the legacy data directory loader to engage.
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader)
            throws BeansException, IOException {
        super.loadBeanDefinitions(reader);

        if (useLegacyGeoServerLoader) {
            BeanDefinition def = reader.getBeanFactory().getBeanDefinition("geoServerLoader");
            def.setBeanClassName("org.geoserver.test.TestGeoServerLoaderProxy");
        }
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

        WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
        WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
    }

    @Override
    protected void initPropertySources() {
        super.initPropertySources();
        WebApplicationContextUtils.initServletPropertySources(
                this.getEnvironment().getPropertySources(), this.servletContext);
    }

    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        super.initBeanDefinitionReader(reader);
        reader.setDocumentReaderClass(LazyBeanDefinitionDocumentReader.class);
    }

    static class LazyBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

        @Override
        protected BeanDefinitionParserDelegate createDelegate(
                XmlReaderContext readerContext,
                Element root,
                BeanDefinitionParserDelegate parentDelegate) {
            root.setAttribute("default-lazy-init", "true");
            BeanDefinitionParserDelegate delegate =
                    super.createDelegate(readerContext, root, parentDelegate);
            return delegate;
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        FileUtils.deleteQuietly(contextTmp);
    }
}
