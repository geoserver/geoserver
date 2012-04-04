package org.geoserver.security.password;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.geoserver.security.GeoServerSecurityTestSupport;

public class URLMasterPasswordProviderTest extends GeoServerSecurityTestSupport {

    public void testEncryption() throws Exception {
        File tmp = File.createTempFile("passwd", "tmp", new File("target"));
        tmp = tmp.getCanonicalFile();

        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("test");
        config.setReadOnly(false);
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        config.setURL(tmp.toURI().toURL());
        config.setEncrypting(true);

        URLMasterPasswordProvider mpp = new URLMasterPasswordProvider();
        mpp.setSecurityManager(getSecurityManager());
        mpp.initializeFromConfig(config);
        mpp.setName(config.getName());
        mpp.doSetMasterPassword("geoserver".toCharArray());

        String encoded = IOUtils.toString(new FileInputStream(tmp));
        assertFalse("geoserver".equals(encoded));

        char[] passwd = mpp.doGetMasterPassword();
        assertTrue(Arrays.equals("geoserver".toCharArray(), passwd));
    }
}
