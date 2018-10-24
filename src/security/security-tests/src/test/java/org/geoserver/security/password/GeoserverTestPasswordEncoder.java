/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class GeoserverTestPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    @Override
    public String getPrefix() {
        return "plain4711";
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
                return new String(rawPass);
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.PLAIN;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(rawPassword);
    }
}
