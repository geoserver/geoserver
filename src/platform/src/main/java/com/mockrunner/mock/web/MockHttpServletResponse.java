package com.mockrunner.mock.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MockHttpServletResponse extends org.springframework.mock.web.MockHttpServletResponse {
    
    boolean wasRedirectSent = false;
    
    int errorCode = SC_OK;

    public int getErrorCode() {
        return errorCode;
    }
    
    
    @Override
    public void sendError(int status) throws IOException {
        this.errorCode = status;
        super.sendError(status);
    }
    
    @Override
    public void sendError(int status, String errorMessage) throws IOException {
        this.errorCode = status;
        super.sendError(status, errorMessage);
    }
    

    public String getOutputStreamContent() throws UnsupportedEncodingException {
        return getContentAsString();
    }

    public int getStatusCode() {
        return getStatus();
    }

    public boolean wasRedirectSent() {
        return wasRedirectSent;
    }
    
    @Override
    public void sendRedirect(String url) throws IOException {
        wasRedirectSent = true;
        super.sendRedirect(url);
    }
    
}
