/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("filterChainList")
public class AuthFilterChainList {
    @XStreamImplicit
    List<AuthFilterChain> filterChains = new ArrayList<>();

    public AuthFilterChainList() {}

    public AuthFilterChainList(List<AuthFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    public List<AuthFilterChain> getFilterChains() {
        return filterChains;
    }

    public void setFilterChains(List<AuthFilterChain> filterChains) {
        this.filterChains = filterChains;
    }
}
