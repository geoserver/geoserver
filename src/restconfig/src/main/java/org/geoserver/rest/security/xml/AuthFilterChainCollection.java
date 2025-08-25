/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("filterchain")
public class AuthFilterChainCollection {
    @XStreamImplicit(itemFieldName = "filters")
    private List<AuthFilterChainFilters> chains = new ArrayList<>();

    public List<AuthFilterChainFilters> getChains() {
        return chains;
    }

    public void setChains(List<AuthFilterChainFilters> chains) {
        this.chains = chains;
    }
}
