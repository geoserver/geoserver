/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;
import static org.geoserver.security.password.URLMasterPasswordProviderException.URL_LOCATION_NOT_READABLE;
import static org.geoserver.security.password.URLMasterPasswordProviderException.URL_REQUIRED;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * Master password provider that retrieves and optionally stores the master password from a url.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public final class URLMasterPasswordProvider extends AbstractURLMasterPasswordProvider {

    @Override
    protected byte[] encrypt(char[] passwd) {
        char[] key = key();
        try {
            return Base64.encodeBase64(encryptor(key).encrypt(toBytes(passwd)));
        } finally {
            scramble(key);
        }
    }

    @Override
    protected byte[] decrypt(byte[] passwd) {
        char[] key = key();
        try {
            return encryptor(key).decrypt(Base64.decodeBase64(passwd));
        } finally {
            scramble(key);
        }
    }

    /** Reading and writing have to be set up the same way, or what one writes the other cannot read. */
    private static StandardPBEByteEncryptor encryptor(char[] key) {
        StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
        encryptor.setPasswordCharArray(key);
        encryptor.setSaltGenerator(SecureRandomGenerator.INSTANCE);
        return encryptor;
    }

    public static class URLMasterPasswordProviderValidator extends SecurityConfigValidator {

        public URLMasterPasswordProviderValidator(GeoServerSecurityManager securityManager) {
            super(securityManager);
        }

        @Override
        public void validate(MasterPasswordProviderConfig config) throws SecurityConfigException {
            super.validate(config);

            URLMasterPasswordProviderConfig urlConfig = (URLMasterPasswordProviderConfig) config;
            URL url = urlConfig.getURL();

            if (url == null) {
                throw new URLMasterPasswordProviderException(URL_REQUIRED);
            }

            if (config.isReadOnly()) {
                // read-only, assure we can read from url
                try {
                    try (InputStream in =
                            input(url, manager.masterPasswordProvider().get(config.getName()))) {
                        in.read();
                    }
                } catch (IOException ex) {
                    throw new URLMasterPasswordProviderException(URL_LOCATION_NOT_READABLE, url);
                }
            }
        }
    }

    public static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public void configure(XStreamPersister xp) {
            super.configure(xp);
            xp.getXStream().alias("urlProvider", URLMasterPasswordProviderConfig.class);
        }

        @Override
        public Class<? extends MasterPasswordProvider> getMasterPasswordProviderClass() {
            return URLMasterPasswordProvider.class;
        }

        @Override
        public MasterPasswordProvider createMasterPasswordProvider(MasterPasswordProviderConfig config)
                throws IOException {
            return new URLMasterPasswordProvider();
        }

        @Override
        public SecurityConfigValidator createConfigurationValidator(GeoServerSecurityManager securityManager) {
            return new URLMasterPasswordProviderValidator(securityManager);
        }
    }
}
