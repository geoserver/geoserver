/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.PasswordEncoder;

/**
 * Password encoder that does absolute nothing, only used for testing.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerNullPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    public GeoServerNullPasswordEncoder() {
        setReversible(true);
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {
            @Override
            public boolean isPasswordValid(String encPass, String rawPass, Object salt)
                    throws DataAccessException {
                return true;
            }
            
            @Override
            public String encodePassword(String rawPass, Object salt) throws DataAccessException {
                return rawPass;
            }
        };
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return true;
            }
            
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return new String(rawPass);
            }
        };
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.NULL;
    }

    @Override
    public String decode(String encPass) throws UnsupportedOperationException {
        return encPass;
    }

    @Override
    public char[] decodeToCharArray(String encPass)
            throws UnsupportedOperationException {
        return decode(encPass).toCharArray();
    }
    
}
