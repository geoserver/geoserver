/*
 * @(#)ResourceFinder.java
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

package com.sun.xacml.finder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;

/**
 * This class is used by the PDP to handle resource scopes other than Immediate. In the case of a
 * scope of Children or Descendants, the PDP needs a list of Resource Ids to evaluate, each of which
 * will get its own Result. Like the PolicyFinder, this is not tied in any way to the rest of the
 * PDP code, and could be provided as a stand-alone resource.
 * <p>
 * This class basically is a coordinator that asks each module in turn if it can handle the given
 * identifier. Evaluation proceeds in order through the given modules, and once a module returns a
 * non-empty response (whether or not it contains any errors or only errors), the evaluation is
 * finished and the result is returned. One of the issues here is ordering, since a given resource
 * may look to several modules like something that they can handle. So, you must be careful when
 * assigning to ordering of the modules in this finder.
 * <p>
 * Note that in release 1.2 the interfaces were updated to include the evaluation context. In the
 * next major release the interfaces without the context information will be removed, but for now
 * both exist. This means that if this finder is called with the context, then only the methods in
 * <code>ResourceFinderModule</code> supporting the context will be called (and likewise only the
 * methods without context will be called when this finder is called without the context). In
 * practice this means that the methods with context will always get invoked, since this is what the
 * default PDP implementation calls.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ResourceFinder {

    // the list of all modules
    private List<ResourceFinderModule> allModules;

    // the list of child modules
    private List<ResourceFinderModule> childModules;

    // the list of descendant modules
    private List<ResourceFinderModule> descendantModules;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(ResourceFinder.class.getName());

    /**
     * Default constructor.
     */
    public ResourceFinder() {
        allModules = new ArrayList<ResourceFinderModule>();
        childModules = new ArrayList<ResourceFinderModule>();
        descendantModules = new ArrayList<ResourceFinderModule>();
    }

    /**
     * Returns the ordered <code>List</code> of <code>ResourceFinderModule</code>s used by this
     * class to find resources.
     * 
     * @return a <code>List</code> of <code>ResourceFinderModule</code>s
     */
    public List<ResourceFinderModule> getModules() {
        return new ArrayList<ResourceFinderModule>(allModules);
    }

    /**
     * Sets the ordered <code>List</code> of <code>ResourceFinderModule</code>s used by this class
     * to find resources. The ordering will be maintained.
     * 
     * @param modules
     *            a code>List</code> of <code>ResourceFinderModule</code>s
     */
    public void setModules(List<ResourceFinderModule> modules) {

        allModules = new ArrayList<ResourceFinderModule>(modules);
        childModules = new ArrayList<ResourceFinderModule>();
        descendantModules = new ArrayList<ResourceFinderModule>();

        for (ResourceFinderModule module : modules) {

            if (module.isChildSupported())
                childModules.add(module);

            if (module.isDescendantSupported())
                descendantModules.add(module);
        }
    }

    /**
     * Finds Resource Ids using the Children scope, and returns all resolved identifiers as well as
     * any errors that occurred. If no modules can handle the given Resource Id, then an empty
     * result is returned.
     * 
     * @param parentResourceId
     *            the root of the resources
     * @param context
     *            the representation of the request data
     * 
     * @return the result of looking for child resources
     */
    public ResourceFinderResult findChildResources(AttributeValue parentResourceId,
            EvaluationCtx context) {
        for (ResourceFinderModule module : childModules) {

            // ask the module to find the resources
            ResourceFinderResult result = module.findChildResources(parentResourceId, context);

            // if we found something, then always return that result
            if (!result.isEmpty())
                return result;
        }

        // no modules applied, so we return an empty result
        if (logger.isLoggable(Level.INFO))
            logger.info("No ResourceFinderModule existed to handle the " + "children of "
                    + parentResourceId.encode());

        return new ResourceFinderResult();
    }

    /**
     * Finds Resource Ids using the Children scope, and returns all resolved identifiers as well as
     * any errors that occurred. If no modules can handle the given Resource Id, then an empty
     * result is returned.
     * 
     * @deprecated As of version 1.2, replaced by
     *             {@link #findChildResources(AttributeValue,EvaluationCtx)}. This version does not
     *             provide the evaluation context to the modules, and will be removed in a future
     *             release.
     * 
     * @param parentResourceId
     *            the root of the resources
     * 
     * @return the result of looking for child resources
     */
    public ResourceFinderResult findChildResources(AttributeValue parentResourceId) {

        for (ResourceFinderModule module : childModules) {
            // ask the module to find the resources
            ResourceFinderResult result = module.findChildResources(parentResourceId);

            // if we found something, then always return that result
            if (!result.isEmpty())
                return result;
        }

        // no modules applied, so we return an empty result
        if (logger.isLoggable(Level.INFO))
            logger.info("No ResourceFinderModule existed to handle the " + "children of "
                    + parentResourceId.encode());

        return new ResourceFinderResult();
    }

    /**
     * Finds Resource Ids using the Descendants scope, and returns all resolved identifiers as well
     * as any errors that occurred. If no modules can handle the given Resource Id, then an empty
     * result is returned.
     * 
     * @param parentResourceId
     *            the root of the resources
     * @param context
     *            the representation of the request data
     * 
     * @return the result of looking for descendant resources
     */
    public ResourceFinderResult findDescendantResources(AttributeValue parentResourceId,
            EvaluationCtx context) {

        for (ResourceFinderModule module : descendantModules) {

            // ask the module to find the resources
            ResourceFinderResult result = module.findDescendantResources(parentResourceId, context);

            // if we found something, then always return that result
            if (!result.isEmpty())
                return result;
        }

        // no modules applied, so we return an empty result
        if (logger.isLoggable(Level.INFO))
            logger.info("No ResourceFinderModule existed to handle the " + "descendants of "
                    + parentResourceId.encode());

        return new ResourceFinderResult();
    }

    /**
     * Finds Resource Ids using the Descendants scope, and returns all resolved identifiers as well
     * as any errors that occurred. If no modules can handle the given Resource Id, then an empty
     * result is returned.
     * 
     * @deprecated As of version 1.2, replaced by
     *             {@link #findDescendantResources(AttributeValue,EvaluationCtx)}. This version does
     *             not provide the evaluation context to the modules, and will be removed in a
     *             future release.
     * 
     * @param parentResourceId
     *            the root of the resources
     * 
     * @return the result of looking for child resources
     */
    public ResourceFinderResult findDescendantResources(AttributeValue parentResourceId) {
        for (ResourceFinderModule module : descendantModules) {

            // ask the module to find the resources
            ResourceFinderResult result = module.findDescendantResources(parentResourceId);

            // if we found something, then always return that result
            if (!result.isEmpty())
                return result;
        }

        // no modules applied, so we return an empty result
        if (logger.isLoggable(Level.INFO))
            logger.info("No ResourceFinderModule existed to handle the " + "descendants of "
                    + parentResourceId.encode());

        return new ResourceFinderResult();
    }

}
