/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

/**
 * System filter chain which cannot be modified
 *
 * @author christian
 */
public class ConstantFilterChain extends RequestFilterChain {

    /** */
    private static final long serialVersionUID = 1L;

    public ConstantFilterChain(String... patterns) {
        super(patterns);
    }

    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean canBeRemoved() {
        return false;
    }
}
