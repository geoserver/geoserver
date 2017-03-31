package org.geoserver.wfs.xslt.rest;

import org.geoserver.wfs.xslt.config.TransformInfo;

import java.util.List;

public final class TransformationsList {

    private final List<TransformInfo> transformations;

    public TransformationsList(List<TransformInfo> transformations) {
        this.transformations = transformations;
    }

    public List<TransformInfo> getTransformations() {
        return transformations;
    }
}
