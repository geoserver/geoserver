/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import javax.crypto.spec.SecretKeySpec;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.security.KeyStoreFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Checks that GeoServer recognizes a keystore from its first bytes, whatever the file is called. */
public class KeyStoreFormatTest {

    private static final char[] PASSWORD = "geoserver".toCharArray();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testFileNameFollowsType() {
        assertEquals("geoserver.jceks", KeyStoreFormat.fileName("JCEKS"));
        assertEquals("geoserver.bcfks", KeyStoreFormat.fileName("BCFKS"));
    }

    @Test
    public void testDetectsJceksWhateverTheNameIs() throws Exception {
        assumeAvailable("JCEKS");
        assertEquals(KeyStoreFormat.JCEKS, KeyStoreFormat.detect(keyStore("JCEKS", "misnamed.bcfks")));
    }

    @Test
    public void testDetectsBcfks() throws Exception {
        CryptoProviders.getProvider();
        assumeAvailable("BCFKS");
        assertEquals(KeyStoreFormat.BCFKS, KeyStoreFormat.detect(keyStore("BCFKS", "geoserver.bcfks")));
    }

    /** JKS holds no secret key, so GeoServer never writes one, but a file left behind still has to be recognized. */
    @Test
    public void testDetectsJks() throws Exception {
        assumeAvailable("JKS");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, PASSWORD);
        Resource resource = resource("geoserver.jks");
        try (OutputStream out = resource.out()) {
            keyStore.store(out, PASSWORD);
        }
        assertEquals(KeyStoreFormat.JKS, KeyStoreFormat.detect(resource));
    }

    @Test
    public void testMissingFileHasNoFormat() throws Exception {
        assertNull(KeyStoreFormat.detect(resource("absent.jceks")));
    }

    @Test
    public void testUnknownContentHasNoFormat() throws Exception {
        Resource resource = resource("geoserver.jceks");
        try (OutputStream out = resource.out()) {
            out.write("not a keystore at all".getBytes(StandardCharsets.UTF_8));
        }
        assertNull(KeyStoreFormat.detect(resource));
    }

    /** A file shorter than the header of any format, which must not be reported as one. */
    @Test
    public void testTooShortFileHasNoFormat() throws Exception {
        Resource resource = resource("geoserver.bcfks");
        try (OutputStream out = resource.out()) {
            out.write(new byte[] {(byte) 0x30});
        }
        assertNull(KeyStoreFormat.detect(resource));
    }

    /** A FIPS runtime has no JCEKS, and a test that cannot write the format has nothing to detect. */
    private static void assumeAvailable(String type) {
        try {
            KeyStore.getInstance(type);
        } catch (KeyStoreException e) {
            assumeNoException(e);
        }
    }

    private Resource keyStore(String type, String fileName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null, PASSWORD);
        keyStore.setEntry(
                "alias",
                new KeyStore.SecretKeyEntry(new SecretKeySpec(new byte[32], "AES")),
                new KeyStore.PasswordProtection(PASSWORD));
        Resource resource = resource(fileName);
        try (OutputStream out = resource.out()) {
            keyStore.store(out, PASSWORD);
        }
        return resource;
    }

    private Resource resource(String fileName) {
        return Files.asResource(new File(folder.getRoot(), fileName));
    }
}
