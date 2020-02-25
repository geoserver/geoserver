/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * Modified by David Winslow <dwinslow@openplans.org> on 2007-12-13.
 */

package org.geoserver.filters;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GZIPFilter implements Filter {

    private Set myCompressedTypes;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            String ae = request.getHeader("accept-encoding");
            if (ae != null && ae.indexOf("gzip") != -1) {
                GZIPResponseWrapper wrappedResponse =
                        new GZIPResponseWrapper(
                                response, myCompressedTypes, request.getRequestURL().toString());
                chain.doFilter(req, wrappedResponse);
                wrappedResponse.finishResponse();
                return;
            }
        }

        chain.doFilter(req, res);
    }

    public void init(FilterConfig filterConfig) {
        try {
            String compressedTypes = filterConfig.getInitParameter("compressed-types");
            String[] typeNames =
                    (compressedTypes == null ? new String[0] : compressedTypes.split(","));
            // TODO: Are commas allowed in mimetypes?
            myCompressedTypes = new HashSet();
            for (int i = 0; i < typeNames.length; i++) {
                myCompressedTypes.add(Pattern.compile(typeNames[i]));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while setting up GZIPFilter; " + e);
        }
    }

    public void destroy() {}
}
