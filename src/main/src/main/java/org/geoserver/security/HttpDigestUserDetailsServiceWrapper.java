/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserDetailsWrapper;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.geoserver.security.password.UserDetailsPasswordWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

/**
 * {@link UserDetailsService} implementation to be used for 
 * HTTP digest authentication
 * 
 * {@link UserDetails} objects have their password alreay md5a1 encoded.
 * 
 *  {@link DigestAuthenticationFilter#setPasswordAlreadyEncoded(boolean)} must
 *  be called with a value of <code>true</code>
 * 
 * @author christian
 *
 */
public class HttpDigestUserDetailsServiceWrapper implements UserDetailsService {
    
    static public class DigestUserDetails extends UserDetailsWrapper {        
        private static final long serialVersionUID = 1L;

        private String password;
        private Collection<GrantedAuthority> roles;
        
        public DigestUserDetails(UserDetails details, String password, Collection<GrantedAuthority> roles) {
            super(details);
            this.password=password;
            this.roles=roles;
        }
        
        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return roles;
        }

        @Override
        public String getPassword() {
            return password;
        }

    }
    
    private GeoServerSecurityManager manager;
    protected GeoServerUserGroupService service;
    protected Charset charSet;
    protected final char[] delimArray= new char[] {':' };
    protected MessageDigest digest;
    protected GeoServerPasswordEncoder enc;
    
    public HttpDigestUserDetailsServiceWrapper(GeoServerUserGroupService service,Charset charSet) {
       this.service= service;
       this.charSet=charSet;
       manager = service.getSecurityManager();

       enc = service.getSecurityManager().loadPasswordEncoder(service.getPasswordEncoderName());
       if ((enc.getEncodingType()==PasswordEncodingType.PLAIN ||
                  enc.getEncodingType()==PasswordEncodingType.ENCRYPT)==false)
           throw new RuntimeException("Invalid configuration, cannot decode passwords");
       
        try {
            enc.initializeFor(service);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
                         
        try {            
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        } 
    }

    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
            DataAccessException {
        
        if (GeoServerUser.ROOT_USERNAME.equals(username))
            return prepareForRootUser ();
        
        GeoServerUser user = (GeoServerUser) service.loadUserByUsername(username);            
        return prepareForUser(user);
    }

    UserDetails prepareForUser (GeoServerUser user) {
        char[] pw = null;
        try {            
             pw = enc.decodeToCharArray(user.getPassword());
            //pw = enc.decode(user.getPassword()).toCharArray();
            String a1 = encodePasswordInA1Format(user.getUsername(), 
                    GeoServerSecurityManager.REALM, pw);
            
            List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
            roles.addAll(user.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            return new DigestUserDetails(user, a1,roles);
        } finally {
            manager.disposePassword(pw);
        }        
    }
    
    UserDetails prepareForRootUser () {
        
        char[] mpw = null;
        try {
            mpw= manager.getMasterPassword();
            String a1 = encodePasswordInA1Format(GeoServerUser.ROOT_USERNAME, 
                    GeoServerSecurityManager.REALM, mpw);
            
            return new UserDetailsPasswordWrapper(
                    GeoServerUser.createRoot(), a1);
        }
        finally {
            if (mpw!=null)
                manager.disposePassword(mpw);
        }
    }
    
    String encodePasswordInA1Format(String username, String realm, char[] password) {
        char[] array = null;
        try {
            char[] usernameArray = username.toCharArray();
            char[] realmArray = realm.toCharArray();
            
            array = new char[usernameArray.length+realmArray.length+password.length+2];
            int pos=0;
            
            System.arraycopy(usernameArray, 0, array, pos, usernameArray.length);
            pos+=usernameArray.length;
            
            System.arraycopy(delimArray, 0, array, pos, 1);
            pos++;
    
            System.arraycopy(realmArray, 0, array, pos, realmArray.length);
            pos+=realmArray.length;
            
            System.arraycopy(delimArray, 0, array, pos, 1);
            pos++;
    
            System.arraycopy(password, 0, array, pos, password.length);
            
            return new String(Hex.encode(digest.digest(SecurityUtils.toBytes(array, charSet))));
        } finally {
            if (array!=null)
                manager.disposePassword(array);
        }                        
    }
    
}
