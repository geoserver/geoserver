/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import org.geoserver.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder which uses digest encoding This encoder cannot be used for authentication mechanisms needing the
 * plain text password. (Http digest authentication as an example)
 *
 * <p>The salt parameter is not used, this implementation computes a random salt as default.
 *
 * <p>{@link #isPasswordValid(String, String, Object)} {@link #encodePassword(String, Object)}
 *
 * @author christian
 */
public class GeoServerDigestPasswordEncoder extends AbstractGeoserverPasswordEncoder {
    private final LegacyPasswordByteDigester digester;

    public GeoServerDigestPasswordEncoder() {
        setReversible(false);
        digester = new LegacyPasswordByteDigester();
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                // legacy Jasypt StandardStringDigester applied NFC normalization, keep it for compat
                byte[] bytes = toBytes(Normalizer.normalize(rawPassword, Normalizer.Form.NFC));
                try {
                    return digester.encode(bytes);
                } finally {
                    scramble(bytes);
                }
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                byte[] bytes = toBytes(Normalizer.normalize(rawPassword, Normalizer.Form.NFC));
                try {
                    return digester.matches(bytes, encodedPassword);
                } finally {
                    scramble(bytes);
                }
            }
        };
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            @Override
            public String encodePassword(char[] rawPassword, Object salt) {
                byte[] bytes = SecurityUtils.toBytes(rawPassword, StandardCharsets.UTF_8);
                try {
                    return digester.encode(bytes);
                } finally {
                    scramble(bytes);
                }
            }

            @Override
            public boolean isPasswordValid(String encPassword, char[] rawPassword, Object salt) {
                byte[] bytes = SecurityUtils.toBytes(rawPassword, StandardCharsets.UTF_8);
                try {
                    return digester.matches(bytes, encPassword);
                } finally {
                    scramble(bytes);
                }
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.DIGEST;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] bytes = toBytes(rawPassword);
        try {
            return digester.encode(bytes);
        } finally {
            scramble(bytes);
        }
    }

    protected byte[] toBytes(CharSequence charSequence) {
        if (charSequence == null) {
            throw new IllegalArgumentException("charSequence cannot be null");
        }

        char[] chars = new char[charSequence.length()];
        try {
            for (int i = 0; i < charSequence.length(); i++) {
                chars[i] = charSequence.charAt(i);
            }
            return SecurityUtils.toBytes(chars, StandardCharsets.UTF_8);
        } finally {
            scramble(chars);
        }
    }
}
