/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Filter to log requests for debugging or statistics-gathering purposes.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class LoggingFilter implements Filter {
    protected Logger logger = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");

    protected boolean enabled = true;
    protected boolean logBodies = true;


    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
        String message = "";
        String body = null;
        String path = "";

        if (enabled){
            if (req instanceof HttpServletRequest){
                HttpServletRequest hreq = (HttpServletRequest) req;

                path = hreq.getRemoteHost() + " \"" + hreq.getMethod() + " " + hreq.getRequestURI();
                if (hreq.getQueryString() != null){
                    path += "?" + hreq.getQueryString();
                }
                path += "\"";

                message = "" + path;
                message += " \"" + noNull(hreq.getHeader("User-Agent"));
                message += "\" \"" + noNull(hreq.getHeader("Referer")) + "\" ";

                if (logBodies && (hreq.getMethod().equals("PUT") || hreq.getMethod().equals("POST"))){
                    message += " request-size: " + hreq.getContentLength();
                    message += " body: ";
                    StringBuffer buff = new StringBuffer();
                    BufferedReader reader = hreq.getReader();
                    char[] readIn = new char[256];
                    int amountRead = 0;
                    while ((amountRead = reader.read(readIn, 0 , 256)) != -1){
                        buff.append(readIn, 0, amountRead);
                    }
                    body = buff.toString();
                    req = new BufferedRequestWrapper(hreq, buff.toString());
                }
            } else {
                message = "" + req.getRemoteHost() + " made a non-HTTP request";
            }

            logger.info(message + (body == null? "" : "\n" + body + "\n"));
            long startTime = System.currentTimeMillis();
            chain.doFilter(req, res);
            long requestTime = System.currentTimeMillis() - startTime;
            logger.info(path +  " took " + requestTime + "ms");
        } else {
            chain.doFilter(req, res);
        }

   }

    public void init(FilterConfig filterConfig) {
        enabled = getConfigBool("enabled", filterConfig);
        logBodies = getConfigBool("log-request-bodies", filterConfig);
    }
    
    protected boolean getConfigBool(String name, FilterConfig conf){
        try {
            String value = conf.getInitParameter(name);
            return Boolean.valueOf(value).booleanValue();
        } catch (Exception e){
            return false;
        }
    }

    protected String noNull(String s){
        if (s == null) return "";
        return s;
    }

    public void destroy() {
    }
}
