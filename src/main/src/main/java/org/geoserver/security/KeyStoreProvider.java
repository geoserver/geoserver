/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import org.geoserver.platform.resource.Resource;

public interface KeyStoreProvider {

    /** Sets the security manager facade. */
    void setSecurityManager(GeoServerSecurityManager securityManager);

    /** @return the default key store {@link Resource} object */
    Resource getResource();

    /** Forces a reload of the key store */
    void reloadKeyStore() throws IOException;

    /** Gets the {@link Key} object for this alias <code>null</code> if the alias does not exist */
    Key getKey(String alias) throws IOException;

    /**
     * Gets the key for encrypting passwords stored in configuration files, may be <code>null</code>
     */
    byte[] getConfigPasswordKey() throws IOException;

    /** Checks if a such a key is available without presenting the key itself */
    boolean hasConfigPasswordKey() throws IOException;

    /** Test it the key store contains a alias */
    boolean containsAlias(String alias) throws IOException;

    /**
     * Returns the key for a {@link org.geoserver.security.GeoServerUserGroupService} service Name.
     * Needed if the service uses symmetric password encryption
     *
     * <p>may be <code>null</code>
     */
    byte[] getUserGroupKey(String serviceName) throws IOException;

    /** Checks if a such a key is available without presenting the key itself */
    boolean hasUserGroupKey(String serviceName) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    SecretKey getSecretKey(String name) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    PublicKey getPublicKey(String name) throws IOException;

    /**
     * Gets the {@link PrivateKey} object for this alias <code>null</code> if the alias does not
     * exist
     *
     * @throws IOException if the key exists but has the wrong type
     */
    PrivateKey getPrivateKey(String name) throws IOException;

    /**
     * @param serviceName for a {@link org.geoserver.security.GeoServerUserGroupService}
     * @return the following String {@link #USERGROUP_PREFIX}+serviceName+{@value
     *     #USERGROUP_POSTFIX}
     */
    String aliasForGroupService(String serviceName);

    /** Tests if the password is the key store password */
    boolean isKeyStorePassword(char[] password) throws IOException;

    /** Adds/replaces a {@link SecretKey} with its alias */
    void setSecretKey(String alias, char[] key) throws IOException;

    /** Sets a secret for the name of a {@link org.geoserver.security.GeoServerUserGroupService} */
    void setUserGroupKey(String serviceName, char[] password) throws IOException;

    /** Remove a key belonging to the alias */
    void removeKey(String alias) throws IOException;

    /** Stores the key store to {@link #ks} */
    void storeKeyStore() throws IOException;

    /**
     * Prepares a master password change. The new password is used to encrypt the {@link KeyStore}
     * and each {@link Entry};
     *
     * <p>The new password is assumed to be already validated by the {@link PasswordValidator} named
     * {@link PasswordValidator#MASTERPASSWORD_NAME}
     *
     * <p>A new key store named {@link #PREPARED_FILE_NAME} is created. All keys a re-encrypted with
     * the new password and stored in the new key store.
     */
    void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword) throws IOException;

    /** Aborts the master password change by removing the file named {@link #PREPARED_FILE_NAME} */
    void abortMasterPasswordChange();

    /**
     * if {@link #DEFAULT_FILE_NAME} and {@link #PREPARED_FILE_NAME} exist, this method checks if
     * {@link #PREPARED_FILE_NAME} can be used with new {@link
     * MasterPasswordProvider#getMasterPassword()} method.
     *
     * <p>YES: replace the old keystore with the new one
     *
     * <p>NO: Do nothing, log the problem and use the old configuration A reason may be that the new
     * master password is not properly injected at startup
     */
    void commitMasterPasswordChange() throws IOException;
}
