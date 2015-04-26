/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.geoserver.data.util.IOUtils;
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
 *
 */
public class GeoServerTestApplicationContext extends ClassPathXmlApplicationContext
    implements WebApplicationContext {
    ServletContext servletContext;

    boolean useLegacyGeoServerLoader = true;

    boolean fullInitRequired;
    
    public GeoServerTestApplicationContext(String configLocation, ServletContext servletContext)
        throws BeansException {
        this(new String[] { configLocation }, servletContext, true);
    }

    public GeoServerTestApplicationContext(String[] configLocation, ServletContext servletContext,
            boolean fullInitRequired)
        throws BeansException {
        super(configLocation, false);
        this.fullInitRequired = true;
        setValidating(false);
        try {
            servletContext.setAttribute(
                "javax.servlet.context.tempdir", 
                IOUtils.createRandomDirectory("./target", "mock", "tmp")
            );
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
            def.setBeanClassName( "org.geoserver.test.TestGeoServerLoaderProxy");
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
        if (!fullInitRequired) {
            reader.setDocumentReaderClass(LazyBeanDefinitionDocumentReader.class);
        }
    }

    static class LazyBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

        @Override
        protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext,
                Element root, BeanDefinitionParserDelegate parentDelegate) {
            root.setAttribute("default-lazy-init", "true");
            BeanDefinitionParserDelegate delegate = super.createHelper(readerContext, root,
                    parentDelegate);
            return delegate;
        }
    }

}
