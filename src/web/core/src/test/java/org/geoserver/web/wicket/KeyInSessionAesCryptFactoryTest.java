/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyInSessionAesCryptFactoryTest {

    private static final String URL = "/web/wicket/bookmarkable/org.geoserver.web.demo.MapPreviewPage?0";

    private WicketTester tester;
    private KeyInSessionAesCryptFactory factory;

    @BeforeEach
    void startApplication() {
        tester = new WicketTester(new MockApplication());
        factory = new KeyInSessionAesCryptFactory();
    }

    @AfterEach
    void stopApplication() {
        tester.destroy();
    }

    @Test
    void testRoundTrip() {
        ICrypt crypt = factory.newCrypt();

        String encrypted = crypt.encryptUrlSafe(URL);

        assertNotEquals(URL, encrypted);
        assertEquals(URL, crypt.decryptUrlSafe(encrypted));
    }

    /** The key lives in the session, so the same URL encrypts differently for two visitors. */
    @Test
    void testKeyDiffersPerSession() {
        String first = factory.newCrypt().encryptUrlSafe(URL);

        tester.getSession().invalidateNow();
        String second = factory.newCrypt().encryptUrlSafe(URL);

        assertNotEquals(first, second);
    }

    /** Inside one session the text has to repeat, or Wicket redirects the browser without end. */
    @Test
    void testSameSessionGivesSameText() {
        assertEquals(factory.newCrypt().encryptUrlSafe(URL), factory.newCrypt().encryptUrlSafe(URL));
    }
}
