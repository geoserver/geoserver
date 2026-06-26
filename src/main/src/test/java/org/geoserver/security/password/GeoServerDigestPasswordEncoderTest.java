/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.codec.binary.Base64;
import org.jasypt.digest.StandardByteDigester;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.Test;

public class GeoServerDigestPasswordEncoderTest {

    @Test
    public void testEmptyPassword() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encodePassword("", null);
        assertNotEquals("", encoded);
        assertTrue(encoded.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "", null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password", null));
    }

    @Test
    public void testStringEncoder() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encodedPassword = passwordEncoder.encodePassword("password", null);
        assertNotEquals("password", encodedPassword);
        assertTrue(encodedPassword.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encodedPassword, "password", null));
        assertFalse(passwordEncoder.isPasswordValid(encodedPassword, "wrong_password", null));
    }

    @Test
    public void testStringEncoderExoticPassword() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encodedPassword = passwordEncoder.encodePassword("øæåñüéöàçдя", null);
        assertNotEquals("øæåñüéöàçдя", encodedPassword);
        assertTrue(encodedPassword.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encodedPassword, "øæåñüéöàçдя", null));
        assertFalse(passwordEncoder.isPasswordValid(encodedPassword, "wrong_password", null));
    }

    @Test
    public void testByteArrayEncoder() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encodePassword("geoserver".toCharArray(), null);
        assertNotEquals("geoserver", encoded);
        assertTrue(encoded.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "geoserver".toCharArray(), null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password".toCharArray(), null));
    }

    @Test
    public void testByteArrayEncoderExoticPassword() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encodePassword("øæåñüéöàçдя".toCharArray(), null);
        assertNotEquals("øæåñüéöàçдя", encoded);
        assertTrue(encoded.startsWith("digest1:"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "øæåñüéöàçдя".toCharArray(), null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password".toCharArray(), null));
    }

    @Test
    public void testEncode() {
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
    public void testEncodeExoticPassword() {
        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        String encoded = passwordEncoder.encode("øæåñüéöàçдя");
        assertNotEquals("øæåñüéöàçдя", encoded);
        assertNotNull(encoded);
        assertFalse(encoded.contains("digest1"));

        assertTrue(passwordEncoder.isPasswordValid(encoded, "øæåñüéöàçдя".toCharArray(), null));
        assertFalse(passwordEncoder.isPasswordValid(encoded, "wrong_password".toCharArray(), null));
    }

    @Test
    public void testJasyptCompatibility() {
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

    @Test
    public void testJasyptStringEncoderDecomposedUnicode() {
        // Old string path went through StrongPasswordEncryptor -> StandardStringDigester,
        // which NFC-normalizes the message before digesting. A password typed in decomposed
        // form was stored as if precomposed. The string encoder must keep normalizing so such
        // a stored password still validates. The char[] path is intentionally not covered (master
        // password digest relies on it staying un-normalized).
        String decomposed = "cafe\u0301"; // 'e' + combining acute (NFD form of "cafe-acute")

        StrongPasswordEncryptor oldEncryptor = new StrongPasswordEncryptor();
        String stored = oldEncryptor.encryptPassword(decomposed);

        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        assertTrue(passwordEncoder.isPasswordValid(stored, decomposed, null));
    }

    @Test
    public void testJasyptCompatibilityExoticPassword() {
        StandardByteDigester jasyptDigester = new StandardByteDigester();
        jasyptDigester.setAlgorithm("SHA-256");
        jasyptDigester.setIterations(100000);
        jasyptDigester.setSaltSizeBytes(16);
        jasyptDigester.initialize();

        String jasyptEncoded = new String(Base64.encodeBase64(jasyptDigester.digest("øæåñüéöàçдя".getBytes())));

        GeoServerDigestPasswordEncoder passwordEncoder = new GeoServerDigestPasswordEncoder();
        passwordEncoder.setPrefix("digest1");

        assertTrue(passwordEncoder.isPasswordValid(jasyptEncoded, "øæåñüéöàçдя", null));
        assertTrue(passwordEncoder.isPasswordValid(jasyptEncoded, "øæåñüéöàçдя".toCharArray(), null));

        assertFalse(passwordEncoder.isPasswordValid(jasyptEncoded, "wrong_password", null));
        assertFalse(passwordEncoder.isPasswordValid(jasyptEncoded, "wrong_password".toCharArray(), null));
    }
}
