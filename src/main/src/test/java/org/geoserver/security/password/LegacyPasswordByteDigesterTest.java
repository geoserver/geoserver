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

class LegacyPasswordByteDigesterTest {

    @Test
    void testEncodeDecode() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = simpleByteDigester.encode("password".getBytes());

        assertNotEquals("password", encoded);
        assertTrue(simpleByteDigester.matches("password".getBytes(), encoded));
        assertFalse(simpleByteDigester.matches("wrong_password".getBytes(), encoded));
    }

    @Test
    void testEmptyPassword() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = simpleByteDigester.encode("".getBytes());

        assertNotNull(encoded);
        assertTrue(simpleByteDigester.matches("".getBytes(), encoded));
        assertFalse(simpleByteDigester.matches("wrong_password".getBytes(), encoded));
    }

    @Test
    void testNullPassword() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = simpleByteDigester.encode("".getBytes());

        assertFalse(simpleByteDigester.matches("".getBytes(), null));
        assertFalse(simpleByteDigester.matches(null, encoded));
    }

    @Test
    void testWrongEncodedPassword() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = "not_A_Byte_Encoded_Password";

        assertFalse(simpleByteDigester.matches("password".getBytes(), encoded));
    }

    @Test
    void testToShortEncodedPassword() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = "Na";

        assertFalse(simpleByteDigester.matches("password".getBytes(), encoded));
    }

    @Test
    void testJasyptCompatibility() {
        StandardByteDigester jasyptDigester = new StandardByteDigester();
        jasyptDigester.setAlgorithm("SHA-256");
        jasyptDigester.setIterations(100000);
        jasyptDigester.setSaltSizeBytes(16);
        jasyptDigester.initialize();

        String jasyptEncoded = new String(Base64.encodeBase64(jasyptDigester.digest("password".getBytes())));

        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        assertTrue(simpleByteDigester.matches("password".getBytes(), jasyptEncoded));
        assertFalse(simpleByteDigester.matches("wrong_password".getBytes(), jasyptEncoded));
    }
}
