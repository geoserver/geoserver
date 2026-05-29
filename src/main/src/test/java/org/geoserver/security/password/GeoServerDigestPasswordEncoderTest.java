/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.codec.binary.Base64;
import org.jasypt.digest.StandardByteDigester;
import org.junit.jupiter.api.Test;

class GeoServerDigestPasswordEncoderTest {

    @Test
    void testEmptyPassword() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encodePassword("", null);
        assertNotEquals("", encoded);
        assertTrue(encoded.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "", null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password", null));
    }

    @Test
    void testStringEncoder() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encodedPassword = passwordEncoder.encodePassword("password", null);
        assertNotEquals("password", encodedPassword);
        assertTrue(encodedPassword.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encodedPassword, "password", null));
        assertFalse(passwordEncoder.isPasswordValid(encodedPassword, "wrong_password", null));
    }

    @Test
    void testByteArrayEncoder() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encodePassword("geoserver".toCharArray(), null);
        assertNotEquals("geoserver", encoded);
        assertTrue(encoded.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "geoserver".toCharArray(), null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password".toCharArray(), null));
    }

    @Test
    void testEncode() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encode("geoserver");
        assertNotEquals("geoserver", encoded);
        assertNotNull(encoded);
        assertFalse(encoded.contains("digest1"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "geoserver".toCharArray(), null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password".toCharArray(), null));
    }

    @Test
    void testJasyptCompatibility() {
        StandardByteDigester jasyptDigester = new StandardByteDigester();
        jasyptDigester.setAlgorithm("SHA-256");
        jasyptDigester.setIterations(100000);
        jasyptDigester.setSaltSizeBytes(16);
        jasyptDigester.initialize();

        String jasyptEncoded = new String(Base64.encodeBase64(jasyptDigester.digest("password".getBytes())));

        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        assertTrue(passwordEncoder.isPasswordValid(jasyptEncoded, "password", null));
        assertTrue(passwordEncoder.isPasswordValid(jasyptEncoded, "password".toCharArray(), null));

        assertFalse(passwordEncoder.isPasswordValid(jasyptEncoded, "wrong_password", null));
        assertFalse(passwordEncoder.isPasswordValid(jasyptEncoded, "wrong_password".toCharArray(), null));
    }
}
