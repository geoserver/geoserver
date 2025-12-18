/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import org.geoserver.security.RequestFilterChain;

/** Dummy RequestFilterChain to test allowed AuthFilterChain creation. */
public class DummyRequestFilterChain extends RequestFilterChain {

    public DummyRequestFilterChain(String... patterns) {
        super(patterns);
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean canBeRemoved() {
        return false;
    }
}
