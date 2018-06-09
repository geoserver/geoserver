/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
