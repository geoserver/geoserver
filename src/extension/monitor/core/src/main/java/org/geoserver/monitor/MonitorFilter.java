/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class MonitorFilter implements GeoServerFilter {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");
    
    // We are are not referring to shared constants as this module does not
    // depend on GWC, which might be missing in a deploy. 
    static final String GEOWEBCACHE_CACHE_RESULT = "geowebcache-cache-result";
    static final String GEOWEBCACHE_MISS_REASON = "geowebcache-miss-reason";
    
    Monitor monitor;
    MonitorRequestFilter requestFilter;
    
    ExecutorService postProcessExecutor;
    
    public MonitorFilter(Monitor monitor, MonitorRequestFilter requestFilter) {
        this.monitor = monitor;
        this.requestFilter = requestFilter;
        
        postProcessExecutor = Executors.newFixedThreadPool(2);
        
        if (monitor.isEnabled()) {
            LOGGER.info("Monitor extension enabled");    
        }
        else {
            String msg ="Monitor extension disabled";
            if (monitor.getConfig().getError() != null) {
                msg += ": " + monitor.getConfig().getError().getLocalizedMessage();
            }
            LOGGER.info(msg);
        }
    }
    
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        
        //check if enabled, and ignore non http requests
        if (!monitor.isEnabled() || !(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        if (requestFilter.filter(req)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(req.getRequestURI() + " was filtered from monitoring");
            }
            //don't monitor this request
            chain.doFilter(request, response);
            return;
        }
        
        //start a new request
        RequestData data = monitor.start();
        data.setStartTime(new Date());
        
        //fill in the initial data
        data.setPath(req.getServletPath() + req.getPathInfo());
        
        if (req.getQueryString() != null) {
            data.setQueryString(URLDecoder.decode(req.getQueryString(), "UTF-8"));
        }
        
        data.setHttpMethod(req.getMethod());
        data.setBodyContentLength(req.getContentLength());
        data.setBodyContentType(req.getContentType());
        
        String serverName = System.getProperty("http.serverName");
        if (serverName == null) {
            serverName = req.getServerName();
        }
        data.setHost(serverName);
        data.setInternalHost(InternalHostname.get());
        data.setRemoteAddr(getRemoteAddr(req));
        data.setStatus(Status.RUNNING);
        data.setHttpReferer(getHttpReferer(req));
        
        
        if (SecurityContextHolder.getContext() != null
                && SecurityContextHolder.getContext().getAuthentication() != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth.getPrincipal();
            if (principal != null) {
                if (principal instanceof UserDetails) {
                    data.setRemoteUser(((UserDetails) principal).getUsername());
                } else if (principal instanceof String) {
                    data.setRemoteUser((String) principal);
                }
            }
        }

        // fallback if the above method fails to get us a user
        if (data.getRemoteUser() == null || data.getRemoteUser().isEmpty()) {
            data.setRemoteUser(req.getRemoteUser());
        }

        data.setRemoteUserAgent(req.getHeader("user-agent"));

        //wrap the request and response
        request = new MonitorServletRequest(req, monitor.getConfig().getMaxBodySize());
        response = new MonitorServletResponse(resp);
        
        monitor.update();
        
        //execute the request
        Throwable error = null;
        try {
            chain.doFilter(request, response);
        }
        catch(Throwable t) {
            error = t;
        }
        
        data = monitor.current();
        
        
        data.setBody(getBody((MonitorServletRequest) request));
        data.setBodyContentLength(((MonitorServletRequest)request).getBytesRead());
        data.setResponseContentType(response.getContentType());
        data.setResponseLength(((MonitorServletResponse)response).getContentLength());
        data.setResponseStatus(((MonitorServletResponse)response).getStatus());

        // GWC headers integration.
        String cacheResult = ((MonitorServletResponse) response).getHeader(GEOWEBCACHE_CACHE_RESULT);
        String missReason = ((MonitorServletResponse) response).getHeader(GEOWEBCACHE_MISS_REASON);
        data.setCacheResult(cacheResult);
        data.setMissReason(missReason);

        if (error != null) {
            data.setStatus(Status.FAILED);
            data.setErrorMessage(error.getLocalizedMessage());
            data.setError(error);
        }
        
        if (data.getStatus() != Status.FAILED) {
            data.setStatus(Status.FINISHED);
        }
        
        data.setEndTime(new Date());
        data.setTotalTime(data.getEndTime().getTime() - data.getStartTime().getTime());
        monitor.update();
        data = monitor.current();
        
        monitor.complete();
        
        //post processing
        postProcessExecutor.execute(new PostProcessTask(monitor, data, req, resp));
        
        if (error != null) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException)error;
            }
            else {
                throw new RuntimeException(error);
            }
        }
    }

    public void destroy() {
        postProcessExecutor.shutdown();
        monitor.dispose();
    }

    String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }
    
    String getHttpReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        
        // "Referer" is in the HTTP spec, but "Referrer" is the correct English spelling.
        // This falls back to the "correct" spelling if the specified one was not used.
        if(referer==null)
            referer = req.getHeader("Referrer");
        
        return referer;
    }
    
    // Get the body and trim to the maximum allowable size if necessary
    byte[] getBody(HttpServletRequest req) {
        long maxBodyLength = monitor.config.getMaxBodySize();
        if (maxBodyLength == 0) return null;
        try {
            byte[] body=((MonitorServletRequest)req).getBodyContent(); // TODO: trimming at this point may now be redundant
            if(body!=null && maxBodyLength!=MonitorServletRequest.BODY_SIZE_UNBOUNDED && body.length>maxBodyLength)
                body=Arrays.copyOfRange(body, 0, (int) maxBodyLength);
            return body;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not read request body", ex);
            return null;
        }
    }
    
    static class PostProcessTask implements Runnable {

        Monitor monitor;
        RequestData data;
        HttpServletRequest request;
        HttpServletResponse response;
        
        PostProcessTask(Monitor monitor, RequestData data, HttpServletRequest request, HttpServletResponse response) {
            this.monitor = monitor;
            this.data = data;
            this.request = request;
            this.response = response;
        }
        
        public void run() {
            try {
                List<RequestPostProcessor> pp = new ArrayList();
                pp.add(new ReverseDNSPostProcessor());
                pp.addAll(GeoServerExtensions.extensions(RequestPostProcessor.class));
                
                for (RequestPostProcessor p : pp) {
                    try {
                        p.run(data, request, response);
                    }
                    catch(Exception e) {
                        LOGGER.log(Level.WARNING, "Post process task failed", e);
                    }
                }

                monitor.postProcessed(data);
            }
            finally {
                monitor = null;
                data = null;
                request = null;
                response = null;
            }
        }
    }

}
