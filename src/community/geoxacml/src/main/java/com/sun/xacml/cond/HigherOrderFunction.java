/*
 * @(#)HigherOrderFunction.java
 *
 * Copyright 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.BooleanAttribute;

/**
 * Represents all of the higher order bag functions, except map, which has its own class due to the
 * issues with its return type. Unlike the other functions that are designed to work over any types
 * (the type-* functions) these functions don't use specific names to describe what type they
 * operate over, so you don't need to install new instances for any new datatypes you define.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class HigherOrderFunction implements Function {

    /**
     * Standard identifier for the any-of function.
     */
    public static final String NAME_ANY_OF = FunctionBase.FUNCTION_NS + "any-of";

    /**
     * Standard identifier for the all-of function.
     */
    public static final String NAME_ALL_OF = FunctionBase.FUNCTION_NS + "all-of";

    /**
     * Standard identifier for the any-of-any function.
     */
    public static final String NAME_ANY_OF_ANY = FunctionBase.FUNCTION_NS + "any-of-any";

    /**
     * Standard identifier for the all-of-any function.
     */
    public static final String NAME_ALL_OF_ANY = FunctionBase.FUNCTION_NS + "all-of-any";

    /**
     * Standard identifier for the any-of-all function.
     */
    public static final String NAME_ANY_OF_ALL = FunctionBase.FUNCTION_NS + "any-of-all";

    /**
     * Standard identifier for the all-of-all function.
     */
    public static final String NAME_ALL_OF_ALL = FunctionBase.FUNCTION_NS + "all-of-all";

    // internal identifiers for each of the supported functions
    private static final int ID_ANY_OF = 0;

    private static final int ID_ALL_OF = 1;

    private static final int ID_ANY_OF_ANY = 2;

    private static final int ID_ALL_OF_ANY = 3;

    private static final int ID_ANY_OF_ALL = 4;

    private static final int ID_ALL_OF_ALL = 5;

    // internal mapping of names to ids
    private static HashMap<String, Integer> idMap;

    // the internal identifier for each function
    private int functionId;

    // the real identifier for each function
    private URI identifier;

    // should the second argument (the first arg passed to the sub-function)
    // be a bag
    private boolean secondIsBag;

    // the stuff used to make sure that we have a valid return type or a
    // known error, just like in the attribute classes
    private static final URI returnTypeURI = URI.create(BooleanAttribute.identifier);

    // try to create the return type URI, and also setup the id map
    static {
        idMap = new HashMap<String, Integer>();

        idMap.put(NAME_ANY_OF, new Integer(ID_ANY_OF));
        idMap.put(NAME_ALL_OF, new Integer(ID_ALL_OF));
        idMap.put(NAME_ANY_OF_ANY, new Integer(ID_ANY_OF_ANY));
        idMap.put(NAME_ALL_OF_ANY, new Integer(ID_ALL_OF_ANY));
        idMap.put(NAME_ANY_OF_ALL, new Integer(ID_ANY_OF_ALL));
        idMap.put(NAME_ALL_OF_ALL, new Integer(ID_ALL_OF_ALL));
    };

    /**
     * Creates a new instance of the given function.
     * 
     * @param functionName
     *            the function to create
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public HigherOrderFunction(String functionName) {
        // try to get the function's identifier
        Integer i = (Integer) (idMap.get(functionName));
        if (i == null)
            throw new IllegalArgumentException("unknown function: " + functionName);
        functionId = i.intValue();

        // setup the URI form of this function's idenitity
        try {
            identifier = new URI(functionName);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("invalid URI");
        }

        // see if the second arg is a bag
        if ((functionId != ID_ANY_OF) && (functionId != ID_ALL_OF))
            secondIsBag = true;
        else
            secondIsBag = false;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        return Collections.unmodifiableSet(idMap.keySet());
    }

    /**
     * Returns the full identifier of this function, as known by the factories.
     * 
     * @return the function's identifier
     */
    public URI getIdentifier() {
        return identifier;
    }

    /**
     * Returns the same value as <code>getReturnType</code>. This is here to support the
     * <code>Expression</code> interface.
     * 
     * @return the return type
     */
    public URI getType() {
        return getReturnType();
    }

    /**
     * Returns the type of attribute value that will be returned by this function.
     * 
     * @return the return type
     */
    public URI getReturnType() {
        return returnTypeURI;
    }

    /**
     * Returns whether or not this function will actually return a bag of values.
     * 
     * @return true if the function returns a bag of values, otherwise false
     */
    public boolean returnsBag() {
        return false;
    }

    /**
     * Evaluates the function, using the specified parameters.
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

        Iterator<? extends Expression> iterator = inputs.iterator();

        // get the first arg, which is the function
        Expression xpr = iterator.next();
        Function function = null;

        if (xpr instanceof Function) {
            function = (Function) xpr;
        } else {
            function = (Function) (((VariableReference) xpr).getReferencedDefinition()
                    .getExpression());
        }

        // get the two inputs, and if anything is INDETERMINATE, then we
        // stop right away
        AttributeValue[] args = new AttributeValue[2];

        Evaluatable eval = (Evaluatable) (iterator.next());
        EvaluationResult result = eval.evaluate(context);
        if (result.indeterminate())
            return result;
        args[0] = (AttributeValue) (result.getAttributeValue());

        eval = (Evaluatable) (iterator.next());
        result = eval.evaluate(context);
        if (result.indeterminate())
            return result;
        args[1] = (AttributeValue) (result.getAttributeValue());

        // now we're ready to do the evaluation
        result = null;

        switch (functionId) {

        case ID_ANY_OF: {

            // param: boolean-function, single value, bag of same type
            // return: boolean
            // using the function, iterate through the bag, and if one
            // of the bag elements matches the single value, return
            // true, otherwise return false

            result = any(args[0], (BagAttribute) (args[1]), function, context, false);
            break;
        }

        case ID_ALL_OF: {

            // param: boolean-function, single value, bag of same type
            // return: boolean
            // using the function, iterate through the bag, and if all
            // of the bag elements match the single value, return
            // true, otherwise return false

            result = all(args[0], (BagAttribute) (args[1]), function, context);
            break;
        }

        case ID_ANY_OF_ANY: {

            // param: boolean-function, bag, bag of same type
            // return: boolean
            // apply the function to every combination of a single value from
            // the first bag and a single value from the second bag, and if
            // any evaluation is true return true, otherwise return false

            result = new EvaluationResult(BooleanAttribute.getInstance(false));
            Iterator<AttributeValue> it = ((BagAttribute) args[0]).iterator();
            BagAttribute bag = (BagAttribute) (args[1]);

            while (it.hasNext()) {
                AttributeValue value = it.next();
                result = any(value, bag, function, context, false);

                if (result.indeterminate())
                    return result;

                if (((BooleanAttribute) (result.getAttributeValue())).getValue())
                    break;
            }
            break;
        }

        case ID_ALL_OF_ANY: {

            // param: boolean-function, bag, bag of same type
            // return: boolean
            // iterate through the first bag, and if for each of those values
            // one of the values in the second bag matches then return true,
            // otherwise return false

            result = allOfAny((BagAttribute) (args[1]), (BagAttribute) (args[0]), function, context);
            break;
        }

        case ID_ANY_OF_ALL: {

            // param: boolean-function, bag, bag of same type
            // return: boolean
            // iterate through the second bag, and if for each of those values
            // one of the values in the first bag matches then return true,
            // otherwise return false

            result = anyOfAll((BagAttribute) (args[0]), (BagAttribute) (args[1]), function, context);
            break;
        }

        case ID_ALL_OF_ALL: {

            // param: boolean-function, bag, bag of same type
            // return: boolean
            // iterate through the first bag, and for each of those values
            // if every value in the second bag matches using the given
            // function, then return true, otherwise return false

            result = new EvaluationResult(BooleanAttribute.getInstance(true));
            Iterator<AttributeValue> it = ((BagAttribute) args[0]).iterator();
            BagAttribute bag = (BagAttribute) (args[1]);

            while (it.hasNext()) {
                AttributeValue value = it.next();
                result = all(value, bag, function, context);

                if (result.indeterminate())
                    return result;

                if (!((BooleanAttribute) (result.getAttributeValue())).getValue())
                    break;
            }
            break;
        }

        }

        return result;
    }

    /**
     * Checks that the given inputs are valid for this function.
     * 
     * @param inputs
     *            a <code>List</code> of <code>Evaluatable</code>s
     * 
     * @throws IllegalArgumentException
     *             if the inputs are invalid
     */
    public void checkInputs(List<? extends Expression> inputs) throws IllegalArgumentException {
        Object[] list = inputs.toArray();

        // first off, check that we got the right number of paramaters
        if (list.length != 3)
            throw new IllegalArgumentException("requires three inputs");

        // now, try to cast the first element into a function
        Function function = null;

        if (list[0] instanceof Function) {
            function = (Function) (list[0]);
        } else if (list[0] instanceof VariableReference) {
            Expression xpr = ((VariableReference) (list[0])).getReferencedDefinition()
                    .getExpression();
            if (xpr instanceof Function)
                function = (Function) xpr;
        }

        if (function == null)
            throw new IllegalArgumentException("first arg to higher-order "
                    + " function must be a function");

        // check that the function returns a boolean
        if (!function.getReturnType().toString().equals(BooleanAttribute.identifier))
            throw new IllegalArgumentException("higher-order function must "
                    + "use a boolean function");

        // get the two inputs
        Evaluatable eval1 = (Evaluatable) (list[1]);
        Evaluatable eval2 = (Evaluatable) (list[2]);

        // the first arg might be a bag
        if (secondIsBag && (!eval1.returnsBag()))
            throw new IllegalArgumentException("first arg has to be a bag");

        // the second arg must be a bag
        if (!eval2.returnsBag())
            throw new IllegalArgumentException("second arg has to be a bag");

        // finally, we need to make sure that the given type will work on
        // the given function
        List<Evaluatable> args = new ArrayList<Evaluatable>();
        args.add(eval1);
        args.add(eval2);
        function.checkInputsNoBag(args);
    }

    /**
     * Checks that the given inputs are valid for this function if all inputs are considered to not
     * be bags. This always throws an exception, since this function by definition must work on
     * bags.
     * 
     * @param inputs
     *            a <code>List</code> of <code>Evaluatable</code>s
     * 
     * @throws IllegalArgumentException
     *             always
     */
    public void checkInputsNoBag(List<? extends Expression> inputs) throws IllegalArgumentException {
        throw new IllegalArgumentException("higher-order functions require " + "use of bags");
    }

    /**
     * Private helper function that performs the any function, but lets you swap the argument order
     * (so it can be used by any-of-all)
     */
    private EvaluationResult any(AttributeValue value, BagAttribute bag, Function function,
            EvaluationCtx context, boolean argumentsAreSwapped) {
        return anyAndAllHelper(value, bag, function, context, false, argumentsAreSwapped);
    }

    /**
     * Private helper function that performs the all function
     */
    private EvaluationResult all(AttributeValue value, BagAttribute bag, Function function,
            EvaluationCtx context) {
        return anyAndAllHelper(value, bag, function, context, true, false);
    }

    /**
     * Private helper for any & all functions
     */
    private EvaluationResult anyAndAllHelper(AttributeValue value, BagAttribute bag,
            Function function, EvaluationCtx context, boolean allFunction,
            boolean argumentsAreSwapped) {
        BooleanAttribute attr = BooleanAttribute.getInstance(allFunction);
        Iterator<AttributeValue> it = bag.iterator();

        while (it.hasNext()) {
            List<AttributeValue> params = new ArrayList<AttributeValue>();

            if (!argumentsAreSwapped) {
                params.add(value);
                params.add((AttributeValue) (it.next()));
            } else {
                params.add((AttributeValue) (it.next()));
                params.add(value);
            }

            EvaluationResult result = function.evaluate(params, context);

            if (result.indeterminate())
                return result;

            BooleanAttribute bool = (BooleanAttribute) (result.getAttributeValue());
            if (bool.getValue() != allFunction) {
                attr = bool;
                break;
            }
        }

        return new EvaluationResult(attr);
    }

    /**
     * any-of-all
     */
    private EvaluationResult anyOfAll(BagAttribute anyBag, BagAttribute allBag, Function function,
            EvaluationCtx context) {
        return allAnyHelper(anyBag, allBag, function, context, true);
    }

    /**
     * all-of-any
     */
    private EvaluationResult allOfAny(BagAttribute anyBag, BagAttribute allBag, Function function,
            EvaluationCtx context) {
        return allAnyHelper(anyBag, allBag, function, context, false);
    }

    /**
     * Private helper for the all-of-any and any-of-all functions
     */
    private EvaluationResult allAnyHelper(BagAttribute anyBag, BagAttribute allBag,
            Function function, EvaluationCtx context, boolean argumentsAreSwapped) {
        Iterator<AttributeValue> it = allBag.iterator();

        while (it.hasNext()) {
            AttributeValue value = it.next();
            EvaluationResult result = any(value, anyBag, function, context, argumentsAreSwapped);

            if (result.indeterminate())
                return result;

            if (!((BooleanAttribute) (result.getAttributeValue())).getValue())
                return result;
        }

        return new EvaluationResult(BooleanAttribute.getTrueInstance());
    }

    /**
     * Encodes this <code>HigherOrderFunction</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>HigherOrderFunction</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        out.println(indenter.makeString() + "<Function FunctionId=\"" + getIdentifier().toString()
                + "\"/>");
    }

}
