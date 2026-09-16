/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.SecretKey;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.security.KeyStoreFormat;
import org.geoserver.security.password.AesGcmCipher;
import org.geoserver.security.password.RandomPasswordProvider;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class KeyStoreProviderTest extends GeoServerSystemTestSupport {

    @Test
    public void testDerivedKeyFollowsTheStoredSecret() throws Exception {
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        String alias = "derivation-test-key";
        ksp.setSecretKey(alias, "a key long enough for derivation".toCharArray());
        ksp.storeKeyStore();

        SecretKey first = ksp.getDerivedKey(alias, AesGcmCipher::deriveKey);
        // making a key takes hundreds of milliseconds, so the same secret has to give the same object back
        assertSame(first, ksp.getDerivedKey(alias, AesGcmCipher::deriveKey));

        // a new secret under the same alias must not keep serving the key derived from the old one
        ksp.setSecretKey(alias, "a different key, also long enough".toCharArray());
        ksp.storeKeyStore();
        SecretKey afterKeyChange = ksp.getDerivedKey(alias, AesGcmCipher::deriveKey);
        assertFalse(Arrays.equals(first.getEncoded(), afterKeyChange.getEncoded()));
    }

    @Test
    public void testKeyStoreProvider() throws Exception {

        // System.setProperty(MasterPasswordProvider.DEFAULT_PROPERTY_NAME, "mymasterpw");
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        ksp.removeKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY);
        ksp.removeKey(ksp.aliasForGroupService("default"));
        ksp.storeKeyStore();
        ksp.reloadKeyStore();

        assertFalse(ksp.hasConfigPasswordKey());
        assertFalse(ksp.hasUserGroupKey("default"));

        ksp.setSecretKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY, "configKey".toCharArray());
        ksp.storeKeyStore();

        assertTrue(ksp.hasConfigPasswordKey());
        assertEquals("configKey", new String(ksp.getConfigPasswordKey()));
        assertFalse(ksp.hasUserGroupKey("default"));

        RandomPasswordProvider rpp = getSecurityManager().getRandomPassworddProvider();
        char[] urlKey = rpp.getRandomPasswordWithDefaultLength();
        // System.out.printf("Random password with length %d : %s\n",urlKey.length,new
        // String(urlKey));
        char[] urlKey2 = rpp.getRandomPasswordWithDefaultLength();
        // System.out.printf("Random password with length %d : %s\n",urlKey2.length,new
        // String(urlKey2));
        assertThat(urlKey, not(equalTo(urlKey2)));

        ksp.setSecretKey(
                KeyStoreProviderImpl.USERGROUP_PREFIX + "default" + KeyStoreProviderImpl.USERGROUP_POSTFIX,
                "defaultKey".toCharArray());

        ksp.storeKeyStore();

        assertTrue(ksp.hasConfigPasswordKey());
        assertEquals("configKey", new String(ksp.getConfigPasswordKey()));
        assertTrue(ksp.hasUserGroupKey("default"));
        assertEquals("defaultKey", new String(ksp.getUserGroupKey("default")));

        assertTrue(ksp.isKeyStorePassword(getSecurityManager().getMasterPassword()));
        assertFalse(ksp.isKeyStorePassword("blabla".toCharArray()));
    }

    /**
     * A keystore left under the name of another type must stop GeoServer, not be replaced by an empty one. Replacing it
     * loses the keys every stored password was encrypted with, and nothing says so at startup.
     */
    @Test
    public void testKeyStoreOfAnotherTypeStopsTheStartup() throws Exception {
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        Resource current = keyStore();
        Resource other = getSecurityManager()
                .security()
                .get(KeyStoreFormat.fileName(otherType().name()));
        byte[] saved = read(current);

        // one keystore, under the name of a type this installation is not configured for
        write(other, saved);
        current.delete();
        try {
            IOException e = assertThrows(IOException.class, ksp::reloadKeyStore);
            assertEquals(
                    "Key store " + other.path() + " holds the keys of this installation, but it is configured for "
                            + KeyStoreProviderImpl.keyStoreType() + ", read from " + current.path()
                            + ". Convert the file, or configure that type.",
                    e.getMessage());
        } finally {
            write(current, saved);
            other.delete();
            ksp.reloadKeyStore();
        }
    }

    /** The name says one type and the content says another: the content wins, and GeoServer stops. */
    @Test
    public void testKeyStoreContentOfAnotherTypeStopsTheStartup() throws Exception {
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        Resource current = keyStore();
        byte[] saved = read(current);

        // the first bytes of a keystore of another format, under the name this installation expects
        write(current, HEADERS.get(otherType()));
        try {
            IOException e = assertThrows(IOException.class, ksp::reloadKeyStore);
            assertEquals(
                    "Key store " + current.path() + " is a " + otherType() + " file, but this installation is "
                            + "configured for " + KeyStoreProviderImpl.keyStoreType()
                            + ". Convert the file, or configure that type.",
                    e.getMessage());
        } finally {
            write(current, saved);
            ksp.reloadKeyStore();
        }
    }

    /** First bytes of each format, so a test can write a keystore header without a provider for that format. */
    private static final Map<KeyStoreFormat, byte[]> HEADERS = Map.of(
            KeyStoreFormat.JCEKS, new byte[] {(byte) 0xCE, (byte) 0xCE, (byte) 0xCE, (byte) 0xCE},
            KeyStoreFormat.BCFKS, new byte[] {0x30, (byte) 0x82, 0x02, (byte) 0x9E});

    /** A format this installation is not configured for: BCFKS under FIPS is configured, so JCEKS is the other one. */
    private static KeyStoreFormat otherType() {
        KeyStoreFormat configured = KeyStoreFormat.valueOf(KeyStoreProviderImpl.keyStoreType());
        return configured == KeyStoreFormat.JCEKS ? KeyStoreFormat.BCFKS : KeyStoreFormat.JCEKS;
    }

    private Resource keyStore() throws Exception {
        getSecurityManager().getKeyStoreProvider().storeKeyStore();
        return getSecurityManager().security().get(KeyStoreProviderImpl.fileName());
    }

    private static byte[] read(Resource resource) throws IOException {
        try (InputStream in = resource.in()) {
            return in.readAllBytes();
        }
    }

    private static void write(Resource resource, byte[] content) throws IOException {
        try (OutputStream out = resource.out()) {
            out.write(content);
        }
    }

    /** The stored keys are random passwords, so the keystore must accept a key of any length. */
    @Test
    public void testRandomLengthKeyRoundTrips() throws Exception {
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        char[] key = getSecurityManager().getRandomPassworddProvider().getRandomPasswordWithDefaultLength();

        ksp.setSecretKey("randomLengthKey", key);
        ksp.storeKeyStore();
        ksp.reloadKeyStore();

        assertEquals(
                KeyStoreProviderImpl.KEY_ALGORITHM,
                ksp.getSecretKey("randomLengthKey").getAlgorithm());
        assertArrayEquals(
                key, SecurityUtils.toChars(ksp.getSecretKey("randomLengthKey").getEncoded()));
    }
}
