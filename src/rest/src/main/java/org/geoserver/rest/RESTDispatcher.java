/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.springframework.beans.BeansException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.noelios.restlet.ext.servlet.ServletConverter;

/**
 * Simple AbstractController implementation that does the translation between
 * Spring requests and Restlet requests.
 */
public class RESTDispatcher extends AbstractController {
    /** HTTP method "PUT" */
    public static final String METHOD_PUT = "PUT";
    /** HTTP method "DELETE" */
    public static final String METHOD_DELETE = "DELETE";
    
    /**
     * logger
     */
    static Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");
    
    /**
     * converter for turning servlet requests into resetlet requests.
     */
    ServletConverter myConverter;
    
    /**
     * the root restlet router
     */
    Router myRouter;
    
    /**
     * rest request callbacks
     */
    List<DispatcherCallback> callbacks;
    
    /**
     * geoserver configuration
     */
    GeoServer gs;
    
    public RESTDispatcher(GeoServer gs) {
        this.gs = gs;
        setSupportedMethods(new String[] {
            METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_DELETE, METHOD_HEAD
        });
    }

    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();

        myConverter = new GeoServerServletConverter(getServletContext());
        myConverter.setTarget(createRoot());
        
        callbacks = 
            GeoServerExtensions.extensions(DispatcherCallback.class, getApplicationContext());
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse resp)
        throws Exception {

        try {
            myConverter.service(req, resp);
        }
        catch( Exception e ) {
            RestletException re = null;
            if ( e instanceof RestletException ) {
                re = (RestletException) e;
            }
            if ( re == null && e.getCause() instanceof RestletException ) {
                re = (RestletException) e.getCause();
            }
            
            if ( re != null ) {
                resp.setStatus( re.getStatus().getCode() );
                
                String reStr = re.getRepresentation().getText();
                if ( reStr != null ) {
                    LOG.severe( reStr );
                    resp.setContentType("text/plain");
                    resp.getOutputStream().write(reStr.getBytes());    
                }
                
                //log the full exception at a higher level
                LOG.log( Level.SEVERE, "", re );
            }
            else {
                LOG.log( Level.SEVERE, "", e );
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                
                if ( e.getMessage() != null ) {
                    resp.getOutputStream().write( e.getMessage().getBytes() );    
                }
            }
            resp.getOutputStream().flush();
        }
            
        return null;
    }

    public void addRoutes(Map m, Router r){
        Iterator it = m.entrySet().iterator();

        while (it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();

            // LOG.info("Found mapping: " + entry.getKey().toString());
            Restlet restlet = 
               (getApplicationContext().getBean(entry.getValue().toString()) instanceof Resource)
               ? new BeanResourceFinder(getApplicationContext(), entry.getValue().toString())
               : new BeanDelegatingRestlet(getApplicationContext(), entry.getValue().toString());

            String path = entry.getKey().toString();

            r.attach(path, restlet);

            if (path.indexOf("?") == -1){
                r.attach(path + "?{q}", restlet);
            } else LOG.fine("Query string already listed in restlet mapping: " + path);
        }
    }

    public Restlet createRoot() {
        if (myRouter == null){
            myRouter = new Router() {
                
                @Override
                protected synchronized void init(Request request,
                        Response response) {
                    super.init(request, response);

                    //set the page uri's
                    
                    // http://host:port/appName
                    String baseURL = request.getRootRef().getParentRef().toString();
                    String rootPath = request.getRootRef().toString().substring(baseURL.length());
                    String pagePath = request.getResourceRef().toString().substring(baseURL.length());
                    String basePath = null;
                    if ( request.getResourceRef().getBaseRef() != null ) {
                        basePath = request.getResourceRef().getBaseRef().toString().substring(baseURL.length());
                    }
                    
                    //strip off the extension
                    String extension = ResponseUtils.getExtension(pagePath);
                    if ( extension != null ) {
                        pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
                    }
                    
                    //trim leading slash
                    if ( pagePath.endsWith( "/" ) ) {
                        pagePath = pagePath.substring(0, pagePath.length()-1);
                    }
                    //create a page info object and put it into a request attribute
                    PageInfo pageInfo = new PageInfo();
                    pageInfo.setBaseURL(baseURL);
                    pageInfo.setRootPath(rootPath);
                    pageInfo.setBasePath(basePath);
                    pageInfo.setPagePath(pagePath);
                    pageInfo.setExtension( extension );
                    request.getAttributes().put( PageInfo.KEY, pageInfo );
                    
                    for (DispatcherCallback callback : callbacks) {
                        callback.init(request, response);
                    }
                }
                
                @Override
                public Restlet getNext(Request request, Response response) {
                    Restlet next = super.getNext(request, response);
                    if (next != null) {
                        for (DispatcherCallback callback : callbacks) {
                            callback.dispatched(request, response, next);
                        }
                    }
                    return next;
                };
                
                @Override
                public void handle(Request request, Response response) {
                    try {
                        super.handle(request, response);
                    }
                    catch(Exception e) {
                        //execute the exception callback
                        for (DispatcherCallback callback : callbacks) {
                            callback.exception(request, response, e);
                        }
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        }
                        throw new RuntimeException(e);
                    }
                    finally {
                        //execute the finished callback
                        for (DispatcherCallback callback : callbacks) {
                            callback.finished(request, response);
                        }
                    }
                };
            };

            //load all the rest mappings and register them with the router
            Iterator i = 
                GeoServerExtensions.extensions(RESTMapping.class).iterator();

            while (i.hasNext()){
                RESTMapping rm = (RESTMapping)i.next();
                addRoutes(rm.getRoutes(), myRouter);
            }

            //create a root mapping
            myRouter.attach("", new IndexRestlet(myRouter));
        }

        return myRouter;
   }
}
