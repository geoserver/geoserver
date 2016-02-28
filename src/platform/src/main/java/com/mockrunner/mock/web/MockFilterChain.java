package com.mockrunner.mock.web;

import javax.servlet.Filter;
import javax.servlet.Servlet;

public class MockFilterChain extends org.springframework.mock.web.MockFilterChain {

    public MockFilterChain() {
        super();
    }

    public MockFilterChain(Servlet servlet, Filter... filters) {
        super(servlet, filters);
    }

    public MockFilterChain(Servlet servlet) {
        super(servlet);
    }

    

}
