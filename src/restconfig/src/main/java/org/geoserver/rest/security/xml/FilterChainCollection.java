/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("filterChain")
public class FilterChainCollection {
    @XStreamImplicit(itemFieldName = "filters")
    private List<FilterChainDTO> chains = new ArrayList<>();

    public List<FilterChainDTO> getChains() {
        return chains;
    }

    public void setChains(List<FilterChainDTO> chains) {
        this.chains = chains;
    }
}
