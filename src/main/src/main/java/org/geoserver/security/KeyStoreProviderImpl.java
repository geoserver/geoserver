/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.geoserver.security.SecurityUtils.toBytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.password.RandomPasswordProvider;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Class for GeoServer-specific key management using BCFKS keystore.
 *
 * <p><strong>Requires a master password</strong> from {@link MasterPasswordProviderImpl}.
 *
 * <p>The keystore type is BCFKS (Bouncy Castle FIPS Keystore) for FIPS compliance.
 *
 * @author christian
 */
public class KeyStoreProviderImpl implements BeanNameAware, KeyStoreProvider {
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    // Define static fields first to avoid forward reference
    public static final String DEFAULT_BEAN_NAME = "DefaultKeyStoreProvider";
    public static final String DEFAULT_FILE_NAME = "geoserver.bcfks";
    public static final String PREPARED_FILE_NAME = "geoserver.bcfks.new";
    public static final String CONFIGPASSWORDKEY = "config:password:key";
    public static final String URLPARAMKEY = "url:param:key";
    public static final String USERGROUP_PREFIX = "ug:";
    public static final String USERGROUP_POSTFIX = ":key";
    public static final String KEYSTORETYPE = "BCFKS";
    public static final String PROVIDER = "BCFIPS";
    public static final String PKCS11_KEYSTORETYPE = "PKCS11";
    public static final String PKCS11_PROVIDER = "SunPKCS11";

    // Instance fields
    protected String name;
    protected Resource keyStoreResource;
    protected KeyStore ks;
    protected GeoServerSecurityManager securityManager;

