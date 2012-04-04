package org.geoserver.security;

import org.geoserver.security.password.RandomPasswordProvider;
import org.geoserver.test.GeoServerTestSupport;

public class KeyStoreProviderTest extends GeoServerTestSupport {

    public void testKeyStoreProvider() throws Exception {
        
        //System.setProperty(MasterPasswordProvider.DEFAULT_PROPERTY_NAME, "mymasterpw");
        KeyStoreProvider ksp = getSecurityManager().getKeyStoreProvider();
        ksp.removeKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY);
        ksp.removeKey(KeyStoreProviderImpl.URLPARAMKEY);
        ksp.removeKey(ksp.aliasForGroupService("default"));
        ksp.storeKeyStore();
        ksp.reloadKeyStore();
        
        assertFalse(ksp.hasConfigPasswordKey());
        assertFalse(ksp.hasUrlParamKey());
        assertFalse(ksp.hasUserGroupKey("default"));
        
                        
        ksp.setSecretKey( KeyStoreProviderImpl.CONFIGPASSWORDKEY, "configKey".toCharArray());
        ksp.storeKeyStore();
        
        assertTrue(ksp.hasConfigPasswordKey());
        assertEquals("configKey",new String(ksp.getConfigPasswordKey()));
        assertFalse(ksp.hasUrlParamKey());
        assertFalse(ksp.hasUserGroupKey("default"));
        
        RandomPasswordProvider rpp = getSecurityManager().getRandomPassworddProvider();
        char[] urlKey = rpp.getRandomPasswordWithDefaultLength();
        System.out.printf("Random password with length %d : %s\n",urlKey.length,new String(urlKey));
        char[] urlKey2 = rpp.getRandomPasswordWithDefaultLength();
        System.out.printf("Random password with length %d : %s\n",urlKey2.length,new String(urlKey2));
        assertFalse(urlKey.equals(urlKey2));

        ksp.setSecretKey( KeyStoreProviderImpl.URLPARAMKEY, urlKey);
        ksp.setSecretKey( KeyStoreProviderImpl.USERGROUP_PREFIX+"default"+
                    KeyStoreProviderImpl.USERGROUP_POSTFIX, "defaultKey".toCharArray());

        ksp.storeKeyStore();
        
        assertTrue(ksp.hasConfigPasswordKey());
        assertEquals("configKey",new String(ksp.getConfigPasswordKey()));
        assertTrue(ksp.hasUrlParamKey());
        assertEquals(new String(urlKey),new String(ksp.getUrlParamKey()));
        assertTrue(ksp.hasUserGroupKey("default"));
        assertEquals("defaultKey",new String(ksp.getUserGroupKey("default")));
        
        assertTrue(ksp.isKeyStorePassword(
            getSecurityManager().getMasterPassword()));
        assertFalse(ksp.isKeyStorePassword( "blabla".toCharArray()));
    }
        
}
