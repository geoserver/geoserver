/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Exercises the keystore re-encryption the migration-safe security REPLACE restore performs when both
 * {@code BK_SOURCE_MASTER_PASSWORD} and {@code BK_TARGET_MASTER_PASSWORD} are supplied: the exact
 * {@link KeyStoreProvider} {@code prepareForMasterPasswordChange -> commitMasterPasswordChange -> reloadKeyStore}
 * sequence, against the real keystore provider.
 *
 * <p>GeoServer deliberately does not expose the running master password to arbitrary code (it is package-private and
 * the REST accessor stack-checks its caller), so the test reaches it via reflection to drive the round-trip. The
 * cross-master-password restore case uses the identical API; only the password the source keystore is decrypted with
 * differs.
 */
public class KeystoreReencryptTest extends GeoServerSystemTestSupport {

    @Test
    public void testKeystoreReencryptRoundTripKeepsItReadable() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        GeoServerSecurityManager secMgr = getSecurityManager();
        KeyStoreProvider ksp = secMgr.getKeyStoreProvider();
        assertNotNull(ksp);

        Method getMasterPassword = GeoServerSecurityManager.class.getDeclaredMethod("getMasterPassword");
        getMasterPassword.setAccessible(true);
        char[] masterPw = (char[]) getMasterPassword.invoke(secMgr);

        // Re-encrypt the keystore to the (same) master password: the exact prepare -> commit -> reload sequence the
        // restore uses to re-key a foreign keystore, run here against the live keystore provider.
        ksp.prepareForMasterPasswordChange(masterPw, masterPw);
        ksp.commitMasterPasswordChange();
        ksp.reloadKeyStore();

        // the keystore is still in place and the whole security subsystem still loads from it
        assertTrue(ksp.getResource().getType() != Resource.Type.UNDEFINED);
        secMgr.reload();
    }
}
