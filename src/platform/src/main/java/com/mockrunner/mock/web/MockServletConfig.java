package com.mockrunner.mock.web;

import javax.servlet.ServletContext;

public class MockServletConfig extends org.springframework.mock.web.MockServletConfig {

    public MockServletConfig() {
        super();
    }

    public MockServletConfig(ServletContext servletContext, String servletName) {
        super(servletContext, servletName);
    }

    public MockServletConfig(ServletContext servletContext) {
        super(servletContext);
    }

    public MockServletConfig(String servletName) {
        super(servletName);
    }

}
