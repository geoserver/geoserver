/*
 * @(#)Apply.java
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;

/**
 * Represents the XACML ApplyType and ConditionType XML types.
 * <p>
 * Note well: as of 2.0, there is no longer a notion of a separate higher- order bag function.
 * Instead, if needed, it is supplied as one of the <code>Expression</code>s in the parameter list.
 * As such, when this <code>Apply</code> is evaluated, it no longer pre-evaluates all the parameters
 * if a bag function is used. It is now up to the implementor of a higher-order function to do this.
 * <p>
 * Also, as of 2.0, the <code>Apply</code> is no longer used to represent a Condition, since the
 * XACML 2.0 specification changed how Condition works. Instead, there is now a
 * <code>Condition</code> class that represents both 1.x and 2.0 style Conditions.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class Apply implements Evaluatable {

    // the function used to evaluate the contents of the apply
    private Function function;

    // the paramaters to the function...ie, the contents of the apply
    private List<Expression> xprs;

    /**
     * Constructs an <code>Apply</code> instance.
     * 
     * @param function
     *            the <code>Function</code> to use in evaluating the elements in the apply
     * @param xprs
     *            the contents of the apply which will be the parameters to the function, each of
     *            which is an <code>Expression</code>
     * 
     * @throws IllegalArgumentException
     *             if the input expressions don't match the signature of the function
     */
    public Apply(Function function, List<Expression> xprs) throws IllegalArgumentException {
        // check that the given inputs work for the function
        function.checkInputs(xprs);

        // if everything checks out, then store the inputs
        this.function = function;
        this.xprs = Collections.unmodifiableList(new ArrayList<Expression>(xprs));
    }

    /**
     * Constructs an <code>Apply</code> instance.
     * 
     * @deprecated As of 2.0 <code>Apply</code> is no longer used for Conditions, so the
     *             <code>isCondition</code> parameter is no longer needed. You should now use the 2
     *             parameter constructor. This constructor will be removed in a future release.
     * 
     * @param function
     *            the <code>Function</code> to use in evaluating the elements in the apply
     * @param xprs
     *            the contents of the apply which will be the parameters to the function, each of
     *            which is an <code>Expression</code>
     * @param isCondition
     *            as of 2.0, this must always be false
     * 
     * @throws IllegalArgumentException
     *             if the input expressions don't match the signature of the function or if
     *             <code>isCondition</code> is true
     */
    public Apply(Function function, List<Expression> xprs, boolean isCondition)
            throws IllegalArgumentException {
        // make sure that no is using this constructor to create a Condition
        if (isCondition)
            throw new IllegalArgumentException("As of version 2.0 an Apply"
                    + " may not represent a" + " Condition");

        // check that the given inputs work for the function
        function.checkInputs(xprs);

        // if everything checks out, then store the inputs
        this.function = function;
        this.xprs = Collections.unmodifiableList(new ArrayList<Expression>(xprs));
    }

    /**
     * Returns an instance of an <code>Apply</code> based on the given DOM root node. This will
     * actually return a special kind of <code>Apply</code>, namely an XML ConditionType, which is
     * the root of the condition logic in a RuleType. A ConditionType is the same as an ApplyType
     * except that it must use a FunctionId that returns a boolean value.
     * <p>
     * Note that as of 2.0 there is a separate <code>Condition</code> class used to support the
     * different kinds of Conditions in XACML 1.x and 2.0. As such, the system no longer treats a
     * ConditionType as a special kind of ApplyType. You may still use this method to get a 1.x
     * style ConditionType, but you will need to convert it into a <code>Condition</code> to use it
     * in evaluation. The preferred way to create a Condition is now through the
     * <code>getInstance</code> method on <code>Condition</code>.
     * 
     * @param root
     *            the DOM root of a ConditionType XML type
     * @param xpathVersion
     *            the XPath version to use in any selectors or XPath functions, or null if this is
     *            unspecified (ie, not supplied in the defaults section of the policy)
     * @param manager
     *            <code>VariableManager</code> used to connect references and definitions while
     *            parsing
     * 
     * @throws ParsingException
     *             if this is not a valid ConditionType
     */
    public static Apply getConditionInstance(Node root, String xpathVersion, VariableManager manager)
            throws ParsingException {
        return getInstance(root, FunctionFactory.getConditionInstance(), new PolicyMetaData(
                PolicyMetaData.XACML_1_0_IDENTIFIER, xpathVersion), manager);
    }

    /**
     * Returns an instance of an <code>Apply</code> based on the given DOM root node. This will
     * actually return a special kind of <code>Apply</code>, namely an XML ConditionType, which is
     * the root of the condition logic in a RuleType. A ConditionType is the same as an ApplyType
     * except that it must use a FunctionId that returns a boolean value.
     * 
     * @deprecated As of 2.0 you should avoid using this method, since it does not provide a
     *             <code>Condition</code> instance and does not handle XACML 2.0 policies correctly.
     *             If you need a similar method you can use the new version that accepts a
     *             <code>VariableManager</code>. This will return an <code>Apply</code> instance for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the DOM root of a ConditionType XML type
     * @param xpathVersion
     *            the XPath version to use in any selectors or XPath functions, or null if this is
     *            unspecified (ie, not supplied in the defaults section of the policy)
     * 
     * @throws ParsingException
     *             if this is not a valid ConditionType
     */
    public static Apply getConditionInstance(Node root, String xpathVersion)
            throws ParsingException {
        return getInstance(root, FunctionFactory.getConditionInstance(), new PolicyMetaData(
                PolicyMetaData.XACML_1_0_IDENTIFIER, xpathVersion), null);
    }

    /**
     * Returns an instance of <code>Apply</code> based on the given DOM root.
     * 
     * @param root
     *            the DOM root of an ApplyType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * @param manager
     *            <code>VariableManager</code> used to connect references and definitions while
     *            parsing
     * 
     * @throws ParsingException
     *             if this is not a valid ApplyType
     */
    public static Apply getInstance(Node root, PolicyMetaData metaData, VariableManager manager)
            throws ParsingException {
        return getInstance(root, FunctionFactory.getGeneralInstance(), metaData, manager);
    }

    /**
     * Returns an instance of <code>Apply</code> based on the given DOM root.
     * 
     * @deprecated As of 2.0 you should avoid using this method, since it does not handle XACML 2.0
     *             policies correctly. If you need a similar method you can use the new version that
     *             accepts a <code>VariableManager</code>. This will return an <code>Apply</code>
     *             instance for XACML 1.x policies.
     * 
     * @param root
     *            the DOM root of an ApplyType XML type
     * @param xpathVersion
     *            the XPath version to use in any selectors or XPath functions, or null if this is
     *            unspecified (ie, not supplied in the defaults section of the policy)
     * 
     * @throws ParsingException
     *             if this is not a valid ApplyType
     */
    public static Apply getInstance(Node root, String xpathVersion) throws ParsingException {
        return getInstance(root, FunctionFactory.getGeneralInstance(), new PolicyMetaData(
                PolicyMetaData.XACML_1_0_IDENTIFIER, xpathVersion), null);
    }

    /**
     * This is a helper method that is called by the two getInstance methods. It takes a factory so
     * we know that we're getting the right kind of function.
     */
    private static Apply getInstance(Node root, FunctionFactory factory, PolicyMetaData metaData,
            VariableManager manager) throws ParsingException {
        Function function = ExpressionHandler.getFunction(root, metaData, factory);
        List<Expression> xprs = new ArrayList<Expression>();

        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Expression xpr = ExpressionHandler.parseExpression(nodes.item(i), metaData, manager);

            if (xpr != null)
                xprs.add(xpr);
        }

        return new Apply(function, xprs);
    }

    /**
     * Returns the <code>Function</code> used by this <code>Apply</code>.
     * 
     * @return the <code>Function</code>
     */
    public Function getFunction() {
        return function;
    }

    /**
     * Returns the <code>List</code> of children for this <code>Apply</code>. The <code>List</code>
     * contains <code>Expression</code>s. The list is unmodifiable, and may be empty.
     * 
     * @return a <code>List</code> of <code>Expression</code>s
     */
    public List<Expression> getChildren() {
        return xprs;
    }

    /**
     * Returns whether or not this ApplyType is actually a ConditionType. As of 2.0 this always
     * returns false;
     * 
     * @deprecated As of 2.0 this method should not be used, since an <code>Apply</code> is never a
     *             Condition.
     * 
     * @return false
     */
    public boolean isCondition() {
        return false;
    }

    /**
     * Evaluates the apply object using the given function. This will in turn call evaluate on all
     * the given parameters, some of which may be other <code>Apply</code> objects.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to evaluate this apply object
     */
    public EvaluationResult evaluate(EvaluationCtx context) {
        // Note that prior to the 2.0 codebase, this method was much more
        // complex, pre-evaluating the higher-order functions. Because this
        // was never really the right behavior (there's no reason that a
        // function can only be at the start of an Apply), we no longer make
        // assumptions at this point, so the higher order functions are
        // left to evaluate their own parameters.
        return function.evaluate(xprs, context);
    }

    /**
     * Returns the type of attribute that this object will return on a call to <code>evaluate</code>
     * . In practice, this will always be the same as the result of calling
     * <code>getReturnType</code> on the function used by this object.
     * 
     * @return the type returned by <code>evaluate</code>
     */
    public URI getType() {
        return function.getReturnType();
    }

    /**
     * Returns whether or not the <code>Function</code> will return a bag of values on evaluation.
     * 
     * @return true if evaluation will return a bag of values, false otherwise
     */
    public boolean returnsBag() {
        return function.returnsBag();
    }

    /**
     * Returns whether or not the <code>Function</code> will return a bag of values on evaluation.
     * 
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @return true if evaluation will return a bag of values, false otherwise
     */
    public boolean evaluatesToBag() {
        return function.returnsBag();
    }

    /**
     * Encodes this <code>Apply</code> into its XML representation and writes this encoding to the
     * given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>Apply</code> into its XML representation and writes this encoding to the
     * given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        out.println(indent + "<Apply FunctionId=\"" + function.getIdentifier() + "\">");
        indenter.in();

        for (Expression xpr : xprs)
            xpr.encode(output, indenter);

        indenter.out();
        out.println(indent + "</Apply>");
    }

}
