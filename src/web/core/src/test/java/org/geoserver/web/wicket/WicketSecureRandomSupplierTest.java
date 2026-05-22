/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WicketSecureRandomSupplierTest {

    @Test
    void testGetSecureRandom() {
        WicketSecureRandomSupplier supplier = new WicketSecureRandomSupplier();

        assertNotNull(supplier.getRandom());
    }

    @Test
    void shouldFallbackWhenDRBGNotAvailable() {
        try (MockedStatic<SecureRandom> mocked = mockStatic(SecureRandom.class)) {
            mocked.when(() -> SecureRandom.getInstance("DRBG"))
                    .thenThrow(new NoSuchAlgorithmException("DRBG not available"));

            assertNotNull(WicketSecureRandomSupplier.createSecureRandom());
        }
    }
}
