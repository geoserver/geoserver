/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events.configuration;

import java.util.List;
import org.geoserver.cluster.impl.events.JMSModifyEvent;
import org.geoserver.config.GeoServerInfo;

/**
 * Serializable event to send over JMS channel <br>
 * a Global modify event containing changes operated over the GeoServerInfo object
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSGlobalModifyEvent extends JMSModifyEvent<GeoServerInfo> {

    public JMSGlobalModifyEvent(
            final GeoServerInfo source,
            final List<String> propertyNames,
            final List<Object> oldValues,
            final List<Object> newValues) {
        super(source, propertyNames, oldValues, newValues);
    }

    /** */
    private static final long serialVersionUID = 1L;
}
