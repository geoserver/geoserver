/*
 * @(#)RequestCtx.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.ctx;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;

/**
 * Represents a request made to the PDP. This is the class that contains all the data used to start
 * a policy evaluation.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 * 
 */
public class RequestCtx {

    // There must be at least one subject
    private Set<Subject> subjects = null;

    // There must be exactly one resource
    private Set<Attribute> resource = null;

    // There must be exactly one action
    private Set<Attribute> action = null;

    // There may be any number of environment attributes
    private Set<Attribute> environment = null;

    // Hold onto the root of the document for XPath searches
    private Node documentRoot = null;

    // The optional, generic resource content
    private String resourceContent;

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     * 
     * @param subjects
     *            a <code>Set</code> of <code>Subject</code>s
     * @param resource
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param action
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param environment
     *            a <code>Set</code> of environment attributes
     */
    public RequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action,
            Set<Attribute> environment) {
        this(subjects, resource, action, environment, null, null);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     * 
     * @param subjects
     *            a <code>Set</code> of <code>Subject</code>s
     * @param resource
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param action
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param environment
     *            a <code>Set</code> of environment attributes
     * @param documentRoot
     *            the root node of the DOM tree for this request
     */
    public RequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action,
            Set<Attribute> environment, Node documentRoot) {
        this(subjects, resource, action, environment, documentRoot, null);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     * 
     * @param subjects
     *            a <code>Set</code> of <code>Subject</code>s
     * @param resource
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param action
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param environment
     *            a <code>Set</code> of environment attributes
     * @param resourceContent
     *            a text-encoded version of the content, suitable for including in the RequestType,
     *            including the root <code>RequestContent</code> node
     */
    public RequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action,
            Set<Attribute> environment, String resourceContent) {
        this(subjects, resource, action, environment, null, resourceContent);
    }

    /**
     * Constructor that creates a <code>RequestCtx</code> from components.
     * 
     * @param subjects
     *            a <code>Set</code> of <code>Subject</code>s
     * @param resource
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param action
     *            a <code>Set</code> of <code>Attribute</code>s
     * @param environment
     *            a <code>Set</code> of environment attributes
     * @param documentRoot
     *            the root node of the DOM tree for this request
     * @param resourceContent
     *            a text-encoded version of the content, suitable for including in the RequestType,
     *            including the root <code>RequestContent</code> node
     * 
     * @throws IllegalArgumentException
     *             if the inputs are not well formed
     */
    public RequestCtx(Set<Subject> subjects, Set<Attribute> resource, Set<Attribute> action,
            Set<Attribute> environment, Node documentRoot, String resourceContent)
            throws IllegalArgumentException {

        this.subjects = Collections.unmodifiableSet(new HashSet<Subject>(subjects));

        this.resource = Collections.unmodifiableSet(new HashSet<Attribute>(resource));

        this.action = Collections.unmodifiableSet(new HashSet<Attribute>(action));

        this.environment = Collections.unmodifiableSet(new HashSet<Attribute>(environment));

        this.documentRoot = documentRoot;
        this.resourceContent = resourceContent;
    }

    /**
     * Create a new <code>RequestCtx</code> by parsing a node. This node should be created by
     * schema-verified parsing of an <code>XML</code> document.
     * 
     * @param root
     *            the node to parse for the <code>RequestCtx</code>
     * 
     * @return a new <code>RequestCtx</code> constructed by parsing
     * 
     * @throws URISyntaxException
     *             if there is a badly formed URI
     * @throws ParsingException
     *             if the DOM node is invalid
     */
    public static RequestCtx getInstance(Node root) throws ParsingException {
        Set<Subject> newSubjects = new HashSet<Subject>();
        Set<Attribute> newResource = null;
        Set<Attribute> newAction = null;
        Set<Attribute> newEnvironment = null;
        // String resourceContent;

        // First check to be sure the node passed is indeed a Request node.
        String tagName = root.getNodeName();
        if (!tagName.equals("Request")) {
            throw new ParsingException("Request cannot be constructed using " + "type: "
                    + root.getNodeName());
        }

        // Now go through its child nodes, finding Subject,
        // Resource, Action, and Environment data
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            String tag = node.getNodeName();

            if (tag.equals("Subject")) {
                // see if there is a category
                Node catNode = node.getAttributes().getNamedItem("SubjectCategory");
                URI category = null;

                if (catNode != null) {
                    try {
                        category = new URI(catNode.getNodeValue());
                    } catch (Exception e) {
                        throw new ParsingException("Invalid Category URI", e);
                    }
                }

                // now we get the attributes
                Set<Attribute> attributes = parseAttributes(node);

                // finally, add the list to the set of subject attributes
                newSubjects.add(new Subject(category, attributes));
            } else if (tag.equals("Resource")) {
                // For now, this code doesn't parse the content, since it's
                // a set of anys with a set of anyAttributes, and therefore
                // no useful data can be gleaned from it anyway. The theory
                // here is that it's only useful in the instance doc, so
                // we won't bother parse it, but we may still want to go
                // back and provide some support at some point...
                newResource = parseAttributes(node);
            } else if (tag.equals("Action")) {
                newAction = parseAttributes(node);
            } else if (tag.equals("Environment")) {
                newEnvironment = parseAttributes(node);
            }
        }

        // if we didn't have an environment section, the only optional section
        // of the four, then create a new empty set for it
        if (newEnvironment == null)
            newEnvironment = new HashSet<Attribute>();

        // Now create and return the RequestCtx from the information
        // gathered
        return new RequestCtx(newSubjects, newResource, newAction, newEnvironment, root);
    }

    /*
     * Helper method that parses a set of Attribute types. The Subject, Action and Environment
     * sections all look like this.
     */
    private static Set<Attribute> parseAttributes(Node root) throws ParsingException {
        Set<Attribute> set = new HashSet<Attribute>();

        // the Environment section is just a list of Attributes
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("Attribute"))
                set.add(Attribute.getInstance(node));
        }

        return set;
    }

    /**
     * Creates a new <code>RequestCtx</code> by parsing XML from an input stream. Note that this a
     * convenience method, and it will not do schema validation by default. You should be parsing
     * the data yourself, and then providing the root node to the other <code>getInstance</code>
     * method. If you use this convenience method, you probably want to turn on validation by
     * setting the context schema file (see the programmer guide for more information on this).
     * 
     * @param input
     *            a stream providing the XML data
     * 
     * @return a new <code>RequestCtx</code>
     * 
     * @throws ParserException
     *             if there is an error parsing the input
     */
    public static RequestCtx getInstance(InputStream input) throws ParsingException {
        return getInstance(InputParser.parseInput(input, "Request"));
    }

    /**
     * Returns a <code>Set</code> containing <code>Subject</code> objects.
     * 
     * @return the request's subject attributes
     */
    public Set<Subject> getSubjects() {
        return subjects;
    }

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     * 
     * @return the request's resource attributes
     */
    public Set<Attribute> getResource() {
        return resource;
    }

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     * 
     * @return the request's action attributes
     */
    public Set<Attribute> getAction() {
        return action;
    }

    /**
     * Returns a <code>Set</code> containing <code>Attribute</code> objects.
     * 
     * @return the request's environment attributes
     */
    public Set<Attribute> getEnvironmentAttributes() {
        return environment;
    }

    /**
     * Returns the root DOM node of the document used to create this object, or null if this object
     * was created by hand (ie, not through the <code>getInstance</code> method) or if the root node
     * was not provided to the constructor.
     * 
     * @return the root DOM node or null
     */
    public Node getDocumentRoot() {
        return documentRoot;
    }

    /**
     * Encodes this context into its XML representation and writes this encoding to the given
     * <code>OutputStream</code>. No indentation is used.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    public void encode(OutputStream output, Indenter indenter) {
        encode(output, indenter, false);
    }

    /**
     * Encodes this context into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter, boolean header) {

        // Make a PrintStream for a nicer printing interface
        PrintStream out = new PrintStream(output);
        if (header)
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        // Prepare the indentation string
        String topIndent = indenter.makeString();
        out.println(topIndent + "<Request>");

        // go in one more for next-level elements...
        indenter.in();
        String indent = indenter.makeString();

        // ...and go in again for everything else
        indenter.in();

        // first off, go through all subjects
        for (Subject subject : subjects) {

            out.print(indent + "<Subject SubjectCategory=\"" + subject.getCategory().toString()
                    + "\"");

            Set<Attribute> subjectAttrs = subject.getAttributes();

            if (subjectAttrs.size() == 0) {
                // there's nothing in this Subject, so just close the tag
                out.println("/>");
            } else {
                // there's content, so fill it in
                out.println(">");

                encodeAttributes(subjectAttrs, out, indenter);

                out.println(indent + "</Subject>");
            }
        }

        // next do the resource
        if ((resource.size() != 0) || (resourceContent != null)) {
            out.println(indent + "<Resource>");
            if (resourceContent != null)
                out.println(indenter.makeString() + "<ResourceContent>" + resourceContent
                        + "</ResourceContent>");
            encodeAttributes(resource, out, indenter);
            out.println(indent + "</Resource>");
        } else {
            out.println(indent + "<Resource/>");
        }

        // now the action
        if (action.size() != 0) {
            out.println(indent + "<Action>");
            encodeAttributes(action, out, indenter);
            out.println(indent + "</Action>");
        } else {
            out.println(indent + "<Action/>");
        }

        // finally the environment, if there are any attrs
        if (environment.size() != 0) {
            out.println(indent + "<Environment>");
            encodeAttributes(environment, out, indenter);
            out.println(indent + "</Environment>");
        }

        // we're back to the top
        indenter.out();
        indenter.out();

        out.println(topIndent + "</Request>");
    }

    /**
     * Private helper function to encode the attribute sets
     */
    private void encodeAttributes(Set<Attribute> attributes, PrintStream out, Indenter indenter) {
        for (Attribute attr : attributes)
            attr.encode(out, indenter);
    }

}
