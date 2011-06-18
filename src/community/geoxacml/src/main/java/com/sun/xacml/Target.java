/*
 * @(#)Target.java
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
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents the TargetType XML type in XACML. This also stores several other XML types: Subjects,
 * Resources, Actions, and Environments (in XACML 2.0 and later). The target is used to quickly
 * identify whether the parent element (a policy set, policy, or rule) is applicable to a given
 * request.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class Target {

    // the four sections of a Target
    private TargetSection subjectsSection;

    private TargetSection resourcesSection;

    private TargetSection actionsSection;

    private TargetSection environmentsSection;

    // the version of XACML of the policy containing this target
    private int xacmlVersion;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(Target.class.getName());

    /**
     * Constructor that creates an XACML 1.x <code>Target</code> from components. Each of the
     * sections must be non-null, but they may match any request. Because this is only used for 1.x
     * Targets, there is no Environments section.
     * 
     * @param subjectsSection
     *            a <code>TargetSection</code> representing the Subjects section of this target
     * @param resourcesSection
     *            a <code>TargetSection</code> representing the Resources section of this target
     * @param actionsSection
     *            a <code>TargetSection</code> representing the Actions section of this target
     */
    public Target(TargetSection subjectsSection, TargetSection resourcesSection,
            TargetSection actionsSection) {
        if ((subjectsSection == null) || (resourcesSection == null) || (actionsSection == null))
            throw new ProcessingException("All sections of a Target must " + "be non-null");

        this.subjectsSection = subjectsSection;
        this.resourcesSection = resourcesSection;
        this.actionsSection = actionsSection;
        this.environmentsSection = new TargetSection(null, TargetMatch.ENVIRONMENT,
                PolicyMetaData.XACML_VERSION_1_0);
        this.xacmlVersion = PolicyMetaData.XACML_VERSION_1_0;
    }

    /**
     * Constructor that creates an XACML 2.0 <code>Target</code> from components. Each of the
     * sections must be non-null, but they may match any request.
     * 
     * @param subjectsSection
     *            a <code>TargetSection</code> representing the Subjects section of this target
     * @param resourcesSection
     *            a <code>TargetSection</code> representing the Resources section of this target
     * @param actionsSection
     *            a <code>TargetSection</code> representing the Actions section of this target
     * @param environmentsSection
     *            a <code>TargetSection</code> representing the Environments section of this target
     */
    public Target(TargetSection subjectsSection, TargetSection resourcesSection,
            TargetSection actionsSection, TargetSection environmentsSection) {
        if ((subjectsSection == null) || (resourcesSection == null) || (actionsSection == null)
                || (environmentsSection == null))
            throw new ProcessingException("All sections of a Target must " + "be non-null");

        this.subjectsSection = subjectsSection;
        this.resourcesSection = resourcesSection;
        this.actionsSection = actionsSection;
        this.environmentsSection = environmentsSection;
        this.xacmlVersion = PolicyMetaData.XACML_VERSION_2_0;
    }

    /**
     * Creates a <code>Target</code> by parsing a node.
     * 
     * @deprecated As of 2.0 you should avoid using this method and should instead use the version
     *             that takes a <code>PolicyMetaData</code> instance. This method will only work for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the node to parse for the <code>Target</code>
     * @param xpathVersion
     *            the XPath version to use in any selectors, or null if this is unspecified (ie, not
     *            supplied in the defaults section of the policy)
     * 
     * @return a new <code>Target</code> constructed by parsing
     * 
     * @throws ParsingException
     *             if the DOM node is invalid
     */
    public static Target getInstance(Node root, String xpathVersion) throws ParsingException {
        return getInstance(root, new PolicyMetaData(PolicyMetaData.XACML_1_0_IDENTIFIER,
                xpathVersion));
    }

    /**
     * Creates a <code>Target</code> by parsing a node.
     * 
     * @param root
     *            the node to parse for the <code>Target</code>
     * @return a new <code>Target</code> constructed by parsing
     * 
     * @throws ParsingException
     *             if the DOM node is invalid
     */
    public static Target getInstance(Node root, PolicyMetaData metaData) throws ParsingException {
        TargetSection subjects = null;
        TargetSection resources = null;
        TargetSection actions = null;
        TargetSection environments = null;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (name.equals("Subjects")) {
                subjects = TargetSection.getInstance(child, TargetMatch.SUBJECT, metaData);
            } else if (name.equals("Resources")) {
                resources = TargetSection.getInstance(child, TargetMatch.RESOURCE, metaData);
            } else if (name.equals("Actions")) {
                actions = TargetSection.getInstance(child, TargetMatch.ACTION, metaData);
            } else if (name.equals("Environments")) {
                environments = TargetSection.getInstance(child, TargetMatch.ENVIRONMENT, metaData);
            }
        }

        // starting in 2.0 an any-matching section is represented by a
        // missing element, and in 1.x there were no Environments elements,
        // so these need to get turned into non-null arguments
        int version = metaData.getXACMLVersion();

        if (subjects == null)
            subjects = new TargetSection(null, TargetMatch.SUBJECT, version);
        if (resources == null)
            resources = new TargetSection(null, TargetMatch.RESOURCE, version);
        if (actions == null)
            actions = new TargetSection(null, TargetMatch.ACTION, version);

        if (version == PolicyMetaData.XACML_VERSION_2_0) {
            if (environments == null)
                environments = new TargetSection(null, TargetMatch.ENVIRONMENT, version);
            return new Target(subjects, resources, actions, environments);
        } else {
            return new Target(subjects, resources, actions);
        }
    }

    /**
     * Returns the Subjects section of this Target.
     * 
     * @return a <code>TargetSection</code> representing the Subjects
     */
    public TargetSection getSubjectsSection() {
        return subjectsSection;
    }

    /**
     * Returns the Resources section of this Target.
     * 
     * @return a <code>TargetSection</code> representing the Resources
     */
    public TargetSection getResourcesSection() {
        return resourcesSection;
    }

    /**
     * Returns the Actions section of this Target.
     * 
     * @return a <code>TargetSection</code> representing the Actions
     */
    public TargetSection getActionsSection() {
        return actionsSection;
    }

    /**
     * Returns the Environments section of this Target. Note that if this is an XACML 1.x policy,
     * then the section will always match anything, since XACML 1.x doesn't support matching on the
     * Environment.
     * 
     * @return a <code>TargetSection</code> representing the Environments
     */
    public TargetSection getEnvironmentsSection() {
        return environmentsSection;
    }

    /**
     * Returns whether or not this <code>Target</code> matches any request.
     * 
     * @return true if this Target matches any request, false otherwise
     */
    public boolean matchesAny() {
        return subjectsSection.matchesAny() && resourcesSection.matchesAny()
                && actionsSection.matchesAny() && environmentsSection.matchesAny();
    }

    /**
     * Determines whether this <code>Target</code> matches the input request (whether it is
     * applicable).
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match the target and the request
     */
    public MatchResult match(EvaluationCtx context) {
        MatchResult result = null;

        // before matching, see if this target matches any request
        if (matchesAny())
            return new MatchResult(MatchResult.MATCH);

        // first, try matching the Subjects section
        result = subjectsSection.match(context);
        if (result.getResult() != MatchResult.MATCH) {
            logger.finer("failed to match Subjects section of Target");
            return result;
        }

        // now try matching the Resources section
        result = resourcesSection.match(context);
        if (result.getResult() != MatchResult.MATCH) {
            logger.finer("failed to match Resources section of Target");
            return result;
        }

        // next, look at the Actions section
        result = actionsSection.match(context);
        if (result.getResult() != MatchResult.MATCH) {
            logger.finer("failed to match Actions section of Target");
            return result;
        }

        // finally, match the Environments section
        result = environmentsSection.match(context);
        if (result.getResult() != MatchResult.MATCH) {
            logger.finer("failed to match Environments section of Target");
            return result;
        }

        // if we got here, then everything matched
        return result;
    }

    /**
     * Encodes this <code>Target</code> into its XML representation and writes this encoding to the
     * given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>Target</code> into its XML representation and writes this encoding to the
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

        // see if this Target matches anything
        boolean matchesAny = (subjectsSection.matchesAny() && resourcesSection.matchesAny()
                && actionsSection.matchesAny() && environmentsSection.matchesAny());

        if (matchesAny && (xacmlVersion == PolicyMetaData.XACML_VERSION_2_0)) {
            // in 2.0, if all the sections match any request, then the Target
            // element is empty and should be encoded simply as en empty tag
            out.println("<Target/>");
        } else {
            out.println(indent + "<Target>");
            indenter.in();

            subjectsSection.encode(output, indenter);
            resourcesSection.encode(output, indenter);
            actionsSection.encode(output, indenter);

            // we should only do this if we're a 2.0 policy
            if (xacmlVersion == PolicyMetaData.XACML_VERSION_2_0)
                environmentsSection.encode(output, indenter);

            indenter.out();
            out.println(indent + "</Target>");
        }
    }

}
