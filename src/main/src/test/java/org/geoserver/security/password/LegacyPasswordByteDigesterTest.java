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

        String encoded = simpleByteDigester.encode("geoserver".getBytes());

        assertNotEquals("geoserver", encoded);
        assertTrue(simpleByteDigester.matches("geoserver".getBytes(), encoded));
        assertFalse(simpleByteDigester.matches("password".getBytes(), encoded));
    }

    @Test
    void testEmptyPassword() {
        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        String encoded = simpleByteDigester.encode("".getBytes());

        assertNotNull(encoded);
        assertTrue(simpleByteDigester.matches("".getBytes(), encoded));
        assertFalse(simpleByteDigester.matches("password".getBytes(), encoded));
    }

    @Test
    void testJasyptCompatibility() {
        StandardByteDigester jasyptDigester = new StandardByteDigester();
        jasyptDigester.setAlgorithm("SHA-256");
        jasyptDigester.setIterations(100000);
        jasyptDigester.setSaltSizeBytes(16);
        jasyptDigester.initialize();

        String jasyptEncoded = new String(Base64.encodeBase64(jasyptDigester.digest("geoserver".getBytes())));

        LegacyPasswordByteDigester simpleByteDigester = new LegacyPasswordByteDigester();

        assertTrue(simpleByteDigester.matches("geoserver".getBytes(), jasyptEncoded));
    }
}
