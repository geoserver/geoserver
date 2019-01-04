/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import com.hazelcast.web.SessionListener;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Extends the hazelcast SessionListener to only act if clustering is enabled.
 *
 * @author Kevin Smith, OpenGeo
 */
public class HzSessionShareListener extends SessionListener {
    HzCluster cluster;

    HzCluster getCluster(HttpSessionEvent evt) {
        // Assuming that there's only ever the one Servlet or Application context to worry about
        if (cluster == null) {
            ServletContext sc = evt.getSession().getServletContext();
            ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
            cluster = ac.getBean("hzCluster", HzCluster.class);
        }
        return cluster;
    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        if (getCluster(httpSessionEvent).isSessionSharing()) super.sessionCreated(httpSessionEvent);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        if (getCluster(httpSessionEvent).isSessionSharing())
            super.sessionDestroyed(httpSessionEvent);
    }
}
