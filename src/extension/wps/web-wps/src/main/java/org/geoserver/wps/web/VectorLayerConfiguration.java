/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.List;

/**
 * The GUI configuration for a vector layer. At the moment just the layer name is provided, but
 * there are plans to add filtering and attribute selection as well
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class VectorLayerConfiguration implements Serializable {
    String layerName;

    List<String> attributes;

    String filter;

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
