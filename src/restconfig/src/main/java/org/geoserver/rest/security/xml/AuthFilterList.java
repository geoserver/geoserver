package org.geoserver.rest.security.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

public class AuthFilterList {
    private List<AuthFilter> filters = new ArrayList<>();

    public AuthFilterList() {}

    public AuthFilterList(List<AuthFilter> jaxbAuthFilters) {
        this.filters = jaxbAuthFilters;
    }

    public List<AuthFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<AuthFilter> filters) {
        this.filters = filters;
    }
}
