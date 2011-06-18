/*
 * @(#)TargetSection.java
 *
 * Copyright 2005-2006 Sun Microsystems, Inc. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ctx.Status;

/**
 * This is a container class for instances of <code>TargetMatchGroup</code> and represents the
 * Subjects, Resources, Actions, and Environments sections of an XACML Target. This section may
 * apply to any request.
 * 
 * @since 2.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class TargetSection {

    // the list of match groups
    private List<TargetMatchGroup> matchGroups;

    // the match type contained in this group
    private int matchType;

    // the version of XACML used by the containing Target
    private int xacmlVersion;

    /**
     * Constructor that takes a group and a version. If the group is null or empty, then this
     * represents a section that matches any request.
     * 
     * @param matchGroups
     *            a possibly null <code>List</code> of <code>TargetMatchGroup</code>s
     * @param matchType
     *            the type as defined in <code>TargetMatch</code>
     * @param xacmlVersion
     *            the version XACML being used
     */
    public TargetSection(List<TargetMatchGroup> matchGroups, int matchType, int xacmlVersion) {
        if (matchGroups == null)
            this.matchGroups = Collections.unmodifiableList(new ArrayList<TargetMatchGroup>());
        else
            this.matchGroups = Collections.unmodifiableList(new ArrayList<TargetMatchGroup>(
                    matchGroups));
        this.matchType = matchType;
        this.xacmlVersion = xacmlVersion;
    }

    /**
     * Creates a <code>Target</code> by parsing a node.
     * 
     * @param root
     *            the node to parse for the <code>Target</code>
     * @param matchType
     *            the type as defined in <code>TargetMatch</code>
     * @param metaData
     *            the meta-data from the enclosing policy
     * 
     * @return a new <code>Target</code> constructed by parsing
     * 
     * @throws ParsingException
     *             if the DOM node is invalid
     */
    public static TargetSection getInstance(Node root, int matchType, PolicyMetaData metaData)
            throws ParsingException {
        List<TargetMatchGroup> groups = new ArrayList<TargetMatchGroup>();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            String typeName = TargetMatch.NAMES[matchType];

            if (name.equals(typeName)) {
                groups.add(TargetMatchGroup.getInstance(child, matchType, metaData));
            } else if (name.equals("Any" + typeName)) {
                // in a schema-valid policy, the Any element will always be
                // the only element, so if we find this we stop
                break;
            }
        }

        // at this point the list is non-empty (it has specific groups to
        // match) or is empty (it applies to any request using the 1.x or
        // 2.0 syntax)
        return new TargetSection(groups, matchType, metaData.getXACMLVersion());
    }

    /**
     * Returns the <code>TargetMatchGroup</code>s contained in this group.
     * 
     * @return a <code>List</code> of <code>TargetMatchGroup</code>s
     */
    public List<TargetMatchGroup> getMatchGroups() {
        return matchGroups;
    }

    /**
     * Returns whether this section matches any request.
     * 
     * @return true if this section matches any request, false otherwise
     */
    public boolean matchesAny() {
        return matchGroups.isEmpty();
    }

    /**
     * Determines whether this <code>TargetSection</code> matches the input request (whether it is
     * applicable).
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match the target and the request
     */
    public MatchResult match(EvaluationCtx context) {
        // if we apply to anything, then we always match
        if (matchGroups.isEmpty())
            return new MatchResult(MatchResult.MATCH);

        // there are specific matching elements, so prepare to iterate
        // through the list
        Status firstIndeterminateStatus = null;

        // in order for this section to match, one of the groups must match
        for (TargetMatchGroup group : matchGroups) {
            // get the next group and try matching it

            MatchResult result = group.match(context);

            // we only need one match, so if this matched, then we're done
            if (result.getResult() == MatchResult.MATCH)
                return result;

            // if we didn't match then it was either a NO_MATCH or
            // INDETERMINATE...in the second case, we need to remember
            // it happened, 'cause if we don't get a MATCH, then we'll
            // be returning INDETERMINATE
            if (result.getResult() == MatchResult.INDETERMINATE) {
                if (firstIndeterminateStatus == null)
                    firstIndeterminateStatus = result.getStatus();
            }
        }

        // if we got here, then none of the sub-matches passed, so
        // we have to see if we got any INDETERMINATE cases
        if (firstIndeterminateStatus == null)
            return new MatchResult(MatchResult.NO_MATCH);
        else
            return new MatchResult(MatchResult.INDETERMINATE, firstIndeterminateStatus);
    }

    /**
     * Encodes this <code>TargetSection</code> into its XML representation and writes this encoding
     * to the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>TargetSection</code> into its XML representation and writes this encoding
     * to the given <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();
        String name = TargetMatch.NAMES[matchType];

        // figure out if this section applies to any request
        if (matchGroups.isEmpty()) {
            // this applies to any, so now we need to encode it based on
            // what version of XACML we're using...in 2.0, we encode an Any
            // by simply omitting the element, so we'll only actually include
            // something if this is a 1.x policy
            if (xacmlVersion == PolicyMetaData.XACML_VERSION_1_0) {
                out.println(indent + "<" + name + "s>");
                indenter.in();
                out.println(indenter.makeString() + "<Any" + name + "/>");
                indenter.out();
                out.println(indent + "</" + name + "s>");
            }
        } else {
            // this has specific rules, so we can now encode them
            out.println(indent + "<" + name + "s>");

            indenter.in();
            for (TargetMatchGroup group : matchGroups) {
                group.encode(output, indenter);
            }
            indenter.out();

            out.println(indent + "</" + name + "s>");
        }
    }

}
