package geoserver

import org.geoserver.config.GeoServerInfo

/**
 * The Global GeoServer Configuration.
 * @author Jared Erickson
 */
class Config {
	
	/**
	 * The GeoServerInfo instance
	 */
	@Delegate
	private GeoServerInfo geoServerInfo
	
	/**
	 * Create a new Config wrapping a GeoServerInfo instance
	 * @param geoServerInfo The GeoServerInfo
	 */
	Config(GeoServerInfo geoServerInfo) {
		this.geoServerInfo = geoServerInfo
	}
}