package org.geoserver.security.password;

import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;

import static org.junit.Assert.fail;

public class GeoServerMultiplexingPasswordEncoderTest
    extends GeoServerMockTestSupport {

    @Test
    public void testEncode() {
        GeoServerMultiplexingPasswordEncoder pwe =
            new GeoServerMultiplexingPasswordEncoder(getSecurityManager());
        try {
            pwe.encodePassword("foo", null);
        }
        catch(Exception e) {
            fail("Multiplexing encoder should be capabile of encoding");
        }
    }
}