    // Static initializer
    static {
        try {
            if (Security.getProvider(PROVIDER) == null) {
                Security.addProvider(new BouncyCastleFipsProvider());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize BCFIPS provider", e);
            throw new RuntimeException("BCFIPS initialization failed", e);
        }
    }

    public KeyStoreProviderImpl() {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public GeoServerSecurityManager getSecurityManager() {
        return securityManager;
    }

    @Override
    public Resource getResource() {
        if (keyStoreResource == null) {
            keyStoreResource = securityManager.security().get(DEFAULT_FILE_NAME);
        }
        return keyStoreResource;
    }

    @Override
    public void reloadKeyStore() throws IOException {
        ks = null;
        assertActivatedKeyStore();
    }

    @Override
    public Key getKey(String alias) throws IOException {
        assertActivatedKeyStore();
        try {
            char[] passwd = securityManager.getMasterPassword();
            try {
                return ks.getKey(alias, passwd);
            } finally {
                securityManager.disposePassword(passwd);
            }
        } catch (Exception e) {
            throw new IOException("Failed to get key for alias: " + alias, e);
        }
    }

    @Override
    public byte[] getConfigPasswordKey() throws IOException {
        SecretKey key = getSecretKey(CONFIGPASSWORDKEY);
        return key != null ? key.getEncoded() : null;
    }

    @Override
    public boolean hasConfigPasswordKey() throws IOException {
        return containsAlias(CONFIGPASSWORDKEY);
    }

    @Override
    public boolean containsAlias(String alias) throws IOException {
        assertActivatedKeyStore();
        try {
            return ks.containsAlias(alias);
        } catch (KeyStoreException e) {
            throw new IOException("Failed to check alias: " + alias, e);
        }
    }

    @Override
    public byte[] getUserGroupKey(String serviceName) throws IOException {
        SecretKey key = getSecretKey(aliasForGroupService(serviceName));
        return key != null ? key.getEncoded() : null;
    }

    @Override
    public boolean hasUserGroupKey(String serviceName) throws IOException {
        return containsAlias(aliasForGroupService(serviceName));
    }

    @Override
    public SecretKey getSecretKey(String name) throws IOException {
        Key key = getKey(name);
        if (key == null) return null;
        if (!(key instanceof SecretKey)) throw new IOException("Invalid key type for: " + name);
        return (SecretKey) key;
    }

    @Override
    public PublicKey getPublicKey(String name) throws IOException {
        Key key = getKey(name);
        if (key == null) return null;
        if (!(key instanceof PublicKey)) throw new IOException("Invalid key type for: " + name);
        return (PublicKey) key;
    }

    @Override
    public PrivateKey getPrivateKey(String name) throws IOException {
        Key key = getKey(name);
        if (key == null) return null;
        if (!(key instanceof PrivateKey)) throw new IOException("Invalid key type for: " + name);
        return (PrivateKey) key;
    }

    @Override
    public String aliasForGroupService(String serviceName) {
        StringBuilder buff = new StringBuilder(USERGROUP_PREFIX);
        buff.append(serviceName).append(USERGROUP_POSTFIX);
        return buff.toString();
    }

    protected void assertActivatedKeyStore() throws IOException {
        if (ks != null) return;

        char[] passwd = securityManager.getMasterPassword();
        try {
            // Determine keystore type and provider
            String keystoreType = getKeystoreType();
            String provider = getKeystoreProvider();

            ks = KeyStore.getInstance(keystoreType, provider);
            if (getResource().getType() == Type.UNDEFINED) { // Create empty keystore
                ks.load(null, passwd);
                addInitialKeys();
                try (OutputStream fos = getResource().out()) {
                    ks.store(fos, passwd);
                }
            } else {
                try (InputStream fis = getResource().in()) {
                    ks.load(fis, passwd);
                }
            }
        } catch (KeyStoreException | NoSuchProviderException | IOException | NoSuchAlgorithmException ex) {
            throw new IOException("Failed to activate keystore", ex);
        } catch (Exception ex) {
            throw new IOException("Unexpected error activating keystore", ex);
        } finally {
            securityManager.disposePassword(passwd);
        }
    }

    /** Get the keystore type to use. Defaults to BCFKS, can be overridden for PKCS#11. */
    protected String getKeystoreType() {
        return KEYSTORETYPE;
    }

    /** Get the keystore provider to use. Defaults to BCFIPS, can be overridden for PKCS#11. */
    protected String getKeystoreProvider() {
        return PROVIDER;
    }

    @Override
    public boolean isKeyStorePassword(char[] password) throws IOException {
        if (password == null) return false;
        assertActivatedKeyStore();

        KeyStore testStore;
        try {
            String keystoreType = getKeystoreType();
            String provider = getKeystoreProvider();
            testStore = KeyStore.getInstance(keystoreType, provider);
        } catch (KeyStoreException | NoSuchProviderException e) {
            throw new IOException("Failed to instantiate keystore", e);
        }
        try (InputStream fis = getResource().in()) {
            testStore.load(fis, password);
        } catch (IOException e) {
            return false; // Invalid password
        } catch (Exception e) {
            throw new IOException("Unexpected error validating keystore password", e);
        }
        return true;
    }

    @Override
    public void setSecretKey(String alias, char[] key) throws IOException {
        assertActivatedKeyStore();
        SecretKey mySecretKey = new SecretKeySpec(toBytes(key), "AES");
        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(mySecretKey);
        char[] passwd = securityManager.getMasterPassword();
        try {
            ks.setEntry(alias, skEntry, new KeyStore.PasswordProtection(passwd));
            storeKeyStore();
        } catch (KeyStoreException e) {
            throw new IOException("Failed to set secret key for alias: " + alias, e);
        } finally {
            securityManager.disposePassword(passwd);
        }
    }

    @Override
    public void setUserGroupKey(String serviceName, char[] password) throws IOException {
        setSecretKey(aliasForGroupService(serviceName), password);
    }

    @Override
    public void removeKey(String alias) throws IOException {
        assertActivatedKeyStore();
        try {
            ks.deleteEntry(alias);
            storeKeyStore();
        } catch (KeyStoreException e) {
            throw new IOException("Failed to remove key for alias: " + alias, e);
        }
    }

    @Override
    public void storeKeyStore() throws IOException {
        assertActivatedKeyStore();
        try (OutputStream fos = getResource().out()) {
            char[] passwd = securityManager.getMasterPassword();
            try {
                ks.store(fos, passwd);
            } catch (Exception e) {
                throw new IOException("Failed to store BCFKS keystore", e);
            } finally {
                securityManager.disposePassword(passwd);
            }
        }
    }

    protected void addInitialKeys() throws IOException {
        RandomPasswordProvider randPasswdProvider = getSecurityManager().getRandomPassworddProvider();
        char[] configKey = randPasswdProvider.getRandomPasswordWithDefaultLength();
        setSecretKey(CONFIGPASSWORDKEY, configKey);
    }

    @Override
    public void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword) throws IOException {
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        if (newKSFile.getType() != Type.UNDEFINED) {
            newKSFile.delete();
        }

        try {
            String keystoreType = getKeystoreType();
            String provider = getKeystoreProvider();

            KeyStore oldKS = KeyStore.getInstance(keystoreType, provider);
            try (InputStream fin = getResource().in()) {
                oldKS.load(fin, oldPassword);
            }

            KeyStore newKS = KeyStore.getInstance(keystoreType, provider);
            newKS.load(null, newPassword);
            KeyStore.PasswordProtection protectionParam = new KeyStore.PasswordProtection(newPassword);

            Enumeration<String> enumeration = oldKS.aliases();
            while (enumeration.hasMoreElements()) {
                String alias = enumeration.nextElement();
                Key key = oldKS.getKey(alias, oldPassword);
                KeyStore.Entry entry = null;
                if (key instanceof SecretKey) {
                    entry = new KeyStore.SecretKeyEntry((SecretKey) key);
                } else if (key instanceof PrivateKey) {
                    entry = new KeyStore.PrivateKeyEntry((PrivateKey) key, oldKS.getCertificateChain(alias));
                } else if (oldKS.getCertificate(alias) != null) {
                    entry = new KeyStore.TrustedCertificateEntry(oldKS.getCertificate(alias));
                }
                if (entry == null) {
                    LOGGER.warning("Unknown key in store, alias: " + alias + " class: "
                            + key.getClass().getName());
                } else {
                    newKS.setEntry(alias, entry, protectionParam);
                }
            }

            try (OutputStream fos = newKSFile.out()) {
                newKS.store(fos, newPassword);
            }
        } catch (Exception ex) {
            throw new IOException("Failed to prepare master password change", ex);
        }
    }

    @Override
    public void abortMasterPasswordChange() {
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        if (newKSFile.getType() != Type.UNDEFINED) {
            newKSFile.delete();
        }
    }

    @Override
    public void commitMasterPasswordChange() throws IOException {
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        Resource oldKSFile = dir.get(DEFAULT_FILE_NAME);

        if (newKSFile.getType() == Type.UNDEFINED) {
            return; // Nothing to do
        }

        if (oldKSFile.getType() == Type.UNDEFINED) {
            return; // Not initialized
        }

        char[] passwd = securityManager.getMasterPassword();
        try {
            try (InputStream fin = newKSFile.in()) {
                String keystoreType = getKeystoreType();
                String provider = getKeystoreProvider();

                KeyStore newKS = KeyStore.getInstance(keystoreType, provider);
                newKS.load(fin, passwd);

                // Verify all keys
                Enumeration<String> enumeration = newKS.aliases();
                while (enumeration.hasMoreElements()) {
                    newKS.getKey(enumeration.nextElement(), passwd);
                }
            }

            if (!oldKSFile.delete()) {
                LOGGER.severe("Cannot delete " + oldKSFile.path());
                return;
            }

            if (!newKSFile.renameTo(oldKSFile)) {
                String msg = "Cannot rename " + newKSFile.path() + " to " + oldKSFile.path();
                msg += ". Try to rename manually and restart.";
                LOGGER.severe(msg);
                return;
            }
            reloadKeyStore();
            LOGGER.info("Successfully changed master password");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error creating new keystore: " + newKSFile.path(), e);
            throw e;
        } catch (Exception ex) {
            throw new IOException("Unexpected error committing master password change", ex);
        } finally {
            securityManager.disposePassword(passwd);
        }
    }
}
