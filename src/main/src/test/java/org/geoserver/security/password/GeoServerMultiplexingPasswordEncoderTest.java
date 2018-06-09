package org.geoserver.security.password;

import static org.junit.Assert.fail;

import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;

public class GeoServerMultiplexingPasswordEncoderTest extends GeoServerMockTestSupport {

    @Test
    public void testEncode() {
        GeoServerMultiplexingPasswordEncoder pwe =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());
        try {
            pwe.encodePassword("foo", null);
        } catch (Exception e) {
            fail("Multiplexing encoder should be capabile of encoding");
        }
    }
}
