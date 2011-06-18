/*
 * @(#)TestUtil.java
 *
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.Obligation;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

/**
 * Simple utility class that provides some equality methods for testing whether evaluation results
 * match.
 * 
 * @author Seth Proctor
 */
public class TestUtil {

    /**
     * Returns whether two XACML Responses are equivalent.
     * 
     * @param response1
     *            an XACML Response
     * @param response2
     *            an XACML Response
     * 
     * @return true if the responses are equivalent, false otherwise
     */
    public static boolean areEquivalent(ResponseCtx response1, ResponseCtx response2) {
        Set<Result> results1 = response1.getResults();
        Set<Result> results2 = response2.getResults();

        // make sure the set of results in each response is the same size
        if (results1.size() != results2.size())
            return false;

        // get an iterator for the first set of Responses
        Iterator<Result> it1 = results1.iterator();

        // setup a temporary Set for the second set of Responses, so we can
        // remove the matching Result at each step
        Set<Result> set2 = new HashSet<Result>(results2);

        // consider each Result in the first Response, and try to find an
        // equivalent one in the second Response
        while (it1.hasNext()) {
            Result result1 = it1.next();
            Iterator<Result> it2 = set2.iterator();
            boolean matched = false;

            // go through the second list, and see if there's a matching Result
            while (it2.hasNext() && (!matched)) {
                Result result2 = it2.next();

                // two results are equivalent if they have the same decision,
                // the same resource (or none in both cases), and if their
                // status and obligations are also equivalent
                if ((result1.getDecision() == result2.getDecision())
                        && (equals(result1.getResource(), result2.getResource()))
                        && areEquivalent(result1.getStatus(), result2.getStatus())
                        && areEquivalent(result1.getObligations(), result2.getObligations())) {
                    matched = true;
                }
            }

            // if there was a match, remove it from the list...otherwise these
            // Responses aren't equivalent
            if (matched)
                it2.remove();
            else
                return false;
        }

        // if we got here then the Responses are equivalent
        return true;
    }

    /**
     * Private helper that sees if two strings are equal, handling null cases.
     */
    private static boolean equals(String str1, String str2) {
        if (str1 == null)
            return (str2 == null);

        if (str2 == null)
            return true;

        return str1.equals(str2);
    }

    /**
     * Compares two XACML Status elements to see if they're equivalent.
     * <p>
     * NOTE: there's no clear way to support comparing status detail or messages, since they're both
     * non-standard and can contain arbitrary content. As a result, this isn't supported at present.
     * 
     * @param status1
     *            status data
     * @param status2
     *            status data
     * 
     * @return true if the two sets of status data are equivalent, false otherwise
     */
    public static boolean areEquivalent(Status status1, Status status2) {
        Iterator<String> it1 = status1.getCode().iterator();
        Iterator<String> it2 = status2.getCode().iterator();

        // check that the same codes appear in each status
        while (it1.hasNext()) {
            // if we already ran out, then they're not equal
            if (!it2.hasNext())
                return false;

            // check that the specific code is the same at each step
            if (!(it1.next()).equals(it2.next()))
                return false;
        }

        // if there's still more in the second list, then they're not equal
        if (it2.hasNext())
            return false;

        // to support detail/messages, add the code here

        return true;
    }

    /**
     * Compares two sets to see if they contain equivalent obligations
     * 
     * @param obs1
     *            a <code>Set</code> of <code>Obligation</code>s
     * @param obs2
     *            a <code>Set</code> of <code>Obligation</code>s
     * 
     * @return true if the sets are equivalent, false otherwise
     */
    public static boolean areEquivalent(Set<Obligation> obs1, Set<Obligation> obs2) {
        if (obs1.size() != obs2.size())
            return false;

        // get an iterator for the first set of Obligations
        Iterator<Obligation> it1 = obs1.iterator();

        // setup a temporary Set for the second set of Obligations, so we can
        // remove the matching the Obligation at each step
        HashSet<Obligation> set2 = new HashSet<Obligation>(obs2);

        // consider each Obligation in the first set, and try to find an
        // equivalent one in the second set
        while (it1.hasNext()) {
            Obligation o1 = it1.next();
            Iterator<Obligation> it2 = set2.iterator();
            boolean matched = false;

            // go through the second set, and see if there's a matching
            // Obligation
            while (it2.hasNext() && (!matched)) {
                Obligation o2 = (Obligation) (it2.next());

                // to be equivalent, they need to have the same identifier
                // and the same fulfillOn setting
                if ((o1.getId().equals(o2.getId())) && (o1.getFulfillOn() == o2.getFulfillOn())) {
                    // get the assignments, and make sure they match
                    List<Attribute> assignments1 = o1.getAssignments();
                    List<Attribute> assignments2 = o2.getAssignments();

                    if (assignments1.size() == assignments2.size()) {
                        Iterator<Attribute> ait1 = assignments1.iterator();
                        Iterator<Attribute> ait2 = assignments2.iterator();
                        boolean assignmentsMatch = true;

                        while (ait1.hasNext() && assignmentsMatch) {
                            Attribute attr1 = ait1.next();
                            Attribute attr2 = ait2.next();

                            if ((!attr1.getId().equals(attr2.getId()))
                                    || (!attr1.getType().equals(attr2.getType()))
                                    || (!attr1.getValue().equals(attr2.getValue())))
                                assignmentsMatch = false;
                        }

                        matched = assignmentsMatch;
                    }
                }
            }

            // if there was a match, remove it from the set...otherwise these
            // Obligations aren't equivalent
            if (matched)
                it2.remove();
            else
                return false;
        }

        // if we got here then the Obligations are equivalent
        return true;
    }

}
