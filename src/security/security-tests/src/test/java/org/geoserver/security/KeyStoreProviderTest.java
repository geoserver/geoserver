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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import javax.crypto.SecretKey;
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
