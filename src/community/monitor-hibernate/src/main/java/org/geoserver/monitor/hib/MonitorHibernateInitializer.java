/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.util.Arrays;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.hibernate.HibUtil;
import org.geoserver.monitor.InternalHostname;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Status;
import org.geotools.util.logging.Logging;
import org.hibernate.SessionFactory;

public class MonitorHibernateInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    Monitor monitor;

    public MonitorHibernateInitializer(Monitor monitor) {
        this.monitor = monitor;
    }

    public void initialize(GeoServer geoServer) throws Exception {
        if (!monitor.isEnabled()) return;

        // special case for hibernate, we need to have a session in order to make this work
        SessionFactory sessionFactory = null;
        if (monitor.getDAO() instanceof HibernateMonitorDAO2) {
            sessionFactory = ((HibernateMonitorDAO2) monitor.getDAO()).getSessionFactory();
            HibUtil.setUpSession(sessionFactory);
        }

        try {
            // clear out any requests that were left in an inconsistent state
            Query query =
                    new Query()
                            .filter(
                                    "status",
                                    Arrays.asList(
                                            Status.RUNNING, Status.WAITING, Status.CANCELLING),
                                    Comparison.IN);
            for (RequestData data : monitor.getDAO().getRequests(query)) {
                if (InternalHostname.get().equals(data.getInternalHost())) {
                    // mark start as INTERRUPTED
                    data.setStatus(Status.INTERRUPTED);
                    monitor.getDAO().save(data);
                }
            }
        } finally {
            if (sessionFactory != null) {
                HibUtil.tearDownSession(sessionFactory, null);
            }
        }

        LOGGER.info("Monitor hibernate extension enabled");
    }
}
