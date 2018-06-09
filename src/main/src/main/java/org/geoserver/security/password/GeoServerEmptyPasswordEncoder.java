/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.PasswordEncoder;

/**
 * Password encoder that encodes passwords to empty strings. This encoder cannot validate a
 * password, the result is always <code>false</code>.
 *
 * <p>This password encoder would be used in cases there {@link GeoServerUserGroupService} is being
 * used for the user database but the actual user passwords and authentication is handled elsewhere.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerEmptyPasswordEncoder extends AbstractGeoserverPasswordEncoder {

    public GeoServerEmptyPasswordEncoder() {
        setReversible(false);
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PasswordEncoder() {

            @Override
            public boolean isPasswordValid(String encPass, String rawPass, Object salt)
                    throws DataAccessException {
                return false;
            }

            @Override
            public String encodePassword(String rawPass, Object salt) throws DataAccessException {
                return "";
            }
        };
    }

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

    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.EMPTY;
    }
}
