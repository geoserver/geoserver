/*
 * @(#)PolicyReference.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.xacml.combine.CombinerElement;
import com.sun.xacml.combine.CombiningAlgorithm;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This class is used as a placeholder for the PolicyIdReference and PolicySetIdReference fields in
 * a PolicySetType. When a reference is used in a policy set, it is telling the PDP to use an
 * external policy in the current policy. Each time the PDP needs to evaluate that policy reference,
 * it asks the policy finder for the policy. Typically the policy finder will have cached the
 * referenced policy, so this isn't too slow.
 * <p>
 * NOTE: all of the accessor methods, the match method, and the evaluate method require this class
 * to ask its <code>PolicyFinder</code> for the referenced policy, which can be a slow operation.
 * Care should be taken, therefore in calling these methods too often. Also note that it's not safe
 * to cache the results of these calls, since the referenced policy may change.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class PolicyReference extends AbstractPolicy {

    /**
     * Identifies this as a reference to a <code>Policy</code>
     */
    public static final int POLICY_REFERENCE = 0;

    /**
     * Identifies this as a reference to a <code>PolicySet</code>
     */
    public static final int POLICYSET_REFERENCE = 1;

    // the reference
    private URI reference;

    // the reference type
    private int policyType;

    // and version constraints on this reference
    private VersionConstraints constraints;

    // the finder to use in finding the referenced policy
    private PolicyFinder finder;

    // the meta-data for the parent policy
    private PolicyMetaData parentMetaData;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(PolicyReference.class.getName());

    /**
     * Creates a new <code>PolicyReference</code> instance. This has no constraints on version
     * matching. Note that an XACML 1.x reference may not have any constraints.
     * 
     * @param reference
     *            the reference to the policy
     * @param policyType
     *            one of the two fields in this class
     * @param finder
     *            the <code>PolicyFinder</code> used to handle the reference
     * @param parentMetaData
     *            the meta-data associated with the containing (parent) policy
     * 
     * @throws IllegalArgumentException
     *             if the input policyType isn't valid
     */
    public PolicyReference(URI reference, int policyType, PolicyFinder finder,
            PolicyMetaData parentMetaData) throws IllegalArgumentException {
        this(reference, policyType, new VersionConstraints(null, null, null), finder,
                parentMetaData);
    }

    /**
     * Creates a new <code>PolicyReference</code> instance with version constraints. Note that an
     * XACML 1.x reference may not have any constraints.
     * 
     * @param reference
     *            the reference to the policy
     * @param policyType
     *            one of the two fields in this class
     * @param constraints
     *            any optional constraints on the version of the referenced policy (this is never
     *            null, but it may impose no constraints, and in fact will never impose constraints
     *            when used from a pre-2.0 XACML policy)
     * @param finder
     *            the <code>PolicyFinder</code> used to handle the reference
     * @param parentMetaData
     *            the meta-data associated with the containing (parent) policy
     * 
     * @throws IllegalArgumentException
     *             if the input policyType isn't valid
     */
    public PolicyReference(URI reference, int policyType, VersionConstraints constraints,
            PolicyFinder finder, PolicyMetaData parentMetaData) throws IllegalArgumentException {

        // check if input policyType is a valid value
        if ((policyType != POLICY_REFERENCE) && (policyType != POLICYSET_REFERENCE))
            throw new IllegalArgumentException("Input policyType is not a" + "valid value");

        this.reference = reference;
        this.policyType = policyType;
        this.constraints = constraints;
        this.finder = finder;
        this.parentMetaData = parentMetaData;
    }

    /**
     * Creates an instance of a <code>PolicyReference</code> object based on a DOM node.
     * 
     * @deprecated As of 2.0 you should avoid using this method and should instead use the version
     *             that takes a <code>PolicyMetaData</code> instance. This method will only work for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the DOM root of a PolicyIdReference or a PolicySetIdReference XML type
     * @param finder
     *            the <code>PolicyFinder</code> used to handle the reference
     * 
     * @exception ParsingException
     *                if the node is invalid
     */
    public static PolicyReference getInstance(Node root, PolicyFinder finder)
            throws ParsingException {
        return getInstance(root, finder, new PolicyMetaData());
    }

    /**
     * Creates an instance of a <code>PolicyReference</code> object based on a DOM node.
     * 
     * @param root
     *            the DOM root of a PolicyIdReference or a PolicySetIdReference XML type
     * @param finder
     *            the <code>PolicyFinder</code> used to handle the reference
     * @param metaData
     *            the meta-data associated with the containing policy
     * 
     * @exception ParsingException
     *                if the node is invalid
     */
    public static PolicyReference getInstance(Node root, PolicyFinder finder,
            PolicyMetaData metaData) throws ParsingException {
        URI reference = null;
        int policyType;

        // see what type of reference we are
        String name = root.getNodeName();
        if (name.equals("PolicyIdReference")) {
            policyType = POLICY_REFERENCE;
        } else if (name.equals("PolicySetIdReference")) {
            policyType = POLICYSET_REFERENCE;
        } else {
            throw new ParsingException("Unknown reference type: " + name);
        }

        // next get the reference
        try {
            reference = new URI(root.getFirstChild().getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Invalid URI in Reference", e);
        }

        // now get any constraints
        NamedNodeMap map = root.getAttributes();

        String versionConstraint = null;
        Node versionNode = map.getNamedItem("Version");
        if (versionNode != null)
            versionConstraint = versionNode.getNodeValue();

        String earlyConstraint = null;
        Node earlyNode = map.getNamedItem("EarliestVersion");
        if (earlyNode != null)
            earlyConstraint = earlyNode.getNodeValue();

        String lateConstraint = null;
        Node lateNode = map.getNamedItem("LatestVersion");
        if (lateNode != null)
            lateConstraint = lateNode.getNodeValue();

        VersionConstraints constraints = new VersionConstraints(versionConstraint, earlyConstraint,
                lateConstraint);

        // finally, create the reference
        return new PolicyReference(reference, policyType, constraints, finder, metaData);
    }

    /**
     * Returns the refernce identitfier used to resolve the policy.
     * 
     * @return the reference <code>URI</code>
     */
    public URI getReference() {
        return reference;
    }

    /**
     * Returns the version constraints associated with this reference. This will never be null,
     * though the constraints may be empty.
     * 
     * @return the version constraints
     */
    public VersionConstraints getConstraints() {
        return constraints;
    }

    /**
     * Returns whether this is a reference to a policy or to a policy set.
     * 
     * @return the reference type, either <code>POLICY_REFERENCE</code> or
     *         <code>POLICYSET_REFERENCE</code>
     */
    public int getReferenceType() {
        return policyType;
    }

    /**
     * Returns the id of this policy. If the policy is invalid or can't be retrieved, then a runtime
     * exception is thrown.
     * 
     * @return the policy id
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public URI getId() {
        return resolvePolicy().getId();
    }

    /**
     * Returns the version of this policy. If the policy is invalid or can't be retrieved, then a
     * runtime exception is thrown.
     * 
     * @return the policy version
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public String getVersion() {
        return resolvePolicy().getVersion();
    }

    /**
     * Returns the combining algorithm used by this policy. If the policy is invalid or can't be
     * retrieved, then a runtime exception is thrown.
     * 
     * @return the combining algorithm
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public CombiningAlgorithm getCombiningAlg() {
        return resolvePolicy().getCombiningAlg();
    }

    /**
     * Returns the given description of this policy or null if there is no description. If the
     * policy is invalid or can't be retrieved, then a runtime exception is thrown.
     * 
     * @return the description or null
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public String getDescription() {
        return resolvePolicy().getDescription();
    }

    /**
     * Returns the target for this policy. If the policy is invalid or can't be retrieved, then a
     * runtime exception is thrown.
     * 
     * @return the policy's target
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public Target getTarget() {
        return resolvePolicy().getTarget();
    }

    /**
     * Returns the default version for this policy. If the policy is invalid or can't be retrieved,
     * then a runtime exception is thrown.
     * 
     * @return the policy's default version
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public String getDefaultVersion() {
        return resolvePolicy().getDefaultVersion();
    }

    /**
     * Returns the child policy nodes under this node in the policy tree. If the policy is invalid
     * or can't be retrieved, then a runtime exception is thrown.
     * 
     * @return the <code>List</code> of child policy nodes
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public List<PolicyTreeElement> getChildren() {
        return resolvePolicy().getChildren();
    }

    /**
     * Returns the child policy nodes and their associated parameters. If the policy is invalid or
     * can't be retrieved, then a runtime exception is thrown.
     * 
     * @return a <code>List</code> of <code>CombinerElement</code>s
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public List<CombinerElement> getChildElements() {
        return resolvePolicy().getChildElements();
    }

    /**
     * Returns the Set of obligations for this policy, which may be empty if there are no
     * obligations. If the policy is invalid or can't be retrieved, then a runtime exception is
     * thrown.
     * 
     * @return the policy's obligations
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public Set<Obligation> getObligations() {
        return resolvePolicy().getObligations();
    }

    /**
     * Returns the meta-data associated with this policy. If the policy is invalid or can't be
     * retrieved, then a runtime exception is thrown. Note that this is the meta-data for the
     * referenced policy, not the meta-data for the parent policy (which is what gets provided to
     * the constructors of this class).
     * 
     * @return the policy's meta-data
     * 
     * @throws ProcessingException
     *             if the referenced policy can't be retrieved
     */
    public PolicyMetaData getMetaData() {
        return resolvePolicy().getMetaData();
    }

    /**
     * Given the input context sees whether or not the request matches this policy. This must be
     * called by combining algorithms before they evaluate a policy. This is also used in the
     * initial policy finding operation to determine which top-level policies might apply to the
     * request. If the policy is invalid or can't be retrieved, then a runtime exception is thrown.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match the policy and the request
     */
    public MatchResult match(EvaluationCtx context) {
        try {
            return getTarget().match(context);
        } catch (ProcessingException pe) {
            // this means that we couldn't resolve the policy
            ArrayList<String> code = new ArrayList<String>();
            code.add(Status.STATUS_PROCESSING_ERROR);
            Status status = new Status(code, "couldn't resolve policy ref");
            return new MatchResult(MatchResult.INDETERMINATE, status);
        }
    }

    /**
     * Private helper method that tried to resolve the policy
     */
    private AbstractPolicy resolvePolicy() {
        // see if this reference was setup with a finder
        if (finder == null) {
            if (logger.isLoggable(Level.WARNING))
                logger.warning("PolicyReference with id " + reference.toString()
                        + " was queried but was " + "not configured with a PolicyFinder");

            throw new ProcessingException("couldn't find the policy with " + "a null finder");
        }

        PolicyFinderResult pfr = finder.findPolicy(reference, policyType, constraints,
                parentMetaData);

        if (pfr.notApplicable())
            throw new ProcessingException("couldn't resolve the policy");

        if (pfr.indeterminate())
            throw new ProcessingException("error resolving the policy");

        return pfr.getPolicy();
    }

    /**
     * Tries to evaluate the policy by calling the combining algorithm on the given policies or
     * rules. The <code>match</code> method must always be called first, and must always return
     * MATCH, before this method is called.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of evaluation
     */
    public Result evaluate(EvaluationCtx context) {
        // if there is no finder, then we return NotApplicable
        if (finder == null)
            return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());

        PolicyFinderResult pfr = finder.findPolicy(reference, policyType, constraints,
                parentMetaData);

        // if we found nothing, then we return NotApplicable
        if (pfr.notApplicable())
            return new Result(Result.DECISION_NOT_APPLICABLE, context.getResourceId().encode());

        // if there was an error, we return that status data
        if (pfr.indeterminate())
            return new Result(Result.DECISION_INDETERMINATE, pfr.getStatus(), context
                    .getResourceId().encode());

        // we must have found a policy
        return pfr.getPolicy().evaluate(context);
    }

    /**
     * Encodes this <code>PolicyReference</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>PolicyReference</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String encoded = indenter.makeString();

        if (policyType == POLICY_REFERENCE) {
            out.println(encoded + "<PolicyIdReference" + encodeConstraints() + ">"
                    + reference.toString() + "</PolicyIdReference>");
        } else {
            out.println(encoded + "<PolicySetIdReference" + encodeConstraints() + ">"
                    + reference.toString() + "</PolicySetIdReference>");
        }
    }

    /**
     * Private helper method that encodes the variable constraints info. Note that if this is a
     * pre-2.0 policy the constraints are always null, so nothing will be added here.
     */
    private String encodeConstraints() {
        String str = "";
        VersionConstraints version = getConstraints();

        String v = version.getVersionConstraint();
        if (v != null)
            str += " Version=\"" + v + "\"";

        String e = version.getEarliestConstraint();
        if (e != null)
            str += " EarliestVersion=\"" + e + "\"";

        String l = version.getLatestConstraint();
        if (l != null)
            str += " LatestVersion=\"" + l + "\"";

        return str;
    }

}
