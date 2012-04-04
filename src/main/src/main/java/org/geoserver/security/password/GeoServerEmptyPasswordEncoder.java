/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.PasswordEncoder;

/**
 * Password encoder that encodes empty passwords.
 * <p>
 * In this case empty means either <code>null</code> or an empty string. This password encoder would
 * be used in cases there {@link GeoServerUserGroupService} is being used for the user database but
 * the actual user passwords and authentication is handled elsewhere.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerEmptyPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {
            
            @Override
            public boolean isPasswordValid(String encPass, String rawPass, Object salt)
                    throws DataAccessException {
                return normalize(encPass) == null && normalize(rawPass) == null;
            }
            
            @Override
            public String encodePassword(String rawPass, Object salt)
                    throws DataAccessException {
                if (normalize(rawPass) == null) {
                    return null;
                }
                throw new IllegalArgumentException("Non null/empty password specified");
            }
        };
    }
    
    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            
            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return normalize(encPass) == null && normalize(rawPass) == null;
            }
            
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                if (normalize(rawPass) == null) {
                    return null;
                }
                throw new IllegalArgumentException("Non null/empty password specified");
            }
        };
    }

    String normalize(String s) {
        return s == null || "".equals(s) ? null : s;
    }

    char[] normalize(char[] ch) {
        return ch == null || ch.length == 0 ? null : ch;
    }

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.EMPTY;
    }
}
