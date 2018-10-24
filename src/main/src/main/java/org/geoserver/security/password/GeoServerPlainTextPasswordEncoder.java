/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder which encodes nothing
 *
 * @author christian
 */
public class GeoServerPlainTextPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {

            @Override
            public boolean matches(CharSequence encPass, String rawPass)
                    throws DataAccessException {
                return Objects.equals(encPass.toString(), encode(rawPass));
            }

            @Override
            public String encode(CharSequence rawPass) throws DataAccessException {
                return rawPass.toString();
            }
        };
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return Objects.equals(encPass, encodePassword(rawPass, salt));
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return new String(rawPass);
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
        return doEncodePassword(rawPassword.toString());
    }
}
