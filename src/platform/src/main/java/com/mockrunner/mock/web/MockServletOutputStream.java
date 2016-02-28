package com.mockrunner.mock.web;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.springframework.mock.web.DelegatingServletOutputStream;

public class MockServletOutputStream extends DelegatingServletOutputStream {

        public MockServletOutputStream(OutputStream targetStream) {
        super(targetStream);
    }
    
    public MockServletOutputStream() {
        super(new ByteArrayOutputStream());
    }

    public byte[] getBinaryContent() {
        return ((ByteArrayOutputStream) getTargetStream()).toByteArray();
    }

}
