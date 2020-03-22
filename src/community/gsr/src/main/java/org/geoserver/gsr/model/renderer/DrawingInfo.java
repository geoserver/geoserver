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
