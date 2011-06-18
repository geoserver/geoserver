/*
 * @(#)StandardCombiningAlgFactory.java
 *
 * Copyright 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.combine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;

/**
 * This factory supports the standard set of algorithms specified in XACML 1.x and 2.0. It is the
 * default factory used by the system, and imposes a singleton pattern insuring that there is only
 * ever one instance of this class.
 * <p>
 * Note that because this supports only the standard algorithms, this factory does not allow the
 * addition of any other algorithms. If you call <code>addAlgorithm</code> on an instance of this
 * class, an exception will be thrown. If you need a standard factory that is modifiable, you should
 * create a new <code>BaseCombiningAlgFactory</code> (or some other <code>CombiningAlgFactory</code>
 * ) and configure it with the standard algorithms using <code>getStandardAlgorithms</code> (or, in
 * the case of <code>BaseAttributeFactory</code>, by providing the datatypes in the constructor).
 * 
 * @since 1.2
 * @author Seth Proctor
 */
public class StandardCombiningAlgFactory extends BaseCombiningAlgFactory {

    // the single factory instance
    private static StandardCombiningAlgFactory factoryInstance = null;

    // the algorithms supported by this factory
    private static Set<CombiningAlgorithm> supportedAlgorithms = null;

    // identifiers for the supported algorithms
    private static Set<String> supportedAlgIds;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(StandardCombiningAlgFactory.class
            .getName());

    /**
     * Default constructor.
     */
    private StandardCombiningAlgFactory() {
        super(supportedAlgorithms);
    }

    /**
     * Private initializer for the supported algorithms. This isn't called until something needs
     * these values, and is only called once.
     */
    private static void initAlgorithms() {
        logger.config("Initializing standard combining algorithms");

        supportedAlgorithms = new HashSet<CombiningAlgorithm>();
        supportedAlgIds = new HashSet<String>();

        supportedAlgorithms.add(new DenyOverridesRuleAlg());
        supportedAlgIds.add(DenyOverridesRuleAlg.algId);
        supportedAlgorithms.add(new DenyOverridesPolicyAlg());
        supportedAlgIds.add(DenyOverridesPolicyAlg.algId);

        supportedAlgorithms.add(new OrderedDenyOverridesRuleAlg());
        supportedAlgIds.add(OrderedDenyOverridesRuleAlg.algId);
        supportedAlgorithms.add(new OrderedDenyOverridesPolicyAlg());
        supportedAlgIds.add(OrderedDenyOverridesPolicyAlg.algId);

        supportedAlgorithms.add(new PermitOverridesRuleAlg());
        supportedAlgIds.add(PermitOverridesRuleAlg.algId);
        supportedAlgorithms.add(new PermitOverridesPolicyAlg());
        supportedAlgIds.add(PermitOverridesPolicyAlg.algId);

        supportedAlgorithms.add(new OrderedPermitOverridesRuleAlg());
        supportedAlgIds.add(OrderedPermitOverridesRuleAlg.algId);
        supportedAlgorithms.add(new OrderedPermitOverridesPolicyAlg());
        supportedAlgIds.add(OrderedPermitOverridesPolicyAlg.algId);

        supportedAlgorithms.add(new FirstApplicableRuleAlg());
        supportedAlgIds.add(FirstApplicableRuleAlg.algId);
        supportedAlgorithms.add(new FirstApplicablePolicyAlg());
        supportedAlgIds.add(FirstApplicablePolicyAlg.algId);

        supportedAlgorithms.add(new OnlyOneApplicablePolicyAlg());
        supportedAlgIds.add(OnlyOneApplicablePolicyAlg.algId);

        supportedAlgIds = Collections.unmodifiableSet(supportedAlgIds);
    }

    /**
     * Returns an instance of this factory. This method enforces a singleton model, meaning that
     * this always returns the same instance, creating the factory if it hasn't been requested
     * before. This is the default model used by the <code>CombiningAlgFactory</code>, ensuring
     * quick access to this factory.
     * 
     * @return the factory instance
     */
    public static StandardCombiningAlgFactory getFactory() {
        if (factoryInstance == null) {
            synchronized (StandardCombiningAlgFactory.class) {
                if (factoryInstance == null) {
                    initAlgorithms();
                    factoryInstance = new StandardCombiningAlgFactory();
                }
            }
        }

        return factoryInstance;
    }

    /**
     * A convenience method that returns a new instance of a <code>CombiningAlgFactory</code> that
     * supports all of the standard algorithms. The new factory allows adding support for new
     * algorithms. This method should only be used when you need a new, mutable instance (eg, when
     * you want to create a new factory that extends the set of supported algorithms). In general,
     * you should use <code>getFactory</code> which is more efficient and enforces a singleton
     * pattern.
     * 
     * @return a new factory supporting the standard algorithms
     */
    public static CombiningAlgFactory getNewFactory() {
        // first we make sure everything's been initialized...
        getFactory();

        // ...then we create the new instance
        return new BaseCombiningAlgFactory(supportedAlgorithms);
    }

    /**
     * Returns the identifiers supported for the given version of XACML. Because this factory
     * supports identifiers from all versions of the XACML specifications, this method is useful for
     * getting a list of which specific identifiers are supported by a given version of XACML.
     * 
     * @param xacmlVersion
     *            a standard XACML identifier string, as provided in <code>PolicyMetaData</code>
     * 
     * @return a <code>Set</code> of identifiers
     * 
     * @throws UnknownIdentifierException
     *             if the version string is unknown
     */
    public static Set<String> getStandardAlgorithms(String xacmlVersion)
            throws UnknownIdentifierException {
        if ((xacmlVersion.equals(PolicyMetaData.XACML_1_0_IDENTIFIER))
                || (xacmlVersion.equals(PolicyMetaData.XACML_2_0_IDENTIFIER)))
            return supportedAlgIds;

        throw new UnknownIdentifierException("Unknown XACML version: " + xacmlVersion);
    }

    /**
     * Throws an <code>UnsupportedOperationException</code> since you are not allowed to modify what
     * a standard factory supports.
     * 
     * @param alg
     *            the combining algorithm to add
     * 
     * @throws UnsupportedOperationException
     *             always
     */
    public void addAlgorithm(CombiningAlgorithm alg) {
        throw new UnsupportedOperationException("a standard factory cannot "
                + "support new algorithms");
    }

}
