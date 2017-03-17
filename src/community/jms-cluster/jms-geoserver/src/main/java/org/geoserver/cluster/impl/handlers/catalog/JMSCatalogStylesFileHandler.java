/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import org.apache.commons.lang.NullArgumentException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.DocumentFileHandler;
import org.geoserver.config.GeoServerDataDirectory;
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
				// find the style associated with this document
				Resource styleFile = getStyleResource(event);
				if (styleFile == null) {
					throw new RuntimeException(String.format(
							"Style for style file '%s' not found.", event.getResourceName()));
				}
				// write the style file
				event.writeTo(styleFile);
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

	/**
	 * Helper method the gets the style associated with a document file.
	 */
	private Resource getStyleResource(DocumentFile event) {
		if (event.getStyleName() == null) {
			// no style name provided os nothing to do
			return null;
		}
		// find the associated style
		StyleInfo styleInfo = catalog.getStyleByName(event.getStyleName());
		if (event.getWorkspaceName() != null) {
			styleInfo = catalog.getStyleByName(event.getWorkspaceName(), event.getStyleName());
		}
		// check if we found the associated style
		if (styleInfo == null) {
			throw new RuntimeException(String.format(
					"Style '%s' not found when handling style file '%s'.",
					event.getStyleName(), event.getResourceName()));
		}
		// get the path of style file associated with the found style
		GeoServerDataDirectory dataDirectory = GeoServerExtensions.bean(GeoServerDataDirectory.class);
		return dataDirectory.get(styleInfo, styleInfo.getFilename());
	}
}
