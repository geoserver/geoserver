/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class GeoServerDigestPasswordEncoderTest {

    @Test
    void testEncodePassword() {
        PasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        String hasshedPassword = passwordEncoder.encode("password");
        assertNotEquals("password", hasshedPassword);

        assertTrue(passwordEncoder.matches("password", hasshedPassword));
    }

    @Test
    void testNullPassword() {
        PasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        String hasshedPassword = passwordEncoder.encode(null);

        assertNotNull(hasshedPassword);

        assertTrue(passwordEncoder.matches(null, hasshedPassword));
    }

    @Test
    void testEmptyPassword() {
        PasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        String hasshedPassword = passwordEncoder.encode("");
        assertNotEquals("", hasshedPassword);

        assertTrue(passwordEncoder.matches("", hasshedPassword));
    }
}
