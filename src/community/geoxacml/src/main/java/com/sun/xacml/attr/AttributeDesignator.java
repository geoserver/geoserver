/*
 * @(#)AttributeDesignator.java
 *
 * Copyright 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.attr;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.cond.Evaluatable;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;
import com.sun.xacml.ctx.Status;

/**
 * Represents all four kinds of Designators in XACML.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class AttributeDesignator implements Evaluatable {

    /**
     * Tells designator to search in the subject section of the request
     */
    public static final int SUBJECT_TARGET = 0;

    /**
     * Tells designator to search in the resource section of the request
     */
    public static final int RESOURCE_TARGET = 1;

    /**
     * Tells designator to search in the action section of the request
     */
    public static final int ACTION_TARGET = 2;

    /**
     * Tells designator to search in the environment section of the request
     */
    public static final int ENVIRONMENT_TARGET = 3;

    /**
     * The standard URI for the default subject category value
     */
    public static final String SUBJECT_CATEGORY_DEFAULT = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    // helper array of strings
    static final private String[] targetTypes = { "Subject", "Resource", "Action", "Environment" };

    // the type of designator we are
    private int target;

    // required attributes
    private URI type;

    private URI id;

    // optional attribute
    private URI issuer;

    // must resolution find something
    private boolean mustBePresent;

    // if we're a subject this is the category
    private URI subjectCategory;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(AttributeDesignator.class.getName());

    /**
     * Creates a new <code>AttributeDesignator</code> without the optional issuer.
     * 
     * @param target
     *            the type of designator as specified by the 4 member *_TARGET fields
     * @param type
     *            the data type resolved by this designator
     * @param id
     *            the attribute id looked for by this designator
     * @param mustBePresent
     *            whether resolution must find a value
     */
    public AttributeDesignator(int target, URI type, URI id, boolean mustBePresent) {
        this(target, type, id, mustBePresent, null);
    }

    /**
     * Creates a new <code>AttributeDesignator</code> with the optional issuer.
     * 
     * @param target
     *            the type of designator as specified by the 4 member *_TARGET fields
     * @param type
     *            the data type resolved by this designator
     * @param id
     *            the attribute id looked for by this designator
     * @param mustBePresent
     *            whether resolution must find a value
     * @param issuer
     *            the issuer of the values to search for or null if no issuer is specified
     * 
     * @throws IllegalArgumentException
     *             if the input target isn't a valid value
     */
    public AttributeDesignator(int target, URI type, URI id, boolean mustBePresent, URI issuer)
            throws IllegalArgumentException {

        // check if input target is a valid value
        if ((target != SUBJECT_TARGET) && (target != RESOURCE_TARGET) && (target != ACTION_TARGET)
                && (target != ENVIRONMENT_TARGET))
            throw new IllegalArgumentException("Input target is not a valid" + "value");
        this.target = target;
        this.type = type;
        this.id = id;
        this.mustBePresent = mustBePresent;
        this.issuer = issuer;

        subjectCategory = null;
    }

    /**
     * Sets the category if this is a SubjectAttributeDesignatorType
     * 
     * @param category
     *            the subject category
     */
    public void setSubjectCategory(URI category) {
        if (target == SUBJECT_TARGET)
            subjectCategory = category;
    }

    /**
     * Creates a new <code>AttributeDesignator</code> based on the DOM root of the XML data.
     * 
     * @deprecated As of 2.0 you should avoid using this method and should instead use the version
     *             that takes a <code>PolicyMetaData</code> instance. This method will only work for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the DOM root of the AttributeDesignatorType XML type
     * @param target
     *            the type of designator to create as specified in the four member *_TARGET fields
     * 
     * @return the designator
     * 
     * @throws ParsingException
     *             if the AttributeDesignatorType was invalid
     */
    public static AttributeDesignator getInstance(Node root, int target) throws ParsingException {
        return getInstance(root, target, new PolicyMetaData());
    }

    /**
     * Creates a new <code>AttributeDesignator</code> based on the DOM root of the XML data.
     * 
     * @param root
     *            the DOM root of the AttributeDesignatorType XML type
     * @param target
     *            the type of designator to create as specified in the four member *_TARGET fields
     * @param metaData
     *            the meta-data associated with the containing policy
     * 
     * @return the designator
     * 
     * @throws ParsingException
     *             if the AttributeDesignatorType was invalid
     */
    public static AttributeDesignator getInstance(Node root, int target, PolicyMetaData metaData)
            throws ParsingException {
        URI type = null;
        URI id = null;
        URI issuer = null;
        boolean mustBePresent = false;
        URI subjectCategory = null;

        NamedNodeMap attrs = root.getAttributes();

        try {
            // there's always an Id
            id = new URI(attrs.getNamedItem("AttributeId").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Required AttributeId missing in " + "AttributeDesignator",
                    e);
        }

        try {
            // there's always a data type
            type = new URI(attrs.getNamedItem("DataType").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Required DataType missing in " + "AttributeDesignator", e);
        }

        try {
            // there might be an issuer
            Node node = attrs.getNamedItem("Issuer");
            if (node != null)
                issuer = new URI(node.getNodeValue());

            // if it's for the Subject section, there's another attr
            if (target == SUBJECT_TARGET) {
                Node scnode = attrs.getNamedItem("SubjectCategory");
                if (scnode != null)
                    subjectCategory = new URI(scnode.getNodeValue());
                else
                    subjectCategory = new URI(SUBJECT_CATEGORY_DEFAULT);
            }

            // there might be a mustBePresent flag
            node = attrs.getNamedItem("MustBePresent");
            if (node != null)
                if (node.getNodeValue().equals("true"))
                    mustBePresent = true;
        } catch (Exception e) {
            // this shouldn't ever happen, but in theory something could go
            // wrong in the code in this try block
            throw new ParsingException(
                    "Error parsing AttributeDesignator " + "optional attributes", e);
        }

        AttributeDesignator ad = new AttributeDesignator(target, type, id, mustBePresent, issuer);
        ad.setSubjectCategory(subjectCategory);

        return ad;
    }

    /**
     * Returns the type of this designator as specified by the *_TARGET fields.
     * 
     * @return the designator type
     */
    public int getDesignatorType() {
        return target;
    }

    /**
     * Returns the type of attribute that is resolved by this designator. While an AD will always
     * return a bag, this method will always return the type that is stored in the bag.
     * 
     * @return the attribute type
     */
    public URI getType() {
        return type;
    }

    /**
     * Returns the AttributeId of the values resolved by this designator.
     * 
     * @return identifier for the values to resolve
     */
    public URI getId() {
        return id;
    }

    /**
     * Returns the subject category for this designator. If this is not a SubjectAttributeDesignator
     * then this will always return null.
     * 
     * @return the subject category or null if this isn't a SubjectAttributeDesignator
     */
    public URI getCategory() {
        return subjectCategory;
    }

    /**
     * Returns the issuer of the values resolved by this designator if specified.
     * 
     * @return the attribute issuer or null if unspecified
     */
    public URI getIssuer() {
        return issuer;
    }

    /**
     * Returns whether or not a value is required to be resolved by this designator.
     * 
     * @return true if a value is required, false otherwise
     */
    public boolean mustBePresent() {
        return mustBePresent;
    }

    /**
     * Always returns true, since a designator always returns a bag of attribute values.
     * 
     * @return true
     */
    public boolean returnsBag() {
        return true;
    }

    /**
     * Always returns true, since a designator always returns a bag of attribute values.
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @return true
     */
    public boolean evaluatesToBag() {
        return true;
    }

    /**
     * Always returns an empty list since designators never have children.
     * 
     * @return an empty <code>List</code>
     */
    public List<Expression> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Evaluates the pre-assigned meta-data against the given context, trying to find some matching
     * values.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult evaluate(EvaluationCtx context) {
        EvaluationResult result = null;

        // look in the right section for some attribute values
        switch (target) {
        case SUBJECT_TARGET:
            result = context.getSubjectAttribute(type, id, issuer, subjectCategory);
            break;
        case RESOURCE_TARGET:
            result = context.getResourceAttribute(type, id, issuer);
            break;
        case ACTION_TARGET:
            result = context.getActionAttribute(type, id, issuer);
            break;
        case ENVIRONMENT_TARGET:
            result = context.getEnvironmentAttribute(type, id, issuer);
            break;
        }

        // if the lookup was indeterminate, then we return immediately
        if (result.indeterminate())
            return result;

        BagAttribute bag = (BagAttribute) (result.getAttributeValue());

        if (bag.isEmpty()) {
            // if it's empty, this may be an error
            if (mustBePresent) {
                if (logger.isLoggable(Level.INFO))
                    logger.info("AttributeDesignator failed to resolve a "
                            + "value for a required attribute: " + id.toString());

                ArrayList<String> code = new ArrayList<String>();
                code.add(Status.STATUS_MISSING_ATTRIBUTE);

                String message = "Couldn't find " + targetTypes[target]
                        + "AttributeDesignator attribute";

                // Note that there is a bug in the XACML spec. You can't
                // specify an identifier without specifying acceptable
                // values. Until this is fixed, this code will only
                // return the status code, and not any hints about what
                // was missing

                /*
                 * List attrs = new ArrayList(); attrs.add(new Attribute(id, ((issuer == null) ?
                 * null : issuer.toString()), null, null)); StatusDetail detail = new
                 * StatusDetail(attrs);
                 */

                return new EvaluationResult(new Status(code, message));
            }
        }

        // if we got here the bag wasn't empty, or mustBePresent was false,
        // so we just return the result
        return result;
    }

    /**
     * Encodes this designator into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this designator into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        String tag = "<" + targetTypes[target] + "AttributeDesignator";

        if ((target == SUBJECT_TARGET) && (subjectCategory != null))
            tag += " SubjectCategory=\"" + subjectCategory.toString() + "\"";

        tag += " AttributeId=\"" + id.toString() + "\"";
        tag += " DataType=\"" + type.toString() + "\"";

        if (issuer != null)
            tag += " Issuer=\"" + issuer.toString() + "\"";

        if (mustBePresent)
            tag += " MustBePresent=\"true\"";

        tag += "/>";

        out.println(indent + tag);
    }

}
