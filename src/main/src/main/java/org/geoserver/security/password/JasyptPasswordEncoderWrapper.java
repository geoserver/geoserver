/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.jasypt.digest.StringDigester;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wrapper class for jasyptPasswordEncoder, for compatibility with the Spring 5.1 API
 *
 * <p>Used by {@link GeoServerDigestPasswordEncoder}
 *
 * @author vickdw Created on 10/23/18
 */
public class JasyptPasswordEncoderWrapper extends AbstractGeoserverPasswordEncoder
        implements PasswordEncoder {

    // The password encryptor or string digester to be internally used
    private PasswordEncryptor passwordEncryptor = null;
    private StringDigester stringDigester = null;
    private Boolean useEncryptor = null;

    /** Creates a new instance of <tt>PasswordEncoder</tt> */
    public JasyptPasswordEncoderWrapper() {
        super();
    }

    /**
     * Sets a password encryptor to be used. Only one of <tt>setPasswordEncryptor</tt> or
     * <tt>setStringDigester</tt> should be called. If both are, the last call will define which
     * method will be used.
     *
     * @param passwordEncryptor the password encryptor instance to be used.
     */
    public void setPasswordEncryptor(final PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
        this.useEncryptor = Boolean.TRUE;
    }

    /**
     * Sets a string digester to be used. Only one of <tt>setPasswordEncryptor</tt> or
     * <tt>setStringDigester</tt> should be called. If both are, the last call will define which
     * method will be used.
     *
     * @param stringDigester the string digester instance to be used.
     */
    public void setStringDigester(final StringDigester stringDigester) {
        this.stringDigester = stringDigester;
        this.useEncryptor = Boolean.FALSE;
    }

    /**
     * Encodes a password. This implementation completely ignores salt, as jasypt's
     * <tt>PasswordEncryptor</tt> and <tt>StringDigester</tt> normally use a random one. Thus, it
     * can be safely passed as <tt>null</tt>.
     *
     * @param rawPass The password to be encoded.
     * @param salt The salt, which will be ignored. It can be null.
     */
    public String encodePassword(final String rawPass, final Object salt) {
        checkInitialization();
        if (this.useEncryptor.booleanValue()) {
            return this.passwordEncryptor.encryptPassword(rawPass);
        }
        return this.stringDigester.digest(rawPass);
    }

    /**
     * Checks a password's validity. This implementation completely ignores salt, as jasypt's
     * <tt>PasswordEncryptor</tt> and <tt>StringDigester</tt> normally use a random one. Thus, it
     * can be safely passed as <tt>null</tt>.
     *
     * @param encPass The encrypted password (digest) against which to check.
     * @param rawPass The password to be checked.
     * @param salt The salt, which will be ignored. It can be null.
     */
    public boolean isPasswordValid(final String encPass, final String rawPass, final Object salt) {
        checkInitialization();
        if (this.useEncryptor.booleanValue()) {
            return this.passwordEncryptor.checkPassword(rawPass, encPass);
        }
        return this.stringDigester.matches(rawPass, encPass);
    }

    /*
     * Checks that the PasswordEncoder has been correctly initialized
     * (either a password encryptor or a string digester has been set).
     */
    private synchronized void checkInitialization() {
        if (this.useEncryptor == null) {
            this.passwordEncryptor = new BasicPasswordEncryptor();
            this.useEncryptor = Boolean.TRUE;
        } else {
            if (this.useEncryptor.booleanValue()) {
                if (this.passwordEncryptor == null) {
                    throw new EncryptionInitializationException(
                            "Password encoder not initialized: password " + "encryptor is null");
                }
            } else {
                if (this.stringDigester == null) {
                    throw new EncryptionInitializationException(
                            "Password encoder not initialized: string " + "digester is null");
                }
            }
        }
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return encPass.equals(new String(rawPass));
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode(new String(rawPass));
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.PLAIN;
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        return removePrefix(encPass);
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        return decode(encPass).toCharArray();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return this.encodePassword(rawPassword.toString(), null);
    }
}
