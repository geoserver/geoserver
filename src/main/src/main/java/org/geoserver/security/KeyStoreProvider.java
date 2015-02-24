/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;

public interface KeyStoreProvider {

    /**
     * Sets the security manager facade.
     */
    void setSecurityManager(GeoServerSecurityManager securityManager);

    /**
     * @return the default key store {@link File} object
     */
    File getFile();

    /**
     * Forces a reload of the key store
     * 
     * @throws IOException
     */
    void reloadKeyStore() throws IOException;

    /**
     * Gets the {@link Key} object for this alias
     * <code>null</code> if the alias does not
     * exist
     * 
     * @param alias
     * @return
     * @throws IOException
     */
    Key getKey(String alias) throws IOException;

    /**
     * Gets the key for encrypting passwords stored
     * in configuration files, may be <code>null</code>
     * 
     * @return
     * @throws IOException
     */
    byte[] getConfigPasswordKey() throws IOException;

    /**
     * Checks if a such a key is available
     * without presenting the key itself
     * 
     * @return
     * @throws IOException
     */
    boolean hasConfigPasswordKey() throws IOException;


    /**
     * Test it the key store contains a alias
     * 
     * @param alias
     * @return
     * @throws IOException
     */
    boolean containsAlias(String alias) throws IOException;

    /**
     * Returns the key for a {@link GeoServerUserGroupService} 
     * service Name. Needed if the service uses symmetric password
     * encryption 
     * 
     * may be <code>null</code>
     * @param serviceName
     * @return
     * @throws IOException
     */
    byte[] getUserGroupKey(String serviceName) throws IOException;

    /**
     * Checks if a such a key is available
     * without presenting the key itself
     * 
     * @return
     * @throws IOException
     */
    boolean hasUserGroupKey(String serviceName) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias
     * <code>null</code> if the alias does not
     * exist
     * @param name
     * @return
     * @throws IOException if the key exists but has the wrong type
     */
    SecretKey getSecretKey(String name) throws IOException;

    /**
     * Gets the {@link SecretKey} object for this alias
     * <code>null</code> if the alias does not
     * exist
     * @param name
     * @return
     * @throws IOException if the key exists but has the wrong type
     */
    PublicKey getPublicKey(String name) throws IOException;

    /**
     * Gets the {@link PrivateKey} object for this alias
     * <code>null</code> if the alias does not
     * exist
     * @param name
     * @return
     * @throws IOException if the key exists but has the wrong type
     */
    PrivateKey getPrivateKey(String name) throws IOException;

    /**
     * 
     * @param serviceName for a {@link GeoServerUserGroupService}
     * @return the following String
     * {@link #USERGROUP_PREFIX}+serviceName+{@value #USERGROUP_POSTFIX}
     */
    String aliasForGroupService(String serviceName);

    /**
     * Tests if the password is the key store password
     * 
     * @param password
     * @return
     * @throws IOException
     */
    boolean isKeyStorePassword(char[] password) throws IOException;

    /**
     * Adds/replaces a {@link SecretKey} with its alias
     * 
     * @param alias
     * @param key
     * @throws Exception
     */
    void setSecretKey(String alias, char[] key) throws IOException;

    /**
     * Sets  a secret for the name of a {@link GeoServerUserGroupService}
     * @param serviceName
     * @param password
     * @throws IOException
     */
    void setUserGroupKey(String serviceName, char[] password) throws IOException;

    /**
     * Remove a key belonging to the alias
     * 
     * @param alias
     * @throws IOException
     */
    void removeKey(String alias) throws IOException;

    /**
     * Stores the key store to {@link #ks}
     * 
     * @throws IOException
     */
    void storeKeyStore() throws IOException;

    /**
     * Prepares a master password change. The new password is used to encrypt
     * the {@link KeyStore} and each {@link Entry}; 
     * 
     * The new password is assumed to be already validated by the {@link PasswordValidator} named
     * {@link PasswordValidator#MASTERPASSWORD_NAME}
     * 
     * A new key store named {@link #PREPARED_FILE_NAME} is created. All keys
     * a re-encrypted with the new password and stored in the new key store.
     *  
     *  
     * @param oldPassword
     * @param newPassword
     * @throws IOException
     */
    void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword)
            throws IOException;

    /**
     * Aborts the master password change by removing
     * the file named {@link #PREPARED_FILE_NAME}
     * 
     */
    void abortMasterPasswordChange();

    /**
     * if {@link #DEFAULT_FILE_NAME} and {@link #PREPARED_FILE_NAME} exist,
     * this method checks if {@link #PREPARED_FILE_NAME} can be used
     * with new {@link MasterPasswordProvider.#getMasterPassword() method.
     * 
     * YES: replace the old keystore with the new one
     * 
     * NO: Do nothing, log the problem and use the old configuration
     * A reason may be that the new master password is not properly injected
     * at startup 
     * 
     * @throws IOException
     */
    void commitMasterPasswordChange() throws IOException;

}
