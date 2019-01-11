/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

import org.geoserver.security.password.RandomPasswordProvider;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class KeyStoreProviderTest extends GeoServerSystemTestSupport {

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
                KeyStoreProviderImpl.USERGROUP_PREFIX
                        + "default"
                        + KeyStoreProviderImpl.USERGROUP_POSTFIX,
                "defaultKey".toCharArray());

        ksp.storeKeyStore();

        assertTrue(ksp.hasConfigPasswordKey());
        assertEquals("configKey", new String(ksp.getConfigPasswordKey()));
        assertTrue(ksp.hasUserGroupKey("default"));
        assertEquals("defaultKey", new String(ksp.getUserGroupKey("default")));

        assertTrue(ksp.isKeyStorePassword(getSecurityManager().getMasterPassword()));
        assertFalse(ksp.isKeyStorePassword("blabla".toCharArray()));
    }
}
