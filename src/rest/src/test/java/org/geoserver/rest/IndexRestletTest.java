package org.geoserver.rest;

import org.geoserver.test.GeoServerTestSupport;

public class IndexRestletTest extends GeoServerTestSupport {

    public void test() throws Exception {
        getAsDOM( "/rest");
    }
}
