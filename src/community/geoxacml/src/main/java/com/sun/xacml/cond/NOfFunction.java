/*
 * @(#)NOfFunction.java
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

package com.sun.xacml.cond;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.IntegerAttribute;

/**
 * A class that implements the n-of function. It requires at least one argument. The first argument
 * must be an integer and the rest of the arguments must be booleans. If the number of boolean
 * arguments that evaluate to true is at least the value of the first argument, the function returns
 * true. Otherwise, it returns false (or indeterminate, as described in the next paragraph.
 * <p>
 * This function evaluates the arguments one at a time, starting with the first one. As soon as the
 * result of the function can be determined, evaluation stops and that result is returned. During
 * this process, if any argument evaluates to indeterminate, an indeterminate result is returned.
 * 
 * @since 1.0
 * @author Steve Hanne
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class NOfFunction extends FunctionBase {

    /**
     * Standard identifier for the n-of function.
     */
    public static final String NAME_N_OF = FUNCTION_NS + "n-of";

    /**
     * Creates a new <code>NOfFunction</code> object.
     * 
     * @param functionName
     *            the standard XACML name of the function to be handled by this object, including
     *            the full namespace
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public NOfFunction(String functionName) {
        super(NAME_N_OF, 0, BooleanAttribute.identifier, false);

        if (!functionName.equals(NAME_N_OF))
            throw new IllegalArgumentException("unknown nOf function: " + functionName);
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_N_OF);

        return set;
    }

    /**
     * Evaluate the function, using the specified parameters.
     * 
     * @param inputs
     *            a <code>List</code> of <code>Evaluatable</code> objects representing the arguments
     *            passed to the function
     * @param context
     *            an <code>EvaluationCtx</code> so that the <code>Evaluatable</code> objects can be
     *            evaluated
     * @return an <code>EvaluationResult</code> representing the function's result
     */
    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        // Evaluate the arguments one by one. As soon as we can return
        // a result, do so. Return Indeterminate if any argument
        // evaluated is indeterminate.
        Iterator<? extends Expression> it = inputs.iterator();
        Evaluatable eval = (Evaluatable) (it.next());

        // Evaluate the first argument
        EvaluationResult result = eval.evaluate(context);
        if (result.indeterminate())
            return result;

        // if there were no problems, we know 'n'
        long n = ((IntegerAttribute) (result.getAttributeValue())).getValue();

        // If the number of trues needed is less than zero, report an error.
        if (n < 0)
            return makeProcessingError("First argument to " + getFunctionName()
                    + " cannot be negative.");

        // If the number of trues needed is zero, return true.
        if (n == 0)
            return EvaluationResult.getTrueInstance();

        // make sure it's possible to find n true values
        long remainingArgs = inputs.size() - 1;
        if (n > remainingArgs)
            return makeProcessingError("not enough arguments to n-of to " + "find " + n
                    + " true values");

        // loop through the inputs, trying to find at least n trues
        while (remainingArgs >= n) {
            eval = (Evaluatable) (it.next());

            // evaluate the next argument
            result = eval.evaluate(context);
            if (result.indeterminate())
                return result;

            // get the next value, and see if it's true
            if (((BooleanAttribute) (result.getAttributeValue())).getValue()) {
                // we're one closer to our goal...see if we met it
                if (--n == 0)
                    return EvaluationResult.getTrueInstance();
            }

            // we're still looking, but we've got one fewer arguments
            remainingArgs--;
        }

        // if we got here then we didn't meet our quota
        return EvaluationResult.getFalseInstance();
    }

    /**
     *
     */
    public void checkInputs(List<? extends Expression> inputs) throws IllegalArgumentException {
        // check that none of the inputs is a bag
        Object[] list = inputs.toArray();
        for (int i = 0; i < list.length; i++)
            if (((Evaluatable) (list[i])).returnsBag())
                throw new IllegalArgumentException("n-of can't use bags");

        // if we got here then there were no bags, so ask the other check
        // method to finish the checking
        checkInputsNoBag(inputs);
    }

    /**
     *
     */
    public void checkInputsNoBag(List<? extends Expression> inputs) throws IllegalArgumentException {
        Object[] list = inputs.toArray();

        // check that there is at least one arg
        if (list.length == 0)
            throw new IllegalArgumentException("n-of requires an argument");

        // check that the first element is an Integer
        Evaluatable eval = (Evaluatable) (list[0]);
        if (!eval.getType().toString().equals(IntegerAttribute.identifier))
            throw new IllegalArgumentException("first argument to n-of must" + " be an integer");

        // now check that the rest of the args are booleans
        for (int i = 1; i < list.length; i++) {
            if (!((Evaluatable) (list[i])).getType().toString().equals(BooleanAttribute.identifier))
                throw new IllegalArgumentException("invalid parameter in n-of"
                        + ": expected boolean");
        }
    }

}
