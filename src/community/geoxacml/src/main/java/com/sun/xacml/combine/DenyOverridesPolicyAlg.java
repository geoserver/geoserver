/*
 * @(#)DenyOverridesPolicyAlg.java
 *
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml.combine;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.MatchResult;
import com.sun.xacml.Obligation;
import com.sun.xacml.ctx.Result;

/**
 * This is the standard Deny Overrides policy combining algorithm. It allows a single evaluation of
 * Deny to take precedence over any number of permit, not applicable or indeterminate results. Note
 * that since this implementation does an ordered evaluation, this class also supports the Ordered
 * Deny Overrides algorithm.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class DenyOverridesPolicyAlg extends PolicyCombiningAlgorithm {

    /**
     * The standard URN used to identify this algorithm
     */
    public static final String algId = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:"
            + "deny-overrides";

    // a URI form of the identifier
    private static final URI identifierURI = URI.create(algId);

    /**
     * Standard constructor.
     */
    public DenyOverridesPolicyAlg() {
        super(identifierURI);
    }

    /**
     * Protected constructor used by the ordered version of this algorithm.
     * 
     * @param identifier
     *            the algorithm's identifier
     */
    protected DenyOverridesPolicyAlg(URI identifier) {
        super(identifier);
    }

    /**
     * Applies the combining rule to the set of policies based on the evaluation context.
     * 
     * @param context
     *            the context from the request
     * @param parameters
     *            a (possibly empty) non-null <code>List</code> of <code>CombinerParameter<code>s
     * @param policyElements
     *            the policies to combine
     * 
     * @return the result of running the combining algorithm
     */
    public Result combine(EvaluationCtx context, List<CombinerParameter> parameters,
            List<? extends CombinerElement> policyElements) {
        boolean atLeastOnePermit = false;
        Set<Obligation> permitObligations = new HashSet<Obligation>();
        Iterator<? extends CombinerElement> it = policyElements.iterator();

        while (it.hasNext()) {
            AbstractPolicy policy = ((PolicyCombinerElement) (it.next())).getPolicy();

            // make sure that the policy matches the context
            MatchResult match = policy.match(context);

            if (match.getResult() == MatchResult.INDETERMINATE)
                return new Result(Result.DECISION_DENY, context.getResourceId().encode());

            if (match.getResult() == MatchResult.MATCH) {
                // evaluate the policy
                Result result = policy.evaluate(context);
                int effect = result.getDecision();

                // unlike in the RuleCombining version of this alg, we always
                // return DENY if any Policy returns DENY or INDETERMINATE
                if ((effect == Result.DECISION_DENY) || (effect == Result.DECISION_INDETERMINATE))
                    return new Result(Result.DECISION_DENY, context.getResourceId().encode(),
                            result.getObligations());

                // remember if at least one Policy said PERMIT
                if (effect == Result.DECISION_PERMIT) {
                    atLeastOnePermit = true;
                    permitObligations.addAll(result.getObligations());
                }
            }
        }

        // if we got a PERMIT, return it, otherwise it's NOT_APPLICABLE
        if (atLeastOnePermit)
            return new Result(Result.DECISION_PERMIT, context.getResourceId().encode(),
                    permitObligations);
        else
            return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());
    }

}
