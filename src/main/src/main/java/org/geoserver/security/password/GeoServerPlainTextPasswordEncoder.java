/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;

/**
 * Password encoder which encodes nothing
 *
 * @author christian
 */
public class GeoServerPlainTextPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PlaintextPasswordEncoder();
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            PlaintextPasswordEncoder encoder = new PlaintextPasswordEncoder();

            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return encoder.isPasswordValid(encPass, new String(rawPass), salt);
            }

            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return encoder.encodePassword(new String(rawPass), salt);
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
}
