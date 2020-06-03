/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.renderer;

import java.util.List;
import org.geoserver.gsr.model.label.Label;

/** Wraper class for renderer */
public class DrawingInfo {
    public final Renderer renderer;
    // transparency - not supported
    public List<Label> labelingInfo;

    public DrawingInfo(Renderer renderer) {
        this.renderer = renderer;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public List<Label> getLabelingInfo() {
        return labelingInfo;
    }

    public void setLabelingInfo(List<Label> labelingInfo) {
        this.labelingInfo = labelingInfo;
    }
}
