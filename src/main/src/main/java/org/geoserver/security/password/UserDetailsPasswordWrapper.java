/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */


package org.geoserver.security.password;

import org.geoserver.security.impl.UserDetailsWrapper;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Wrapper class needed if the password is needed in
 * a modified form (plain text as an example) 
 *  
 * @author mcr
 *
 */
public class UserDetailsPasswordWrapper extends UserDetailsWrapper {

    private static final long serialVersionUID = 1L;

    public UserDetailsPasswordWrapper(UserDetails details, String password) {
        super(details);
        this.password=password;
    }
    
    private String password;
    

    public String getPassword() {
        return password;
    }

}
