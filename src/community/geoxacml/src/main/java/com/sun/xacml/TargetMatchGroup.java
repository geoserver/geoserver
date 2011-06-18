/*
 * @(#)TargetMatchGroup.java
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

package com.sun.xacml;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains a group of <code>TargetMatch</code> instances and represents the Subject,
 * Resource, Action, and Environment elements in an XACML Target.
 * 
 * @since 2.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class TargetMatchGroup {

    // the list of matches
    private List<TargetMatch> matches;

    // the match type contained in this group
    private int matchType;

    // the logger we'll use for all messages
    // private static final Logger logger =
    // Logger.getLogger(Target.class.getName());

    /**
     * Constructor that creates a new <code>TargetMatchGroup</code> based on the given elements.
     * 
     * @param matchElements
     *            a <code>List</code> of <code>TargetMatch</code>
     * @param matchType
     *            the match type as defined in <code>TargetMatch</code>
     */
    public TargetMatchGroup(List<TargetMatch> matchElements, int matchType) {
        if (matchElements == null)
            matches = Collections.unmodifiableList(new ArrayList<TargetMatch>());
        else
            matches = Collections.unmodifiableList(new ArrayList<TargetMatch>(matchElements));
        this.matchType = matchType;
    }

    /**
     * Creates a <code>Target</code> based on its DOM node.
     * 
     * @param root
     *            the node to parse for the target group
     * @param matchType
     *            the type of the match
     * @param metaData
     *            meta-date associated with the policy
     * 
     * @return a new <code>TargetMatchGroup</code> constructed by parsing
     * 
     * @throws ParsingException
     *             if the DOM node is invalid
     */
    public static TargetMatchGroup getInstance(Node root, int matchType, PolicyMetaData metaData)
            throws ParsingException {
        List<TargetMatch> matches = new ArrayList<TargetMatch>();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();

            if (name.equals(TargetMatch.NAMES[matchType] + "Match")) {
                matches.add(TargetMatch.getInstance(child, matchType, metaData));
            }
        }

        return new TargetMatchGroup(matches, matchType);
    }

    /**
     * Determines whether this <code>TargetMatchGroup</code> matches the input request (whether it
     * is applicable).
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match the group with the context
     */
    public MatchResult match(EvaluationCtx context) {
        MatchResult result = null;

        for (TargetMatch tm : matches) {
            result = tm.match(context);
            if (result.getResult() != MatchResult.MATCH)
                break;
        }

        return result;
    }

    /**
     * Encodes this <code>TargetMatchGroup</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this <code>TargetMatchGroup</code> into its XML representation and writes this
     * encoding to the given <code>OutputStream</code> with indentation.
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

        out.println(indent + "<" + name + ">");
        indenter.in();

        for (TargetMatch tm : matches) {
            tm.encode(output, indenter);
        }

        out.println(indent + "</" + name + ">");
        indenter.out();
    }

}
