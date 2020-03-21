package com.boundlessgeo.gsr.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration public class GSRRestConfig implements WebMvcConfigurer {

    public GSRRestConfig() {
        super();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/gsr-demos/**").addResourceLocations("/demos/");
    }
}
