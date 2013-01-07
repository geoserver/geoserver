package geoserver

import static org.junit.Assert.*
import org.geoserver.test.GeoServerTestSupport

class GeoServerTest extends GeoServerTestSupport {

	void testConstructor() {
		def gs = new GeoServer()
		assertNotNull(gs.geoserver)
	}

	void testGetConfig() {
		def gs = new GeoServer()
		assertNotNull(gs.config)
	}
	
	void testGetCatalog() {
		def gs = new GeoServer()
		assertNotNull(gs.catalog)
	}
}
