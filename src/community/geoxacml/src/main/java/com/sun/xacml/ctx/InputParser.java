/*
 * @(#)InputParser.java
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

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xacml.ParsingException;

/**
 * A package-private helper that provides a single static routine for parsing input based on the
 * context schema.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
class InputParser implements ErrorHandler {

    // the schema file, if provided
    private File schemaFile;

    // the single reference, which is null unless a schema file is provided
    private static InputParser ipReference = null;

    // the property string to set to turn on validation
    private static final String CONTEXT_SCHEMA_PROPERTY = "com.sun.xacml.ContextSchema";

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(InputParser.class.getName());

    // standard strings for setting validation

    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /**
     * Look for the property that names the schema, and if it exists get the file name and create a
     * single InputParser instance
     */
    static {
        String schemaName = System.getProperty(CONTEXT_SCHEMA_PROPERTY);

        if (schemaName != null)
            ipReference = new InputParser(new File(schemaName));
    };

    /**
     * Constructor that takes the schema file.
     */
    private InputParser(File schemaFile) {
        this.schemaFile = schemaFile;
    }

    /**
     * Tries to Parse the given output as a Context document.
     * 
     * @param input
     *            the stream to parse
     * @param rootTage
     *            either "Request" or "Response"
     * 
     * @return the root node of the request/response
     * 
     * @throws ParsingException
     *             if a problem occurred parsing the document
     */
    static Node parseInput(InputStream input, String rootTag) throws ParsingException {
        NodeList nodes = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);

            DocumentBuilder builder = null;

            // as of 1.2, we always are namespace aware
            factory.setNamespaceAware(true);

            if (ipReference == null) {
                // we're not validating
                factory.setValidating(false);

                builder = factory.newDocumentBuilder();
            } else {
                // we are validating
                factory.setValidating(true);

                factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                factory.setAttribute(JAXP_SCHEMA_SOURCE, ipReference.schemaFile);

                builder = factory.newDocumentBuilder();
                builder.setErrorHandler(ipReference);
            }

            Document doc = builder.parse(input);
            nodes = doc.getElementsByTagName(rootTag);
        } catch (Exception e) {
            throw new ParsingException("Error tring to parse " + rootTag + "Type", e);
        }

        if (nodes.getLength() != 1)
            throw new ParsingException("Only one " + rootTag + "Type allowed "
                    + "at the root of a Context doc");

        return nodes.item(0);
    }

    /**
     * Standard handler routine for the XML parsing.
     * 
     * @param exception
     *            information on what caused the problem
     */
    public void warning(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING))
            logger.warning("Warning on line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
    }

    /**
     * Standard handler routine for the XML parsing.
     * 
     * @param exception
     *            information on what caused the problem
     * 
     * @throws SAXException
     *             always to halt parsing on errors
     */
    public void error(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING))
            logger.warning("Error on line " + exception.getLineNumber() + ": "
                    + exception.getMessage());

        throw new SAXException("invalid context document");
    }

    /**
     * Standard handler routine for the XML parsing.
     * 
     * @param exception
     *            information on what caused the problem
     * 
     * @throws SAXException
     *             always to halt parsing on errors
     */
    public void fatalError(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING))
            logger.warning("FatalError on line " + exception.getLineNumber() + ": "
                    + exception.getMessage());

        throw new SAXException("invalid context document");
    }

}
