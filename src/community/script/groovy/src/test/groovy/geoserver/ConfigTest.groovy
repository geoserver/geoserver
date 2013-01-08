package geoserver;

import static org.junit.Assert.*;

import org.geoserver.test.GeoServerTestSupport;

class ConfigTest extends GeoServerTestSupport {

	void testConstructor() {
		def gs = new GeoServer()
		assertNotNull(gs.geoserver)
		def config = new Config(gs.geoserver.global)
		assertNotNull(config)
		assertNotNull(config.geoServerInfo)
		println config['contact']
	}
	
}
