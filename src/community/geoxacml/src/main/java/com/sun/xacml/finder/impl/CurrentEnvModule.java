/*
 * @(#)CurrentEnvModule.java
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

package com.sun.xacml.finder.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.TimeAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.AttributeFinderModule;

/**
 * Supports the current date, time, and dateTime values. The XACML specification states that these
 * three values must always be available to a PDP. They may be included in the request, but if
 * they're not, a PDP must be able to recognize the attribute and generate a correct value.
 * <p>
 * The XACML specification doesn't require that values be cached (ie, remain consistent within an
 * evaluation), but does allow it. Any caching, as well as details of which time to use (time at the
 * PEP, PDP, etc.) is taken care of by the <code>EvaluationCtx</code> which is used to supply the
 * current values.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class CurrentEnvModule extends AttributeFinderModule {

    /**
     * Standard environment variable that represents the current time
     */
    public static final String ENVIRONMENT_CURRENT_TIME = "urn:oasis:names:tc:xacml:1.0:environment:current-time";

    /**
     * Standard environment variable that represents the current date
     */
    public static final String ENVIRONMENT_CURRENT_DATE = "urn:oasis:names:tc:xacml:1.0:environment:current-date";

    /**
     * Standard environment variable that represents the current date and time
     */
    public static final String ENVIRONMENT_CURRENT_DATETIME = "urn:oasis:names:tc:xacml:1.0:environment:current-dateTime";

    /**
     * Returns true always because this module supports designators.
     * 
     * @return true always
     */
    public boolean isDesignatorSupported() {
        return true;
    }

    /**
     * Returns a <code>Set</code> with a single <code>Integer</code> specifying that environment
     * attributes are supported by this module.
     * 
     * @return a <code>Set</code> with <code>AttributeDesignator.ENVIRONMENT_TARGET</code> included
     */
    public Set<Integer> getSupportedDesignatorTypes() {
        HashSet<Integer> set = new HashSet<Integer>();
        set.add(new Integer(AttributeDesignator.ENVIRONMENT_TARGET));
        return set;
    }

    /**
     * Used to get the current time, date, or dateTime. If one of those values isn't being asked
     * for, or if the types are wrong, then an empty bag is returned.
     * 
     * @param attributeType
     *            the datatype of the attributes to find, which must be time, date, or dateTime for
     *            this module to resolve a value
     * @param attributeId
     *            the identifier of the attributes to find, which must be one of the three
     *            ENVIRONMENT_* fields for this module to resolve a value
     * @param issuer
     *            the issuer of the attributes, or null if unspecified
     * @param subjectCategory
     *            the category of the attribute or null, which ignored since this only handles
     *            non-subjects
     * @param context
     *            the representation of the request data
     * @param designatorType
     *            the type of designator, which must be ENVIRONMENT_TARGET for this module to
     *            resolve a value
     * 
     * @return the result of attribute retrieval, which will be a bag with a single attribute, an
     *         empty bag, or an error
     */
    public EvaluationResult findAttribute(URI attributeType, URI attributeId, URI issuer,
            URI subjectCategory, EvaluationCtx context, int designatorType) {
        // we only know about environment attributes
        if (designatorType != AttributeDesignator.ENVIRONMENT_TARGET)
            return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();

        if (attrName.equals(ENVIRONMENT_CURRENT_TIME)) {
            return handleTime(attributeType, issuer, context);
        } else if (attrName.equals(ENVIRONMENT_CURRENT_DATE)) {
            return handleDate(attributeType, issuer, context);
        } else if (attrName.equals(ENVIRONMENT_CURRENT_DATETIME)) {
            return handleDateTime(attributeType, issuer, context);
        }

        // if we got here, then it's an attribute that we don't know
        return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }

    /**
     * Handles requests for the current Time.
     */
    private EvaluationResult handleTime(URI type, URI issuer, EvaluationCtx context) {
        // make sure they're asking for a time attribute
        if (!type.toString().equals(TimeAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));

        // get the value from the context
        return makeBag(context.getCurrentTime());
    }

    /**
     * Handles requests for the current Date.
     */
    private EvaluationResult handleDate(URI type, URI issuer, EvaluationCtx context) {
        // make sure they're asking for a date attribute
        if (!type.toString().equals(DateAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));

        // get the value from the context
        return makeBag(context.getCurrentDate());
    }

    /**
     * Handles requests for the current DateTime.
     */
    private EvaluationResult handleDateTime(URI type, URI issuer, EvaluationCtx context) {
        // make sure they're asking for a dateTime attribute
        if (!type.toString().equals(DateTimeAttribute.identifier))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));

        // get the value from the context
        return makeBag(context.getCurrentDateTime());
    }

    /**
     * Private helper that generates a new processing error status and includes the given string.
     */
    // private EvaluationResult makeProcessingError(String message) {
    // ArrayList<String> code = new ArrayList<String>();
    // code.add(Status.STATUS_PROCESSING_ERROR);
    // return new EvaluationResult(new Status(code, message));
    // }
    /**
     * Private helper that makes a bag containing only the given attribute.
     */
    private EvaluationResult makeBag(AttributeValue attribute) {
        Set<AttributeValue> set = new HashSet<AttributeValue>();
        set.add(attribute);

        BagAttribute bag = new BagAttribute(attribute.getType(), set);

        return new EvaluationResult(bag);
    }

}
