package org.geoserver.wfs.xslt.rest;

import java.util.List;
import org.geoserver.wfs.xslt.config.TransformInfo;

public final class TransformationsList {

    private final List<TransformInfo> transformations;

    public TransformationsList(List<TransformInfo> transformations) {
        this.transformations = transformations;
    }

    public List<TransformInfo> getTransformations() {
        return transformations;
    }
}
