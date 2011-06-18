/*
 * @(#)PolicyCombinerElement.java
 *
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.Indenter;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicyReference;
import com.sun.xacml.PolicySet;

/**
 * Specific version of <code>CombinerElement</code> used for policy combining.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class PolicyCombinerElement extends CombinerElement {

    /**
     * Constructor that only takes an <code>AbstractPolicy</code. No parameters
     * are associated with this <code>AbstractPolicy</code> when combining.
     * 
     * @param policy
     *            an <code>AbstractPolicy</code> to use in combining
     */
    public PolicyCombinerElement(AbstractPolicy policy) {
        super(policy);
    }

    /**
     * Constructor that takes both the <code>AbstractPolicy</code> to combine and its associated
     * combiner parameters.
     * 
     * @param policy
     *            an <code>AbstractPolicy</code> to use in combining
     * @param parameters
     *            a (possibly empty) non-null <code>List</code> of
     *            <code>CombinerParameter<code>s provided for general
     *                   use (for all pre-2.0 policies this must be empty)
     */
    public PolicyCombinerElement(AbstractPolicy policy, List<CombinerParameter> parameters) {
        super(policy, parameters);
    }

    /**
     * Returns the <code>AbstractPolicy</code> in this element.
     * 
     * @return the element's <code>AbstractPolicy</code>
     */
    public AbstractPolicy getPolicy() {
        return (AbstractPolicy) (getElement());
    }

    /**
     * Encodes this element's <code>AbstractPolicy</code> and parameters into their XML
     * representation and writes this encoding to the given <code>OutputStream</code> with
     * indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        if (!getParameters().isEmpty()) {
            AbstractPolicy policy = getPolicy();

            // FIXME: This is ugly and happens in several places...maybe this
            // should get folded into the AbstractPolicy API?
            if (policy instanceof Policy) {
                encodeParamaters(output, indenter, "Policy", policy.getId().toString());
            } else if (policy instanceof PolicySet) {
                encodeParamaters(output, indenter, "PolicySet", policy.getId().toString());
            } else {
                PolicyReference ref = (PolicyReference) policy;
                if (ref.getReferenceType() == PolicyReference.POLICY_REFERENCE)
                    encodeParamaters(output, indenter, "Policy", ref.getReference().toString());
                else
                    encodeParamaters(output, indenter, "PolicySet", ref.getReference().toString());
            }
        }

        getPolicy().encode(output, indenter);
    }

    /**
     * Private helper that encodes the parameters based on the type
     */
    private void encodeParamaters(OutputStream output, Indenter indenter, String prefix, String id) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        out.println(indent + "<" + prefix + "CombinerParameters " + prefix + "IdRef=\"" + id
                + "\">");
        indenter.in();

        for (CombinerParameter param : getParameters())
            param.encode(output, indenter);

        out.println(indent + "</" + prefix + "CombinerParameters>");
        indenter.out();
    }

}
