package org.geoserver.bxml.wfs_1_1;

import java.util.ArrayList;
import java.util.List;

public class SimpleFeatureAttributes {

    private List<Object> attributes = new ArrayList<Object>();

    public List<Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Object> attributes) {
        this.attributes = attributes;
    }

}
