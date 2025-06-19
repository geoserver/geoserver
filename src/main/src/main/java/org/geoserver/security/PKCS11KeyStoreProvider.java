/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * PKCS#11-backed KeyStore Provider for GeoServer.
 *
 * <p>This implementation uses PKCS#11 (Hardware Security Module) for keystore operations. Requires proper PKCS#11
 * configuration in java.security.
 *
 * @author christian
 */
public class PKCS11KeyStoreProvider extends KeyStoreProviderImpl {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    private String pkcs11ConfigPath;
    private String slotId;

    public PKCS11KeyStoreProvider() {
        super();
    }

    /** Set the PKCS#11 configuration file path */
    public void setPkcs11ConfigPath(String pkcs11ConfigPath) {
        this.pkcs11ConfigPath = pkcs11ConfigPath;
    }

    /** Set the PKCS#11 slot ID */
    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    @Override
    protected String getKeystoreType() {
        return PKCS11_KEYSTORETYPE;
    }

    @Override
    protected String getKeystoreProvider() {
        // Initialize PKCS#11 provider if not already done
        initializePKCS11Provider();
        return PKCS11_PROVIDER;
    }

    private void initializePKCS11Provider() {
        try {
            // Check if PKCS#11 provider is already registered
            if (Security.getProvider(PKCS11_PROVIDER) == null) {
                // Configure PKCS#11 provider
                if (pkcs11ConfigPath != null) {
                    System.setProperty("sun.security.pkcs11.config", pkcs11ConfigPath);
                }

                // Register PKCS#11 provider using reflection to avoid module access issues
                try {
                    Class<?> sunPKCS11Class = Class.forName("sun.security.pkcs11.SunPKCS11");
                    Object pkcs11Provider =
                            sunPKCS11Class.getDeclaredConstructor().newInstance();
                    Security.addProvider((java.security.Provider) pkcs11Provider);
                    LOGGER.info("PKCS#11 provider initialized successfully");
                } catch (ClassNotFoundException e) {
                    LOGGER.warning("PKCS#11 provider not available - ensure jdk.crypto.cryptoki module is accessible");
                    throw new RuntimeException("PKCS#11 provider not available", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize PKCS#11 provider", e);
            throw new RuntimeException("PKCS#11 initialization failed", e);
        }
    }

    @Override
    public void reloadKeyStore() throws IOException {
        // For PKCS#11, we need to reinitialize the provider
        initializePKCS11Provider();
        super.reloadKeyStore();
    }
}
