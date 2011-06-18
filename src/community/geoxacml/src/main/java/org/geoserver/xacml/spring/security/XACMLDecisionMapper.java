/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.xacml.spring.security;

import org.springframework.security.vote.AccessDecisionVoter;

import com.sun.xacml.ctx.Result;

/**
 * 
 * Maps XACML Decisions to Spring Security Descisons
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLDecisionMapper {
    public final static XACMLDecisionMapper Exact = new XACMLDecisionMapper(
            AccessDecisionVoter.ACCESS_ABSTAIN);

    public final static XACMLDecisionMapper UnknownIsPermit = new XACMLDecisionMapper(
            AccessDecisionVoter.ACCESS_GRANTED);

    public final static XACMLDecisionMapper UnknownIsDeny = new XACMLDecisionMapper(
            AccessDecisionVoter.ACCESS_DENIED);

    private int mappingForNotApplicable;

    private XACMLDecisionMapper(int mappingForNotApplicable) {
        this.mappingForNotApplicable = mappingForNotApplicable;
    }

    int getSpringSecurityDecisionFor(int xacmlDecision) {
        switch (xacmlDecision) {
        case Result.DECISION_DENY:
            return AccessDecisionVoter.ACCESS_DENIED;
        case Result.DECISION_PERMIT:
            return AccessDecisionVoter.ACCESS_GRANTED;
        case Result.DECISION_NOT_APPLICABLE:
            return mappingForNotApplicable;
        case Result.DECISION_INDETERMINATE:
            throw new RuntimeException(
                    "Never should reach this point, no existing spring security mapping for xacml DECISION_INDETERMINATE");
        }
        throw new RuntimeException("Never should reach this point");

    }

}
