/*
 * @(#)VersionConstraints.java
 *
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml;

import java.util.StringTokenizer;

/**
 * Supports the three version constraints that can be included with a policy reference. This class
 * also provides a simple set of comparison methods for matching against the constraints. Note that
 * this feature was introduced in XACML 2.0, which means that constraints are never used in pre-2.0
 * policy references.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class VersionConstraints {

    // internal identifiers used to specify the kind of match
    private static final int COMPARE_EQUAL = 0;

    private static final int COMPARE_LESS = 1;

    private static final int COMPARE_GREATER = 2;

    // the three constraint strings
    private String version;

    private String earliest;

    private String latest;

    /**
     * Creates a <code>VersionConstraints</code> with the three optional constraint strings. Each of
     * the three strings must conform to the VersionMatchType type defined in the XACML schema. Any
     * of the strings may be null to specify that the given constraint is not used.
     * 
     * @param version
     *            a matching constraint on the version or null
     * @param earliest
     *            a lower-bound constraint on the version or null
     * @param latest
     *            an upper-bound constraint on the version or null
     */
    public VersionConstraints(String version, String earliest, String latest) {
        this.version = version;
        this.earliest = earliest;
        this.latest = latest;
    }

    /**
     * Returns the matching constraint string, which will be null if there is no constraint on
     * matching the version.
     * 
     * @return the version constraint
     */
    public String getVersionConstraint() {
        return version;
    }

    /**
     * Returns the lower-bound constraint string, which will be null if there is no lower-bound
     * constraint on the version.
     * 
     * @return the lower-bound constraint
     */
    public String getEarliestConstraint() {
        return earliest;
    }

    /**
     * Returns the upper-bound constraint string, which will be null if there is no upper-bound
     * constraint on the version.
     * 
     * @return the upper-bound constraint
     */
    public String getLatestConstraint() {
        return latest;
    }

    /**
     * Checks if the given version string meets all three constraints.
     * 
     * @param version
     *            the version to compare, which is formatted as a VersionType XACML type
     * 
     * @return true if the given version meets all the constraints
     */
    public boolean meetsConstraint(String version) {
        return (matches(version, this.version) && isEarlier(version, latest) && isLater(version,
                earliest));
    }

    /**
     * Checks if the given version string matches the constraint string.
     * 
     * @param version
     *            the version string to check
     * @param constraint
     *            a constraint string to use in matching
     * 
     * @return true if the version string matches the constraint
     */
    public static boolean matches(String version, String constraint) {
        return compareHelper(version, constraint, COMPARE_EQUAL);
    }

    /**
     * Checks if the given version string is less-than or equal-to the constraint string.
     * 
     * @param version
     *            the version string to check
     * @param constraint
     *            a constraint string to use in matching
     * 
     * @return true if the version string is earlier than the constraint
     */
    public static boolean isEarlier(String version, String constraint) {
        return compareHelper(version, constraint, COMPARE_LESS);
    }

    /**
     * Checks if the given version string is greater-than or equal-to the constraint string.
     * 
     * @param version
     *            the version string to check
     * @param constraint
     *            a constraint string to use in matching
     * 
     * @return true if the version string is later than the constraint
     */
    public static boolean isLater(String version, String constraint) {
        return compareHelper(version, constraint, COMPARE_GREATER);
    }

    /**
     * Private helper that handles all three comparisons.
     */
    private static boolean compareHelper(String version, String constraint, int type) {
        // check that a constraint was provided...
        if (constraint == null)
            return true;

        // ...and a version too
        // FIXME: this originally returned false, but I think it should
        // return true, since we always match if the contstraint is
        // unbound (null) ... is that right?
        if (version == null)
            return true;

        // setup tokenizers
        StringTokenizer vtok = new StringTokenizer(version, ".");
        StringTokenizer ctok = new StringTokenizer(constraint, ".");

        while (vtok.hasMoreTokens()) {
            // if there's nothing left in the constraint, then this means
            // we didn't match, unless this is the greater-than function
            if (!ctok.hasMoreTokens()) {
                if (type == COMPARE_GREATER)
                    return true;
                else
                    return false;
            }

            // get the next constraint token...
            String c = ctok.nextToken();

            // ...and if it's a + then it's done and we match
            if (c.equals("+"))
                return true;
            String v = vtok.nextToken();

            // if it's a * then we always match, otherwise...
            if (!c.equals("*")) {
                // if it's a match then we just keep going, otherwise...
                if (!v.equals(c)) {
                    // if we're matching on equality, then we failed
                    if (type == COMPARE_EQUAL)
                        return false;

                    // convert both tokens to integers...
                    int cint = Integer.valueOf(c).intValue();
                    int vint = Integer.valueOf(v).intValue();

                    // ...and do the right kind of comparison
                    if (type == COMPARE_LESS)
                        return vint <= cint;
                    else
                        return vint >= cint;
                }
            }
        }

        // if we got here, then we've finished the processing the version,
        // so see if there's anything more in the constrant, which would
        // mean we didn't match unless we're doing less-than
        if (ctok.hasMoreTokens()) {
            if (type == COMPARE_LESS)
                return true;
            else
                return false;
        }

        // we got through everything, so the constraint is met
        return true;
    }

}
