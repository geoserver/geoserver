/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.password;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Multiplexing password encoder.
 * <p>
 * This password encoder actually only decodes password, it does not encode them. The purpose of 
 * which is to decode a previously encoded password without knowing before hand which password 
 * encoder was used. The prefix contained in the encoded password is used to route to the appropriate
 * delegate encoder. Therefore only {@link GeoserverPasswordEncoder} implementations that use a 
 * prefix in the encoded password are valid for this encoder.  
 * </p>
 * 
 * @author christian
 *
 */
public class GeoServerMultiplexingPasswordEncoder implements PasswordEncoder {
    
    protected Set<GeoServerPasswordEncoder> encoders;

    public GeoServerMultiplexingPasswordEncoder(GeoServerSecurityManager secMgr) {
        this(secMgr, null);
    }

    public GeoServerMultiplexingPasswordEncoder(GeoServerSecurityManager secMgr, GeoServerUserGroupService service) {
        encoders=new HashSet<GeoServerPasswordEncoder>();
        for (GeoServerPasswordEncoder enc : secMgr.loadPasswordEncoders()) {
            if (StringUtils.hasLength(enc.getPrefix())) {
                if (service!=null) {
                    try {
                        if (enc instanceof GeoServerPBEPasswordEncoder) {
                            if (!secMgr.getKeyStoreProvider().hasUserGroupKey(service.getName())) { 
                                continue;  //   cannot use pbe encoder, no key
                            }
                        }
                        enc.initializeFor(service);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                encoders.add(enc);
            }
        }
    }
    
    GeoServerPasswordEncoder lookupEncoderForEncodedPassword(String encPass) throws UnsupportedOperationException{
        for (GeoServerPasswordEncoder enc : encoders) {
            if (enc.isResponsibleForEncoding(encPass))
                return enc;
        }
        throw new UnsupportedOperationException("No password decoder for: "+encPass);
    }
    
    @Override
    public String encodePassword(String rawPass, Object salt) throws UnsupportedOperationException{
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.isPasswordValid(encPass, rawPass, salt);
    }
    
    public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.isPasswordValid(encPass, rawPass, salt);
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.decode(encPass);        
    }

    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        GeoServerPasswordEncoder enc = lookupEncoderForEncodedPassword(encPass);
        return enc.decodeToCharArray(encPass);        
    }

}
