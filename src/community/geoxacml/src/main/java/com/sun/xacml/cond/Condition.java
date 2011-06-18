/*
 * @(#)Condition.java
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
import com.sun.xacml.attr.BooleanAttribute;

/**
 * Represents the XACML ConditionType type. It contains exactly one child expression that is boolean
 * and returns a single value. This class was added in XACML 2.0
 * 
 * @since 2.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class Condition implements Evaluatable {

    // a local Boolean URI that is used as the return type
    private static URI booleanIdentifier;

    // regardless of version, this contains the Condition's children
    private List<Expression> children;

    // regardless of version, this is an expression that can be evaluated
    // directly
    private Expression expression;

    // the condition function, which is only used if this is a 1.x condition
    private Function function;

    // flags whether this is XACML 1.x or 2.0
    private boolean isVersionOne;

    // initialize the boolean identifier
    static {
        try {
            booleanIdentifier = new URI(BooleanAttribute.identifier);
        } catch (Exception e) {
            // we ignore this, since it cannot happen, but it should be
            // flagged in case something changes to trip this case
            booleanIdentifier = null;
        }
    }

    /**
     * Constructs a <code>Condition</code> as used in XACML 1.x.
     * 
     * @param function
     *            the <code>Function</code> to use in evaluating the elements in the Condition
     * @param xprs
     *            the contents of the Condition which will be the parameters to the function, each
     *            of which is an <code>Expression</code>
     * 
     * @throws IllegalArgumentException
     *             if the input expressions don't match the signature of the function or if the
     *             function is invalid for use in a Condition
     */
    public Condition(Function function, List<Expression> expressions)
            throws IllegalArgumentException {
        isVersionOne = true;

        // check that the function is valid for a Condition
        checkExpression(function);

        // turn the parameters into an Apply for simplicity
        expression = new Apply(function, expressions);

        // keep track of the function and the children
        this.function = function;
        children = ((Apply) expression).getChildren();
    }

    /**
     * Constructs a <code>Condition</code> as used in XACML 2.0.
     * 
     * @param expression
     *            the child <code>Expression</code>
     * 
     * @throws IllegalArgumentException
     *             if the expression is not boolean or returns a bag
     */
    public Condition(Expression expression) throws IllegalArgumentException {
        isVersionOne = false;

        // check that the function is valid for a Condition
        checkExpression(expression);

        // store the expression
        this.expression = expression;

        // there is no function in a 2.0 Condition
        function = null;

        // store the expression as the child
        List<Expression> list = new ArrayList<Expression>();
        list.add(this.expression);
        children = Collections.unmodifiableList(list);
    }

    /**
     * Private helper for the constructors that checks if a given expression is valid for the root
     * of a Condition
     */
    private void checkExpression(Expression xpr) {
        // make sure it's a boolean expression...
        if (!xpr.getType().equals(booleanIdentifier))
            throw new IllegalArgumentException("A Condition must return a "
                    + "boolean...cannot create " + "with " + xpr.getType());

        // ...and that it never returns a bag
        if (xpr.returnsBag())
            throw new IllegalArgumentException("A Condition must not return " + "a Bag");
    }

    /**
     * Returns an instance of <code>Condition</code> based on the given DOM root.
     * 
     * @param root
     *            the DOM root of a ConditionType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * @param manager
     *            <code>VariableManager</code> used to connect references and definitions while
     *            parsing
     * 
     * @throws ParsingException
     *             if this is not a valid ConditionType
     */
    public static Condition getInstance(Node root, PolicyMetaData metaData, VariableManager manager)
            throws ParsingException {
        if (metaData.getXACMLVersion() < PolicyMetaData.XACML_VERSION_2_0) {
            Apply cond = Apply.getConditionInstance(root, metaData.getXPathIdentifier(), manager);
            return new Condition(cond.getFunction(), cond.getChildren());
        } else {
            Expression xpr = null;
            NodeList nodes = root.getChildNodes();

            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    xpr = ExpressionHandler.parseExpression(nodes.item(i), metaData, manager);
                    break;
                }
            }

            return new Condition(xpr);
        }
    }

    /**
     * Returns the <code>Function</code> used by this <code>Condition</code> if this is a 1.x
     * condition, or null if this is a 2.0 condition.
     * 
     * @return a <code>Function</code> or null
     */
    public Function getFunction() {
        return function;
    }

    /**
     * Returns the <code>List</code> of children for this <code>Condition</code>. The
     * <code>List</code> contains <code>Expression</code>s. The list is unmodifiable.
     * 
     * @return a <code>List</code> of <code>Expression</code>s
     */
    public List<Expression> getChildren() {
        return children;
    }

    /**
     * Returns the type of attribute that this object will return on a call to <code>evaluate</code>
     * . This is always a boolean, since that's all that a Condition is allowed to return.
     * 
     * @return the boolean type
     */
    public URI getType() {
        return booleanIdentifier;
    }

    /**
     * Returns whether or not this <code>Condition</code> will return a bag of values on evaluation.
     * This always returns false, since a Condition isn't allowed to return a bag.
     * 
     * @return false
     */
    public boolean returnsBag() {
        return false;
    }

    /**
     * Returns whether or not this <code>Condition</code> will return a bag of values on evaluation.
     * This always returns false, since a Condition isn't allowed to return a bag.
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @return false
     */
    public boolean evaluatesToBag() {
        return false;
    }

    /**
     * Evaluates the <code>Condition</code> by evaluating its child <code>Expression</code>.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to evaluate this condition object
     */
    public EvaluationResult evaluate(EvaluationCtx context) {
        // Note that it's technically possible for this expression to
        // be something like a Function, which isn't Evaluatable. It
        // wouldn't make sense to have this, but it is possible. Because
        // it makes no sense, however, it's unlcear exactly what the
        // error should be, so raising the ClassCastException here seems
        // as good an approach as any for now...
        return ((Evaluatable) expression).evaluate(context);
    }

    /**
     * Encodes this <code>Condition</code> into its XML representation and writes this encoding to
     * the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>Condition</code> into its XML representation and writes this encoding to
     * the given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        if (isVersionOne) {
            out.println(indent + "<Condition FunctionId=\"" + function.getIdentifier() + "\">");
            indenter.in();

            for (Expression xpr : children)
                xpr.encode(output, indenter);
        } else {
            out.println(indent + "<Condition>");
            indenter.in();

            expression.encode(output, indenter);
        }

        indenter.out();
        out.println(indent + "</Condition>");
    }

}
