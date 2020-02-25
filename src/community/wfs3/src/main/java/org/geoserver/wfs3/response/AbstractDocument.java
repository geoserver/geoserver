/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *
 */

package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AbstractDocument {
    protected final List<Link> links = new ArrayList<>();

    public void addLink(Link link) {
        links.add(link);
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
    }

    public String getLinkUrl(String classification, String type) {
        return links.stream()
                .filter(l -> Objects.equals(classification, l.getClassification()))
                .filter(l -> type.equals(l.getType()))
                .map(l -> l.getHref())
                .findFirst()
                .orElse(null);
    }

    public List<Link> getLinksExcept(String classification, String excludedType) {
        return links.stream()
                .filter(
                        l ->
                                classification == null
                                        || Objects.equals(classification, l.getClassification()))
                .filter(l -> excludedType == null || !excludedType.equals(l.getType()))
                .collect(Collectors.toList());
    }
}
