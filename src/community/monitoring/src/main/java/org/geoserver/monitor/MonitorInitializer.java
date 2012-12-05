/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.Arrays;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.hibernate.HibUtil;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.monitor.hib.HibernateMonitorDAO2;
import org.hibernate.SessionFactory;

public class MonitorInitializer implements GeoServerInitializer {

    Monitor monitor;
    
    public MonitorInitializer(Monitor monitor) {
        this.monitor = monitor;
    }
    
    public void initialize(GeoServer geoServer) throws Exception {
        monitor.setServer(geoServer);

        if (!monitor.isEnabled()) return;

        //special case for hibernate, we need to have a session in order to make this work
        SessionFactory sessionFactory = null;
        if (monitor.getDAO() instanceof HibernateMonitorDAO2) {
            sessionFactory = ((HibernateMonitorDAO2)monitor.getDAO()).getSessionFactory();
            HibUtil.setUpSession(sessionFactory);
        }
        
        try {
            //clear out any requests that were left in an inconsistent state
            Query query = new Query().filter("status",
                Arrays.asList(Status.RUNNING, Status.WAITING, Status.CANCELLING), Comparison.IN);
            for (RequestData data : monitor.getDAO().getRequests(query)) {
                if (InternalHostname.get().equals(data.getInternalHost())) {
                    //mark start as INTERRUPTED
                    data.setStatus(Status.INTERRUPTED);
                    monitor.getDAO().save(data);
                }
            }
        }
        finally {
            if (sessionFactory != null) {
                HibUtil.tearDownSession(sessionFactory, null);
            }
        }
    }

}
