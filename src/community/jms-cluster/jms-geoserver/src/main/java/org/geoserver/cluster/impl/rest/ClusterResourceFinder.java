/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cluster.impl.rest;

import org.geoserver.cluster.configuration.JMSConfiguration;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
public class ClusterResourceFinder extends Finder {
	
	@Autowired
	public volatile transient Controller controller;

	@Autowired
	public volatile transient JMSConfiguration config;

	protected ClusterResourceFinder() {
		super();
	}

	@Override
	public Resource findTarget(Request request, Response response) {
		return new ClusterResource(getContext(), request, response, controller, config);
	}
}
