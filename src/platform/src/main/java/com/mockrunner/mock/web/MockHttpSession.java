package com.mockrunner.mock.web;

import javax.servlet.ServletContext;

public class MockHttpSession extends org.springframework.mock.web.MockHttpSession {

    public MockHttpSession() {
        super();
    }

    public MockHttpSession(ServletContext servletContext, String id) {
        super(servletContext, id);
    }

    public MockHttpSession(ServletContext servletContext) {
        super(servletContext);
    }

}
