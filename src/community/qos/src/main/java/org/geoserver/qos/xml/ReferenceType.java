/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReferenceType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String href;
    private String title;
    private String format;
    private List<OwsAbstract> abstracts;

    public ReferenceType() {}

    public ReferenceType(String href, String title) {
        super();
        this.href = href;
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<OwsAbstract> getAbstracts() {
        return abstracts;
    }

    public void setAbstracts(List<OwsAbstract> abstracts) {
        this.abstracts = abstracts;
    }

    public String getAbstractOne() {
        if (abstracts == null || abstracts.isEmpty()) return null;
        return abstracts.get(0).getValue();
    }

    public void setAbstractOne(String abstractOne) {
        abstracts = new ArrayList<>();
        abstracts.add(new OwsAbstract(abstractOne));
    }

    @Override
    public String toString() {
        return "ReferenceType {href=" + href + ", title=" + title + "}";
    }
}
