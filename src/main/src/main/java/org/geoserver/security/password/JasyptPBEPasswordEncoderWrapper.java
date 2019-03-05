/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.util.Objects;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.TextEncryptor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wrapper class for jasypt PBEPasswordEncoder enabling the class to return the Spring 5.1 version
 * of PasswordEncoder
 *
 * <p>Used by {@link GeoServerPBEPasswordEncoder}
 *
 * @author vickdw Created on 10/23/18
 */
public class JasyptPBEPasswordEncoderWrapper extends AbstractGeoserverPasswordEncoder
        implements PasswordEncoder {
    private TextEncryptor textEncryptor = null;
    private PBEStringEncryptor pbeStringEncryptor = null;
    private Boolean useTextEncryptor = null;

    public JasyptPBEPasswordEncoderWrapper() {}

    /** Creates the encoder instance used when source is a string. */
    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {

            @Override
            public boolean matches(CharSequence encPass, String rawPass)
                    throws DataAccessException {
                return false;
            }

            @Override
            public String encode(CharSequence rawPass) throws DataAccessException {
                return "";
            }
        };
    }

    /** Creates the encoder instance used when source is a char array. */
    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return false;
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return "";
            }
        };
    }

    public void setTextEncryptor(TextEncryptor textEncryptor) {
        this.textEncryptor = textEncryptor;
        this.useTextEncryptor = Boolean.TRUE;
    }

    public void setPbeStringEncryptor(PBEStringEncryptor pbeStringEncryptor) {
        this.pbeStringEncryptor = pbeStringEncryptor;
        this.useTextEncryptor = Boolean.FALSE;
    }

    public String encodePassword(String rawPass, Object salt) {
        this.checkInitialization();
        return this.useTextEncryptor
                ? this.textEncryptor.encrypt(rawPass)
                : this.pbeStringEncryptor.encrypt(rawPass);
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        this.checkInitialization();
        String decPassword = null;
        if (this.useTextEncryptor) {
            decPassword = this.textEncryptor.decrypt(encPass);
        } else {
            decPassword = this.pbeStringEncryptor.decrypt(encPass);
        }

        return Objects.equals(decPassword, rawPass);
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.ENCRYPT;
    }

    private synchronized void checkInitialization() {
        if (this.useTextEncryptor == null) {
            this.textEncryptor = new BasicTextEncryptor();
            this.useTextEncryptor = Boolean.TRUE;
        } else if (this.useTextEncryptor) {
            if (this.textEncryptor == null) {
                throw new EncryptionInitializationException(
                        "PBE Password encoder not initialized: text encryptor is null");
            }
        } else if (this.pbeStringEncryptor == null) {
            throw new EncryptionInitializationException(
                    "PBE Password encoder not initialized: PBE string encryptor is null");
        }
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword != null) {
            return encodePassword(rawPassword.toString(), null);
        }
        return null;
    }
}
