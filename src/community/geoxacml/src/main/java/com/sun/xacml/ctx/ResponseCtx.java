/*
 * @(#)ResponseCtx.java
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;

/**
 * Represents the response to a request made to the XACML PDP.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ResponseCtx {

    // The set of Result objects returned by the PDP
    private Set<Result> results = null;

    /**
     * Constructor that creates a new <code>ResponseCtx</code> with only a single
     * <code>Result</code> (a common case).
     * 
     * @param result
     *            the single result in the response
     */
    public ResponseCtx(Result result) {
        results = new HashSet<Result>();
        results.add(result);
    }

    /**
     * Constructor that creates a new <code>ResponseCtx</code> with a <code>Set</code> of
     * <code>Result</code>s. The <code>Set</code> must be non-empty.
     * 
     * @param results
     *            a <code>Set</code> of <code>Result</code> objects
     */
    public ResponseCtx(Set<Result> results) {
        this.results = Collections.unmodifiableSet(new HashSet<Result>(results));
    }

    /**
     * Creates a new instance of <code>ResponseCtx</code> based on the given DOM root node. A
     * <code>ParsingException</code> is thrown if the DOM root doesn't represent a valid
     * ResponseType.
     * 
     * @param root
     *            the DOM root of a ResponseType
     * 
     * @return a new <code>ResponseCtx</code>
     * 
     * @throws ParsingException
     *             if the node is invalid
     */
    public static ResponseCtx getInstance(Node root) throws ParsingException {
        Set<Result> results = new HashSet<Result>();

        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("Result")) {
                results.add(Result.getInstance(node));
            }
        }

        if (results.size() == 0)
            throw new ParsingException("must have at least one Result");

        return new ResponseCtx(results);
    }

    /**
     * Creates a new <code>ResponseCtx</code> by parsing XML from an input stream. Note that this is
     * a convenience method, and it will not do schema validation by default. You should be parsing
     * the data yourself, and then providing the root node to the other <code>getInstance</code>
     * method. If you use this convenience method, you probably want to turn on validation by
     * setting the context schema file (see the programmer guide for more information on this).
     * 
     * @param input
     *            a stream providing the XML data
     * 
     * @return a new <code>ResponseCtx</code>
     * 
     * @throws ParserException
     *             if there is an error parsing the input
     */
    public static ResponseCtx getInstance(InputStream input) throws ParsingException {
        return getInstance(InputParser.parseInput(input, "Response"));
    }

    /**
     * Get the set of <code>Result</code>s from this response.
     * 
     * @return a <code>Set</code> of results
     */
    public Set<Result> getResults() {
        return results;
    }

    /**
     * Encodes this context into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    public void encode(OutputStream output, Indenter indenter) {
        encode(output, indenter);
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
        String indent = indenter.makeString();

        // Now write the XML...

        out.println(indent + "<Response>");

        // Go through all results
        for (Result result : results)
            result.encode(out, indenter);

        indenter.out();

        // Finish the XML for a response
        out.println(indent + "</Response>");

    }

}
