/*
 * @(#)DenyOverridesRuleAlg.java
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
import java.util.Iterator;
import java.util.List;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Rule;
import com.sun.xacml.ctx.Result;

/**
 * This is the standard Deny Overrides rule combining algorithm. It allows a single evaluation of
 * Deny to take precedence over any number of permit, not applicable or indeterminate results. Note
 * that since this implementation does an ordered evaluation, this class also supports the Ordered
 * Deny Overrides algorithm.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class DenyOverridesRuleAlg extends RuleCombiningAlgorithm {

    /**
     * The standard URN used to identify this algorithm
     */
    public static final String algId = "urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:"
            + "deny-overrides";

    // a URI form of the identifier
    private static final URI identifierURI = URI.create(algId);

    /**
     * Standard constructor.
     */
    public DenyOverridesRuleAlg() {
        super(identifierURI);
    }

    /**
     * Protected constructor used by the ordered version of this algorithm.
     * 
     * @param identifier
     *            the algorithm's identifier
     */
    protected DenyOverridesRuleAlg(URI identifier) {
        super(identifier);
    }

    /**
     * Applies the combining rule to the set of rules based on the evaluation context.
     * 
     * @param context
     *            the context from the request
     * @param parameters
     *            a (possibly empty) non-null <code>List</code> of <code>CombinerParameter<code>s
     * @param ruleElements
     *            the rules to combine
     * 
     * @return the result of running the combining algorithm
     */
    public Result combine(EvaluationCtx context, List<CombinerParameter> parameters,
            List<? extends CombinerElement> ruleElements) {
        boolean atLeastOneError = false;
        boolean potentialDeny = false;
        boolean atLeastOnePermit = false;
        Result firstIndeterminateResult = null;
        Iterator<? extends CombinerElement> it = ruleElements.iterator();

        while (it.hasNext()) {
            Rule rule = ((RuleCombinerElement) (it.next())).getRule();
            Result result = rule.evaluate(context);
            int value = result.getDecision();

            // if there was a value of DENY, then regardless of what else
            // we've seen, we always return DENY
            if (value == Result.DECISION_DENY)
                return result;

            // if it was INDETERMINATE, then we couldn't figure something
            // out, so we keep track of these cases...
            if (value == Result.DECISION_INDETERMINATE) {
                atLeastOneError = true;

                // there are no rules about what to do if multiple cases
                // cause errors, so we'll just return the first one
                if (firstIndeterminateResult == null)
                    firstIndeterminateResult = result;

                // if the Rule's effect is DENY, then we can't let this
                // alg return PERMIT, since this Rule might have denied
                // if it could do its stuff
                if (rule.getEffect() == Result.DECISION_DENY)
                    potentialDeny = true;
            } else {
                // keep track of whether we had at least one rule that
                // actually pertained to the request
                if (value == Result.DECISION_PERMIT)
                    atLeastOnePermit = true;
            }
        }

        // we didn't explicitly DENY, but we might have had some Rule
        // been evaluated, so we have to return INDETERMINATE
        if (potentialDeny)
            return firstIndeterminateResult;

        // some Rule said PERMIT, so since nothing could have denied,
        // we return PERMIT
        if (atLeastOnePermit)
            return new Result(Result.DECISION_PERMIT, context.getResourceId().encode());

        // we didn't find anything that said PERMIT, but if we had a
        // problem with one of the Rules, then we're INDETERMINATE
        if (atLeastOneError)
            return firstIndeterminateResult;

        // if we hit this point, then none of the rules actually applied
        // to us, so we return NOT_APPLICABLE
        return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());
    }

}
