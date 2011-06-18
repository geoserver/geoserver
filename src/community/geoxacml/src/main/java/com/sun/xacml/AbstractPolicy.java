/*
 * @(#)AbstractPolicy.java
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.combine.CombinerElement;
import com.sun.xacml.combine.CombinerParameter;
import com.sun.xacml.combine.CombiningAlgFactory;
import com.sun.xacml.combine.CombiningAlgorithm;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.combine.RuleCombiningAlgorithm;
import com.sun.xacml.ctx.Result;

/**
 * Represents an instance of an XACML policy.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public abstract class AbstractPolicy implements PolicyTreeElement {

    // atributes associated with this policy
    private URI idAttr;

    private String version;

    private CombiningAlgorithm combiningAlg;

    // the elements in the policy
    private String description;

    private Target target;

    // the value in defaults, or null if there was no default value
    private String defaultVersion;

    // the meta-data associated with this policy
    private PolicyMetaData metaData;

    // the child elements under this policy represented simply as the
    // PolicyTreeElements...
    private List<PolicyTreeElement> children;

    // ...or the CombinerElements that are passed to combining algorithms
    private List<CombinerElement> childElements;

    // any obligations held by this policy
    private Set<Obligation> obligations;

    // the list of combiner parameters
    private List<CombinerParameter> parameters;

    // the logger we'll use for all messages
    // private static final Logger logger =
    // Logger.getLogger(AbstractPolicy.class.getName());

    /**
     * Constructor used by <code>PolicyReference</code>, which supplies its own values for the
     * methods in this class.
     */
    protected AbstractPolicy() {

    }

    /**
     * Constructor used to create a policy from concrete components.
     * 
     * @param id
     *            the policy id
     * @param version
     *            the policy version or null for the default (this is always null for pre-2.0
     *            policies)
     * @param combiningAlg
     *            the combining algorithm to use
     * @param description
     *            describes the policy or null if there is none
     * @param target
     *            the policy's target
     */
    protected AbstractPolicy(URI id, String version, CombiningAlgorithm combiningAlg,
            String description, Target target) {
        this(id, version, combiningAlg, description, target, null);
    }

    /**
     * Constructor used to create a policy from concrete components.
     * 
     * @param id
     *            the policy id
     * @param version
     *            the policy version or null for the default (this is always null for pre-2.0
     *            policies)
     * @param combiningAlg
     *            the combining algorithm to use
     * @param description
     *            describes the policy or null if there is none
     * @param target
     *            the policy's target
     * @param defaultVersion
     *            the XPath version to use for selectors
     */
    protected AbstractPolicy(URI id, String version, CombiningAlgorithm combiningAlg,
            String description, Target target, String defaultVersion) {
        this(id, version, combiningAlg, description, target, defaultVersion, null, null);
    }

    /**
     * Constructor used to create a policy from concrete components.
     * 
     * @param id
     *            the policy id
     * @param version
     *            the policy version or null for the default (this is always null for pre-2.0
     *            policies)
     * @param combiningAlg
     *            the combining algorithm to use
     * @param description
     *            describes the policy or null if there is none
     * @param target
     *            the policy's target
     * @param defaultVersion
     *            the XPath version to use for selectors
     * @param obligations
     *            the policy's obligations
     */
    protected AbstractPolicy(URI id, String version, CombiningAlgorithm combiningAlg,
            String description, Target target, String defaultVersion, Set<Obligation> obligations,
            List<CombinerParameter> parameters) {
        idAttr = id;
        this.combiningAlg = combiningAlg;
        this.description = description;
        this.target = target;
        this.defaultVersion = defaultVersion;

        if (version == null)
            this.version = "1.0";
        else
            this.version = version;

        // FIXME: this needs to fill in the meta-data correctly
        metaData = null;

        if (obligations == null)
            this.obligations = Collections.emptySet();
        else
            this.obligations = Collections.unmodifiableSet(new HashSet<Obligation>(obligations));

        if (parameters == null)
            this.parameters = Collections.emptyList();
        else
            this.parameters = Collections.unmodifiableList(new ArrayList<CombinerParameter>(
                    parameters));
    }

    /**
     * Constructor used by child classes to initialize the shared data from a DOM root node.
     * 
     * @param root
     *            the DOM root of the policy
     * @param policyPrefix
     *            either "Policy" or "PolicySet"
     * @param combiningName
     *            name of the field naming the combining alg
     * 
     * @throws ParsingException
     *             if the policy is invalid
     */
    protected AbstractPolicy(Node root, String policyPrefix, String combiningName)
            throws ParsingException {
        // get the attributes, all of which are common to Policies
        NamedNodeMap attrs = root.getAttributes();

        try {
            // get the attribute Id
            idAttr = new URI(attrs.getNamedItem(policyPrefix + "Id").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Error parsing required attribute " + policyPrefix + "Id", e);
        }

        // see if there's a version
        Node versionNode = attrs.getNamedItem("Version");
        if (versionNode != null) {
            version = versionNode.getNodeValue();
        } else {
            // assign the default version
            version = "1.0";
        }

        // now get the combining algorithm...
        try {
            URI algId = new URI(attrs.getNamedItem(combiningName).getNodeValue());
            CombiningAlgFactory factory = CombiningAlgFactory.getInstance();
            combiningAlg = factory.createAlgorithm(algId);
        } catch (Exception e) {
            throw new ParsingException("Error parsing combining algorithm" + " in " + policyPrefix,
                    e);
        }

        // ...and make sure it's the right kind
        if (policyPrefix.equals("Policy")) {
            if (!(combiningAlg instanceof RuleCombiningAlgorithm))
                throw new ParsingException("Policy must use a Rule " + "Combining Algorithm");
        } else {
            if (!(combiningAlg instanceof PolicyCombiningAlgorithm))
                throw new ParsingException("PolicySet must use a Policy " + "Combining Algorithm");
        }

        // do an initial pass through the elements to pull out the
        // defaults, if any, so we can setup the meta-data
        NodeList children = root.getChildNodes();
        // String xpathVersion = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(policyPrefix + "Defaults"))
                handleDefaults(child);
        }

        // with the defaults read, create the meta-data
        metaData = new PolicyMetaData(root.getNamespaceURI(), defaultVersion);

        // now read the remaining policy elements
        obligations = new HashSet<Obligation>();
        parameters = new ArrayList<CombinerParameter>();
        children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String cname = child.getNodeName();

            if (cname.equals("Description")) {
                if (child.hasChildNodes())
                    description = child.getFirstChild().getNodeValue();
            } else if (cname.equals("Target")) {
                target = Target.getInstance(child, metaData);
            } else if (cname.equals("Obligations")) {
                parseObligations(child);
            } else if (cname.equals("CombinerParameters")) {
                handleParameters(child);
            }
        }

        // finally, make sure the obligations and parameters are immutable
        obligations = Collections.unmodifiableSet(obligations);
        parameters = Collections.unmodifiableList(parameters);
    }

    /**
     * Helper routine to parse the obligation data
     */
    private void parseObligations(Node root) throws ParsingException {
        NodeList nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("Obligation"))
                obligations.add(Obligation.getInstance(node));
        }
    }

    /**
     * There used to be multiple things in the defaults type, but now there's just the one string
     * that must be a certain value, so it doesn't seem all that useful to have a class for
     * this...we could always bring it back, however, if it started to do more
     */
    private void handleDefaults(Node root) throws ParsingException {
        defaultVersion = null;
        NodeList nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("XPathVersion"))
                defaultVersion = node.getFirstChild().getNodeValue();
        }
    }

    /**
     * Handles all the CombinerParameters in the policy or policy set
     */
    private void handleParameters(Node root) throws ParsingException {
        NodeList nodes = root.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("CombinerParameter"))
                parameters.add(CombinerParameter.getInstance(node));
        }
    }

    /**
     * Returns the id of this policy
     * 
     * @return the policy id
     */
    public URI getId() {
        return idAttr;
    }

    /**
     * Returns the version of this policy. If this is an XACML 1.x policy then this will always
     * return <code>"1.0"</code>.
     * 
     * @return the policy version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the combining algorithm used by this policy
     * 
     * @return the combining algorithm
     */
    public CombiningAlgorithm getCombiningAlg() {
        return combiningAlg;
    }

    /**
     * Returns the list of input parameters for the combining algorithm. If this is an XACML 1.x
     * policy then the list will always be empty.
     * 
     * @return a <code>List</code> of <code>CombinerParameter</code>s
     */
    public List<CombinerParameter> getCombiningParameters() {
        return parameters;
    }

    /**
     * Returns the given description of this policy or null if there is no description
     * 
     * @return the description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the target for this policy
     * 
     * @return the policy's target
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Returns the XPath version to use or null if none was specified
     * 
     * @return XPath version or null
     */
    public String getDefaultVersion() {
        return defaultVersion;
    }

    /**
     * Returns the <code>List</code> of children under this node in the policy tree. Depending on
     * what kind of policy this node represents the children will either be
     * <code>AbstractPolicy</code> objects or <code>Rule</code>s.
     * 
     * @return a <code>List</code> of child nodes
     */
    public List<PolicyTreeElement> getChildren() {
        return children;
    }

    /**
     * Returns the <code>List</code> of <code>CombinerElement</code>s that is provided to the
     * combining algorithm. This returns the same set of children that <code>getChildren</code>
     * provides along with any associated combiner parameters.
     * 
     * @return a <code>List</code> of <code>CombinerElement</code>s
     */
    public List<CombinerElement> getChildElements() {
        return childElements;
    }

    /**
     * Returns the Set of obligations for this policy, which may be empty
     * 
     * @return the policy's obligations
     */
    public Set<Obligation> getObligations() {
        return obligations;
    }

    /**
     * Returns the meta-data associated with this policy
     */
    public PolicyMetaData getMetaData() {
        return metaData;
    }

    /**
     * Given the input context sees whether or not the request matches this policy. This must be
     * called by combining algorithms before they evaluate a policy. This is also used in the
     * initial policy finding operation to determine which top-level policies might apply to the
     * request.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match the policy and the request
     */
    public MatchResult match(EvaluationCtx context) {
        return target.match(context);
    }

    /**
     * Sets the child policy tree elements for this node, which are passed to the combining
     * algorithm on evaluation. The <code>List</code> must contain <code>CombinerElement</code>s,
     * which in turn will contain <code>Rule</code>s or <code>AbstractPolicy</code>s, but may not
     * contain both types of elements.
     * 
     * @param children
     *            a <code>List</code> of <code>CombinerElement</code>s representing the child
     *            elements used by the combining algorithm
     */
    protected void setChildren(List<? extends CombinerElement> children) {
        // we always want a concrete list, since we're going to pass it to
        // a combiner that expects a non-null input
        if (children == null) {
            this.children = Collections.emptyList();
        } else {
            // NOTE: since this is only getting called by known child
            // classes we don't check that the types are all the same
            List<PolicyTreeElement> list = new ArrayList<PolicyTreeElement>();
            for (CombinerElement element : children)
                list.add(element.getElement());

            this.children = Collections.unmodifiableList(list);
            childElements = Collections.unmodifiableList(children);
        }
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
        // evaluate
        Result result = combiningAlg.combine(context, parameters, childElements);

        // if we have no obligations, we're done
        if (obligations.size() == 0)
            return result;

        // now, see if we should add any obligations to the set
        int effect = result.getDecision();

        if ((effect == Result.DECISION_INDETERMINATE) || (effect == Result.DECISION_NOT_APPLICABLE)) {
            // we didn't permit/deny, so we never return obligations
            return result;
        }

        for (Obligation obligation : obligations) {
            if (obligation.getFulfillOn() == effect)
                result.addObligation(obligation);
        }

        // finally, return the result
        return result;
    }

    /**
     * Routine used by <code>Policy</code> and <code>PolicySet</code> to encode some common
     * elements.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    protected void encodeCommonElements(OutputStream output, Indenter indenter) {

        for (CombinerElement elem : childElements)
            elem.encode(output, indenter);

        if (obligations.size() != 0) {
            PrintStream out = new PrintStream(output);
            String indent = indenter.makeString();

            out.println(indent + "<Obligations>");
            indenter.in();

            for (Obligation obligation : obligations)
                obligation.encode(output, indenter);

            out.println(indent + "</Obligations>");
            indenter.out();
        }
    }

}
