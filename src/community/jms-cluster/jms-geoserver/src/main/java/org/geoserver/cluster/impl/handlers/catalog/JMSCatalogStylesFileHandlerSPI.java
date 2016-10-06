/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.DocumentFileHandlerSPI;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class JMSCatalogStylesFileHandlerSPI extends DocumentFileHandlerSPI {
	
	final Catalog catalog;
	final XStream xstream;
	
	@Autowired
	public JMSConfiguration config;
	
	public JMSCatalogStylesFileHandlerSPI(final int priority, Catalog cat, XStream xstream) {
		super(priority,xstream);
		this.catalog=cat;
		this.xstream=xstream;
	}

	@Override
	public boolean canHandle(final Object event) {
		if (event instanceof DocumentFile)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,DocumentFile> createHandler() {
		JMSCatalogStylesFileHandler styleHandler = new JMSCatalogStylesFileHandler(catalog,xstream,JMSCatalogStylesFileHandlerSPI.class);
		styleHandler.setConfig(config);
		return styleHandler;
	}


}
