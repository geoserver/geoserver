package org.geoserver.catalog;

import org.geoserver.test.GeoServerTestSupport;

import junit.framework.Test;

public class StylesTest extends GeoServerTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new StylesTest());
    }

    public void testLookup() throws Exception {
        assertTrue(
            Styles.lookupHandler(SLD10Handler.FORMAT, SLD10Handler.VERSION) instanceof SLD10Handler);
        assertTrue(
            Styles.lookupHandler(SLD11Handler.FORMAT, SLD11Handler.VERSION) instanceof SLD11Handler);
        assertTrue(
            Styles.lookupHandler(SLD10Handler.FORMAT, null) instanceof SLD10Handler);
        assertTrue(
            Styles.lookupHandler(SLD11Handler.FORMAT, null) instanceof SLD10Handler);
        try {
            Styles.lookupHandler(null, null);
            fail();
        }
        catch(Exception e) {}

        try {
            Styles.lookupHandler("foo", null);
            fail();
        }
        catch(Exception e) {}
    }
}
