/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.toBytes;

import org.apache.commons.codec.binary.Base64;
import org.jasypt.digest.StandardByteDigester;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder which uses digest encoding This encoder cannot be used for authentication
 * mechanisms needing the plain text password. (Http digest authentication as an example)
 *
 * <p>The salt parameter is not used, this implementation computes a random salt as default.
 *
 * <p>{@link #isPasswordValid(String, String, Object)} {@link #encodePassword(String, Object)}
 *
 * @author christian
 */
public class GeoServerDigestPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    public GeoServerDigestPasswordEncoder() {
        setReversible(false);
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        PasswordEncoder encoder = new JasyptPasswordEncoderWrapper();
        ((JasyptPasswordEncoderWrapper) encoder)
                .setPasswordEncryptor(new StrongPasswordEncryptor());
        ((JasyptPasswordEncoderWrapper) encoder).setPrefix(getPrefix());
        return encoder;
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            StandardByteDigester digester = new StandardByteDigester();

            {
                digester.setAlgorithm("SHA-256");
                digester.setIterations(100000);
                digester.setSaltSizeBytes(16);
                digester.initialize();
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return new String(Base64.encodeBase64(digester.digest(toBytes(rawPass))));
            }

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return digester.matches(toBytes(rawPass), Base64.decodeBase64(encPass.getBytes()));
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.DIGEST;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return createCharEncoder().encodePassword(decodeToCharArray(rawPassword.toString()), null);
    }
}
