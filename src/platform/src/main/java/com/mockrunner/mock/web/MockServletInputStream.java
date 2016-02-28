package com.mockrunner.mock.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.mock.web.DelegatingServletInputStream;

public class MockServletInputStream extends DelegatingServletInputStream {

    public MockServletInputStream(InputStream sourceStream) {
        super(sourceStream);
    }
    
    public MockServletInputStream(byte[] bytes) {
        super(new ByteArrayInputStream(bytes));
    }

}
