/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

public class LimitedAreaRequestConstraints implements Serializable {
    private static final long serialVersionUID = 1L;

    private AreaConstraint areaConstraint;
    private List<String> layerNames;
    private String crs;
    private OwsRange imageWidth;
    private OwsRange imageHeight;
    private List<String> outputFormat;

    public LimitedAreaRequestConstraints() {}

    public AreaConstraint getAreaConstraint() {
        return areaConstraint;
    }

    public void setAreaConstraint(AreaConstraint areaConstraint) {
        this.areaConstraint = areaConstraint;
    }

    public List<String> getLayerNames() {
        return layerNames;
    }

    public void setLayerNames(List<String> layerNames) {
        this.layerNames = layerNames;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public OwsRange getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(OwsRange imageWidth) {
        this.imageWidth = imageWidth;
    }

    public OwsRange getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(OwsRange imageHeight) {
        this.imageHeight = imageHeight;
    }

    public List<String> getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(List<String> outputFormat) {
        this.outputFormat = outputFormat;
    }
}
