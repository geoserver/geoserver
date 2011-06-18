/*
 * @(#)PolicyReader.java
 *
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.support.finder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicySet;
import com.sun.xacml.finder.PolicyFinder;

/**
 * This class is provided as a utility for reading policies from common, simple sources:
 * <code>InputStream</code>s, <code>File</code>s, and <code>URL</code>s. It can optionally schema
 * validate the policies.
 * <p>
 * Note: some of this functionality was previously provided in
 * <code>com.sun.xacml.finder.impl.FilePolicyModule</code>, but as of the 2.0 release, that class
 * has been removed. This new <code>PolicyReader</code> class provides much better functionality for
 * loading policies.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class PolicyReader implements ErrorHandler {

    /**
     * The property which is used to specify the schema file to validate against (if any). Note that
     * this isn't used directly by <code>PolicyReader</code>, but is referenced by many classes that
     * use this class to load policies.
     */
    public static final String POLICY_SCHEMA_PROPERTY = "com.sun.xacml.PolicySchema";

    // the standard attribute for specifying the XML schema language
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    // the standard identifier for the XML schema specification
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    // the standard attribute for specifying schema source
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    // the finder, which is used by PolicySets
    private PolicyFinder finder;

    // the builder used to create DOM documents
    private DocumentBuilder builder;

    // the optional logger used for error reporting
    private Logger logger;

    /**
     * Creates a <code>PolicyReader</code> that does not schema-validate policies.
     * 
     * @param finder
     *            a <code>PolicyFinder</code> that is used by policy sets, which may be null only if
     *            no references are used
     * @param logger
     *            a <code>Logger</code> used to report parsing errors
     */
    public PolicyReader(PolicyFinder finder, Logger logger) {
        this(finder, logger, null);
    }

    /**
     * Creates a <code>PolicyReader</code> that may schema-validate policies.
     * 
     * @param finder
     *            a <code>PolicyFinder</code> that is used by policy sets, which may be null only if
     *            no references are used
     * @param logger
     *            a <code>Logger</code> used to report parsing errors
     * @param schemaFile
     *            the schema file used to validate policies, or null if schema validation is not
     *            desired
     */
    public PolicyReader(PolicyFinder finder, Logger logger, File schemaFile) {
        this.logger = logger;
        this.finder = finder;

        // create the factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);

        // see if we want to schema-validate policies
        if (schemaFile == null) {
            factory.setValidating(false);
        } else {
            factory.setValidating(true);
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            factory.setAttribute(JAXP_SCHEMA_SOURCE, schemaFile);
        }

        // now use the factory to create the document builder
        try {
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(this);
        } catch (ParserConfigurationException pce) {
            throw new IllegalArgumentException("Filed to setup reader: " + pce.toString());
        }
    }

    /**
     * Tries to read an XACML policy or policy set from the given file.
     * 
     * @param file
     *            the file containing the policy to read
     * 
     * @return a (potentially schema-validated) policy loaded from the given file
     * 
     * @throws ParsingException
     *             if an error occurs while reading or parsing the policy
     */
    public synchronized AbstractPolicy readPolicy(File file) throws ParsingException {
        try {
            return handleDocument(builder.parse(file));
        } catch (IOException ioe) {
            throw new ParsingException("Failed to read the file", ioe);
        } catch (SAXException saxe) {
            throw new ParsingException("Failed to parse the file", saxe);
        }
    }

    /**
     * Tries to read an XACML policy or policy set from the given stream.
     * 
     * @param input
     *            the stream containing the policy to read
     * 
     * @return a (potentially schema-validated) policy loaded from the given file
     * 
     * @throws ParsingException
     *             if an error occurs while reading or parsing the policy
     */
    public synchronized AbstractPolicy readPolicy(InputStream input) throws ParsingException {
        try {
            return handleDocument(builder.parse(input));
        } catch (IOException ioe) {
            throw new ParsingException("Failed to read the stream", ioe);
        } catch (SAXException saxe) {
            throw new ParsingException("Failed to parse the stream", saxe);
        }
    }

    /**
     * Tries to read an XACML policy or policy set based on the given URL. This may be any
     * resolvable URL, like a file or http pointer.
     * 
     * @param url
     *            a URL pointing to the policy to read
     * 
     * @return a (potentially schema-validated) policy loaded from the given file
     * 
     * @throws ParsingException
     *             if an error occurs while reading or parsing the policy, or if the URL can't be
     *             resolved
     */
    public synchronized AbstractPolicy readPolicy(URL url) throws ParsingException {
        try {
            return readPolicy(url.openStream());
        } catch (IOException ioe) {
            throw new ParsingException("Failed to resolve the URL: " + url.toString(), ioe);
        }
    }

    /**
     * A private method that handles reading the policy and creates the correct kind of
     * AbstractPolicy.
     */
    private AbstractPolicy handleDocument(Document doc) throws ParsingException {
        // handle the policy, if it's a known type
        Element root = doc.getDocumentElement();
        String name = root.getTagName();

        // see what type of policy this is
        if (name.equals("Policy")) {
            return Policy.getInstance(root);
        } else if (name.equals("PolicySet")) {
            return PolicySet.getInstance(root, finder);
        } else {
            // this isn't a root type that we know how to handle
            throw new ParsingException("Unknown root document type: " + name);
        }
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
                    + exception.getMessage() + " ... " + "Policy will not be available");

        throw new SAXException("error parsing policy");
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
            logger.warning("Fatal error on line " + exception.getLineNumber() + ": "
                    + exception.getMessage() + " ... " + "Policy will not be available");

        throw new SAXException("fatal error parsing policy");
    }

}
