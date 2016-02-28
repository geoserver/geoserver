package com.mockrunner.mock.web;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

public class MockHttpServletRequest extends org.springframework.mock.web.MockHttpServletRequest {

    public MockHttpServletRequest() {
        super();
    }

    public MockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
        super(servletContext, method, requestURI);
    }

    public MockHttpServletRequest(ServletContext servletContext) {
        super(servletContext);
    }

    public MockHttpServletRequest(String method, String requestURI) {
        super(method, requestURI);
    }

    public void setBodyContent(byte[] byteArray) {
        setContent(byteArray);
    }

    public void setBodyContent(String body) throws UnsupportedEncodingException {
        setContent(body.getBytes("UTF-8"));
    }

    public void setHeader(String name, Object value) {
        // danger zone, this is not equivalent to setHeader, but could not
        // find a way to reset the header before adding it back
        addHeader(name, value);
        
    }

    public void setupAddParameter(String name, String value) {
        addParameter(name, value);
        
    }

    public void addCookie(Cookie cookie) {
        // not equivalent semantically, but in practice it's called just once
        setCookies(cookie);
    }

    public void setupAddParameter(String key, String[] values) {
        addParameter(key, values);
    }

    public void setUserInRole(String role, boolean isInRole) {
        if(isInRole) {
            addUserRole(role);
        }
    }

}
