/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.xacml.spring.security;

import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.InsufficientAuthenticationException;
import org.springframework.security.vote.AbstractAccessDecisionManager;
import org.springframework.security.vote.AccessDecisionVoter;

/**
 * Spring Security AccessDecsionsManger implementation for Services
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLFilterAccessDecisionManager extends AbstractAccessDecisionManager {

    public void decide(Authentication auth, Object arg1, ConfigAttributeDefinition arg2)
            throws AccessDeniedException, InsufficientAuthenticationException {

        AccessDecisionVoter voter = (AccessDecisionVoter) this.getDecisionVoters().get(0);
        int decision = voter.vote(auth, arg1, arg2);
        if (decision != AccessDecisionVoter.ACCESS_GRANTED) {
            throw new AccessDeniedException("Access Denied: " + arg1.toString());
        }
    }

}
