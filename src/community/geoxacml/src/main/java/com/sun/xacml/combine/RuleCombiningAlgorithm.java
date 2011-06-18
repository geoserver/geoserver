/*
 * @(#)RuleCombiningAlgorithm.java
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
import java.util.List;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ctx.Result;

/**
 * The base type for all Rule combining algorithms.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 */
public abstract class RuleCombiningAlgorithm extends CombiningAlgorithm {

    /**
     * Constructor that takes the algorithm's identifier.
     * 
     * @param identifier
     *            the algorithm's identifier
     */
    public RuleCombiningAlgorithm(URI identifier) {
        super(identifier);
    }

    /**
     * Combines the rules based on the context to produce some unified result. This is the one
     * function of a combining algorithm.
     * 
     * @param context
     *            the representation of the request
     * @param parameters
     *            a (possibly empty) non-null <code>List</code> of <code>CombinerParameter<code>s
     * @param ruleElements
     *            a <code>List</code> of <code>CombinerElement<code>s
     * 
     * @return a single unified result based on the combining logic
     */
    public abstract Result combine(EvaluationCtx context, List<CombinerParameter> parameters,
            List<? extends CombinerElement> ruleElements);

}
