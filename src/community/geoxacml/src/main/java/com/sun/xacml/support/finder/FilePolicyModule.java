/*
 * @(#)FilePolicyModule.java
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

package com.sun.xacml.support.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ParsingException;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This module represents a collection of files containing polices, each of which will be searched
 * through when trying to find a policy that is applicable to a specific request. It does not
 * support policy references.
 * <p>
 * Note that this class used to be provided in the <code>com.sun.xacml.finder.impl</code> package
 * with a warning that it would move out of the core packages eventually. This is partly because
 * this class doesn't represent standard functionality, and partly because it isn't designed to be
 * generally useful as anything more than an example. Because so many people have used this class,
 * however, it stayed in place until the 2.0 release.
 * <p>
 * As of the 2.0 release, you may still use this class (in its new location), but you are encouraged
 * to migrate to the new support modules that are much richer and designed for general-purpose use.
 * Also, note that the <code>loadPolicy</code> methods that used to be available from this class
 * have been removed. That functionality has been replaced by the much more useful
 * <code>PolicyReader</code> class. If you need to load policies directly, you should consider that
 * new class.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class FilePolicyModule extends PolicyFinderModule {

    // the schema file we're using, if any
    private File schemaFile = null;

    // the filenames for the files we'll load
    private Set<String> fileNames;

    // the actual loaded policies
    private PolicyCollection policies;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(FilePolicyModule.class.getName());

    /**
     * Constructor which retrieves the schema file to validate policies against from the
     * <code>PolicyReader.POLICY_SCHEMA_PROPERTY</code>. If the retrieved property is null, then no
     * schema validation will occur.
     */
    public FilePolicyModule() {
        fileNames = new HashSet<String>();
        policies = new PolicyCollection();

        String schemaName = System.getProperty(PolicyReader.POLICY_SCHEMA_PROPERTY);

        if (schemaName != null)
            schemaFile = new File(schemaName);
    }

    /**
     * Constructor that uses the specified <code>File</code> as the schema file for XML validation.
     * If schema validation is not desired, a null value should be used.
     * 
     * @param schemaFile
     *            the schema file to validate policies against, or null if schema validation is not
     *            desired.
     */
    public FilePolicyModule(File schemaFile) {
        fileNames = new HashSet<String>();
        policies = new PolicyCollection();

        this.schemaFile = schemaFile;
    }

    /**
     * Constructor that uses the specified <code>String</code> as the schema file for XML
     * validation. If schema validation is not desired, a null value should be used.
     * 
     * @param schemaFile
     *            the schema file to validate policies against, or null if schema validation is not
     *            desired.
     */
    public FilePolicyModule(String schemaFile) {
        this((schemaFile != null) ? new File(schemaFile) : null);
    }

    /**
     * Constructor that specifies a set of initial policy files to use. This retrieves the schema
     * file to validate policies against from the <code>PolicyReader.POLICY_SCHEMA_PROPERTY</code>.
     * If the retrieved property is null, then no schema validation will occur.
     * 
     * @param fileNames
     *            a <code>List</code> of <code>String</code>s that identify policy files
     */
    public FilePolicyModule(List<String> fileNames) {
        this();

        if (fileNames != null)
            this.fileNames.addAll(fileNames);
    }

    /**
     * Constructor that specifies a set of initial policy files to use and the schema file used to
     * validate the policies. If schema validation is not desired, a null value should be used.
     * 
     * @param fileNames
     *            a <code>List</code> of <code>String</code>s that identify policy files
     * @param schemaFile
     *            the schema file to validate policies against, or null if schema validation is not
     *            desired.
     */
    public FilePolicyModule(List<String> fileNames, String schemaFile) {
        this(schemaFile);

        if (fileNames != null)
            this.fileNames.addAll(fileNames);
    }

    /**
     * Adds a file (containing a policy) to the collection of filenames associated with this module.
     * Note that this doesn't actually load the policy file. Policies aren't loaded from their files
     * until the module is initialized through the <code>init</code> method (which is called
     * automatically by the <code>PolicyFinder</code> when the system comes up).
     * 
     * @param filename
     *            the file to add to this module's collection of files
     */
    public boolean addPolicy(String filename) {
        return fileNames.add(filename);
    }

    /**
     * Indicates whether this module supports finding policies based on a request (target matching).
     * Since this module does support finding policies based on requests, it returns true.
     * 
     * @return true, since finding policies based on requests is supported
     */
    public boolean isRequestSupported() {
        return true;
    }

    /**
     * Initializes the <code>FilePolicyModule</code> by loading the policies contained in the
     * collection of files associated with this module. This method also uses the specified
     * <code>PolicyFinder</code> to help in instantiating PolicySets.
     * 
     * @param finder
     *            a PolicyFinder used to help in instantiating PolicySets
     */
    public void init(PolicyFinder finder) {
        PolicyReader reader = new PolicyReader(finder, logger, schemaFile);

        for (String fname : fileNames) {
            try {
                AbstractPolicy policy = reader.readPolicy(new FileInputStream(fname));
                policies.addPolicy(policy);
            } catch (FileNotFoundException fnfe) {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "File couldn't be read: " + fname, fnfe);
            } catch (ParsingException pe) {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Error reading policy from file " + fname, pe);
            }
        }
    }

    /**
     * Finds a policy based on a request's context. If more than one applicable policy is found,
     * this will return an error. Note that this is basically just a subset of the OnlyOneApplicable
     * Policy Combining Alg that skips the evaluation step. See comments in there for details on
     * this algorithm.
     * 
     * @param context
     *            the representation of the request data
     * 
     * @return the result of trying to find an applicable policy
     */
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        try {
            AbstractPolicy policy = policies.getPolicy(context);
            if (policy == null)
                return new PolicyFinderResult();
            else
                return new PolicyFinderResult(policy);
        } catch (TopLevelPolicyException tlpe) {
            return new PolicyFinderResult(tlpe.getStatus());
        }
    }

}
