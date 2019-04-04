/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.net.URL;
import javax.servlet.http.HttpServletRequest;

/**
 * Controller which publishes files through a web interface from the classpath
 *
 * <p>To use this controller, it should be mapped to a particular url in the url mapping of the
 * spring dispatcher servlet. Example:
 *
 * <pre>
 * <code>
 *   &lt;bean id="filePublisher" class="org.geoserver.ows.FilePublisher"/&gt;
 *   &lt;bean id="dispatcherMappings"
 *      &lt;property name="alwaysUseFullPath" value="true"/&gt;
 *      class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *      &lt;property name="mappings"&gt;
 *        &lt;prop key="/schemas/** /*.xsd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/schemas/** /*.dtd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/styles/*"&gt;filePublisher&lt;/prop&gt;
 *      &lt;/property&gt;
 *   &lt;/bean&gt;
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime - GeoSolutions
 */
public class ClasspathPublisher extends AbstractURLPublisher {
    Class clazz;

    /**
     * Creates the new classpath publisher.
     *
     * @param clazz the class used to perform classpath lookups with {@link
     *     Class#getResource(String)}
     */
    public ClasspathPublisher(Class clazz) {
        this.clazz = clazz;
    }

    public ClasspathPublisher() {
        this.clazz = ClasspathPublisher.class;
    }

    @Override
    protected URL getUrl(HttpServletRequest request) {
        String ctxPath = request.getContextPath();
        String reqPath = request.getRequestURI();
        reqPath = reqPath.substring(ctxPath.length());

        // try a few lookups
        URL url = clazz.getResource(reqPath);
        if (url == null && !reqPath.startsWith("/")) {
            url = clazz.getResource("/");
        }
        if (url == null && (reqPath.length() > 1) && reqPath.startsWith("/")) {
            url = clazz.getResource(reqPath.substring(1));
        }
        return url;
    }
}
