/*
 * @(#)VariableReference.java
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
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.ProcessingException;

/**
 * This class supports the VariableReferenceType type introuced in XACML 2.0. It allows an
 * expression to reference a variable definition. If there is no such definition then the Policy is
 * invalid. A reference can be included anywwhere in an expression where the referenced expression
 * would be valid.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class VariableReference implements Expression {

    // the identifier used to resolve the reference
    private String variableId;

    // the actual definition we refernce, if it's known
    private VariableDefinition definition = null;

    // a manager for resolving references, if it's been provided
    private VariableManager manager = null;

    /**
     * Simple constructor that takes only the identifier. This is provided for tools that want to
     * build policies only for the sake of encoding or displaying them. This constructor will not
     * create a reference that can be followed to its associated definition, so it cannot be used in
     * evaluation.
     * 
     * @param variableId
     *            the reference identifier
     */
    public VariableReference(String variableId) {
        this.variableId = variableId;
    }

    /**
     * Constructor that takes the definition referenced by this class. If you're building policies
     * programatically, this is typically the form you use. It does make the connection from
     * reference to definition, so this will result in an evaluatable reference.
     * 
     * @param definition
     *            the definition this class references
     */
    public VariableReference(VariableDefinition definition) {
        this.variableId = definition.getVariableId();
        this.definition = definition;
    }

    /**
     * Constructor that takes the reference identifier and a manager. This is typically only used by
     * parsing code, since the manager is used to handle out-of-order definitions and circular
     * references.
     * 
     * @param variableId
     *            the reference identifier
     * @param manager
     *            a <code>VariableManager</code> used to handle the dependencies between references
     *            and definitions during parsing
     */
    public VariableReference(String variableId, VariableManager manager) {
        this.variableId = variableId;
        this.manager = manager;
    }

    /**
     * Returns a new instance of the <code>VariableReference</code> class based on a DOM node. The
     * node must be the root of an XML VariableReferenceType.
     * 
     * @param root
     *            the DOM root of a VariableReferenceType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * @param manager
     *            the <code>VariableManager</code> used to connect this reference to its definition
     * 
     * @throws ParsingException
     *             if the VariableReferenceType is invalid
     */
    public static VariableReference getInstance(Node root, PolicyMetaData metaData,
            VariableManager manager) throws ParsingException {
        // pretty easy, since there's just an attribute...
        String variableId = root.getAttributes().getNamedItem("VariableId").getNodeValue();

        // ...but we keep the manager since after this we'll probably get
        // asked for our type, etc., and the manager will also be used to
        // resolve the actual definition
        return new VariableReference(variableId, manager);
    }

    /**
     * Returns the reference identifier.
     * 
     * @return the reference's identifier
     */
    public String getVariableId() {
        return variableId;
    }

    /**
     * Returns the <code>VariableDefinition</code> referenced by this class, or null if the
     * definition cannot be resolved.
     * 
     * @return the referenced definition or null
     */
    public VariableDefinition getReferencedDefinition() {
        // if this was created with a concrete definition, then that's what
        // we return, otherwise we query the manager (if we have one)
        if (definition != null) {
            return definition;
        } else if (manager != null) {
            return manager.getDefinition(variableId);
        }

        // if the simple constructor was used, then we have nothing
        return null;
    }

    /**
     * Evaluates the referenced expression using the given context, and either returns an error or a
     * resulting value. If this doesn't reference an evaluatable expression (eg, a single Function)
     * then this will throw an exception.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of evaluation
     */
    public EvaluationResult evaluate(EvaluationCtx context) {
        Expression xpr = getReferencedDefinition().getExpression();

        // Note that it's technically possible for this expression to
        // be something like a Function, which isn't Evaluatable. It
        // wouldn't make sense to have this, but it is possible. Because
        // it makes no sense, however, it's unlcear exactly what the
        // error should be, so raising the ClassCastException here seems
        // as good an approach as any for now...
        return ((Evaluatable) xpr).evaluate(context);
    }

    /**
     * Returns the type of the referenced expression.
     * 
     * @return the attribute return type of the referenced expression
     * 
     * @throws ProcessingException
     *             if the type couldn't be resolved
     */
    public URI getType() {
        // if we have a concrete definition, then ask it for the type,
        // otherwise query the manager using the getVariableType method,
        // since this handles type-checking for definitions that haven't
        // been parsed yet
        if (definition != null) {
            return definition.getExpression().getType();
        } else {
            if (manager != null)
                return manager.getVariableType(variableId);
        }

        throw new ProcessingException("couldn't resolve the type");
    }

    /**
     * Tells whether evaluation will return a bag or a single value.
     * 
     * @return true if evaluation will return a bag, false otherwise
     * 
     * @throws ProcessingException
     *             if the return type couldn't be resolved
     */
    public boolean returnsBag() {
        // see comment in getType()
        if (definition != null) {
            return getReferencedDefinition().getExpression().returnsBag();
        } else {
            if (manager != null)
                return manager.returnsBag(variableId);
        }

        throw new ProcessingException("couldn't resolve the return type");
    }

    /**
     * Tells whether evaluation will return a bag or a single value.
     * 
     * @return true if evaluation will return a bag, false otherwise
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @throws ProcessingException
     *             if the return type couldn't be resolved
     */
    public boolean evaluatesToBag() {
        return returnsBag();
    }

    /**
     * Always returns an empty list since references never have children in the policy tree. Note
     * that the referenced definition may still have children, so tools may want to treat these as
     * children of this reference, but must take care since circular references could create a tree
     * of infinite depth.
     * 
     * @return an empty <code>List</code>
     */
    public List<Expression> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Encodes this class into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this class into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        out.println(indent + "<VariableReference VariableId=\"" + variableId + "\"/>");
    }

}
