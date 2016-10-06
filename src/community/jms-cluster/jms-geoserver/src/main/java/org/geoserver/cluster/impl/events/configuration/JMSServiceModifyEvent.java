/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events.configuration;

import java.util.List;

import org.geoserver.cluster.impl.events.JMSModifyEvent;
import org.geoserver.cluster.impl.handlers.configuration.JMSServiceHandler;
import org.geoserver.config.ServiceInfo;

/**
 * 
 * This Class define a wrapper of the {@link JMSModifyEvent} class to define an
 * event which can be recognized by the {@link JMSServiceHandler} as ServiceInfo modified
 * events.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSServiceModifyEvent extends JMSModifyEvent<ServiceInfo> {

	public JMSServiceModifyEvent(final ServiceInfo source,
			final List<String> propertyNames, final List<Object> oldValues,
			final List<Object> newValues) {
		super(source, propertyNames, oldValues, newValues);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
