/*
 * @(#)Rule.java
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

package com.sun.xacml;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.cond.Apply;
import com.sun.xacml.cond.Condition;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.VariableManager;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

/**
 * Represents the RuleType XACML type. This has a target for matching, and encapsulates the
 * condition and all sub-operations that make up the heart of most policies.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class Rule implements PolicyTreeElement {

    // the attributes associated with this Rule
    private URI idAttr;

    private int effectAttr;

    // the elements in the rule, each of which is optional
    private String description = null;

    private Target target = null;

    private Condition condition = null;

    /**
     * Creates a new <code>Rule</code> object for XACML 1.x and 2.0.
     * 
     * @param id
     *            the rule's identifier
     * @param effect
     *            the effect to return if the rule applies (either Pemit or Deny) as specified in
     *            <code>Result</code>
     * @param description
     *            a textual description, or null
     * @param target
     *            the rule's target, or null if the target is to be inherited from the encompassing
     *            policy
     * @param condition
     *            the rule's condition, or null if there is none
     */
    public Rule(URI id, int effect, String description, Target target, Condition condition) {
        idAttr = id;
        effectAttr = effect;
        this.description = description;
        this.target = target;
        this.condition = condition;
    }

    /**
     * Creates a new <code>Rule</code> object for XACML 1.x only.
     * 
     * @deprecated As of 2.0 you should use the Constructor that accepts the new
     *             <code>Condition</code> class.
     * 
     * @param id
     *            the rule's identifier
     * @param effect
     *            the effect to return if the rule applies (either Pemit or Deny) as specified in
     *            <code>Result</code>
     * @param description
     *            a textual description, or null
     * @param target
     *            the rule's target, or null if the target is to be inherited from the encompassing
     *            policy
     * @param condition
     *            the rule's condition, or null if there is none
     */
    public Rule(URI id, int effect, String description, Target target, Apply condition) {
        idAttr = id;
        effectAttr = effect;
        this.description = description;
        this.target = target;
        this.condition = new Condition(condition.getFunction(), condition.getChildren());
    }

    /**
     * Returns a new instance of the <code>Rule</code> class based on a DOM node. The node must be
     * the root of an XML RuleType.
     * 
     * @deprecated As of 2.0 you should avoid using this method and should instead use the version
     *             that takes a <code>PolicyMetaData</code> instance. This method will only work for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the DOM root of a RuleType XML type
     * @param xpathVersion
     *            the XPath version to use in any selectors or XPath functions, or null if this is
     *            unspecified (ie, not supplied in the defaults section of the policy)
     * 
     * @throws ParsingException
     *             if the RuleType is invalid
     */
    public static Rule getInstance(Node root, String xpathVersion) throws ParsingException {
        return getInstance(root, new PolicyMetaData(PolicyMetaData.XACML_1_0_IDENTIFIER,
                xpathVersion), null);
    }

    /**
     * Returns a new instance of the <code>Rule</code> class based on a DOM node. The node must be
     * the root of an XML RuleType.
     * 
     * @param root
     *            the DOM root of a RuleType XML type
     * @param metaData
     *            the meta-data associated with this Rule's policy
     * @param manager
     *            the <code>VariableManager</code> used to connect <code>VariableReference</code>s
     *            to their cooresponding <code>VariableDefinition<code>s
     * 
     * @throws ParsingException
     *             if the RuleType is invalid
     */
    public static Rule getInstance(Node root, PolicyMetaData metaData, VariableManager manager)
            throws ParsingException {
        URI id = null;
        // String name = null;
        int effect = 0;
        String description = null;
        Target target = null;
        Condition condition = null;

        // first, get the attributes
        NamedNodeMap attrs = root.getAttributes();

        try {
            // get the two required attrs...
            id = new URI(attrs.getNamedItem("RuleId").getNodeValue());
        } catch (URISyntaxException use) {
            throw new ParsingException("Error parsing required attribute " + "RuleId", use);
        }

        String str = attrs.getNamedItem("Effect").getNodeValue();
        if (str.equals("Permit")) {
            effect = Result.DECISION_PERMIT;
        } else if (str.equals("Deny")) {
            effect = Result.DECISION_DENY;
        } else {
            throw new ParsingException("Invalid Effect: " + effect);
        }

        // next, get the elements
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String cname = child.getNodeName();

            if (cname.equals("Description")) {
                description = child.getFirstChild().getNodeValue();
            } else if (cname.equals("Target")) {
                target = Target.getInstance(child, metaData);
            } else if (cname.equals("Condition")) {
                condition = Condition.getInstance(child, metaData, manager);
            }
        }

        return new Rule(id, effect, description, target, condition);
    }

    /**
     * Returns the effect that this <code>Rule</code> will return from the evaluate method (Permit
     * or Deny) if the request applies.
     * 
     * @return a decision effect, as defined in <code>Result</code>
     */
    public int getEffect() {
        return effectAttr;
    }

    /**
     * Returns the id of this <code>Rule</code>
     * 
     * @return the rule id
     */
    public URI getId() {
        return idAttr;
    }

    /**
     * Returns the given description of this <code>Rule</code> or null if there is no description
     * 
     * @return the description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the target for this <code>Rule</code> or null if there is no target
     * 
     * @return the rule's target
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Since a rule is always a leaf in a policy tree because it can have no children, this always
     * returns an empty <code>List</code>.
     * 
     * @return a <code>List</code> with no elements
     */
    public List<PolicyTreeElement> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Returns the condition for this <code>Rule</code> or null if there is no condition
     * 
     * @return the rule's condition
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Given the input context sees whether or not the request matches this <code>Rule</code>'s
     * <code>Target</code>. Note that unlike the matching done by the <code>evaluate</code> method,
     * if the <code>Target</code> is missing than this will return Indeterminate. This lets you
     * write your own custom matching routines for rules but lets evaluation proceed normally.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match this rule and the request
     */
    public MatchResult match(EvaluationCtx context) {
        if (target == null) {
            ArrayList<String> code = new ArrayList<String>();
            code.add(Status.STATUS_PROCESSING_ERROR);
            Status status = new Status(code, "no target available for " + "matching a rule");

            return new MatchResult(MatchResult.INDETERMINATE, status);
        }

        return target.match(context);
    }

    /**
     * Evaluates the rule against the supplied context. This will check that the target matches, and
     * then try to evaluate the condition. If the target and condition apply, then the rule's effect
     * is returned in the result.
     * <p>
     * Note that rules are not required to have targets. If no target is specified, then the rule
     * inherits its parent's target. In the event that this <code>Rule</code> has no
     * <code>Target</code> then the match is assumed to be true, since evaluating a policy tree to
     * this level required the parent's target to match.
     * 
     * @param context
     *            the representation of the request we're evaluating
     * 
     * @return the result of the evaluation
     */
    public Result evaluate(EvaluationCtx context) {
        // If the Target is null then it's supposed to inherit from the
        // parent policy, so we skip the matching step assuming we wouldn't
        // be here unless the parent matched
        if (target != null) {
            MatchResult match = target.match(context);
            int result = match.getResult();

            // if the target didn't match, then this Rule doesn't apply
            if (result == MatchResult.NO_MATCH)
                return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());

            // if the target was indeterminate, we can't go on
            if (result == MatchResult.INDETERMINATE)
                return new Result(Result.DECISION_INDETERMINATE, match.getStatus(), context
                        .getResourceId().encode());
        }

        // if there's no condition, then we just return the effect...
        if (condition == null)
            return new Result(effectAttr, context.getResourceId().encode());

        // ...otherwise we evaluate the condition
        EvaluationResult result = condition.evaluate(context);

        if (result.indeterminate()) {
            // if it was INDETERMINATE, then that's what we return
            return new Result(Result.DECISION_INDETERMINATE, result.getStatus(), context
                    .getResourceId().encode());
        } else {
            // otherwise we return the effect on tue, and NA on false
            BooleanAttribute bool = (BooleanAttribute) (result.getAttributeValue());

            if (bool.getValue())
                return new Result(effectAttr, context.getResourceId().encode());
            else
                return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());
        }
    }

    /**
     * Encodes this <code>Rule</code> into its XML representation and writes this encoding to the
     * given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>Rule</code> into its XML representation and writes this encoding to the
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

        out.print(indent + "<Rule RuleId=\"" + idAttr.toString() + "\" Effect=\""
                + Result.DECISIONS[effectAttr] + "\"");

        if ((description != null) || (target != null) || (condition != null)) {
            // there is some content in the Rule
            out.println(">");

            indenter.in();
            String nextIndent = indenter.makeString();

            if (description != null)
                out.println(nextIndent + "<Description>" + description + "</Description>");

            if (target != null)
                target.encode(output, indenter);

            if (condition != null)
                condition.encode(output, indenter);

            indenter.out();
            out.println(indent + "</Rule>");
        } else {
            // the Rule is empty, so close the tag and we're done
            out.println("/>");
        }
    }

}
