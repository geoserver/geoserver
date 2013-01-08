package geoserver

import geoserver.catalog.Catalog
import org.geoserver.config.GeoServer as GSGeoServer
import org.geoserver.platform.GeoServerExtensions

/**
 * The GeoServer class allows access to it's Catalog
 * and further Configuration.
 * @author Jared Erickson
 */
class GeoServer {
	
	/**
	 * The org.geoserver.config.GeoServer instance
	 */
	GSGeoServer geoserver
	
	/**
	 * Create a new GeoServer object
	 */
	GeoServer() {
		geoserver = GeoServerExtensions.bean("geoServer")	
	}
	
	/**
	 * Get a new Catalog
	 * @return The new Catalog
	 */
	Catalog getCatalog() {
		new Catalog()
	}
	
	/**
	 * Get the new Config
	 * @return The new Config
	 */
	Config getConfig() {
		new Config(geoserver.getGlobal())
	}
}