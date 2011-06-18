/*
 * @(#)OnlyOneApplicablePolicyAlg.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.MatchResult;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

/**
 * This is the standard Only One Applicable Policy combining algorithm. This is a special algorithm
 * used at the root of a policy/pdp to make sure that pdp only selects one policy per request.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class OnlyOneApplicablePolicyAlg extends PolicyCombiningAlgorithm {

    /**
     * The standard URN used to identify this algorithm
     */
    public static final String algId = "urn:oasis:names:tc:xacml:1.0:policy-combining-algorithm:"
            + "only-one-applicable";

    // a URI form of the identifier
    private static final URI identifierURI = URI.create(algId);

    /**
     * Standard constructor.
     */
    public OnlyOneApplicablePolicyAlg() {
        super(identifierURI);
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
        boolean atLeastOne = false;
        AbstractPolicy selectedPolicy = null;
        Iterator<? extends CombinerElement> it = policyElements.iterator();

        while (it.hasNext()) {
            AbstractPolicy policy = ((PolicyCombinerElement) (it.next())).getPolicy();

            // see if the policy matches the context
            MatchResult match = policy.match(context);
            int result = match.getResult();

            // if there is an error in trying to match any of the targets,
            // we always return INDETERMINATE immediately
            if (result == MatchResult.INDETERMINATE)
                return new Result(Result.DECISION_INDETERMINATE, match.getStatus(), context
                        .getResourceId().encode());

            if (result == MatchResult.MATCH) {
                // if this isn't the first match, then this is an error
                if (atLeastOne) {
                    List<String> code = new ArrayList<String>();
                    code.add(Status.STATUS_PROCESSING_ERROR);
                    String message = "Too many applicable policies";
                    return new Result(Result.DECISION_INDETERMINATE, new Status(code, message),
                            context.getResourceId().encode());
                }

                // if this was the first applicable policy in the set, then
                // remember it for later
                atLeastOne = true;
                selectedPolicy = policy;
            }
        }

        // if we got through the loop and found exactly one match, then
        // we return the evaluation result of that policy
        if (atLeastOne)
            return selectedPolicy.evaluate(context);

        // if we didn't find a matching policy, then we don't apply
        return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());
    }

}
