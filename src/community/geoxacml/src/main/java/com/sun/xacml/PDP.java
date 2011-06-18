/*
 * @(#)PDP.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.finder.ResourceFinder;
import com.sun.xacml.finder.ResourceFinderResult;

/**
 * This is the core class for the XACML engine, providing the starting point for request evaluation.
 * To build an XACML policy engine, you start by instantiating this object.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class PDP {

    // the single attribute finder that can be used to find external values
    private AttributeFinder attributeFinder;

    // the single policy finder that will be used to resolve policies
    private PolicyFinder policyFinder;

    // the single resource finder that will be used to resolve resources
    private ResourceFinder resourceFinder;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(PDP.class.getName());

    /**
     * Constructs a new <code>PDP</code> object with the given configuration information.
     * 
     * @param config
     *            user configuration data defining how to find policies, resolve external
     *            attributes, etc.
     */
    public PDP(PDPConfig config) {
        logger.fine("creating a PDP");

        attributeFinder = config.getAttributeFinder();

        policyFinder = config.getPolicyFinder();
        policyFinder.init();

        resourceFinder = config.getResourceFinder();
    }

    /**
     * Attempts to evaluate the request against the policies known to this PDP. This is really the
     * core method of the entire XACML specification, and for most people will provide what you
     * want. If you need any special handling, you should look at the version of this method that
     * takes an <code>EvaluationCtx</code>.
     * <p>
     * Note that if the request is somehow invalid (it was missing a required attribute, it was
     * using an unsupported scope, etc), then the result will be a decision of INDETERMINATE.
     * 
     * @param request
     *            the request to evaluate
     * 
     * @return a response paired to the request
     */
    public ResponseCtx evaluate(RequestCtx request) {
        // try to create the EvaluationCtx out of the request
        try {
            return evaluate(new BasicEvaluationCtx(request, attributeFinder));
        } catch (ParsingException pe) {
            logger.log(Level.INFO, "the PDP receieved an invalid request", pe);

            // there was something wrong with the request, so we return
            // Indeterminate with a status of syntax error...though this
            // may change if a more appropriate status type exists
            ArrayList<String> code = new ArrayList<String>();
            code.add(Status.STATUS_SYNTAX_ERROR);
            Status status = new Status(code, pe.getMessage());

            return new ResponseCtx(new Result(Result.DECISION_INDETERMINATE, status));
        }
    }

    /**
     * Uses the given <code>EvaluationCtx</code> against the available policies to determine a
     * response. If you are starting with a standard XACML Request, then you should use the version
     * of this method that takes a <code>RequestCtx</code>. This method should be used only if you
     * have a real need to directly construct an evaluation context (or if you need to use an
     * <code>EvaluationCtx</code> implementation other than <code>BasicEvaluationCtx</code>).
     * 
     * @param context
     *            representation of the request and the context used for evaluation
     * 
     * @return a response based on the contents of the context
     */
    public ResponseCtx evaluate(EvaluationCtx context) {
        // see if we need to call the resource finder
        if (context.getScope() != EvaluationCtx.SCOPE_IMMEDIATE) {
            AttributeValue parent = context.getResourceId();
            ResourceFinderResult resourceResult = null;

            if (context.getScope() == EvaluationCtx.SCOPE_CHILDREN)
                resourceResult = resourceFinder.findChildResources(parent, context);
            else
                resourceResult = resourceFinder.findDescendantResources(parent, context);

            // see if we actually found anything
            if (resourceResult.isEmpty()) {
                // this is a problem, since we couldn't find any resources
                // to work on...the spec is not explicit about what kind of
                // error this is, so we're treating it as a processing error
                ArrayList<String> code = new ArrayList<String>();
                code.add(Status.STATUS_PROCESSING_ERROR);
                String msg = "Couldn't find any resources to work on.";

                return new ResponseCtx(new Result(Result.DECISION_INDETERMINATE, new Status(code,
                        msg), context.getResourceId().encode()));
            }

            // setup a set to keep track of the results
            HashSet<Result> results = new HashSet<Result>();

            // at this point, we need to go through all the resources we
            // successfully found and start collecting results

            for (AttributeValue resource : resourceResult.getResources()) {
                // get the next resource, and set it in the EvaluationCtx
                context.setResourceId(resource);

                // do the evaluation, and set the resource in the result
                Result result = evaluateContext(context);
                result.setResource(resource.encode());

                // add the result
                results.add(result);
            }

            // now that we've done all the successes, we add all the failures
            // from the finder result
            Map<AttributeValue, Status> failureMap = resourceResult.getFailures();

            for (AttributeValue resource : failureMap.keySet()) {
                // get the next resource, and use it to get its Status data
                Status status = (failureMap.get(resource));
                // add a new result
                results.add(new Result(Result.DECISION_INDETERMINATE, status, resource.encode()));
            }

            // return the set of results
            return new ResponseCtx(results);
        } else {
            // the scope was IMMEDIATE (or missing), so we can just evaluate
            // the request and return whatever we get back
            return new ResponseCtx(evaluateContext(context));
        }
    }

    /**
     * A private helper routine that resolves a policy for the given context, and then tries to
     * evaluate based on the policy
     */
    private Result evaluateContext(EvaluationCtx context) {
        // first off, try to find a policy
        PolicyFinderResult finderResult = policyFinder.findPolicy(context);

        // see if there weren't any applicable policies
        if (finderResult.notApplicable())
            return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());

        // see if there were any errors in trying to get a policy
        if (finderResult.indeterminate())
            return new Result(Result.DECISION_INDETERMINATE, finderResult.getStatus(), context
                    .getResourceId().encode());

        // we found a valid policy, so we can do the evaluation
        return finderResult.getPolicy().evaluate(context);
    }

    /**
     * A utility method that wraps the functionality of the other evaluate method with input and
     * output streams. This is useful if you've got a PDP that is taking inputs from some stream and
     * is returning responses through the same stream system. If the Request is invalid, then this
     * will always return a decision of INDETERMINATE.
     * 
     * @deprecated As of 1.2 this method should not be used. Instead, you should do your own stream
     *             handling, and then use one of the other <code>evaluate</code> methods. The
     *             problem with this method is that it often doesn't handle stream termination
     *             correctly (eg, with sockets).
     * 
     * @param input
     *            a stream that contains an XML RequestType
     * 
     * @return a stream that contains an XML ResponseType
     */
    public OutputStream evaluate(InputStream input) {
        RequestCtx request = null;
        ResponseCtx response = null;

        try {
            request = RequestCtx.getInstance(input);
        } catch (Exception pe) {
            // the request wasn't formed correctly
            ArrayList<String> code = new ArrayList<String>();
            code.add(Status.STATUS_SYNTAX_ERROR);
            Status status = new Status(code, "invalid request: " + pe.getMessage());

            response = new ResponseCtx(new Result(Result.DECISION_INDETERMINATE, status));
        }

        // if we didn't have a problem above, then we should go ahead
        // with the evaluation
        if (response == null)
            response = evaluate(request);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.encode(out, new Indenter());

        return out;
    }

}
