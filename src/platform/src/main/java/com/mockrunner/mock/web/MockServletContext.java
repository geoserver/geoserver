package com.mockrunner.mock.web;

import org.springframework.core.io.ResourceLoader;

public class MockServletContext extends org.springframework.mock.web.MockServletContext {

    public MockServletContext() {
        super();
    }

    public MockServletContext(ResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    public MockServletContext(String resourceBasePath, ResourceLoader resourceLoader) {
        super(resourceBasePath, resourceLoader);
    }

    public MockServletContext(String resourceBasePath) {
        super(resourceBasePath);
    }

}
