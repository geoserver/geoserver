package org.geoserver.restng;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.restng.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.restng.converters.XStreamJSONMessageConverter;
import org.geoserver.restng.converters.StyleConverter;
import org.geoserver.restng.converters.XStreamXMLMessageConverter;
import org.geoserver.restng.converters.XStreamCatalogListConverter;
import org.geotools.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.xml.sax.EntityResolver;

import java.util.List;

/**
 * Configure various aspects of Spring MVC, in particular message converters
 */
@Configuration
public class MVCConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");

        //Ordered according to priority (highest first)
        converters.add(new FreemarkerHTMLMessageConverter(applicationContext, "UTF-8"));
        converters.add(new XStreamXMLMessageConverter(applicationContext));
        converters.add(new XStreamJSONMessageConverter(applicationContext));
        converters.add(new XStreamCatalogListConverter.XMLXStreamListConverter(applicationContext));
        converters.add(new XStreamCatalogListConverter.JSONXStreamListConverter(applicationContext));

        //Deal with the various Style handler
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                converters.add(new StyleConverter(sh.mimeType(ver), ver, sh, entityResolver));
            }
        }

        //use the default ones as lowest priority
        super.addDefaultHttpMessageConverters(converters);
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RestInterceptor());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("sld", MediaType.valueOf(SLDHandler.MIMETYPE_11));
//        configurer.favorPathExtension(true);
        //todo properties files are only supported for test cases. should try to find a way to
        //support them without polluting prod code with handling
//        configurer.mediaType("properties", MediaType.valueOf("application/prs.gs.psl"));
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        //Force MVC to use /restng endpoint. If we need something more advanced, we should make a custom PathHelper
        configurer.setUrlPathHelper(mvcUrlPathHelper());
        configurer.getUrlPathHelper().setAlwaysUseFullPath(true);
    }
}
