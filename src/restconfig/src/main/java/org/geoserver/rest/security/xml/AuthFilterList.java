/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest.security.xml;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "authFilters")
@JsonRootName(value = "authFilters")
public class AuthFilterList {
    private List<AuthFilter> filters = new ArrayList<>();

    public AuthFilterList() {}

    public AuthFilterList(List<AuthFilter> jaxbAuthFilters) {
        this.filters = jaxbAuthFilters;
    }

    @XmlElement(name = "authFilter")
    public List<AuthFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<AuthFilter> filters) {
        this.filters = filters;
    }
}
