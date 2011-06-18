/*
 * @(#)VariableDefinition.java
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;

/**
 * This class supports the VariableDefinitionType type introuced in XACML 2.0. It allows a Policy to
 * pre-define any number of expression blocks for general use. Note that it's legal (though not
 * usually useful) to define expressions that don't get referenced within the Policy. It is illegal
 * to have more than one definition with the same identifier within a Policy.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class VariableDefinition {

    // the identitifer for this definition
    private String variableId;

    // the actual expression defined here
    private Expression expression;

    /**
     * Creates a new <code>VariableDefinition</code> with the given identifier and expression.
     * 
     * @param variableId
     *            the identifier for this definition
     * @param expression
     *            the expression defined here
     */
    public VariableDefinition(String variableId, Expression expression) {
        this.variableId = variableId;
        this.expression = expression;
    }

    /**
     * Returns a new instance of the <code>VariableDefinition</code> class based on a DOM node. The
     * node must be the root of an XML VariableDefinitionType.
     * 
     * @param root
     *            the DOM root of a VariableDefinitionType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * @param manager
     *            <code>VariableManager</code> used to connect references to this definition
     * 
     * @throws ParsingException
     *             if the VariableDefinitionType is invalid
     */
    public static VariableDefinition getInstance(Node root, PolicyMetaData metaData,
            VariableManager manager) throws ParsingException {
        String variableId = root.getAttributes().getNamedItem("VariableId").getNodeValue();

        // get the first element, which is the expression node
        NodeList nodes = root.getChildNodes();
        Node xprNode = nodes.item(0);
        int i = 1;
        while (xprNode.getNodeType() != Node.ELEMENT_NODE)
            xprNode = nodes.item(i++);

        // use that node to get the expression
        Expression xpr = ExpressionHandler.parseExpression(xprNode, metaData, manager);

        return new VariableDefinition(variableId, xpr);
    }

    /**
     * Returns the identifier for this definition.
     * 
     * @return the definition's identifier
     */
    public String getVariableId() {
        return variableId;
    }

    /**
     * Returns the expression provided by this definition.
     * 
     * @return the definition's expression
     */
    public Expression getExpression() {
        return expression;
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

        out.println(indent + "<VariableDefinition VariableId=\"" + variableId + "\">");
        indenter.in();

        expression.encode(output, indenter);

        out.println("</VariableDefinition>");
        indenter.out();
    }

}
