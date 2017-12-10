/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
