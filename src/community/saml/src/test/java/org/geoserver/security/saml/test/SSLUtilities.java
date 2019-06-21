/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public final class SSLUtilities {

    public static void registerKeyStore(String keyStoreName) {
        try {
            ClassLoader classLoader = SSLUtilities.class.getClassLoader();
            InputStream keyStoreInputStream = classLoader.getResourceAsStream(keyStoreName);
            if (keyStoreInputStream == null) {
                throw new FileNotFoundException(
                        "Could not find file named '" + keyStoreName + "' in the CLASSPATH");
            }

            // load the keystore
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(keyStoreInputStream, null);

            // add to known keystore
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            // default SSL connections are initialized with the keystore above
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, null);
            SSLContext.setDefault(sc);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
