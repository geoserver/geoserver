package com.mockrunner.mock.web;

import javax.servlet.ServletContext;

public class MockFilterConfig extends org.springframework.mock.web.MockFilterConfig {

    public MockFilterConfig() {
        super();
    }

    public MockFilterConfig(ServletContext servletContext, String filterName) {
        super(servletContext, filterName);
    }

    public MockFilterConfig(ServletContext servletContext) {
        super(servletContext);
    }

    public MockFilterConfig(String filterName) {
        super(filterName);
    }

}
