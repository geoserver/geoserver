package com.boundlessgeo.gsr.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration public class GSRRestConfig extends WebMvcConfigurerAdapter {

    public GSRRestConfig() {
        super();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        super.addResourceHandlers(registry);
        registry.addResourceHandler("/gsr-demos/**").addResourceLocations("/demos/");
    }
}
