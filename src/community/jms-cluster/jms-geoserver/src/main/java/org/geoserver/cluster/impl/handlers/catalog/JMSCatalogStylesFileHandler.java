/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import java.io.File;

import org.apache.commons.lang.NullArgumentException;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.DocumentFileHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSCatalogStylesFileHandler extends DocumentFileHandler {
	private final Catalog catalog;

	private JMSConfiguration config;

	public JMSCatalogStylesFileHandler(Catalog catalog, XStream xstream,
			Class clazz) {
		super(xstream, clazz);
		this.catalog = catalog;
	}

	public void setConfig(JMSConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean synchronize(DocumentFile event) throws Exception {
		if (event == null) {
			throw new NullArgumentException("Incoming object is null");
		}
		if (config == null) {
			throw new IllegalStateException("Unable to load configuration");
		} else if (!ReadOnlyConfiguration.isReadOnly(config)) {
			try {
				GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
				final String fileName = File.separator + "styles"
						+ File.separator + event.getPath().name();
				Resource file = loader.get(fileName);
				
				if ( !Resources.exists(file) ) {
					final String styleAbsolutePath = event.getPath().path();
					if ( styleAbsolutePath.indexOf("workspaces") > 0 ) {
						final String styleFileName = File.separator + 
								styleAbsolutePath.substring(styleAbsolutePath.indexOf("workspaces"));
						file =  loader.get(styleFileName);
					}
				}
				
				event.writeTo(file);
				return true;
			} catch (Exception e) {
				if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
					LOGGER.severe(this.getClass()
							+ " is unable to synchronize the incoming event: "
							+ event);
				throw e;
			}
		}
		return true;
	}

}
